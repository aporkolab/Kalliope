package hu.porkolab.kalliope.api;

import hu.porkolab.kalliope.Analysis;
import hu.porkolab.kalliope.Analyzer;
import hu.porkolab.kalliope.MetricCanon;
import hu.porkolab.kalliope.Scansion;
import hu.porkolab.kalliope.Settings;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Az elemzés végpontja. Szándékosan nincs service-réteg a controller és a motor
 * között: a motor már a teljes üzleti logika, egy továbbadó osztály csak
 * elfedné, kit kell hibáztatni.
 */
@RestController
@RequestMapping("/api")
public class AnalyzeController {

    /** Legfeljebb ekkora szöveget fogadunk el — a motor sorkorlátja mellé. */
    private static final int MAX_CHARS = 200_000;

    /** Egy kézi szótaghosszúság-felülbírálás: „ezt a szótagot én hosszúnak olvasom”. */
    public record OverrideRequest(int line, int syllable, String quantity) {}

    public record AnalyzeRequest(
            @NotBlank(message = "A vers szövege nem lehet üres.")
            @Size(max = MAX_CHARS, message = "A szöveg legfeljebb " + MAX_CHARS + " karakter lehet.")
            String text,

            Map<String, Boolean> settings,
            List<OverrideRequest> overrides) {}

    @PostMapping(value = "/analyze", produces = MediaType.APPLICATION_JSON_VALUE)
    public Analysis analyze(@Valid @RequestBody AnalyzeRequest request) {
        Settings settings = MetricCanon.DEFAULT_SETTINGS.with(request.settings());
        return Analyzer.analyze(request.text(), settings, overrides(request));
    }

    private static List<Scansion.Override> overrides(AnalyzeRequest request) {
        if (request.overrides() == null || request.overrides().isEmpty()) {
            return List.of();
        }
        List<Scansion.Override> out = new ArrayList<>(request.overrides().size());
        for (OverrideRequest o : request.overrides()) {
            if (o.quantity() == null || o.quantity().length() != 1) {
                throw new IllegalArgumentException("A hosszúság csak U, - vagy ? lehet: " + o.quantity());
            }
            out.add(new Scansion.Override(o.line(), o.syllable(), o.quantity().charAt(0)));
        }
        return out;
    }
}
