package hu.porkolab.kalliope;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Aranyminta-korpusz: tíz valódi, hiteles szövegű magyar versrészlet, mindegyik
 * dokumentált formával.
 *
 * <p>A korpusz szándékosan vegyes. Van benne olyan, amit a motornak fel KELL
 * ismernie (hexameter, disztichon, alkaioszi és aszklepiadeszi strófa), és olyan
 * is, amire a helyes válasz a <b>nincs találat</b>: a hangsúlyos-magyaros vers
 * (Zrínyi, Arany) nem időmértékes, és aki ezekre klasszikus mértéket mond, az
 * téved. A harmadik csoport a szabálytalan eset: az Íliász kezdősora csak
 * költői licenciával hexameter — ezt a motor nem hallgatja el, hanem külön
 * kapcsolóhoz köti.
 */
class CorpusTest {

    private static final Settings LICENCE =
            MetricCanon.DEFAULT_SETTINGS.with(Map.of(Settings.WORD_INITIAL_STRESS, true));

    /**
     * @param meterPerLine minden sortól elvárt mérték neve, vagy {@code null}, ha
     *     a sornak NEM szabad klasszikus mértéket kapnia
     */
    record Expectation(Examples poem, String rhyme, String stanzaForm, String meterPerLine, Settings settings) {}

