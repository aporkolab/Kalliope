package hu.porkolab.kalliope;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MorphologyTest {

    /** Áll-e határ a szóban a {@code left} előtag után. */
    private static boolean boundaryAfter(String word, String left) {
        assertThat(word).startsWith(left);
        return Morphology.boundaryBefore(word, left.length());
    }

    @Test
    @DisplayName("képzőhatár: z/c végű tő + -ság/-ség, ragozva is")
    void suffixBoundary() {
        assertThat(boundaryAfter("igazságot", "igaz")).isTrue();
        assertThat(boundaryAfter("községben", "köz")).isTrue();
        assertThat(boundaryAfter("rácság", "rác")).isTrue();
        assertThat(boundaryAfter("kukacság", "kukac")).isTrue();
    }

    @Test
    @DisplayName("a valódi zs, cs, sz, dzs egy hang marad")
    void realDigraphsStay() {
        assertThat(Morphology.splits("mazsola", 2, 2)).isFalse();
        // masszázs|ágy: az ágy-összetétel nem -ság képző
        assertThat(Morphology.splits("masszázságy", 6, 2)).isFalse();
        // a kettőzött ccs egy hosszú hang: naccsága
        assertThat(Morphology.splits("naccsága", 2, 3)).isFalse();
        // a szó elejéhez kötött előtag a szó belsejében nem áll meg
        assertThat(Morphology.splits("piperecsomag", 6, 2)).isFalse();
        assertThat(Morphology.splits("szilvadzsem", 6, 3)).isFalse();
        // a gyakorisági listán ütköző előtagok kimaradtak: le|veszem, pénz|eszköz
        assertThat(Morphology.splits("leveszem", 4, 2)).isFalse();
        assertThat(Morphology.splits("pénzeszköz", 5, 2)).isFalse();
        // a zs+z is lehet: varázs|zene — általános z|sz szabály nincs
        assertThat(Morphology.splits("varázszene", 4, 2)).isFalse();
    }

    @Test
    @DisplayName("összetétel: az ellenőrzött előtag után határ áll")
    void compoundBoundary() {
        assertThat(boundaryAfter("tejeszacskó", "tejes")).isTrue();
        assertThat(boundaryAfter("szívritmuszavar", "szívritmus")).isTrue();
        assertThat(boundaryAfter("malacsült", "malac")).isTrue();
        assertThat(boundaryAfter("vadzab", "vad")).isTrue();
        assertThat(boundaryAfter("közszolgálat", "köz")).isTrue();
    }

    @Test
    @DisplayName("az előtag-lista minden tétele kisbetűs, és legfeljebb a ^ jel nem betű benne")
    void firstElementsAreWellFormed() {
        for (String entry : Morphology.FIRST_ELEMENTS) {
            String element = entry.startsWith("^") ? entry.substring(1) : entry;
            assertThat(element).as(entry).isNotEmpty().isLowerCase();
            assertThat(element.chars().allMatch(Character::isLetter)).as(entry).isTrue();
        }
    }
}
