package hu.porkolab.kalliope;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * A kézi vágás egyenértékűsége a JDK {@code String.split}-jével.
 *
 * <p>Ez a teszt nem a saját elképzelésemhez mér, hanem a JDK kimenetéhez: a
 * {@code Strings} pontosan azt a viselkedést örökli, ami eddig volt. Így a
 * regex kiváltása nem hozhat viselkedésváltozást a motorba.
 */
class StringsTest {

    private static final List<String> LINE_CASES = List.of(
            "",
            "egy",
            "egy\nkettő",
            "egy\r\nkettő",
            "egy\rkettő",
            "egy\n\nkettő",
            "\nvezető",
            "záró\n",
            "záró\n\n",
            "egy\r\n\r\nkettő",
            "vegyes\nsor\r\nvég\r",
            "\u000bfüggőleges",
            "lap\fdobás",
            "unicode\u2028sor",
            "unicode\u2029bekezdés",
            "nel\u0085sor");

    private static final List<String> RUN_CASES = List.of(
            "",
            " ",
            "  ",
            "a",
            "a b",
            "a  b",
            " a b",
            "  a   b  ",
            "a b ",
            " ",
            "a",
            "a|b",
            "a||b",
            "|a",
            "a|",
            "||a||");

    @Test
    @DisplayName("a sorokra vágás egyenértékű a split(\"\\\\R\", -1) hívással")
    void linesMatchTheJdk() {
        for (String text : LINE_CASES) {
            assertThat(Strings.lines(text))
                    .as("sorok: %s", text.replace("\n", "\\n").replace("\r", "\\r"))
                    .containsExactly(text.split("\\R", -1));
        }
    }

    @Test
    @DisplayName("a szóközfutam mentén vágás egyenértékű a split(\" +\") hívással")
    void spaceRunsMatchTheJdk() {
        for (String text : RUN_CASES) {
            assertThat(Strings.splitRuns(text, ' ')).as("szóköz: [%s]", text).containsExactly(text.split(" +"));
        }
    }

    @Test
    @DisplayName("a lábhatár mentén vágás egyenértékű a split(\"\\\\|+\") hívással")
    void barRunsMatchTheJdk() {
        for (String text : RUN_CASES) {
            assertThat(Strings.splitRuns(text, '|')).as("láb: [%s]", text).containsExactly(text.split("\\|+"));
        }
    }

    @Test
    @DisplayName("a motor valódi bemenetein is egyezik")
    void realEngineInputs() {
        for (Examples e : Examples.ALL) {
            assertThat(Strings.lines(e.text())).containsExactly(e.text().split("\\R", -1));
        }
        for (Meter m : MetricCanon.ALL_METERS) {
            assertThat(Strings.splitRuns(m.pattern(), '|'))
                    .containsExactly(m.pattern().split("\\|+"));
        }
    }
}