    static Stream<Expectation> corpus() {
        Settings def = MetricCanon.DEFAULT_SETTINGS;
        return Stream.of(
                new Expectation(Examples.SZIGETI, "aaaa", null, null, def),
                new Expectation(Examples.TOLDI, "aabb", null, null, def),
                new Expectation(Examples.ZALAN, "xxxx", null, "hexameter", def),
                new Expectation(Examples.HETEDIK_ECLOGA, "xxxx", null, "hexameter", def),
                new Expectation(Examples.ILIASZ, "xaxa", null, "hexameter", LICENCE),
                new Expectation(Examples.NAGY_TITOK, "xx", "disztichon", null, def),
                new Expectation(Examples.MAGYAROKHOZ, "xxxx", "alkaioszi strófa", null, def),
                new Expectation(Examples.KOZELITO_TEL, "xxxx", "aszklepiadeszi B", null, def),
                new Expectation(Examples.HORAC, "xxxx", "aszklepiadeszi B", null, def),
                new Expectation(Examples.SZEPTEMBER_VEGEN, "abab", null, null, def));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("corpus")
    @DisplayName("a korpusz minden darabja a dokumentált formát adja")
    void corpusMatchesTheLiterature(Expectation e) {
        Analysis analysis = Analyzer.analyze(e.poem().text(), e.settings());
        assertThat(analysis.stanzas()).as("%s: egy szakasz", e.poem().title()).hasSize(1);
        Analysis.Stanza stanza = analysis.stanzas().get(0);

        assertThat(stanza.rhymePattern()).as("%s rímképlete", e.poem().title()).isEqualTo(e.rhyme());

        if (e.stanzaForm() != null) {
            assertThat(stanza.forms())
                    .as("%s szakaszmértéke", e.poem().title())
                    .extracting(f -> f.form().name())
                    .contains(e.stanzaForm());
        }

        if (e.meterPerLine() != null) {
            for (Analysis.Line line : stanza.lines()) {
                assertThat(names(line))
                        .as("%s — „%s”", e.poem().title(), line.text())
                        .contains(e.meterPerLine());
            }
        }
    }

    @Test
    @DisplayName("a hangsúlyos-magyaros versre helyesen NINCS klasszikus mérték")
    void accentualVerseHasNoClassicalMeter() {
        for (Examples poem : List.of(Examples.SZIGETI, Examples.TOLDI)) {
            Analysis a = Analyzer.analyze(poem.text());
            for (Analysis.Line line : a.stanzas().get(0).lines()) {
                assertThat(line.meters())
                        .as("%s — „%s” nem időmértékes sor", poem.title(), line.text())
                        .isEmpty();
            }
        }
    }

    @Test
    @DisplayName("a két Berzsenyi-vers ugyanazt a szakaszmértéket kapja")
    void bothBerzsenyiPoemsAreTheSameForm() {
        String a = form(Examples.KOZELITO_TEL);
        String b = form(Examples.HORAC);
        assertThat(a).isEqualTo(b).isEqualTo("aszklepiadeszi B");
    }

    @Test
    @DisplayName("az Íliász kezdősora alapbeállítással nem illeszkedik — ez a hű válasz")
    void iliadOpeningIsHonestlyReported() {
        Analysis strict = Analyzer.analyze(Examples.ILIASZ.text());
        assertThat(names(strict.stanzas().get(0).lines().get(0))).doesNotContain("hexameter");
        // a többi három viszont igen, licencia nélkül is
        for (int i = 1; i < 4; i++) {
            assertThat(names(strict.stanzas().get(0).lines().get(i))).contains("hexameter");
        }
    }

    @Test
    @DisplayName("a korpusz minden sorának minden szótagja indokolt")
    void everySyllableIsExplained() {
        for (Examples poem : Examples.ALL) {
            Analysis a = Analyzer.analyze(poem.text());
            for (Analysis.Stanza stanza : a.stanzas()) {
                for (Analysis.Line line : stanza.lines()) {
                    assertThat(line.syllables())
                            .as("%s — „%s”", poem.title(), line.text())
                            .isNotEmpty()
                            .hasSize(line.scansion().length())
                            .allSatisfy(s -> {
                                assertThat(s.reason()).isNotNull();
                                assertThat(s.text()).isNotBlank();
                            });
                }
            }
        }
    }

    @Test
    @DisplayName("a szótagok szövege minden sorban visszaadja az eredeti szavakat")
    void syllableTextsReconstructEveryLine() {
        for (Examples poem : Examples.ALL) {
            Analysis a = Analyzer.analyze(poem.text());
            for (Analysis.Stanza stanza : a.stanzas()) {
                for (Analysis.Line line : stanza.lines()) {
                    String fromSyllables = String.join(
                            "",
                            line.syllables().stream()
                                    .map(Scansion.Syllable::text)
                                    .toList());
                    String expected = String.join(
                            "", TextNormalizer.words(line.text(), a.settings().letterSyllables()));
                    assertThat(fromSyllables)
                            .as("%s — „%s”", poem.title(), line.text())
                            .isEqualTo(expected);
                }
            }
        }
    }

    @Test
    @DisplayName("a korpusz elemzése gyors marad")
    void corpusIsFast() {
        long start = System.nanoTime();
        for (int i = 0; i < 50; i++) {
            for (Examples poem : Examples.ALL) {
                Analyzer.analyze(poem.text());
            }
        }
        long millis = (System.nanoTime() - start) / 1_000_000;
        assertThat(millis).as("500 elemzés ezredmásodpercben").isLessThan(5_000L);
    }

    @Test
    @DisplayName("az egész korpusz egyben, üres sorokkal elválasztva is elemezhető")
    void wholeCorpusAsOneDocument() {
        String all =
                String.join("\n\n", Examples.ALL.stream().map(Examples::text).toList());
        Analysis a = Analyzer.analyze(all);
        assertThat(a.stanzas()).hasSize(Examples.ALL.size());
        assertThat(a.summary().lineCount()).isEqualTo(38);
        assertThat(a.summary().meters()).contains("hexameter", "pentameter");
        assertThat(a.summary().stanzaForms()).contains("disztichon", "alkaioszi strófa");
    }

    private static List<String> names(Analysis.Line line) {
        return line.meters().stream().map(m -> m.meter().name()).toList();
    }

    private static String form(Examples poem) {
        List<MeterMatcher.StanzaMatch> forms =
                Analyzer.analyze(poem.text()).stanzas().get(0).forms();
        assertThat(forms).as("%s szakaszmértéke", poem.title()).isNotEmpty();
        return forms.get(0).form().name();
    }
}
