package hu.porkolab.kalliope;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * JSON-kiírás, függőség nélkül.
 *
 * <p>Miért nem a Jackson? Mert ugyanez a motor <b>böngészőben</b> is fut: a
 * {@code kalliope-js} modul TeaVM-mel JavaScriptre fordítja, és ott nincs
 * reflexió, tehát nincs Jackson sem. Ha az API Jacksont használna, a webes
 * változat pedig egy külön kézi szerializálót, akkor két JSON-alak volna, és
 * előbb-utóbb elcsúsznának. Ezért <b>egyetlen</b> szerializáló van, itt, és az
 * API is ezt hívja — amit az API mezőnkénti tesztjei ellenőriznek.
 *
 * <p>Az alak megegyezik azzal, amit a Jackson adott a rekordokra: minden
 * rekordkomponens a saját nevén, a felsorolások a {@code name()} értékükön, a
 * {@code null} pedig kiírva marad (a felület megkülönbözteti a hiányzót az
 * üres stringtől).
 */
public final class Json {

    private Json() {}

    // ---------- Nyilvános alakok ----------

    /** Egy elemzés — a {@code POST /api/analyze} válasza. */
    public static String of(Analysis a) {
        StringBuilder b = new StringBuilder(1 << 14);
        b.append('{');
        key(b, "stanzas");
        list(b, a.stanzas(), Json::stanza);
        comma(b);
        key(b, "settings");
        settings(b, a.settings());
        comma(b);
        key(b, "summary");
        summary(b, a.summary());
        comma(b);
        key(b, "verse");
        verse(b, a.verse());
        return b.append('}').toString();
    }

    /** A metrikai kánon — a {@code GET /api/canon} válasza. */
    public static String canon(
            List<Meter> meters,
            List<StanzaForm> stanzas,
            Map<String, Boolean> settingDefaults,
            List<String> unstressedWords) {
        StringBuilder b = new StringBuilder(1 << 15);
        b.append('{');
        pair(b, "originVersion", MetricCanon.ORIGIN_VERSION);
        comma(b);
        pair(b, "canonClosed", MetricCanon.CANON_CLOSED);
        comma(b);
        key(b, "meters");
        list(b, meters, Json::meter);
        comma(b);
        key(b, "stanzas");
        list(b, stanzas, Json::stanzaForm);
        comma(b);
        key(b, "settings");
        b.append('[');
        boolean first = true;
        for (Map.Entry<String, Boolean> e : settingDefaults.entrySet()) {
            if (!first) {
                comma(b);
            }
            first = false;
            b.append('{');
            pair(b, "key", e.getKey());
            comma(b);
            pair(b, "label", Settings.describe(e.getKey()));
            comma(b);
            key(b, "defaultValue");
            b.append(e.getValue().booleanValue());
            b.append('}');
        }
        b.append(']');
        comma(b);
        key(b, "reasons");
        b.append('[');
        Scansion.Reason[] reasons = Scansion.Reason.values();
        for (int i = 0; i < reasons.length; i++) {
            if (i > 0) {
                comma(b);
            }
            b.append('{');
            pair(b, "name", reasons[i].name());
            comma(b);
            pair(b, "explanation", reasons[i].explanation());
            b.append('}');
        }
        b.append(']');
        comma(b);
        key(b, "unstressedWords");
        strings(b, unstressedWords);
        return b.append('}').toString();
    }

    /** A példatár — a {@code GET /api/examples} válasza. */
    public static String examples(List<Examples> all) {
        StringBuilder b = new StringBuilder(1 << 15);
        list(b, all, (sb, e) -> {
            sb.append('{');
            pair(sb, "id", e.id());
            comma(sb);
            pair(sb, "title", e.title());
            comma(sb);
            pair(sb, "author", e.author());
            comma(sb);
            pair(sb, "expected", e.expected());
            comma(sb);
            pair(sb, "text", e.text());
            sb.append('}');
        });
        return b.toString();
    }

    /** Egyetlen mérték — a {@code GET /api/canon/{id}} válasza. */
    public static String of(Meter m) {
        StringBuilder b = new StringBuilder(256);
        meter(b, m);
        return b.toString();
    }

