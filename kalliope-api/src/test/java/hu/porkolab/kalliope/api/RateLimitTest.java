package hu.porkolab.kalliope.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;

/**
 * A kérésszám-korlát valódi HTTP-n. A tesztkliens a 127.0.0.1-ről jön, ami
 * alapból megbízható proxy volna — itt kikapcsoljuk, hogy egy internetről
 * érkező, idegen kliens helyzetét mérjük.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"kalliope.rate-limit.requests-per-minute=3", "server.tomcat.remoteip.internal-proxies="})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class RateLimitTest {

    @LocalServerPort
    private int port;

    private final HttpClient client = HttpClient.newHttpClient();

    private int analyze() throws Exception {
        return analyze("/api/analyze", null);
    }

    private int analyze(String path, String forwardedFor) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"text\":\"vers\"}", StandardCharsets.UTF_8));
        if (forwardedFor != null) {
            request.header("X-Forwarded-For", forwardedFor);
        }
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString())
                .statusCode();
    }

    @Test
    @DisplayName("a korlát fölött 429-et ad, ProblemDetail alakban")
    void limitIsEnforced() throws Exception {
        assertThat(analyze()).isEqualTo(200);
        assertThat(analyze()).isEqualTo(200);
        assertThat(analyze()).isEqualTo(200);
        assertThat(analyze()).as("a negyedik kérés").isEqualTo(429);

        HttpResponse<String> tooMany = client.send(
                HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/analyze"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString("{\"text\":\"vers\"}", StandardCharsets.UTF_8))
                        .build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertThat(tooMany.headers().firstValue("Content-Type").orElse("")).contains("problem+json");
        assertThat(tooMany.body()).contains("Túl sok kérés");
    }

    @Test
    @DisplayName("idegen kliens X-Forwarded-For fejléce nem nyit új számlálót")
    void spoofedForwardedForDoesNotBypassLimit() throws Exception {
        for (int i = 1; i <= 3; i++) {
            assertThat(analyze("/api/analyze", "10.0.0." + i)).isEqualTo(200);
        }
        assertThat(analyze("/api/analyze", "10.0.0.4"))
                .as("negyedik, új hamis címmel")
                .isEqualTo(429);
    }

    @Test
    @DisplayName("path-paraméteres alak is a korlát alá esik")
    void pathParameterDoesNotBypassLimit() throws Exception {
        for (int i = 0; i < 3; i++) {
            assertThat(analyze()).isEqualTo(200);
        }
        assertThat(analyze("/api/analyze;x=1", null)).isEqualTo(429);
    }

    @Test
    @DisplayName("a korlát csak az elemzésre vonatkozik, a kánonra nem")
    void otherEndpointsAreNotLimited() throws Exception {
        for (int i = 0; i < 10; i++) {
            int status = client.send(
                            HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/canon"))
                                    .GET()
                                    .build(),
                            HttpResponse.BodyHandlers.ofString())
                    .statusCode();
            assertThat(status).isEqualTo(200);
        }
    }
}
