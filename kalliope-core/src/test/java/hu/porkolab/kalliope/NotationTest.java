package hu.porkolab.kalliope;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NotationTest {

    @Test
    @DisplayName("a közös szótag mindkét irányban elfogadó")
    void ancepsMatchesBoth() {
        assertThat(Notation.matches("?", "U")).isTrue();
        assertThat(Notation.matches("?", "-")).isTrue();
        assertThat(Notation.matches("U", "?")).isTrue();
        assertThat(Notation.matches("-", "?")).isTrue();
        assertThat(Notation.matches("U", "-")).isFalse();
        assertThat(Notation.matches("-", "U")).isFalse();
    }

    @Test
    @DisplayName("a '=' vagy egy hosszúra, vagy két rövidre oldódik")
    void resolveConsumesOneOrTwo() {
        assertThat(Notation.matches("--", "-=")).isTrue();
        assertThat(Notation.matches("-UU", "-=")).isTrue();
        assertThat(Notation.matches("-U", "-=")).isFalse();
        assertThat(Notation.matches("-U-", "-=")).isFalse();
    }

    @Test
    @DisplayName("a lábhatár és a cezúra nem számít szótagnak")
    void separatorsAreIgnored() {
        assertThat(Notation.symbolsOnly("-=|-=|-||-UU|-UU|?")).isEqualTo("-=-=--UU-UU?");
        assertThat(Notation.matches("-UU-UU--UU-UU?", "-=|-=|-||-UU|-UU|?")).isTrue();
    }

    @Test
    @DisplayName("regresszió: sok közös szótag nem robbantja fel az illesztést, és nem ad hamis találatot")
    void manyAncepsDoesNotExplode() {
        // A korábbi változat a realizációk kifejtésével dolgozott, 8192 fölött
        // CSONKOLT, és a csonkolt előtagokat hasonlította — egy negyven szótagos
        // sor így „hexameter" lett.
        String forty = "?".repeat(40);
        long start = System.nanoTime();
        assertThat(Notation.matches(forty, MetricCanon.HEXAMETER.pattern())).isFalse();
        assertThat(System.nanoTime() - start).isLessThan(1_000_000_000L);

        // ugyanakkor a valódi hosszúságú közös sor illeszkedik
        assertThat(Notation.matches("?".repeat(17), MetricCanon.HEXAMETER.pattern()))
                .isTrue();
    }

    @Test
    @DisplayName("a realizáció a szkennelt sort követi, nem az első azonos hosszúságút")
    void realizationFollowsTheScan() {
        String scan = "---UU-----UU-?";
        String realization = Notation.realize(scan, MetricCanon.HEXAMETER.pattern());
        assertThat(realization).hasSameSizeAs(scan);
        for (int i = 0; i < scan.length(); i++) {
            char s = scan.charAt(i);
            if (s != Notation.ANCEPS) {
                assertThat(realization.charAt(i)).isEqualTo(s);
            }
        }
    }

    @Test
    @DisplayName("az iktus a verslábak első szótagján áll")
    void ictusMarksFootStarts() {
        boolean[] ictus = Notation.ictusPositions("-UU-UU-UU-UU-UU--", MetricCanon.HEXAMETER.pattern());
        assertThat(ictus).isNotNull();
        List<Integer> positions = new java.util.ArrayList<>();
        for (int i = 0; i < ictus.length; i++) {
            if (ictus[i]) {
                positions.add(i);
            }
        }
        assertThat(positions).containsExactly(0, 3, 6, 9, 12, 15);
    }

    @Test
    @DisplayName("nem illeszkedő minta null igazítást ad")
    void noMatchYieldsNull() {
        assertThat(Notation.align("UUU", MetricCanon.HEXAMETER.pattern())).isNull();
        assertThat(Notation.realize("UUU", MetricCanon.HEXAMETER.pattern())).isNull();
    }
}
