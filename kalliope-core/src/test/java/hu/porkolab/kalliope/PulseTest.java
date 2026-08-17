package hu.porkolab.kalliope;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * A lüktetés kimutatása — a három korláttal együtt.
 *
 * <p>Váradi Nagy Pál jelentette, hogy egy soros bemenetre a program feladja
 * („szabadvers vagy próza”), pedig a daktilikus lüktetés kihallható. A lüktetés
 * ezt mondja ki — de nem mértékként.
 */
class PulseTest {

    @Test
    @DisplayName("Pál tesztesete: hat daktilus a sor élén, aztán megszakad")
    void detectsTheDactylicRun() {
        Analysis.Line line = Analyzer.analyze("Elmegy a kugli egy este berúgni me' ő az a kugli ki nincs fából")
                .stanzas()
                .get(0)
                .lines()
                .get(0);
        assertThat(line.meters()).as("huszonegy szótagos kánoni sorfajta nincs").isEmpty();

        Pulse.Result p = line.pulse();
        assertThat(p).isNotNull();
        assertThat(p.footName()).isEqualTo("daktilus");
        assertThat(p.feet()).isEqualTo(6);
        assertThat(p.syllables()).isEqualTo(18);
        assertThat(p.breaksAt()).isEqualTo(18);
        assertThat(p.whole()).isFalse();
        assertThat(p.resolved()).startsWith("-UU-UU-UU-UU-UU-UU");
        assertThat(p.resolved()).hasSameSizeAs(line.scansion());
        assertThat(p.summary()).contains("6 daktilus").contains("19. szótagnál megszakad");
    }

    @Test
    @DisplayName("korlát: bizonyíték kell hozzá, nem engedély — a csupa közös sorra nem áll")
    void ancepsAloneIsNotEvidence() {
        // Minden lábba beleillik, tehát semmit nem bizonyít.
        assertThat(Pulse.detect("????????????")).isNull();
        // Fele eldöntött, és egyezik: ez már bizonyíték.
        assertThat(Pulse.detect("-U?-U?-U?")).isNotNull();
    }

    @Test
    @DisplayName("korlát: legalább három láb, és a sor felét fedje")
    void needsThreeFeetAndHalfTheLine() {
        assertThat(Pulse.detect("-UU-UU")).as("két daktilus kevés").isNull();
        assertThat(Pulse.detect("-UU-UU-UU")).as("három daktilus, végig").isNotNull();
        // Három trocheus egy húsz szótagos sor élén nem a sor lüktetése.
        assertThat(Pulse.detect("-U-U-U--------------")).isNull();
    }

    @Test
    @DisplayName("korlát: sorfajtát nem állítunk, és a lüktetés nem szólal meg találat mellett")
    void neverClaimsALineType() {
        // Illeszkedő mérték mellett nincs lüktetés: azt a mérték mondja meg.
        for (Examples e : Examples.ALL) {
            for (Analysis.Stanza st : Analyzer.analyze(e.text()).stanzas()) {
                for (Analysis.Line l : st.lines()) {
                    if (!l.meters().isEmpty()) {
                        assertThat(l.pulse())
                                .as("%s — „%s”: van mérték, tehát nincs lüktetés", e.title(), l.text())
                                .isNull();
                    }
                }
            }
        }
    }

    @Test
    @DisplayName("a spondeus és a pirrichius ismétlése nem lüktetés")
    void uniformQuantityIsNotAPulse() {
        assertThat(Pulse.detect("----------")).isNull();
        assertThat(Pulse.detect("UUUUUUUUUU")).isNull();
    }

    @Test
    @DisplayName("a korpusz ütemhangsúlyos sorain nem szólal meg hamisan")
    void noFalsePositivesOnTheCorpus() {
        // Zrínyi és Arany sorai magyarosak, nem időmértékesek: itt a helyes
        // válasz a hallgatás. Ha ez a teszt elbukik, a lüktetés mintakereséssé
        // vált.
        for (Examples e : java.util.List.of(Examples.SZIGETI, Examples.TOLDI)) {
            for (Analysis.Stanza st : Analyzer.analyze(e.text()).stanzas()) {
                for (Analysis.Line l : st.lines()) {
                    assertThat(l.pulse()).as("%s — „%s”", e.title(), l.text()).isNull();
                }
            }
        }
    }
}
