# syntax=docker/dockerfile:1.20

# ---------- 1. Angular felület ----------
FROM node:24-alpine AS web
WORKDIR /web
COPY kalliope-web/package.json kalliope-web/package-lock.json ./
RUN --mount=type=cache,target=/root/.npm \
    npm ci --no-audit --no-fund
COPY kalliope-web/ ./
RUN npm run build

# ---------- 2. Maven build ----------
# A pom-ok külön rétegben: kódmódosításkor nem tölti újra a világot.
FROM maven:3-eclipse-temurin-26-alpine AS build
WORKDIR /src
COPY pom.xml ./
COPY kalliope-core/pom.xml kalliope-core/
COPY kalliope-api/pom.xml kalliope-api/
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -ntp -q dependency:go-offline -DexcludeGroupIds=hu.porkolab.kalliope || true
COPY kalliope-core/src kalliope-core/src
COPY kalliope-api/src kalliope-api/src
COPY --from=web /web/dist/kalliope-web/browser/ kalliope-web/dist/kalliope-web/browser/
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -ntp -Dmaven.test.skip=true -Dspotless.check.skip=true -Djacoco.skip=true package && \
    cp kalliope-api/target/kalliope-api-*.jar /src/application.jar

# ---------- 3. Rétegekre bontás ----------
# Spring Boot 4.1: a -Djarmode=layertools MEGSZŰNT, a helyes hívás a tools jarmode.
FROM eclipse-temurin:25.0.3_9-jre-alpine AS extract
WORKDIR /builder
COPY --from=build /src/application.jar ./application.jar
RUN java -Djarmode=tools -jar application.jar extract --layers --destination extracted

# ---------- 4. Futtatás ----------
FROM eclipse-temurin:25.0.3_9-jre-alpine
WORKDIR /application

COPY --from=extract /builder/extracted/dependencies/ ./
COPY --from=extract /builder/extracted/spring-boot-loader/ ./
COPY --from=extract /builder/extracted/snapshot-dependencies/ ./
COPY --from=extract /builder/extracted/application/ ./

# AOT-gyorsítótár tanítófutása UGYANEZZEL a JVM-mel — enélkül a gyorsítótár
# némán érvénytelen. Indulás ~2,5 s helyett ~1 s alatt.
RUN java -XX:AOTCacheOutput=app.aot -Dspring.context.exit=onRefresh -jar application.jar || true

RUN addgroup -S -g 10001 app && adduser -S -u 10001 -G app app && chown -R app:app /application
USER 10001:10001

ENV SPRING_THREADS_VIRTUAL_ENABLED=true
EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=15s --retries=3 \
    CMD wget -q -O /dev/null http://127.0.0.1:8080/api/canon || exit 1

ENTRYPOINT ["java", \
  "-XX:AOTCache=app.aot", \
  "-XX:MaxRAMPercentage=75.0", \
  "-XX:+ExitOnOutOfMemoryError", \
  "-jar", "application.jar"]
