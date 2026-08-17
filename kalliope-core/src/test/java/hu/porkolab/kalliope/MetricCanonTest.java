package hu.porkolab.kalliope;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MetricCanonTest {

    @Test
    @DisplayName("a kánon mérete megegyezik az eredeti adatbáziséval")
    void canonSize() {
        assertThat(MetricCanon.FEET).hasSize(11);
        assertThat(MetricCanon.COLA).hasSize(38);
        assertThat(MetricCanon.COMPLEXES).hasSize(8);
        assertThat(MetricCanon.STANZAS).hasSize(20);
        assertThat(MetricCanon.UNSTRESSED_WORDS).hasSize(16);
    }

    @Test
    @DisplayName("minden azonosító egyedi")
    void idsAreUnique() {
        Set<String> ids = new HashSet<>();
        for (Meter m : MetricCanon.ALL_METERS) {
            assertThat(ids.add(m.id())).as("ütköző azonosító: %s", m.id()).isTrue();
        }
    }

    @Test
    @DisplayName("regresszió: minden összetett mérték teljesen feloldódik, nyers névmaradvány nélkül")
    void complexMetersFullyResolve() {
        // Az eredeti adatban a #complex hivatkozások egyike sem oldódott fel
        // (nib.alex.1.fiktiv ↔ .fictive, „adonisi kolon" elgépelés), és a feloldó
        // a fel nem oldott NEVET fűzte be nyers mintaként. Java-adatban ez nem
        // fordulhat elő: a hivatkozás objektumhivatkozás.
        for (Meter m : MetricCanon.COMPLEXES) {
            assertThat(m.pattern()).matches("[U\\-?=|]+");
        }
        assertThat(MetricCanon.BI_ADONISZI.minSyllables()).isEqualTo(2 * MetricCanon.ADONISZI.minSyllables());
        assertThat(MetricCanon.NIB_ALEX_A.minSyllables()).isGreaterThan(12);
    }

    @Test
    @DisplayName("minden szakaszmérték sora valódi, feloldott mérték")
    void stanzaLinesResolve() {
        for (StanzaForm form : MetricCanon.STANZAS) {
            assertThat(form.lines()).isNotEmpty();
            for (Meter line : form.lines()) {
                assertThat(Notation.symbolsOnly(line.pattern())).isNotEmpty();
            }
        }
    }

    @Test
    @DisplayName("a javított minták megőrzik az eredeti értéket és a forrást")
    void correctionsAreDocumented() {
        assertThat(MetricCanon.CHOLIAMBUS.correction()).isNotNull();
        assertThat(MetricCanon.CHOLIAMBUS.correction().original()).isEqualTo("?-U-?-U-U-U?");
        assertThat(MetricCanon.CHOLIAMBUS.correction().source()).startsWith("http");
        assertThat(MetricCanon.ALKAIOSZI_3.correction()).isNotNull();
        assertThat(MetricCanon.ASZKLEPIADESZI_A123.correction()).isNull();
    }

    @Test
    @DisplayName("a sánta jambust az utolsó előtti hosszú különbözteti meg a trimetertől")
    void choliambIsNotAPlainTrimeter() {
        String plainTrimeter = "U-U-U-U-U-U-";
        assertThat(Notation.matches(plainTrimeter, MetricCanon.CHOLIAMBUS.pattern()))
                .isFalse();
        String scazon = "U-U-U-U-U---";
        assertThat(Notation.matches(scazon, MetricCanon.CHOLIAMBUS.pattern())).isTrue();
    }

    @Test
    @DisplayName("az alkaioszi kilences ötödik pozíciója közös")
    void alcaicEnneasyllableAllowsLongFifth() {
        assertThat(Notation.matches("--U---U--", MetricCanon.ALKAIOSZI_3.pattern()))
                .isTrue();
    }

    @Test
    @DisplayName("keresés ékezet- és kisbetű-tűrő")
    void searchIsAccentTolerant() {
        assertThat(MetricCanon.search("szapphoi")).extracting(Meter::name).contains("szapphói sor");
        assertThat(MetricCanon.search("ALKAIOSZI")).isNotEmpty();
        assertThat(MetricCanon.search("-UU-UU-")).isNotEmpty();
    }

    @Test
    @DisplayName("ismeretlen azonosító beszédes hibát ad")
    void unknownIdFails() {
        assertThat(org.junit.jupiter.api.Assertions.assertThrows(
                        IllegalArgumentException.class, () -> MetricCanon.meter("nincs-ilyen")))
                .hasMessageContaining("Ismeretlen mérték");
    }
}
