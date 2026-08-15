package hu.porkolab.kalliope.api;

import hu.porkolab.kalliope.Examples;
import hu.porkolab.kalliope.Meter;
import hu.porkolab.kalliope.MetricCanon;
import hu.porkolab.kalliope.Scansion;
import hu.porkolab.kalliope.Settings;
import hu.porkolab.kalliope.StanzaForm;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * A metrikai kánon és a felülethez tartozó szótárak. A felület ezt egyszer
 * kéri le induláskor; a beállítások leírása és a szótaghosszúság-indoklások is
 * innen jönnek, hogy a magyar szövegnek egyetlen forrása legyen (a motor).
 */
@RestController
@RequestMapping("/api")
public class CanonController {

    public record SettingInfo(String key, String label, boolean defaultValue) {}

    public record ReasonInfo(String name, String explanation) {}

    public record StanzaFormInfo(
            String id, String name, List<String> lineMeterIds, String rhymeScheme, boolean closed) {}

    public record Canon(
            String originVersion,
            String canonClosed,
            List<Meter> meters,
            List<StanzaFormInfo> stanzas,
            List<SettingInfo> settings,
            List<ReasonInfo> reasons,
            List<String> unstressedWords) {}

    @GetMapping(value = "/canon", produces = MediaType.APPLICATION_JSON_VALUE)
    public Canon canon(@RequestParam(required = false) String q) {
        List<Meter> meters = MetricCanon.search(q);
        List<StanzaFormInfo> stanzas = new ArrayList<>(MetricCanon.STANZAS.size());
        for (StanzaForm f : MetricCanon.STANZAS) {
            stanzas.add(new StanzaFormInfo(
                    f.id(), f.name(), f.lines().stream().map(Meter::id).toList(), f.rhymeScheme(), f.closed()));
        }
        List<SettingInfo> settings = new ArrayList<>();
        Map<String, Boolean> defaults = MetricCanon.DEFAULT_SETTINGS.asMap();
        defaults.forEach((key, value) -> settings.add(new SettingInfo(key, Settings.describe(key), value)));
        List<ReasonInfo> reasons = new ArrayList<>();
        for (Scansion.Reason r : Scansion.Reason.values()) {
            reasons.add(new ReasonInfo(r.name(), r.explanation()));
        }
        return new Canon(
                MetricCanon.ORIGIN_VERSION,
                MetricCanon.CANON_CLOSED,
                meters,
                stanzas,
                settings,
                reasons,
                MetricCanon.UNSTRESSED_WORDS.stream().sorted().toList());
    }

    @GetMapping(value = "/canon/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Meter meter(@PathVariable String id) {
        return MetricCanon.meter(id);
    }

    @GetMapping(value = "/examples", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Examples> examples() {
        return Examples.ALL;
    }
}
