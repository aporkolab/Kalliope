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
 * Aranyminta-korpusz: tizenegy valódi magyar vers, lehetőleg teljes egészében,
 * hiteles szövegforrásból.
 *
 * <p>Valódi versen mérni azért érdemes, mert a valódi vers szabálytalan. A
 * korpusz szándékosan vegyes:
 *
 * <ul>
 *   <li>amit fel KELL ismerni: hexameter, disztichon, alkaioszi és
 *       aszklepiadeszi strófa;
 *   <li>amire a helyes válasz a <b>nincs találat</b>: a hangsúlyos-magyaros vers
 *       (Zrínyi, Arany) nem időmértékes;
 *   <li>a szabálytalan eset: az Íliász kezdősora csak költői licenciával
 *       hexameter — ezt a motor nem hallgatja el.
 * </ul>
 *
 * <p>Az időmértékes versekre <b>arányt</b> várunk el, nem hibátlanságot: egy
 * negyvensoros eposzrészletben mindig akad sor, amely licenciára épül. A
 * küszöb így is szigorú, és ha romlik, a teszt elbukik.
 */
class CorpusTest {

    private static final Settings LICENCE =
            MetricCanon.DEFAULT_SETTINGS.with(Map.of(Settings.WORD_INITIAL_STRESS, true));

    /**
     * @param meter a sorok többségétől elvárt mérték, vagy {@code null}, ha a
     *     versnek NEM szabad klasszikus mértéket kapnia
     * @param minRatio a mértéket felmutató sorok minimális aránya százalékban
     * @param stanzaForm minden szakasztól elvárt szakaszmérték, vagy {@code null}
     */
    record Expectation(Examples poem, String meter, int minRatio, String stanzaForm, Settings settings) {
        @Override
        public String toString() {
            return poem.title();
        }
    }