    // ---------- Elemzés ----------

    private static void stanza(StringBuilder b, Analysis.Stanza s) {
        b.append('{');
        pair(b, "index", s.index());
        comma(b);
        key(b, "lines");
        list(b, s.lines(), Json::line);
        comma(b);
        pair(b, "rhymePattern", s.rhymePattern());
        comma(b);
        pair(b, "rhymePatternName", s.rhymePatternName());
        comma(b);
        key(b, "forms");
        list(b, s.forms(), Json::stanzaMatch);
        comma(b);
        key(b, "accentual");
        dominant(b, s.accentual());
        comma(b);
        key(b, "dualRhythm");
        b.append(s.dualRhythm());
        b.append('}');
    }

    private static void line(StringBuilder b, Analysis.Line l) {
        b.append('{');
        pair(b, "index", l.index());
        comma(b);
        pair(b, "text", l.text());
        comma(b);
        pair(b, "scansion", l.scansion());
        comma(b);
        pair(b, "realized", l.realized());
        comma(b);
        key(b, "syllables");
        list(b, l.syllables(), Json::syllable);
        comma(b);
        key(b, "synizesis");
        b.append(l.synizesis());
        comma(b);
        key(b, "meters");
        list(b, l.meters(), Json::meterMatch);
        comma(b);
        key(b, "accentual");
        list(b, l.accentual(), Json::accentualMatch);
        comma(b);
        key(b, "nearMiss");
        nearMiss(b, l.nearMiss());
        comma(b);
        pair(b, "rhymeLabel", l.rhymeLabel());
        comma(b);
        pair(b, "rhymeKey", l.rhymeKey());
        comma(b);
        pair(b, "rhymeKind", l.rhymeKind() == null ? null : l.rhymeKind().name());
        comma(b);
        key(b, "caesurae");
        list(b, l.caesurae(), (sb, c) -> {
            sb.append('{');
            pair(sb, "afterSyllable", c.afterSyllable());
            comma(sb);
            pair(sb, "name", c.name());
            sb.append('}');
        });
        comma(b);
        key(b, "unstressedWords");
        strings(b, l.unstressedWords());
        comma(b);
        pair(b, "ictusRow", l.ictusRow());
        b.append('}');
    }

    private static void syllable(StringBuilder b, Scansion.Syllable s) {
        b.append('{');
        pair(b, "text", s.text());
        comma(b);
        pair(b, "quantity", String.valueOf(s.quantity()));
        comma(b);
        pair(b, "reason", s.reason() == null ? null : s.reason().name());
        comma(b);
        pair(b, "wordIndex", s.wordIndex());
        b.append('}');
    }

    private static void meterMatch(StringBuilder b, MeterMatcher.Match m) {
        b.append('{');
        key(b, "meter");
        meter(b, m.meter());
        comma(b);
        pair(b, "realization", m.realization());
        comma(b);
        key(b, "ictusSyllables");
        ints(b, m.ictusSyllables());
        b.append('}');
    }

    private static void accentualMatch(StringBuilder b, AccentualMatcher.Match m) {
        b.append('{');
        key(b, "form");
        accentualForm(b, m.form());
        comma(b);
        pair(b, "wordBoundaryMeasures", m.wordBoundaryMeasures());
        comma(b);
        key(b, "caesuraOnWordBoundary");
        b.append(m.caesuraOnWordBoundary());
        comma(b);
        key(b, "pure");
        b.append(m.pure());
        comma(b);
        pair(b, "quality", m.quality());
        b.append('}');
    }

    private static void accentualForm(StringBuilder b, AccentualForm f) {
        if (f == null) {
            b.append("null");
            return;
        }
        b.append('{');
        pair(b, "id", f.id());
        comma(b);
        pair(b, "name", f.name());
        comma(b);
        key(b, "measures");
        ints(b, f.measures());
        comma(b);
        pair(b, "caesuraAfter", f.caesuraAfter());
        comma(b);
        pair(b, "note", f.note());
        comma(b);
        pair(b, "division", f.division());
        b.append('}');
    }

