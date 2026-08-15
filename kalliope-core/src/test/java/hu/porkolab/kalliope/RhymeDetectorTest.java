package hu.porkolab.kalliope;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RhymeDetectorTest {

    private static String pattern(List<String> lines) {
        return RhymeDetector.scheme(lines, true, false).pattern();
    }

    @Test
    @DisplayName("regresszió: a magánhangzóra végződő sorvégek nem rímelnek pusztán az utolsó magánhangzótól")
    void openEndingsNeedTwoSyllables() {
        // A korábbi kulcs az UTOLSÓ magánhangzótól indult, ezért a "haza",
        // "soha", "béka", "anya" mind ugyanazt a kulcsot ("a") kapta.
        // Marad viszont a valódi asszonánc: a "haza" és az "anya" magánhangzóváza
        // egyaránt a–a, ami magyar fül számára gyenge, de létező összecsengés.
        assertThat(pattern(List.of("otthon a haza", "nem múlik soha", "kicsi béka", "alszik az anya")))
                .isEqualTo("axxa");
        // szigorú módban egyik sem rímel
        assertThat(RhymeDetector.scheme(
                                List.of("otthon a haza", "nem múlik soha", "kicsi béka", "alszik az anya"),
                                false,
                                false)
                        .pattern())
                .isEqualTo("xxxx");
    }

    @Test
    @DisplayName("a zárt sorvégi szótag rímel: hatalmát / szablyáját")
    void closedEndingsRhyme() {
        assertThat(pattern(List.of("török hatalmát", "rettegte szablyáját"))).isEqualTo("aa");
    }

    @Test
    @DisplayName("regresszió: a szóvégi mássalhangzót nem mossuk össze — kard és part nem rímel")
    void finalConsonantIsStrict() {
        // A korábbi tábla az r→l zöngétlenítést a szó VÉGÉN is alkalmazta, majd
        // az lt→tt szabály is ráfutott: "kard", "part" és "halt" egy kulcsra esett.
        assertThat(pattern(List.of("éles kard", "puha part"))).isEqualTo("xx");
        assertThat(pattern(List.of("magas hegy", "arra megy"))).isEqualTo("aa");
    }

    @Test
    @DisplayName("az asszonánc a magánhangzóvázon fog: virágok / világot")
    void assonanceCatchesNearRhyme() {
        assertThat(pattern(List.of("a kerti virágok", "a téli világot"))).isEqualTo("aa");
        // kikapcsolt asszonánccal viszont nem
        assertThat(RhymeDetector.scheme(List.of("a kerti virágok", "a téli világot"), false, false)
                        .pattern())
                .isEqualTo("xx");
    }

    @Test
    @DisplayName("a rímtelen sor vaksor: x")
    void blankLinesGetX() {
        assertThat(pattern(List.of("egy szép ház", "zöldell a fa", "magas ház")))
                .isEqualTo("axa");
    }

    @Test
    @DisplayName("regresszió: a rímbetűk sosem futnak ki az ábécéből")
    void labelsNeverLeaveTheAlphabet() {
        // A korábbi változat 'z' után '{', '|', '}', DEL karaktereket írt.
        assertThat(RhymeDetector.label(0)).isEqualTo("a");
        assertThat(RhymeDetector.label(25)).isEqualTo("z");
        assertThat(RhymeDetector.label(26)).isEqualTo("aa");
        assertThat(RhymeDetector.label(27)).isEqualTo("ab");

        List<String> lines = new ArrayList<>();
        for (int i = 0; i < 60; i++) {
            lines.add("sor " + i + " vége" + (char) ('a' + i % 20) + "t");
            lines.add("másik " + i + " vége" + (char) ('a' + i % 20) + "t");
        }
        String scheme = RhymeDetector.scheme(lines, false, false).pattern();
        assertThat(scheme).matches("[a-zx]+");
    }

    @Test
    @DisplayName("magánhangzó nélküli sorok nem rímelnek egymással")
    void vowellessLinesDoNotRhyme() {
        assertThat(pattern(List.of("Elment a nyár", "- - -", "1914", "Maradt a tél")))
                .isEqualTo("xxxx");
    }

    @Test
    @DisplayName("a keresztrím és az ölelkező rím helyesen jön ki")
    void classicSchemes() {
        assertThat(pattern(List.of("a ház", "a kert", "a láz", "a szert"))).isEqualTo("abab");
        assertThat(pattern(List.of("a ház", "a kert", "a szert", "a láz"))).isEqualTo("abba");
    }
}
