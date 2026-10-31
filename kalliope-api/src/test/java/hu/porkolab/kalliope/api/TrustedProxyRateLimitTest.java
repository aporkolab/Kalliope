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

/**
 * Megbízható proxy (alapbeállítás: a 127.0.0.1 is az) mögött a korlát a
 * valódi klienst számolja, nem a proxyt — különben egy reverse proxy mögött
 * minden látogató egy közös percenkénti kereten osztozna.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "kalliope.rate-limit.requests-per-minute=2")
class TrustedProxyRateLimitTest {

    @LocalServerPort
    private int port;

    private final HttpClient client = HttpClient.newHttpClient();

    private int analyzeAs(String forwardedFor) throws Exception {
        return client.send(
                        HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/analyze"))
                                .header("Content-Type", "application/json")
                                .header("X-Forwarded-For", forwardedFor)
                                .POST(HttpRequest.BodyPublishers.ofString(
                                        "{\"text\":\"vers\"}", StandardCharsets.UTF_8))
                                .build(),
                        HttpResponse.BodyHandlers.ofString())
                .statusCode();
    }

    @Test
    @DisplayName("megbízható proxy mögött kliensenként számol")
    void countsPerForwardedClient() throws Exception {
        assertThat(analyzeAs("203.0.113.1")).isEqualTo(200);
        assertThat(analyzeAs("203.0.113.1")).isEqualTo(200);
        assertThat(analyzeAs("203.0.113.1"))
                .as("az első kliens harmadik kérése")
                .isEqualTo(429);
        assertThat(analyzeAs("203.0.113.2")).as("másik kliens, saját kerettel").isEqualTo(200);
    }

    @Test
    @DisplayName("a kliens által elé írt hamis cím nem számít: a lánc jobbról olvasódik")
    void prependedAddressIsIgnored() throws Exception {
        assertThat(analyzeAs("198.51.100.7")).isEqualTo(200);
        assertThat(analyzeAs("198.51.100.7")).isEqualTo(200);
        assertThat(analyzeAs("192.0.2.99, 198.51.100.7")).isEqualTo(429);
    }
}
