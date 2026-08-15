package hu.porkolab.kalliope;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ScansionTest {

    private static final Settings STRICT = MetricCanon.DEFAULT_SETTINGS.with(Map.of(
            Settings.WORD_FINAL_CONSONANT_ANCEPS, false,
            Settings.SHORT_WORDS_ANCEPS, false,
            Settings.ALLOW_SYNIZESIS, false));

    private static String scan(String line) {
        return Scansion.scan(line, STRICT).pattern();
    }

    @Test
    @DisplayName("természetes hosszúság: a hosszú magánhangzó hosszú szótagot ad")
    void naturalLength() {
        assertThat(scan("háború")).startsWith("-");
        assertThat(Scansion.scan("háború", STRICT).syllables().get(0).reason()).isEqualTo(Scansion.Reason.NATURAL_LONG);
    }

    @Test
    @DisplayName("helyzeti hosszúság: rövid magánhangzó után két mássalhangzó")
    void positionalLength() {
        assertThat(scan("kertben")).startsWith("-");
        assertThat(Scansion.scan("kertben", STRICT).syllables().get(0).reason())
                .isEqualTo(Scansion.Reason.POSITION_LONG);
    }

    @Test
    @DisplayName("regresszió: a kétjegyű betű NEM zárhang és NEM likvida — nincs hamis muta cum liquida")
    void digraphsAreNotStopsOrLiquids() {
        // A korábbi változat a digráfot az első betűjére csonkolta, így a 'gy'
        // zárhangnak, az 'ly' likvidának látszott: "hegyre" és "szablya" közös
        // lett hosszú helyett.
        assertThat(scan("hegyre")).startsWith("-");
        assertThat(scan("szablyáját")).startsWith("-");
        assertThat(Scansion.scan("hegyre", STRICT).syllables().get(0).reason())
                .isEqualTo(Scansion.Reason.POSITION_LONG);

        // az igazi muta cum liquida viszont közös marad
        assertThat(scan("apraja")).startsWith("?");
        assertThat(Scansion.scan("apraja", STRICT).syllables().get(0).reason())
                .isEqualTo(Scansion.Reason.MUTA_CUM_LIQUIDA);
    }

    @Test
    @DisplayName("regresszió: az x két hangot jelöl (ksz), tehát helyzeti hosszút ad")
    void xIsTwoConsonants() {
        assertThat(scan("taxi")).startsWith("-");
        assertThat(scan("maximum")).startsWith("-");
    }

    @Test
    @DisplayName("regresszió: a dz és a dzs kettőzés nélkül is hosszú mássalhangzó")
    void dzIsLong() {
        assertThat(scan("bodza")).startsWith("-");
        assertThat(scan("madzag")).startsWith("-");
        assertThat(scan("maharadzsa")).startsWith("UU-");
    }

    @Test
    @DisplayName("a kettőzött kétjegyű betű egy hosszú mássalhangzó (AkH. 7. § b)")
    void doubledDigraphs() {
        assertThat(scan("asszony")).startsWith("-");
        assertThat(scan("meggy")).startsWith("?"); // egyetlen szótag: sorvégi közös
        assertThat(scan("könnyű")).startsWith("-");
    }

    @Test
    @DisplayName("a szótag átlépi a szóhatárt")
    void syllableCrossesWordBoundary() {
        assertThat(scan("vak róka")).startsWith("--");
        assertThat(scan("has hashoz")).startsWith("-");
    }

    @Test
    @DisplayName("a sorvégi szótag közös, a névelő közös")
    void ancepsPositions() {
        Scansion.Reading r = Scansion.scan("a nagy ház", STRICT);
        assertThat(r.pattern()).isEqualTo("?-?");
        assertThat(r.syllables().get(0).reason()).isEqualTo(Scansion.Reason.ARTICLE);
        assertThat(r.syllables().get(2).reason()).isEqualTo(Scansion.Reason.LINE_END);
    }

    @Test
    @DisplayName("az y idegen szóban magánhangzó, de a kétjegyű betűben nem")
    void foreignY() {
        assertThat(Scansion.scan("Zephyr", STRICT).syllableCount()).isEqualTo(2);
        assertThat(Scansion.scan("hegy", STRICT).syllableCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("a görög kettőshangzó változatot ad, nem dönt helyettünk")
    void synizesisProducesVariants() {
        Settings withDiphthongs = STRICT.with(Map.of(Settings.ALLOW_SYNIZESIS, true));
        var readings = Scansion.readings("Európa", withDiphthongs);
        assertThat(readings).hasSizeGreaterThan(1);
        assertThat(readings.get(0).syllableCount()).isEqualTo(4);
        assertThat(readings.get(1).syllableCount()).isEqualTo(3);
        assertThat(readings.get(1).synizesis()).isTrue();
    }

    @Test
    @DisplayName("a szótagok szövege kiadja az eredeti szót")
    void syllableTextsReconstructTheWord() {
        var syllables = Scansion.scan("szablyáját", STRICT).syllables();
        StringBuilder sb = new StringBuilder();
        syllables.forEach(s -> sb.append(s.text()));
        assertThat(sb.toString()).isEqualTo("szablyáját");
    }

    @Test
    @DisplayName("üres és értelmetlen bemenet nem dob kivételt")
    void emptyInputIsSafe() {
        assertThat(scan("")).isEmpty();
        assertThat(scan("   ")).isEmpty();
        assertThat(scan("!!! ??? ...")).isEmpty();
        assertThat(scan("漢字 Привет")).isEmpty();
        assertThat(Scansion.scan(null, STRICT).pattern()).isEmpty();
    }
}
