package hu.porkolab.kalliope.api;

import static org.assertj.core.api.Assertions.assertThat;

import hu.porkolab.kalliope.Examples;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Teljes integrációs teszt: a valódi alkalmazás indul valódi porton, és valódi
 * HTTP-n keresztül kérdezzük — nincs mockolt réteg, a Jackson-szerializáció, a
 * validáció, a hibakezelés és a felület-kiszolgálás is éles.
 *
 * <p>A JDK saját HttpClientjét használjuk, hogy a teszt semmilyen külön kliens-
 * függőségre ne épüljön.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class KalliopeIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private ObjectMapper json;

    private final HttpClient client =
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    private HttpResponse<String> get(String path) throws Exception {
        return client.send(
                HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private HttpResponse<String> post(String path, String body) throws Exception {
        return client.send(
                HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                        .build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private static String analyzeBody(String poem) {
        return "{\"text\":" + quote(poem) + "}";
    }

    private static String quote(String s) {
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\"";
    }

    @Test
    @DisplayName("a teljes korpusz végigmegy éles HTTP-n, a dokumentált eredménnyel")
    void wholeCorpusOverHttp() throws Exception {
        Map<String, String> expectedForm = Map.of(
                "a-nagy-titok", "disztichon",
                "a-magyarokhoz", "alkaioszi strófa",
                "a-kozelito-tel", "aszklepiadeszi B",
                "horac", "aszklepiadeszi B");
        Map<String, String> expectedRhyme = Map.of(
                "szigeti-veszedelem", "aaaa",
                "toldi", "aabb",
                "szeptember-vegen", "abab",
                "zalan-futasa", "xxxx");

        for (Examples poem : Examples.ALL) {
            HttpResponse<String> response = post("/api/analyze", analyzeBody(poem.text()));
            assertThat(response.statusCode()).as(poem.title()).isEqualTo(200);
            JsonNode stanza = json.readTree(response.body()).get("stanzas").get(0);

            String rhyme = expectedRhyme.get(poem.id());
            if (rhyme != null) {
                assertThat(stanza.get("rhymePattern").asString())
                        .as("%s rímképlete", poem.title())
                        .isEqualTo(rhyme);
            }
            String form = expectedForm.get(poem.id());
            if (form != null) {
                List<String> forms = stanza.get("forms")
                        .valueStream()
                        .map(f -> f.get("form").get("name").asString())
                        .toList();
                assertThat(forms).as("%s szakaszmértéke", poem.title()).contains(form);
            }
            // minden sor minden szótagja indokolt — ezt jeleníti meg a felület
            for (JsonNode line : stanza.get("lines")) {
                assertThat(line.get("syllables").size())
                        .as("%s — %s", poem.title(), line.get("text").asString())
                        .isEqualTo(line.get("scansion").asString().length());
                for (JsonNode syllable : line.get("syllables")) {
                    assertThat(syllable.get("reason").asString()).isNotBlank();
                    assertThat(syllable.get("text").asString()).isNotBlank();
                }
            }
        }
    }

    @Test
    @DisplayName("a beállítás átmegy a dróton és megváltoztatja az eredményt")
    void settingsTravelOverTheWire() throws Exception {
        String poem = quote(Examples.ILIASZ.text());
        String strict = post("/api/analyze", "{\"text\":" + poem + "}").body();
        String loose = post(
                        "/api/analyze", "{\"text\":" + poem + ",\"settings\":{\"a_szokezdo_hangsuly_nyujthat\":true}}")
                .body();

        assertThat(firstLineMeters(strict)).doesNotContain("hexameter");
        assertThat(firstLineMeters(loose)).contains("hexameter");
    }

    private List<String> firstLineMeters(String body) {
        return json.readTree(body)
                .get("stanzas")
                .get(0)
                .get("lines")
                .get(0)
                .get("meters")
                .valueStream()
                .map(m -> m.get("meter").get("name").asString())
                .toList();
    }

    @Test
    @DisplayName("többszakaszos vers szakaszonként bomlik, saját rímképlettel")
    void multiStanzaPoem() throws Exception {
        String poem = Examples.MAGYAROKHOZ.text() + "\n\n" + Examples.KOZELITO_TEL.text();
        JsonNode body = json.readTree(post("/api/analyze", analyzeBody(poem)).body());
        assertThat(body.get("stanzas").size()).isEqualTo(2);
        assertThat(body.get("summary").get("lineCount").asInt()).isEqualTo(8);
        assertThat(body.get("summary")
                        .get("stanzaForms")
                        .valueStream()
                        .map(JsonNode::asString)
                        .toList())
                .contains("alkaioszi strófa", "aszklepiadeszi B");
    }

    @Test
    @DisplayName("a kánon egyben kiszolgálja a felület minden szótárát")
    void canonServesEverythingTheUiNeeds() throws Exception {
        HttpResponse<String> response = get("/api/canon");
        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode canon = json.readTree(response.body());
        assertThat(canon.get("meters").size()).isGreaterThan(100);
        assertThat(canon.get("stanzas").size()).isEqualTo(18);
        assertThat(canon.get("settings").size()).isEqualTo(10);
        assertThat(canon.get("reasons").size()).isGreaterThan(5);
        for (JsonNode setting : canon.get("settings")) {
            assertThat(setting.get("label").asString()).isNotBlank();
        }
        // a javítások az eredeti mintával és forrással együtt jönnek
        boolean hasCorrection = canon.get("meters")
                .valueStream()
                .anyMatch(m -> m.has("correction") && !m.get("correction").isNull());
        assertThat(hasCorrection).isTrue();
    }

    @Test
    @DisplayName("a példatár szövege és a motor korpusza ugyanaz")
    void examplesAreTheCorpus() throws Exception {
        JsonNode examples = json.readTree(get("/api/examples").body());
        assertThat(examples.size()).isEqualTo(Examples.ALL.size());
        assertThat(examples.get(0).get("expected").asString()).isNotBlank();
    }

    @Test
    @DisplayName("a felület kiszolgálódik, és a kliensoldali útvonalak is oda vezetnek")
    void spaIsServed() throws Exception {
        HttpResponse<String> root = get("/");
        assertThat(root.statusCode()).isEqualTo(200);
        assertThat(root.headers().firstValue("Content-Type").orElse("")).contains("text/html");
        assertThat(root.body()).contains("app-root");

        // deep link: a kliensoldali útvonal is az index.html-t kapja
        HttpResponse<String> deepLink = get("/valami/kliens/utvonal");
        assertThat(deepLink.statusCode()).isEqualTo(200);
        assertThat(deepLink.body()).contains("app-root");
    }

    @Test
    @DisplayName("ismeretlen API-útvonal 404-et ad, NEM a felület HTML-jét")
    void unknownApiPathDoesNotFallBackToHtml() throws Exception {
        HttpResponse<String> response = get("/api/nincs-ilyen-vegpont");
        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(response.body()).doesNotContain("<html");
    }

    @Test
    @DisplayName("hibás kérés ProblemDetail alakban jön vissza")
    void errorsAreProblemDetails() throws Exception {
        HttpResponse<String> empty = post("/api/analyze", "{\"text\":\"\"}");
        assertThat(empty.statusCode()).isEqualTo(400);
        assertThat(empty.headers().firstValue("Content-Type").orElse("")).contains("application/problem+json");

        HttpResponse<String> unknown = post("/api/analyze", "{\"text\":\"vers\",\"settings\":{\"nincs_ilyen\":true}}");
        assertThat(unknown.statusCode()).isEqualTo(400);
        assertThat(json.readTree(unknown.body()).get("detail").asString()).contains("Ismeretlen beállítás");

        HttpResponse<String> meter = get("/api/canon/nincs-ilyen");
        assertThat(meter.statusCode()).isEqualTo(400);
    }

    @Test
    @DisplayName("ellenséges bemenet sem dönti meg a szervert")
    void hostileInputIsHandled() throws Exception {
        assertThat(post("/api/analyze", analyzeBody("!!! ??? ...")).statusCode())
                .isEqualTo(200);
        assertThat(post("/api/analyze", analyzeBody("漢字の詩\nПривет мир")).statusCode())
                .isEqualTo(200);
        assertThat(post("/api/analyze", analyzeBody("a ".repeat(2000))).statusCode())
                .isEqualTo(200);
        assertThat(post("/api/analyze", analyzeBody("x".repeat(50_000))).statusCode())
                .isEqualTo(400);
        assertThat(post("/api/analyze", "{").statusCode()).isEqualTo(400);
    }

    @Test
    @DisplayName("az ékezetes kimenet UTF-8-ként érkezik")
    void encodingIsUtf8() throws Exception {
        String body =
                post("/api/analyze", analyzeBody(Examples.MAGYAROKHOZ.text())).body();
        assertThat(body).contains("alkaioszi strófa");
        assertThat(body).contains("ő");
    }
}
