package hu.porkolab.kalliope;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SettingsTest {

    @Test
    @DisplayName("minden kapcsolónak van emberi nyelvű leírása")
    void everySettingHasALabel() {
        Map<String, Boolean> all = MetricCanon.DEFAULT_SETTINGS.asMap();
        assertThat(all).hasSize(10);
        for (String key : all.keySet()) {
            String label = Settings.describe(key);
            assertThat(label).as("%s leírása", key).isNotBlank().isNotEqualTo(key);
        }
    }

    @Test
    @DisplayName("ismeretlen kulcs leírása önmaga, nem kivétel")
    void unknownKeyDescribesItself() {
        assertThat(Settings.describe("nincs_ilyen")).isEqualTo("nincs_ilyen");
    }

    @Test
    @DisplayName("a felülírás csak a megadott kapcsolót változtatja")
    void withChangesOnlyTheGivenKeys() {
        Settings base = MetricCanon.DEFAULT_SETTINGS;
        Settings changed = base.with(Map.of(Settings.SHOW_ICTUS, true));
        assertThat(changed.showIctus()).isTrue();
        assertThat(base.showIctus()).isFalse();
        assertThat(changed.sConjunctionAnceps()).isEqualTo(base.sConjunctionAnceps());
        assertThat(changed.allowSynizesis()).isEqualTo(base.allowSynizesis());
    }

    @Test
    @DisplayName("üres és null felülírás ugyanazt adja vissza")
    void emptyOverrideIsIdentity() {
        Settings base = MetricCanon.DEFAULT_SETTINGS;
        assertThat(base.with(Map.of())).isEqualTo(base);
        assertThat(base.with(null)).isEqualTo(base);
    }

    @Test
    @DisplayName("null érték a térképben nem írja felül a meglévőt")
    void nullValueKeepsTheCurrent() {
        Map<String, Boolean> overrides = new HashMap<>();
        overrides.put(Settings.SHOW_ICTUS, null);
        assertThat(MetricCanon.DEFAULT_SETTINGS.with(overrides).showIctus()).isFalse();
    }

    @Test
    @DisplayName("ismeretlen kapcsoló felülírása hibát dob")
    void unknownOverrideFails() {
        assertThat(assertThrows(
                        IllegalArgumentException.class,
                        () -> MetricCanon.DEFAULT_SETTINGS.with(Map.of("nincs_ilyen", true))))
                .hasMessageContaining("Ismeretlen beállítás");
    }

    @Test
    @DisplayName("az adatbázisból olvasott érték csak az „1” esetén igaz")
    void onlyOneIsTrue() {
        Settings s = Settings.fromDatabase(Map.of(
                Settings.SHOW_ICTUS, "1",
                Settings.MULTIPLE_MATCHES, "0",
                Settings.EXPLAIN_UNSTRESSED, "igaz"));
        assertThat(s.showIctus()).isTrue();
        assertThat(s.multipleMatches()).isFalse();
        assertThat(s.explainUnstressed()).as("a nem-boolean érték hamis").isFalse();
    }

    @Test
    @DisplayName("hiányzó kulcs esetén az alapérték érvényes")
    void missingKeyFallsBack() {
        Settings s = Settings.fromDatabase(Map.of());
        assertThat(s.sConjunctionAnceps()).isTrue();
        assertThat(s.letterSyllables()).isFalse();
        assertThat(s.wordInitialStressLengthens()).isFalse();
    }
}
