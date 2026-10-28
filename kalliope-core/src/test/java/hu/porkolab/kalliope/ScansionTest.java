package hu.porkolab.kalliope;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
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

    private static Scansion.Reason reason(String line, int syllable) {
        return Scansion.scan(line, STRICT).syllables().get(syllable).reason();
    }

    private static List<String> texts(String line) {
        return Scansion.scan(line, STRICT).syllables().stream()
                .map(Scansion.Syllable::text)
                .toList();
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
    @DisplayName("regresszió: a képzőhatáron álló z+s, c+s két mássalhangzó — ragozva és -ség képzővel is")
    void suffixBoundaryIsTwoConsonants() {
        // A z/c végű tő és a -ság/-ség képző határán az írásképi zs/cs két hang, a
        // rövid magánhangzó utáni szótag tehát helyzeténél fogva hosszú. A
        // binárisból örökölt „gazság → gasság" átírás ebből csak a szóvégi
        // igazság-típust fogta meg: „Az igazságot" ?UU-? lett ?U--? helyett, a
        // „község" első szótaga pedig rövid maradt.
        for (String word : List.of("igazság", "igazságot", "igazságos", "igazságtalan", "malacság", "kukacság")) {
            assertThat(scan(word)).as(word).startsWith("U-");
            assertThat(reason(word, 1)).as(word).isEqualTo(Scansion.Reason.POSITION_LONG);
        }
        for (String word : List.of("község", "községben", "gazságot")) {
            assertThat(scan(word)).as(word).startsWith("-");
            assertThat(reason(word, 0)).as(word).isEqualTo(Scansion.Reason.POSITION_LONG);
        }

        // A kiírt szótag az eredeti betűket mutatja: a régi átírás „i | gas | ság"-ot
        // írt, a „cság → csség" pedig a magánhangzót is elcsúsztatta („rács | ség").
        assertThat(texts("igazság")).containsExactly("i", "gaz", "ság");
        assertThat(texts("rácság")).containsExactly("rác", "ság");

        // a valódi zs viszont egy mássalhangzó marad — tőben és ágy-összetételben is
        assertThat(scan("mazsola")).startsWith("UU");
        assertThat(texts("masszázságy")).endsWith("zságy");
    }

    @Test
    @DisplayName("regresszió: az ellenőrzött összetételekben a határon álló betűpár két hang")
    void compoundBoundaryIsTwoConsonants() {
        // kékes|zöld, tejes|zacskó, malac|sült: íráskép szerint sz és cs, valójában
        // két hang — a rövid magánhangzó utáni szótag helyzeténél fogva hosszú
        assertThat(texts("tejeszacskó")).containsExactly("te", "jes", "zacs", "kó");
        assertThat(reason("tejeszacskó", 1)).isEqualTo(Scansion.Reason.POSITION_LONG);
        assertThat(texts("kékeszöld")).containsExactly("ké", "kes", "zöld");
        assertThat(reason("kékeszöld", 1)).isEqualTo(Scansion.Reason.POSITION_LONG);
        assertThat(texts("malacsült")).containsExactly("ma", "lac", "sült");
        assertThat(reason("malacsült", 1)).isEqualTo(Scansion.Reason.POSITION_LONG);
        // a d+z a skandálást nem érinti, csak a szótagolást
        assertThat(texts("vadzab")).containsExactly("vad", "zab");

        // a szó elejéhez kötött előtag nem áll meg a szó belsejében: pipere|csomag
        assertThat(texts("piperecsomag")).containsExactly("pi", "pe", "re", "cso", "mag");
        assertThat(scan("piperecsomag")).startsWith("UUU");
        assertThat(texts("szilvadzsem")).containsExactly("szil", "va", "dzsem");
    }

    @Test
    @DisplayName("a görög aspiráta (kh, th, ph) írásképe kétértelmű, ezért közös")
    void greekAspiratesAreAmbiguous() {
        // Egy hang a görög névben (Akhilleusz = χ), kettő a magyar
        // morfémahatáron (csak+hogy, halandó+k+hoz, kap+hat, át+hat). A leírt
        // alakból nem dönthető el, melyik — a mérték választ.
        // A leletet a szerző webes változatával összevetve találtuk: az „akháj"
        // sorokat mi rontottuk el, az „-okhoz" ragosakat ő.
        assertThat(scan("Akhilleusz")).startsWith("?");
        assertThat(scan("Ithaka")).startsWith("?");
        assertThat(scan("csakhogy")).startsWith("?");
        assertThat(scan("kaphat")).startsWith("?");
        assertThat(Scansion.scan("kaphat", STRICT).syllables().get(0).reason())
                .isEqualTo(Scansion.Reason.AMBIGUOUS_CLUSTER);

        // aspiráta + likvida: a szótag közös marad (a görög olvasat muta cum
        // liquidája és a magyar kétmássalhangzós olvasat is megengedi)
        assertThat(scan("Aphrodité")).startsWith("?");
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
        assertThat(scan("meggy")).startsWith("?"); // egyetlen szótag: sorvégi közömbös helyzet
        assertThat(scan("könnyű")).startsWith("-");
    }

    @Test
    @DisplayName("a szótag átlépi a szóhatárt")
    void syllableCrossesWordBoundary() {
        assertThat(scan("vak róka")).startsWith("--");
        assertThat(scan("has hashoz")).startsWith("-");
    }

    @Test
    @DisplayName("a sorvég közömbös szótaghelyzet, a névelő közös szótag")
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