    private static void dominant(StringBuilder b, AccentualMatcher.Dominant d) {
        if (d == null) {
            b.append("null");
            return;
        }
        b.append('{');
        key(b, "form");
        accentualForm(b, d.form());
        comma(b);
        pair(b, "strength", d.strength() == null ? null : d.strength().name());
        comma(b);
        pair(b, "cleanLines", d.cleanLines());
        b.append('}');
    }

    private static void nearMiss(StringBuilder b, NearMiss.Result r) {
        if (r == null) {
            b.append("null");
            return;
        }
        b.append('{');
        key(b, "meter");
        meter(b, r.meter());
        comma(b);
        key(b, "differences");
        list(b, r.differences(), (sb, d) -> {
            sb.append('{');
            pair(sb, "syllable", d.syllable());
            comma(sb);
            pair(sb, "actual", String.valueOf(d.actual()));
            comma(sb);
            pair(sb, "expected", String.valueOf(d.expected()));
            comma(sb);
            pair(sb, "explanation", d.explanation());
            sb.append('}');
        });
        comma(b);
        pair(b, "summary", r.summary());
        b.append('}');
    }

    private static void stanzaMatch(StringBuilder b, MeterMatcher.StanzaMatch m) {
        b.append('{');
        key(b, "form");
        b.append('{');
        pair(b, "id", m.form().id());
        comma(b);
        pair(b, "name", m.form().name());
        comma(b);
        key(b, "lines");
        list(b, m.form().lines(), Json::meter);
        comma(b);
        pair(b, "rhymeScheme", m.form().rhymeScheme());
        comma(b);
        key(b, "closed");
        b.append(m.form().closed());
        b.append('}');
        comma(b);
        pair(b, "repetitions", m.repetitions());
        comma(b);
        key(b, "rhymeSchemeMatches");
        b.append(m.rhymeSchemeMatches());
        b.append('}');
    }

    private static void summary(StringBuilder b, Analysis.Summary s) {
        b.append('{');
        pair(b, "stanzaCount", s.stanzaCount());
        comma(b);
        pair(b, "lineCount", s.lineCount());
        comma(b);
        pair(b, "syllableCount", s.syllableCount());
        comma(b);
        key(b, "meters");
        strings(b, s.meters());
        comma(b);
        key(b, "stanzaForms");
        strings(b, s.stanzaForms());
        comma(b);
        key(b, "accentualForms");
        strings(b, s.accentualForms());
        comma(b);
        pair(b, "simultaneousLines", s.simultaneousLines());
        b.append('}');
    }

    private static void verse(StringBuilder b, VerseSummary v) {
        if (v == null) {
            b.append("null");
            return;
        }
        b.append('{');
        pair(b, "system", v.system() == null ? null : v.system().name());
        comma(b);
        pair(b, "headline", v.headline());
        comma(b);
        key(b, "details");
        strings(b, v.details());
        b.append('}');
    }

    private static void settings(StringBuilder b, Settings s) {
        b.append('{');
        key(b, "sConjunctionAnceps");
        b.append(s.sConjunctionAnceps());
        comma(b);
        key(b, "letterSyllables");
        b.append(s.letterSyllables());
        comma(b);
        key(b, "explainUnstressed");
        b.append(s.explainUnstressed());
        comma(b);
        key(b, "multipleMatches");
        b.append(s.multipleMatches());
        comma(b);
        key(b, "assonanceAsRhyme");
        b.append(s.assonanceAsRhyme());
        comma(b);
        key(b, "showIctus");
        b.append(s.showIctus());
        comma(b);
        key(b, "shortWordsAnceps");
        b.append(s.shortWordsAnceps());
        comma(b);
        key(b, "allowSynizesis");
        b.append(s.allowSynizesis());
        comma(b);
        key(b, "wordFinalConsonantAnceps");
        b.append(s.wordFinalConsonantAnceps());
        comma(b);
        key(b, "wordInitialStressLengthens");
        b.append(s.wordInitialStressLengthens());
        b.append('}');
    }

