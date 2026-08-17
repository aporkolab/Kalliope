package hu.porkolab.kalliope;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * A kézi JSON-kiírás a Jackson rekord-leképezésével azonos alakot ad.
 *
 * <p>A motor a böngészőben is fut ({@code kalliope-js}, TeaVM-mel fordítva),
 * ahol nincs reflexió, tehát Jackson sem — ezért van kézi szerializáló. Ez a
 * teszt azt őrzi, hogy a kézi kiírás pontosan azt adja, amit a reflexió adna:
 * ha valaki felvesz egy rekordkomponenst és a {@link Json}-t elfelejti, ez
 * bukik el.
 *
 * <p>A Jackson itt csak teszt hatókörű; a motornak futásidőben nulla
 * függősége van. A HTTP-szerződést külön a {@code JsonEquivalenceTest} méri az
 * API modulban, a böngészős kimenetet pedig a {@code js-diff.mjs}.
 */
class JsonTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    @DisplayName("az elemzés alakja azonos a reflexiós leképezéssel — a teljes korpuszon")
    void analysisMatchesReflection() {
        for (Examples e : Examples.ALL) {
            Analysis a = Analyzer.analyze(e.text());
            assertThat(mapper.readTree(Json.of(a))).as("elemzés: %s", e.title()).isEqualTo(mapper.valueToTree(a));
        }
    }

    @Test
    @DisplayName("a kánon, a példatár és minden egyes mérték alakja is azonos")
    void canonMatchesReflection() {
        List<String> words = new ArrayList<>(MetricCanon.UNSTRESSED_WORDS);
        Collections.sort(words);
        JsonNode canon = mapper.readTree(
                Json.canon(MetricCanon.ALL_METERS, MetricCanon.STANZAS, MetricCanon.DEFAULT_SETTINGS.asMap(), words));
        assertThat(canon.get("meters")).isEqualTo(mapper.valueToTree(MetricCanon.ALL_METERS));
        assertThat(canon.get("unstressedWords")).isEqualTo(mapper.valueToTree(words));
        assertThat(canon.get("originVersion").asString()).isEqualTo(MetricCanon.ORIGIN_VERSION);
        assertThat(canon.get("settings"))
                .hasSize(MetricCanon.DEFAULT_SETTINGS.asMap().size());
        assertThat(canon.get("reasons")).hasSize(Scansion.Reason.values().length);

        assertThat(mapper.readTree(Json.examples(Examples.ALL))).isEqualTo(mapper.valueToTree(Examples.ALL));
        for (Meter m : MetricCanon.ALL_METERS) {
            assertThat(mapper.readTree(Json.of(m))).as("mérték: %s", m.id()).isEqualTo(mapper.valueToTree(m));
        }
    }

    @Test
    @DisplayName("az idézőjel és a visszaperjel escape-elve, a null pedig null marad")
    void escapingAndNulls() {
        Analysis a = Analyzer.analyze("Idézet: \"ez itt\" és egy \\ jel");
        String text = mapper.readTree(Json.of(a))
                .get("stanzas")
                .get(0)
                .get("lines")
                .get(0)
                .get("text")
                .asString();
        assertThat(text).contains("\"ez itt\"").contains("\\");

        // A találat nélküli sor realized mezője null, nem üres string — a
        // felület a kettőt megkülönbözteti.
        JsonNode line = mapper.readTree(Json.of(Analyzer.analyze("Ez nem versmérték egyáltalán")))
                .get("stanzas")
                .get(0)
                .get("lines")
                .get(0);
        assertThat(line.get("realized").isNull()).isTrue();
        assertThat(line.get("scansion").isNull()).isFalse();
    }

    @Test
    @DisplayName("a kettős ritmus benne van a JSON-ban — rekordkomponens, nem származtatott metódus")
    void dualRhythmIsSerialized() {
        // Amíg származtatott metódus volt, a szerializáló nem látta, és a
        // felület „kettős ritmus" jelzése sosem jelent meg.
        JsonNode stanza = mapper.readTree(Json.of(Analyzer.analyze(Examples.KOZELITO_TEL.text())))
                .get("stanzas")
                .get(0);
        assertThat(stanza.has("dualRhythm")).isTrue();
        assertThat(stanza.get("dualRhythm").isBoolean()).isTrue();
    }
}
