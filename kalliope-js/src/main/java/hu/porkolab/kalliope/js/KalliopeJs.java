package hu.porkolab.kalliope.js;

import hu.porkolab.kalliope.Analyzer;
import hu.porkolab.kalliope.Examples;
import hu.porkolab.kalliope.Json;
import hu.porkolab.kalliope.MetricCanon;
import hu.porkolab.kalliope.Scansion;
import hu.porkolab.kalliope.Settings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSObject;

/**
 * A motor böngészős belépési pontja.
 *
 * <p>A TeaVM ezt az osztályt fordítja JavaScriptre a teljes {@code
 * kalliope-core}-ral együtt, és a {@code main} lefutásakor három függvényt tesz
 * a {@code window.kalliope} alá. A visszatérési érték mindenhol <b>JSON string
 * — pontosan az, amit a REST API ad</b>: ugyanaz a {@link Json} írja, és ezt a
 * {@code JsonEquivalenceTest} a valódi HTTP-válaszhoz méri. Így a felület
 * ugyanazt a választ kapja szerverrel és szerver nélkül.
 *
 * <p>A beállításokat és a felülbírálásokat szándékosan tömör szöveges alakban
 * kapjuk, nem JSON-ként: a magban nincs JSON-<i>olvasó</i>, és nem is kell —
 * ezt a két bemenetet a felület állítja elő, a formátum belső ügy.
 */
public final class KalliopeJs {

    private KalliopeJs() {}

    public static void main(String[] args) {
        exportAnalyze(KalliopeJs::analyze);
        exportCanon(KalliopeJs::canon);
        exportExamples(KalliopeJs::examples);
    }

    /**
     * Elemzés.
     *
     * @param text a vers; az üres sor szakaszhatár
     * @param settings {@code kulcs=1;kulcs=0} alakban, vagy üresen az alapértékekhez
     * @param overrides {@code sor:szótag:jel} hármasok vesszővel, vagy üresen
     */
    static String analyze(String text, String settings, String overrides) {
        return Json.of(Analyzer.analyze(text, parseSettings(settings), parseOverrides(overrides)));
    }

    static String canon() {
        List<String> words = new ArrayList<>(MetricCanon.UNSTRESSED_WORDS);
        Collections.sort(words);
        return Json.canon(
                MetricCanon.ALL_METERS, MetricCanon.STANZAS, MetricCanon.DEFAULT_SETTINGS.asMap(), words);
    }

    static String examples() {
        return Json.examples(Examples.ALL);
    }

    private static Settings parseSettings(String raw) {
        if (raw == null || raw.isEmpty()) {
            return MetricCanon.DEFAULT_SETTINGS;
        }
        Map<String, Boolean> map = new LinkedHashMap<>();
        for (String entry : raw.split(";")) {
            int eq = entry.indexOf('=');
            if (eq > 0) {
                map.put(entry.substring(0, eq), "1".equals(entry.substring(eq + 1)));
            }
        }
        return MetricCanon.DEFAULT_SETTINGS.with(map);
    }

    private static List<Scansion.Override> parseOverrides(String raw) {
        List<Scansion.Override> out = new ArrayList<>();
        if (raw == null || raw.isEmpty()) {
            return out;
        }
        for (String entry : raw.split(",")) {
            int first = entry.indexOf(':');
            int second = entry.indexOf(':', first + 1);
            if (first > 0 && second > first && second + 1 < entry.length()) {
                out.add(new Scansion.Override(
                        Integer.parseInt(entry.substring(0, first)),
                        Integer.parseInt(entry.substring(first + 1, second)),
                        entry.charAt(second + 1)));
            }
        }
        return out;
    }

    // ---------- JS-oldali kötés ----------

    @JSFunctor
    interface Analyze extends JSObject {
        String call(String text, String settings, String overrides);
    }

    @JSFunctor
    interface Supply extends JSObject {
        String call();
    }

    @JSBody(
            params = {"fn"},
            script = "(self.kalliope = self.kalliope || {}).analyze = fn;")
    static native void exportAnalyze(Analyze fn);

    @JSBody(
            params = {"fn"},
            script = "(self.kalliope = self.kalliope || {}).canon = fn;")
    static native void exportCanon(Supply fn);

    @JSBody(
            params = {"fn"},
            script = "(self.kalliope = self.kalliope || {}).examples = fn;")
    static native void exportExamples(Supply fn);
}