    static Stream<Expectation> corpus() {
        Settings def = MetricCanon.DEFAULT_SETTINGS;
        return Stream.of(
                new Expectation(Examples.SZIGETI, null, 0, null, def),
                new Expectation(Examples.TOLDI, null, 0, null, def),
                new Expectation(Examples.ILIASZ, "hexameter", 85, null, def),
                new Expectation(Examples.ODUSSZEIA, "hexameter", 85, null, def),
                new Expectation(Examples.ZALAN, "hexameter", 90, null, def),
                new Expectation(Examples.HETEDIK_ECLOGA, "hexameter", 90, null, def),
                new Expectation(Examples.NAGY_TITOK, null, 0, "disztichon", def),
                new Expectation(Examples.MAGYAROKHOZ, null, 0, "alkaioszi strófa", def),
                new Expectation(Examples.KOZELITO_TEL, null, 0, "aszklepiadeszi B", def),
                new Expectation(Examples.HORAC, null, 0, "aszklepiadeszi B", def),
                new Expectation(Examples.SZEPTEMBER_VEGEN, null, 0, null, def),
                new Expectation(Examples.ILIASZ, "hexameter", 90, null, LICENCE));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("corpus")
    @DisplayName("a korpusz minden darabja a dokumentált formát adja")
    void corpusMatchesTheLiterature(Expectation e) {
        Analysis analysis = Analyzer.analyze(e.poem().text(), e.settings());
        assertThat(analysis.stanzas()).as("%s: van szakasz", e.poem().title()).isNotEmpty();

        if (e.meter() != null) {
            int lines = 0;
            int matched = 0;
            for (Analysis.Stanza stanza : analysis.stanzas()) {
                for (Analysis.Line line : stanza.lines()) {
                    lines++;
                    if (names(line).contains(e.meter())) {
                        matched++;
                    }
                }
            }
            assertThat(100 * matched / lines)
                    .as("%s — %s arány (%d/%d sor)", e.poem().title(), e.meter(), matched, lines)
                    .isGreaterThanOrEqualTo(e.minRatio());
        }

        if (e.stanzaForm() != null) {
            // A szakaszok TÖBBSÉGÉTŐL várjuk el a formát: egy teljes versben
            // akad szakasz, amelynek egyetlen sora licenciára épül, és az egész
            // szakaszt megbuktatja. A küszöb így is szigorú.
            long ok = analysis.stanzas().stream()
                    .filter(st ->
                            st.forms().stream().anyMatch(f -> f.form().name().equals(e.stanzaForm())))
                    .count();
            assertThat(ok * 4)
                    .as(
                            "%s — %s szakaszok aránya (%d/%d)",
                            e.poem().title(),
                            e.stanzaForm(),
                            ok,
                            analysis.stanzas().size())
                    .isGreaterThanOrEqualTo(analysis.stanzas().size() * 3L);
        }
    }

    @Test
    @DisplayName("a hangsúlyos-magyaros versre helyesen NINCS klasszikus mérték")
    void accentualVerseHasNoClassicalMeter() {
        for (Examples poem : List.of(Examples.SZIGETI, Examples.TOLDI)) {
            Analysis a = Analyzer.analyze(poem.text());
            for (Analysis.Stanza stanza : a.stanzas()) {
                for (Analysis.Line line : stanza.lines()) {
                    assertThat(line.meters())
                            .as("%s — „%s” nem időmértékes sor", poem.title(), line.text())
                            .isEmpty();
                }
            }
        }
    }

    @Test
    @DisplayName("a hangsúlyos verset viszont felező tizenkettesként felismeri")
    void accentualVerseIsRecognisedAsSuch() {
        Analysis toldi = Analyzer.analyze(Examples.TOLDI.text());
        assertThat(toldi.stanzas()).allSatisfy(stanza -> {
            assertThat(stanza.accentual().form())
                    .as("Toldi %d. szakasza", stanza.index() + 1)
                    .isEqualTo(AccentualCanon.FELEZO_TIZENKETTES);
            assertThat(stanza.accentual().strength())
                    .as("Toldi %d. szakaszának ütemtagolása", stanza.index() + 1)
                    .isEqualTo(AccentualMatcher.Strength.TISZTA);
        });
        assertThat(toldi.summary().accentualForms()).contains("felező tizenkettes");

        // Zrínyi is felező tizenkettes, csak lazább metszettel — ezt is kimondjuk
        Analysis zrinyi = Analyzer.analyze(Examples.SZIGETI.text());
        assertThat(zrinyi.stanzas().get(0).accentual().form()).isEqualTo(AccentualCanon.FELEZO_TIZENKETTES);
        assertThat(zrinyi.stanzas().get(0).accentual().strength()).isEqualTo(AccentualMatcher.Strength.LAZA);
    }

    @Test
    @DisplayName("az időmértékes eposzokra nem aggat hamis ütemtagolást")
    void hexameterIsNotLabelledAccentual() {
        for (Examples poem : List.of(Examples.ZALAN, Examples.HETEDIK_ECLOGA)) {
            Analysis a = Analyzer.analyze(poem.text());
            for (Analysis.Stanza stanza : a.stanzas()) {
                assertThat(stanza.dualRhythm())
                        .as("%s — %d. szakasz kettős ritmusa", poem.title(), stanza.index() + 1)
                        .isFalse();
            }
        }
    }

    @Test
    @DisplayName("a hexameter metszete kimutatható")
    void hexameterCaesuraIsDetected() {
        Analysis a = Analyzer.analyze(Examples.ZALAN.text());
        long withCaesura = a.stanzas().get(0).lines().stream()
                .filter(l -> l.caesurae().stream().anyMatch(c -> c.name().startsWith("penthémimerész")))
                .count();
        assertThat(withCaesura)
                .as("penthémimerész metszetű sorok a Zalán futásában")
                .isGreaterThan(a.stanzas().get(0).lines().size() / 2);
    }

    @Test
    @DisplayName("az Íliász kezdősora alapbeállítással nem illeszkedik — és megmondjuk, min múlik")
    void iliadOpeningIsHonestlyReported() {
        Analysis strict = Analyzer.analyze(Examples.ILIASZ.text());
        Analysis.Line first = strict.stanzas().get(0).lines().get(0);
        assertThat(names(first)).doesNotContain("hexameter");
        assertThat(first.nearMiss()).isNotNull();
        assertThat(first.nearMiss().meter().name()).isEqualTo("hexameter");
        assertThat(first.nearMiss().differences()).hasSize(1);
        assertThat(first.nearMiss().differences().get(0).syllable()).isZero();
    }

    @Test
    @DisplayName("a rímfajtákat megnevezi")
    void rhymeKindsAreNamed() {
        Analysis zrinyi = Analyzer.analyze(Examples.SZIGETI.text());
        assertThat(zrinyi.stanzas().get(0).rhymePattern()).isEqualTo("aaaa");
        assertThat(zrinyi.stanzas().get(0).rhymePatternName()).isEqualTo("bokorrím");
        assertThat(zrinyi.stanzas().get(0).lines())
                .allSatisfy(l -> assertThat(l.rhymeKind()).isEqualTo(RhymeDetector.Kind.TISZTA));

        // az Íliász „-nak / -nak" egybeesése ragrím, nem valódi rím
        Analysis iliasz = Analyzer.analyze(Examples.ILIASZ.text());
        assertThat(iliasz.stanzas().get(0).lines())
                .anySatisfy(l -> assertThat(l.rhymeKind()).isEqualTo(RhymeDetector.Kind.RAGRIM));
    }

    @Test
    @DisplayName("a kézi felülbírálás felülírja a skandálást és új találatot hozhat")
    void manualOverrideWins() {
        Analysis strict = Analyzer.analyze(Examples.ILIASZ.text());
        assertThat(names(strict.stanzas().get(0).lines().get(0))).doesNotContain("hexameter");

        Analysis fixed = Analyzer.analyze(
                Examples.ILIASZ.text(),
                MetricCanon.DEFAULT_SETTINGS,
                List.of(new Scansion.Override(0, 0, Notation.LONG)));
        Analysis.Line first = fixed.stanzas().get(0).lines().get(0);
        assertThat(names(first)).contains("hexameter");
        assertThat(first.syllables().get(0).reason()).isEqualTo(Scansion.Reason.MANUAL);
    }

    @Test
    @DisplayName("a teljes versek szakaszokra bomlanak")
    void fullPoemsSplitIntoStanzas() {
        assertThat(Analyzer.analyze(Examples.SZEPTEMBER_VEGEN.text()).stanzas()).hasSize(3);
        assertThat(Analyzer.analyze(Examples.KOZELITO_TEL.text()).stanzas()).hasSize(6);
        assertThat(Analyzer.analyze(Examples.HORAC.text()).stanzas()).hasSize(4);
        assertThat(Analyzer.analyze(Examples.HETEDIK_ECLOGA.text()).stanzas()).hasSize(6);
    }

    @Test
    @DisplayName("a korpusz minden sorának minden szótagja indokolt, és kiadja az eredeti szöveget")
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
                    String fromSyllables = String.join(
                            "",
                            line.syllables().stream()
                                    .map(Scansion.Syllable::text)
                                    .toList());
                    // a szóközök nem számítanak: a magánhangzó nélküli szó (az „s”
                    // kötőszó) az előző szótaghoz tapad, szóközzel elválasztva
                    assertThat(fromSyllables.replace(" ", ""))
                            .as("%s — „%s” szótagjai", poem.title(), line.text())
                            .isEqualTo(String.join("", TextNormalizer.words(line.text(), false)));
                }
            }
        }
    }

    @Test
    @DisplayName("korpusz-riport: az egész korpusz legalább 80%-a illeszkedik")
    void corpusReport() {
        int lines = 0;
        int matched = 0;
        StringBuilder report = new StringBuilder("\n");
        for (Examples poem : Examples.ALL) {
            Analysis a = Analyzer.analyze(poem.text());
            int poemLines = 0;
            int poemMatched = 0;
            for (Analysis.Stanza stanza : a.stanzas()) {
                for (Analysis.Line line : stanza.lines()) {
                    poemLines++;
                    if (line.matched()) {
                        poemMatched++;
                    }
                }
            }
            lines += poemLines;
            matched += poemMatched;
            report.append("  %-34s %3d sor  %3d%%%n".formatted(poem.title(), poemLines, 100 * poemMatched / poemLines));
        }
        report.append("  ÖSSZESEN %d sor, %d illeszkedik%n".formatted(lines, matched));
        // A hangsúlyos versek (Zrínyi, Toldi) szándékosan nulla százalékkal
        // szerepelnek benne, tehát a küszöb rájuk is vonatkozik.
        assertThat(100 * matched / lines).as(report.toString()).isGreaterThanOrEqualTo(80);
    }

    @Test
    @DisplayName("a korpusz elemzése gyors marad")
    void corpusIsFast() {
        long start = System.nanoTime();
        for (int i = 0; i < 20; i++) {
            for (Examples poem : Examples.ALL) {
                Analyzer.analyze(poem.text());
            }
        }
        long millis = (System.nanoTime() - start) / 1_000_000;
        assertThat(millis).as("a korpusz húszszori elemzése ezredmásodpercben").isLessThan(10_000L);
    }

    private static List<String> names(Analysis.Line line) {
        return line.meters().stream().map(m -> m.meter().name()).toList();
    }
}
