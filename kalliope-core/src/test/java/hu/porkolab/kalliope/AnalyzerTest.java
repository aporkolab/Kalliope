package hu.porkolab.kalliope;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Aranyminta-tesztek: valódi, dokumentált metrumú magyar versrészletek. Ha ezek
 * elromlanak, a motor romlott el — nem a teszt.
 */
class AnalyzerTest {

    private static List<String> meterNames(Analysis.Stanza stanza, int line) {
        return stanza.lines().get(line).meters().stream()
                .map(m -> m.meter().name())
                .toList();
    }

    @Test
    @DisplayName("Zrínyi: felező tizenkettes — nincs klasszikus mérték, a rím aaaa")
    void zriniIsNotClassical() {
        Analysis a = Analyzer.analyze(Examples.SZIGETI.text());
        Analysis.Stanza stanza = a.stanzas().get(0);
        assertThat(stanza.rhymePattern()).isEqualTo("aaaa");
        for (Analysis.Line line : stanza.lines()) {
            assertThat(line.meters())
                    .as("Zrínyi sora nem klasszikus mérték: %s", line.text())
                    .isEmpty();
        }
    }

    @Test
    @DisplayName("Radnóti: Hetedik ecloga — mind a négy sor hexameter")
    void radnotiIsHexameter() {
        Analysis a = Analyzer.analyze(Examples.HETEDIK_ECLOGA.text());
        Analysis.Stanza stanza = a.stanzas().get(0);
        for (int i = 0; i < stanza.lines().size(); i++) {
            assertThat(meterNames(stanza, i)).as("%s. sor", i + 1).contains("hexameter");
        }
        assertThat(stanza.rhymePattern()).isEqualTo("xxxx");
    }

    @Test
    @DisplayName("Kazinczy: A nagy titok — hexameter + pentameter = disztichon")
    void kazinczyIsDistich() {
        Analysis a = Analyzer.analyze(Examples.NAGY_TITOK.text());
        Analysis.Stanza stanza = a.stanzas().get(0);
        assertThat(meterNames(stanza, 0)).contains("hexameter");
        assertThat(meterNames(stanza, 1)).contains("pentameter");
        assertThat(stanza.forms()).extracting(f -> f.form().name()).contains("disztichon");
    }

    @Test
    @DisplayName("Berzsenyi: A magyarokhoz — alkaioszi strófa")
    void berzsenyiIsAlcaic() {
        Analysis a = Analyzer.analyze(Examples.MAGYAROKHOZ.text());
        Analysis.Stanza stanza = a.stanzas().get(0);
        assertThat(stanza.forms()).extracting(f -> f.form().name()).contains("alkaioszi strófa");
    }

    @Test
    @DisplayName("Petőfi: Szeptember végén — keresztrím")
    void petofiRhymesAbab() {
        Analysis a = Analyzer.analyze(Examples.SZEPTEMBER_VEGEN.text());
        assertThat(a.stanzas().get(0).rhymePattern()).isEqualTo("abab");
    }

    @Test
    @DisplayName("az Íliász kezdősora a szókezdő nyújtás licenciájával lesz hexameter")
    void iliadOpeningNeedsTheLicence() {
        Analysis strict = Analyzer.analyze(Examples.ILIASZ.text());
        assertThat(meterNames(strict.stanzas().get(0), 0)).doesNotContain("hexameter");

        Settings licence = MetricCanon.DEFAULT_SETTINGS.with(Map.of(Settings.WORD_INITIAL_STRESS, true));
        Analysis loose = Analyzer.analyze(Examples.ILIASZ.text(), licence);
        for (int i = 0; i < loose.stanzas().get(0).lines().size(); i++) {
            assertThat(meterNames(loose.stanzas().get(0), i))
                    .as("%s. sor", i + 1)
                    .contains("hexameter");
        }
    }

    @Test
    @DisplayName("regresszió: a vers üres sorok mentén szakaszokra bomlik")
    void blankLinesSplitStanzas() {
        // A korábbi változat eldobta az üres sorokat, ezért a szakaszmérték-
        // illesztés többstrófás versen sosem szólalt meg.
        String twoStanzas = Examples.MAGYAROKHOZ.text() + "\n\n" + Examples.MAGYAROKHOZ.text();
        Analysis a = Analyzer.analyze(twoStanzas);
        assertThat(a.stanzas()).hasSize(2);
        assertThat(a.summary().lineCount()).isEqualTo(8);
        for (Analysis.Stanza stanza : a.stanzas()) {
            assertThat(stanza.forms()).extracting(f -> f.form().name()).contains("alkaioszi strófa");
        }
    }

