package hu.porkolab.kalliope.api;

import hu.porkolab.kalliope.Analysis;
import hu.porkolab.kalliope.Analyzer;
import hu.porkolab.kalliope.MetricCanon;
import hu.porkolab.kalliope.Settings;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
    private static final int MAX_CHARS = 40_000;

    public record AnalyzeRequest(
            @NotBlank(message = "A vers szövege nem lehet üres.")
            @Size(max = MAX_CHARS, message = "A szöveg legfeljebb " + MAX_CHARS + " karakter lehet.")
            String text,

            Map<String, Boolean> settings) {}

    @PostMapping(value = "/analyze", produces = MediaType.APPLICATION_JSON_VALUE)
    public Analysis analyze(@Valid @RequestBody AnalyzeRequest request) {
        Settings settings = MetricCanon.DEFAULT_SETTINGS.with(request.settings());
        return Analyzer.analyze(request.text(), settings);
    }
}
