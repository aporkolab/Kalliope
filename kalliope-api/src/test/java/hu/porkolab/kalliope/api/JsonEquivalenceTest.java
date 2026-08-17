package hu.porkolab.kalliope.api;

import static org.assertj.core.api.Assertions.assertThat;

import hu.porkolab.kalliope.Analyzer;
import hu.porkolab.kalliope.Examples;
import hu.porkolab.kalliope.Json;
import hu.porkolab.kalliope.MetricCanon;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * A kézi JSON-kiírás egyenértékűsége a valódi HTTP-válasszal.
 *
 * <p>A motor a böngészőben is fut ({@code kalliope-js}, TeaVM-mel fordítva),
 * ahol nincs reflexió, tehát Jackson sem — ezért a szerializálás a magban van,
 * kézzel, és <b>ugyanaz</b> a kód szolgálja ki a webes változatot meg az API-t.
 *
 * <p>A hivatkozási alap szándékosan a <b>tényleges HTTP-válasz</b>, nem egy
 * kézzel példányosított {@code ObjectMapper}: a Spring web-rétege nem
 * feltétlenül ugyanazzal a mapperrel dolgozik, mint az injektált bean (a
 * {@code dualRhythm()} például származtatott metódus, nem rekordkomponens, és a
 * kettő máshogy kezeli). Ha a szerződés az, ami a hálózaton kimegy, akkor ahhoz
 * kell mérni.
 *
 * <p>Ha valaki felvesz egy rekordkomponenst és elfelejti a szerializálót, ez a
 * teszt bukik el — nem a webes felület három hét múlva.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "kalliope.rate-limit.requests-per-minute=0")
class JsonEquivalenceTest {

    @LocalServerPort
    private int port;

    @Autowired
    private ObjectMapper json;

    private final HttpClient client =
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    private JsonNode get(String path) throws Exception {
        HttpResponse<String> r = client.send(
                HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertThat(r.statusCode()).isEqualTo(200);
        return json.readTree(r.body());
    }

    private JsonNode post(String path, String body) throws Exception {
        HttpResponse<String> r = client.send(
                HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                        .build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertThat(r.statusCode()).isEqualTo(200);
        return json.readTree(r.body());
    }

    @Test
    @DisplayName("az elemzés JSON-ja mezőre azonos az API válaszával — a teljes korpuszon")
    void analysisMatchesTheApi() throws Exception {
        for (Examples e : Examples.ALL) {
            JsonNode api = post("/api/analyze", json.writeValueAsString(new Body(e.text())));
            JsonNode mine = json.readTree(Json.of(Analyzer.analyze(e.text())));
            assertThat(mine).as("elemzés: %s", e.title()).isEqualTo(api);
        }
    }

    @Test
    @DisplayName("a kánon JSON-ja mezőre azonos az API válaszával")
    void canonMatchesTheApi() throws Exception {
        JsonNode mine = json.readTree(Json.canon(
                MetricCanon.search(null),
                MetricCanon.STANZAS,
                MetricCanon.DEFAULT_SETTINGS.asMap(),
                MetricCanon.UNSTRESSED_WORDS.stream().sorted().toList()));
        assertThat(mine).isEqualTo(get("/api/canon"));
    }

    @Test
    @DisplayName("a példatár és az egyes mértékek JSON-ja is azonos")
    void examplesAndMetersMatchTheApi() throws Exception {
        assertThat(json.readTree(Json.examples(Examples.ALL))).isEqualTo(get("/api/examples"));
        for (var m : MetricCanon.ALL_METERS) {
            assertThat(json.readTree(Json.of(m))).as("mérték: %s", m.id()).isEqualTo(get("/api/canon/" + m.id()));
        }
    }

    /** Az /api/analyze kérésteste. */
    private record Body(String text) {}
}