    // ---------- Kánon ----------

    private static void meter(StringBuilder b, Meter m) {
        if (m == null) {
            b.append("null");
            return;
        }
        b.append('{');
        pair(b, "id", m.id());
        comma(b);
        pair(b, "name", m.name());
        comma(b);
        pair(b, "pattern", m.pattern());
        comma(b);
        pair(b, "kind", m.kind() == null ? null : m.kind().name());
        comma(b);
        key(b, "fictive");
        b.append(m.fictive());
        comma(b);
        pair(b, "note", m.note());
        comma(b);
        key(b, "correction");
        Meter.Correction c = m.correction();
        if (c == null) {
            b.append("null");
        } else {
            b.append('{');
            pair(b, "original", c.original());
            comma(b);
            pair(b, "reason", c.reason());
            comma(b);
            pair(b, "source", c.source());
            b.append('}');
        }
        b.append('}');
    }

    private static void stanzaForm(StringBuilder b, StanzaForm f) {
        b.append('{');
        pair(b, "id", f.id());
        comma(b);
        pair(b, "name", f.name());
        comma(b);
        key(b, "lineMeterIds");
        b.append('[');
        List<Meter> lines = f.lines();
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) {
                comma(b);
            }
            string(b, lines.get(i).id());
        }
        b.append(']');
        comma(b);
        pair(b, "rhymeScheme", f.rhymeScheme());
        comma(b);
        key(b, "closed");
        b.append(f.closed());
        b.append('}');
    }

    // ---------- Írás ----------

    /** Egy elem kiírója; a {@code java.util.function} nélkül, hogy TeaVM alatt is gond nélkül fusson. */
    private interface Writer<T> {
        void write(StringBuilder b, T value);
    }

    private static <T> void list(StringBuilder b, Collection<T> items, Writer<T> writer) {
        b.append('[');
        boolean first = true;
        for (T item : items) {
            if (!first) {
                comma(b);
            }
            first = false;
            writer.write(b, item);
        }
        b.append(']');
    }

    private static void strings(StringBuilder b, Collection<String> items) {
        b.append('[');
        boolean first = true;
        for (String s : items) {
            if (!first) {
                comma(b);
            }
            first = false;
            string(b, s);
        }
        b.append(']');
    }

    private static void ints(StringBuilder b, Collection<Integer> items) {
        b.append('[');
        boolean first = true;
        for (Integer i : items) {
            if (!first) {
                comma(b);
            }
            first = false;
            b.append(i.intValue());
        }
        b.append(']');
    }

    private static void pair(StringBuilder b, String name, String value) {
        key(b, name);
        string(b, value);
    }

    private static void pair(StringBuilder b, String name, int value) {
        key(b, name);
        b.append(value);
    }

    private static void key(StringBuilder b, String name) {
        string(b, name);
        b.append(':');
    }

    private static void comma(StringBuilder b) {
        b.append(',');
    }

    /**
     * Stringliterál kiírása. A JSON csak a kettős idézőjelet, a visszaperjelet
     * és a vezérlőkaraktereket kívánja escape-elni; a magyar ékezeteket és a
     * verstani jeleket (— ∪ × ‖) UTF-8-ban hagyjuk, mert a válasz úgyis UTF-8.
     */
    private static void string(StringBuilder b, String value) {
        if (value == null) {
            b.append("null");
            return;
        }
        b.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> b.append("\\\"");
                case '\\' -> b.append("\\\\");
                case '\n' -> b.append("\\n");
                case '\r' -> b.append("\\r");
                case '\t' -> b.append("\\t");
                case '\b' -> b.append("\\b");
                case '\f' -> b.append("\\f");
                default -> {
                    if (c < 0x20) {
                        b.append("\\u00");
                        b.append(HEX[(c >> 4) & 0xF]);
                        b.append(HEX[c & 0xF]);
                    } else {
                        b.append(c);
                    }
                }
            }
        }
        b.append('"');
    }

    private static final char[] HEX = "0123456789abcdef".toCharArray();
}