    @Test
    @DisplayName("regresszió: a disztichon ismétlődhet — egy hatsoros elégia három disztichon")
    void distichRepeats() {
        String threeCouplets =
                String.join("\n", Examples.NAGY_TITOK.text(), Examples.NAGY_TITOK.text(), Examples.NAGY_TITOK.text());
        Analysis a = Analyzer.analyze(threeCouplets);
        assertThat(a.stanzas()).hasSize(1);
        assertThat(a.stanzas().get(0).forms()).anySatisfy(f -> {
            assertThat(f.form().name()).isEqualTo("disztichon");
            assertThat(f.repetitions()).isEqualTo(3);
        });
    }

    @Test
    @DisplayName("regresszió: fiktív segédmérték sosem kerül a találatok közé")
    void fictiveMetersAreNotReported() {
        Analysis a = Analyzer.analyze("adjon az ég");
        for (Analysis.Line line : a.stanzas().get(0).lines()) {
            assertThat(line.meters())
                    .allSatisfy(m -> assertThat(m.meter().fictive()).isFalse());
        }
    }

    @Test
    @DisplayName("a beállítás valóban változtat a kimeneten")
    void settingsAffectOutput() {
        Settings noIctus = MetricCanon.DEFAULT_SETTINGS.with(Map.of(Settings.SHOW_ICTUS, false));
        Settings ictus = MetricCanon.DEFAULT_SETTINGS.with(Map.of(Settings.SHOW_ICTUS, true));
        Analysis without = Analyzer.analyze(Examples.HETEDIK_ECLOGA.text(), noIctus);
        Analysis with = Analyzer.analyze(Examples.HETEDIK_ECLOGA.text(), ictus);
        assertThat(without.stanzas().get(0).lines().get(0).ictusRow()).isNull();
        assertThat(with.stanzas().get(0).lines().get(0).ictusRow()).containsAnyOf("÷", "Ú");
    }

    @Test
    @DisplayName("ismeretlen beállításnév beszédes hibát ad")
    void unknownSettingFails() {
        assertThat(org.junit.jupiter.api.Assertions.assertThrows(
                        IllegalArgumentException.class,
                        () -> MetricCanon.DEFAULT_SETTINGS.with(Map.of("nincs_ilyen", true))))
                .hasMessageContaining("Ismeretlen beállítás");
    }

    @Test
    @DisplayName("szélsőséges bemenet nem dob kivételt és nem fagy le")
    void hostileInputIsSafe() {
        assertThat(Analyzer.analyze(null).stanzas()).isEmpty();
        assertThat(Analyzer.analyze("").stanzas()).isEmpty();
        assertThat(Analyzer.analyze("\n\n\n").stanzas()).isEmpty();
        assertThat(Analyzer.analyze("a ".repeat(500)).stanzas()).hasSize(1);

        StringBuilder longPoem = new StringBuilder();
        for (int i = 0; i < 3000; i++) {
            longPoem.append("Fegyvert, s vitézt éneklek, török hatalmát,\n");
        }
        long start = System.nanoTime();
        Analysis a = Analyzer.analyze(longPoem.toString());
        assertThat(a.summary().lineCount()).isLessThanOrEqualTo(Analyzer.MAX_LINES);
        assertThat(System.nanoTime() - start).isLessThan(30_000_000_000L);
    }

    @Test
    @DisplayName("a szótagok indoklása minden sorban ki van töltve")
    void everySyllableHasAReason() {
        Analysis a = Analyzer.analyze(Examples.HETEDIK_ECLOGA.text());
        for (Analysis.Stanza stanza : a.stanzas()) {
            for (Analysis.Line line : stanza.lines()) {
                assertThat(line.syllables()).isNotEmpty();
                assertThat(line.syllables()).allSatisfy(s -> {
                    assertThat(s.reason()).isNotNull();
                    assertThat(s.reason().explanation()).isNotBlank();
                    assertThat(s.text()).isNotEmpty();
                });
                assertThat(line.scansion())
                        .hasSameSizeAs(line.syllables().toString().isEmpty() ? line.scansion() : line.scansion());
                assertThat(line.syllables()).hasSize(line.scansion().length());
            }
        }
    }
}
