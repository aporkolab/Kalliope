package hu.porkolab.kalliope;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Az összegzés azt mondja ki, amit egy verstani jegyzet mondana. Ezek a
 * tesztek a valódi korpuszon ellenőrzik, hogy a mondat helyes-e.
 */
class VerseSummaryTest {

    private static VerseSummary summary(Examples poem) {
        return Analyzer.analyze(poem.text()).verse();
    }

    @Test
    @DisplayName("a hexameteres eposz időmértékes")
    void epicIsQuantitative() {
        for (Examples poem : java.util.List.of(Examples.ILIASZ, Examples.ODUSSZEIA, Examples.ZALAN)) {
            VerseSummary v = summary(poem);
            assertThat(v.system()).as(poem.title()).isEqualTo(VerseSummary.System.IDOMERTEKES);
            assertThat(v.headline()).isEqualTo("Időmértékes verselés: hexameterek.");
        }
    }

    @Test
    @DisplayName("a disztichont néven nevezi")
    void distichIsNamed() {
        assertThat(summary(Examples.NAGY_TITOK).headline()).isEqualTo("Időmértékes verselés: disztichonok.");
    }

    @Test
    @DisplayName("az alkaioszi strófát néven nevezi")
    void alcaicIsNamed() {
        assertThat(summary(Examples.MAGYAROKHOZ).headline()).isEqualTo("Időmértékes verselés: alkaioszi strófa.");
    }

    @Test
    @DisplayName("a hangsúlyos vers ütemhangsúlyos — laza metszettel is")
    void accentualIsNamed() {
        for (Examples poem : java.util.List.of(Examples.SZIGETI, Examples.TOLDI)) {
            VerseSummary v = summary(poem);
            assertThat(v.system()).as(poem.title()).isEqualTo(VerseSummary.System.UTEMHANGSULYOS);
            assertThat(v.headline()).isEqualTo("Ütemhangsúlyos (magyaros) verselés: felező tizenkettes.");
        }
        // Zrínyinél külön kimondjuk, hogy a metszet szóba esik
        assertThat(summary(Examples.SZIGETI).details())
                .anySatisfy(d -> assertThat(d).contains("a metszet gyakran szóba esik"));
        assertThat(summary(Examples.TOLDI).details())
                .anySatisfy(d -> assertThat(d).contains("a metszet a szóhatáron van"));
    }

    @Test
    @DisplayName("a kettős rendű vers szimultán, mindkét formát megnevezve")
    void simultaneousNamesBoth() {
        VerseSummary v = summary(Examples.KOZELITO_TEL);
        assertThat(v.system()).isEqualTo(VerseSummary.System.SZIMULTAN);
        assertThat(v.headline()).contains("aszklepiadeszi").contains("felező tizenkettes ütemtagolással");
    }

    @Test
    @DisplayName("a részletek megadják a szerkezetet, a sorfajtákat és a rímet")
    void detailsCoverTheEssentials() {
        VerseSummary v = summary(Examples.KOZELITO_TEL);
        assertThat(v.details()).anySatisfy(d -> assertThat(d).contains("6 szakasz, egyenként 4 sor"));
        assertThat(v.details()).anySatisfy(d -> assertThat(d).startsWith("Szakaszmérték:"));
        assertThat(v.details()).anySatisfy(d -> assertThat(d).startsWith("Sorfajták:"));
    }

    @Test
    @DisplayName("rímtelen eposzban nem tesz úgy, mintha rímelne")
    void rhymelessEpicIsNotPretendedToRhyme() {
        assertThat(summary(Examples.ZALAN).details())
                .anySatisfy(d -> assertThat(d).contains("Rímtelen").contains("esetlegesek"));
    }

    @Test
    @DisplayName("a rímes versnél kiírja a képlet nevét")
    void rhymedPoemGetsItsSchemeName() {
        assertThat(summary(Examples.SZEPTEMBER_VEGEN).details())
                .anySatisfy(d -> assertThat(d).contains("keresztrím"));
        assertThat(summary(Examples.SZIGETI).details())
                .anySatisfy(d -> assertThat(d).contains("bokorrím"));
    }

    @Test
    @DisplayName("üres szövegre nem állít semmit")
    void emptyTextSaysNothing() {
        VerseSummary v = Analyzer.analyze("").verse();
        assertThat(v.system()).isEqualTo(VerseSummary.System.SZABAD);
        assertThat(v.details()).isEmpty();
    }

    @Test
    @DisplayName("prózára nem erőltet ritmusrendet")
    void proseGetsNoSystem() {
        String prose = "Ez itt egy egészen hétköznapi mondat, semmiféle versmérték nincs benne.\n"
                + "Ez a második mondat, és ez sem verssor, csak szöveg a sor végéig.";
        assertThat(Analyzer.analyze(prose).verse().system())
                .isIn(VerseSummary.System.SZABAD, VerseSummary.System.VEGYES);
    }
}
