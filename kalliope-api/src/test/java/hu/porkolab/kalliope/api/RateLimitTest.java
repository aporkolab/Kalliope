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

/** A kérésszám-korlát valódi HTTP-n. */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "kalliope.rate-limit.requests-per-minute=3")
class RateLimitTest {

    @LocalServerPort
    private int port;

    private final HttpClient client = HttpClient.newHttpClient();

    private int analyze() throws Exception {
        return client.send(
                        HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/analyze"))
                                .header("Content-Type", "application/json")
                                .POST(HttpRequest.BodyPublishers.ofString(
                                        "{\"text\":\"vers\"}", StandardCharsets.UTF_8))
                                .build(),
                        HttpResponse.BodyHandlers.ofString())
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
