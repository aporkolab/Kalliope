import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Kalliopé — Hungarian metrical (időmérték) + rhyme-scheme engine.
 *
 * Single self-contained Java port of the original Delphi program. The prosody
 * database (feet, cola, line/stanza meters, constants, complex meters,
 * unstressed words, settings, stored stanzas) is EMBEDDED below as DATABASE —
 * the classical/antique metrical canon is effectively fixed, so there is no
 * external file to load.
 *
 * The grammar is taken verbatim from the database header (author's own doc) and
 * cross-checked against the decompiled parser (FUN_004665ac, FUN_00466388).
 * Two front-end stages — the Hungarian text→U/- scanner and the rhyme matcher —
 * are marked RECONSTRUCTED: their exact rules lived in the binary's data
 * section, which the decompile did not carry, so they are rebuilt from standard
 * Hungarian prosody and should be verified against Kalliopé's own output.
 *
 *   Build & run:  java Kalliope.java
 */
public final class Kalliope {

    /* ===================================================================== *
     *  Metrical notation:  U short   - long   ? anceps   = "UU"|"-"          *
     *                      | foot boundary    || caesura (reminder only)     *
     * ===================================================================== */
    static final char SHORT = 'U', LONG = '-', ANCEPS = '?', RESOLVE = '=';

    static String symbolsOnly(String pattern) {
        StringBuilder sb = new StringBuilder(pattern.length());
        for (int i = 0; i < pattern.length(); i++) {
            char c = pattern.charAt(i);
            if (c == SHORT || c == LONG || c == ANCEPS || c == RESOLVE) sb.append(c);
        }
        return sb.toString();
    }

    static List<String> expand(String pattern) {
        List<String> acc = new ArrayList<>();
        acc.add("");
        for (int i = 0; i < pattern.length(); i++) {
            char c = pattern.charAt(i);
            switch (c) {
                case SHORT, LONG -> { for (int k = 0; k < acc.size(); k++) acc.set(k, acc.get(k) + c); }
                case ANCEPS -> fork(acc, "U", "-");
                case RESOLVE -> fork(acc, "UU", "-");
                default -> { }
            }
            if (acc.size() > 8192) break; // defensive guard vs. pathological ?/= blow-up (real data peaks ~32)
        }
        return acc;
    }
    private static void fork(List<String> acc, String a, String b) {
        List<String> next = new ArrayList<>(acc.size() * 2);
        for (String p : acc) { next.add(p + a); next.add(p + b); }
        acc.clear(); acc.addAll(next);
    }

    /* ===================================================================== *
     *  Data model                                                          *
     * ===================================================================== */
    enum Section { FEET, COLA, LINE_METERS, STANZA_METERS, SETTINGS, OTHER }
    record NamedMeter(String pattern, String name, String osn, boolean fictive) {}
    record ComplexMeter(String expr, String name) {}
    record Constant(String name, String value) {}
    record StanzaMeter(String formula, String name, String rhyme) {}
    record StanzaLine(String meterRef, char rhymeTag) {}
    static final class StoredStanza {
        String name = "", place = "";
        final List<StanzaLine> lines = new ArrayList<>();
    }

    static final class Database {
        final List<NamedMeter> feet = new ArrayList<>();
        final List<NamedMeter> cola = new ArrayList<>();
        final List<NamedMeter> lineMeters = new ArrayList<>();
        final List<StanzaMeter> stanzaMeters = new ArrayList<>();
        final List<Constant> constants = new ArrayList<>();
        final List<ComplexMeter> complexes = new ArrayList<>();
        final List<String> unstressedWords = new ArrayList<>();
        final Map<String, String> settings = new LinkedHashMap<>();
        final List<StoredStanza> storedStanzas = new ArrayList<>();

        Map<String, NamedMeter> byOsn() {
            Map<String, NamedMeter> m = new LinkedHashMap<>();
            for (NamedMeter n : feet) m.putIfAbsent(n.osn(), n);
            for (NamedMeter n : cola) m.putIfAbsent(n.osn(), n);
            for (NamedMeter n : lineMeters) m.putIfAbsent(n.osn(), n);
            return m;
        }
        Map<String, String> constantsByName() {
            Map<String, String> m = new LinkedHashMap<>();
            for (Constant c : constants) m.put(c.name(), c.value());
            return m;
        }
        boolean boolSetting(String key) { return "1".equals(settings.get(key)); }
        String setting(String key) { return settings.getOrDefault(key, ""); }
    }

    /* ===================================================================== *
     *  osn — name up to first '(', trimmed, spaces → underscores.           *
     * ===================================================================== */
    static String osnOf(String name) {
        // Latent: a name containing '/' (e.g. "hendecasyllabus/B") yields an osn
        // with '/', which split("/") in a formula would break. Not triggered by
        // the current data (such meters are never referenced in formulas).
        int paren = name.indexOf('(');
        String s = (paren >= 0 ? name.substring(0, paren) : name).trim();
        return s.replace(' ', '_');
    }
    static boolean isFictive(String name) {
        String n = name.toLowerCase();
        return n.contains(".fictive") || n.contains(".fiktiv");
    }

    /* ===================================================================== *
     *  Parser                                                              *
     * ===================================================================== */
    private static final Pattern TWO_QUOTED = Pattern.compile("\"([^\"]*)\"\\s+\"([^\"]*)\"");
    private static final Pattern RIMKEPLET  = Pattern.compile("\\brimkeplet=(\\S+)");
    private static final Pattern RIM_TAG    = Pattern.compile("\\brim=(\\S)");

    static Database parse(String text) {
        List<String> lines = List.of(text.split("\n", -1));
        Database db = new Database();
        Section section = Section.OTHER;
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.isBlank()) continue;
            char c = line.charAt(0);
            if (c == ';') continue;
            if (c == '.') {
                if (line.trim().equals(".")) continue;
                section = classify(line.substring(1));
                continue;
            }
            if (c == '!') { putSetting(db, line); continue; }
            if (c == '$') { db.unstressedWords.add(line.substring(1).trim()); continue; }
            if (c == '@') { db.constants.add(constant(line.substring(1))); continue; }
            if (c == ':') { db.stanzaMeters.add(stanzaMeter(line.substring(1))); continue; }
            if (line.startsWith("#define")) { db.constants.add(constant(line.substring(7))); continue; }
            if (line.startsWith("#complex")) {
                Matcher m = TWO_QUOTED.matcher(line);
                if (m.find()) db.complexes.add(new ComplexMeter(m.group(1), m.group(2)));
                continue;
            }
            if (line.startsWith("#start_strofa")) { i = readStoredStanza(lines, i, db); continue; }
            if (c == '#') continue;
            if (c == SHORT || c == LONG || c == ANCEPS || c == RESOLVE) {
                NamedMeter nm = patternEntry(line);
                switch (section) {
                    case FEET -> db.feet.add(nm);
                    case COLA -> db.cola.add(nm);
                    default  -> db.lineMeters.add(nm);
                }
            }
        }
        return db;
    }

    private static Section classify(String header) {
        String h = header.toLowerCase();
        if (h.contains("versláb")) return Section.FEET;
        if (h.contains("koló")) return Section.COLA;
        if (h.contains("sorfajták")) return Section.LINE_METERS;
        if (h.contains("strófák") || h.contains("sortömb")) return Section.STANZA_METERS;
        if (h.contains("beallitasok")) return Section.SETTINGS;
        return Section.OTHER;
    }
    private static void putSetting(Database db, String line) {
        String body = line.substring(1);
        int eq = body.indexOf('=');
        if (eq < 0) { db.settings.put(body.trim(), ""); return; }
        db.settings.put(body.substring(0, eq).trim(), body.substring(eq + 1).trim());
    }
    private static Constant constant(String body) {
        String s = body.strip();
        int sp = firstSpace(s);
        if (sp < 0) return new Constant(s, "");
        return new Constant(s.substring(0, sp), s.substring(sp).strip());
    }
    private static NamedMeter patternEntry(String line) {
        int sp = firstSpace(line);
        String pattern = sp < 0 ? line.trim() : line.substring(0, sp);
        String name = sp < 0 ? "" : line.substring(sp).strip();
        return new NamedMeter(pattern, name, osnOf(name), isFictive(name));
    }
    private static StanzaMeter stanzaMeter(String body) {
        String s = body.strip();
        String formula, rest;
        if (s.startsWith("#")) {
            int close = s.indexOf('#', 1);
            formula = close >= 0 ? s.substring(0, close + 1) : s;
            rest = close >= 0 ? s.substring(close + 1).strip() : "";
        } else {
            int sp = firstSpace(s);
            formula = sp < 0 ? s : s.substring(0, sp);
            rest = sp < 0 ? "" : s.substring(sp).strip();
        }
        String rhyme = "";
        Matcher m = RIMKEPLET.matcher(rest);
        if (m.find()) { rhyme = m.group(1); rest = m.replaceAll("").strip(); }
        return new StanzaMeter(formula, rest, rhyme);
    }
    private static int readStoredStanza(List<String> lines, int start, Database db) {
        StoredStanza st = new StoredStanza();
        int i = start + 1;
        for (; i < lines.size(); i++) {
            String l = lines.get(i);
            if (l.startsWith("#end_strofa")) break;
            if (l.isBlank() || l.trim().equals(".")) continue;
            if (l.startsWith("%name="))  { st.name  = l.substring(6).trim(); continue; }
            if (l.startsWith("%place=")) { st.place = stripQuotes(l.substring(7).trim()); continue; }
            if (l.startsWith(";")) continue;
            char tag = 0; String ref = l;
            Matcher m = RIM_TAG.matcher(l);
            if (m.find()) { tag = m.group(1).charAt(0); ref = l.substring(0, m.start()).strip(); }
            st.lines.add(new StanzaLine(ref, tag));
        }
        db.storedStanzas.add(st);
        return i;
    }
    private static int firstSpace(String s) {
        for (int i = 0; i < s.length(); i++) if (Character.isWhitespace(s.charAt(i))) return i;
        return -1;
    }
    private static String stripQuotes(String s) {
        if (s.length() >= 2 && s.charAt(0) == '"' && s.charAt(s.length() - 1) == '"')
            return s.substring(1, s.length() - 1);
        return s;
    }

    /* ===================================================================== *
     *  Formula resolution                                                  *
     * ===================================================================== */
    static List<String> resolveFormula(Database db, String formula) {
        String f = formula;
        if (f.startsWith("#") && f.endsWith("#") && f.length() >= 2) f = f.substring(1, f.length() - 1);
        Map<String, String> consts = db.constantsByName();
        Map<String, NamedMeter> meters = db.byOsn();
        List<String> out = new ArrayList<>();
        for (String tok : f.split("/")) {
            tok = tok.strip();
            if (!tok.isEmpty()) resolveToken(tok, consts, meters, out, 0);
        }
        return out;
    }
    private static void resolveToken(String tok, Map<String, String> consts,
                                     Map<String, NamedMeter> meters, List<String> out, int depth) {
        if (depth > 64) { out.add("<cycle:" + tok + ">"); return; }
        if (consts.containsKey(tok)) {
            for (String sub : consts.get(tok).split("/")) {
                sub = sub.strip();
                if (!sub.isEmpty()) resolveToken(sub, consts, meters, out, depth + 1);
            }
        } else if (meters.containsKey(tok)) {
            out.add(meters.get(tok).pattern());
        } else {
            out.add("<?" + tok + ">");
        }
    }

    /* ===================================================================== *
     *  Matcher                                                             *
     * ===================================================================== */
    /** Resolve a #complex meter ("part1+part2+…") to one concatenated pattern:
     *  each part is a component meter name (by osn) or already a raw pattern. */
    static String resolveComplex(Database db, ComplexMeter cm) {
        Map<String, NamedMeter> byOsn = db.byOsn();
        Map<String, NamedMeter> byName = new LinkedHashMap<>();
        for (NamedMeter n : db.lineMeters) byName.put(n.name(), n);
        for (NamedMeter n : db.cola) byName.put(n.name(), n);
        for (NamedMeter n : db.feet) byName.put(n.name(), n);
        StringBuilder sb = new StringBuilder();
        for (String part : cm.expr().split("\\+")) {
            String p = part.strip();
            if (p.startsWith("||")) { sb.append("||"); p = p.substring(2).strip(); }
            NamedMeter m = byOsn.get(osnOf(p));
            if (m == null) m = byName.get(p);
            if (m != null) sb.append(m.pattern());
            else sb.append(p); // already a raw U/-/? pattern (e.g. "--|--|U-|?")
        }
        return sb.toString();
    }
    /** Line meters (and #complex meters) whose realization the scanned line fits.
     *  Strict: long<->long, short<->short, közös (?)<->either. */
    static List<NamedMeter> matchLine(Database db, String scanned) {
        Set<String> scanSet = new HashSet<>(expand(scanned)); // scanned may contain '?'
        List<NamedMeter> hits = new ArrayList<>();
        for (NamedMeter m : db.lineMeters) {
            for (String r : expand(m.pattern())) if (scanSet.contains(r)) { hits.add(m); break; }
        }
        for (ComplexMeter cm : db.complexes) {
            String pat = resolveComplex(db, cm);
            for (String r : expand(pat)) if (scanSet.contains(r)) {
                hits.add(new NamedMeter(pat, cm.name(), osnOf(cm.name()), isFictive(cm.name()))); break;
            }
        }
        return hits;
    }


    /* ===================================================================== *
     *  Hungarian phonology                                                 *
     * ===================================================================== */
    static final String LONG_V  = "áéíóőúű";
    static final String SHORT_V = "aeiouöü";
    static final String[] DIGRAPHS = {"dzs", "cs", "gy", "ly", "ny", "sz", "ty", "zs", "dz"};

    static boolean isVowel(char c) {
        char l = Character.toLowerCase(c);
        return LONG_V.indexOf(l) >= 0 || SHORT_V.indexOf(l) >= 0;
    }
    static boolean isLongVowel(char c) { return LONG_V.indexOf(Character.toLowerCase(c)) >= 0; }
    static boolean isConsonantLetter(char c) { return Character.isLetter(c) && !isVowel(c); }

    /* ===================================================================== *
     *  RECONSTRUCTED front-end #1 — text → U/- scansion.                    *
     *  Vowel length is phonemic (á é í ó ő ú ű long); a short vowel closed  *
     *  by ≥2 consonants is long by position; digraphs count as ONE          *
     *  consonant. Rebuilt from standard Hungarian prosody, NOT the binary — *
     *  verify edge cases against Kalliopé.                                  *
     * ===================================================================== */
    /** Scan a single word into U/- by its natural + within-word positional length
     *  (no line-level rules). Utility; scanLine below is the real scanner. */
    static String scanWord(String word) {
        String w = word.toLowerCase();
        StringBuilder ul = new StringBuilder();
        List<Integer> vp = new ArrayList<>();
        for (int i = 0; i < w.length(); i++) if (isVowel(w.charAt(i))) vp.add(i);
        for (int vi = 0; vi < vp.size(); vi++) {
            char v = w.charAt(vp.get(vi));
            if (isLongVowel(v)) { ul.append(LONG); continue; }
            ul.append(hardConsAfter(w, vp.get(vi) + 1) >= 2 ? LONG : SHORT);
        }
        return ul.toString();
    }

    /* Text normalization EXTRACTED FROM kalliope.exe (FUN_00462a94): expand
     * abbreviations and standalone consonant letters to their pronounceable
     * forms so they can be syllabified. These pairs are the binary's own data.
     * (The binary also confirmed the digraph list and the muta-cum-liquida
     * stop/liquid sets used below match this port exactly.) */
    static final String[][] NORMALIZE = {
        {" vc ", " vécé "}, {" tv ", " tévé "}, {" cd ", " cédé "},
        {"gazság ", "gasság "}, {"cság ", "csség "},
        {" b ", " bé "}, {" c ", " cé "}, {" d ", " dé "}, {" f ", " eff "},
        {" g ", " gé "}, {" h ", " há "}, {" j ", " jé "}, {" k ", " ká "},
        {" l ", " ell "}, {" m ", " emm "}, {" n ", " enn "}, {" p ", " pé "},
        {" q ", " kú "}, {" r ", " err "}, {" t ", " té "}, {" v ", " vé "},
        {" x ", " iksz "}, {" z ", " zé "},
    };
    /** Apply the binary's normalization table (lowercased, space-padded). */
    static String normalize(String line) {
        String s = " " + line.toLowerCase().replace('w', 'v') + " ";
        for (String[] p : NORMALIZE) {
            int i;
            while ((i = s.indexOf(p[0])) >= 0)
                s = s.substring(0, i) + p[1] + s.substring(i + p[0].length());
        }
        return s.trim();
    }

    /**
     * Scan a whole line into U / - / ?, per sourced Hungarian rules
     * (Fazekas-enciklopédia, Magyartanár, Pannon Enciklopédia):
     *  - a syllable spans to the next vowel, CROSSING word boundaries;
     *  - long vowel => long; short vowel + >=2 hard consonants => long;
     *  - ANCEPS ('?') at "közös" positions:
     *      * muta cum liquida (stop p/t/k/b/d/g + liquid r/l) as the sole cluster,
     *      * the "s" / "és" clitic counted as an OPTIONAL consonant
     *        (governed by the setting az_s_kotoszo_kozombos),
     *      * the definite article "a"/"az" (standalone word),
     *      * the line-final syllable (brevis in longo).
     */
    static String scanLine(String line, boolean sNeutral) {
        // Build a letter stream with per-char flags for the neutral "s" clitic
        // and the article, keeping word boundaries as spaces.
        StringBuilder cs = new StringBuilder();
        List<Boolean> soft = new ArrayList<>(); // char belongs to a neutral s/és clitic
        List<Boolean> art  = new ArrayList<>(); // char belongs to article a/az
        for (String tokRaw : normalize(line).split("\\s+")) {
            String t = tokRaw.replaceAll("[^a-záéíóőúűöü]", "");
            if (t.isEmpty()) continue;
            boolean sClit   = sNeutral && (t.equals("s") || t.equals("és"));
            boolean article = t.equals("a") || t.equals("az");
            for (int i = 0; i < t.length(); i++) { cs.append(t.charAt(i)); soft.add(sClit); art.add(article); }
            cs.append(' '); soft.add(false); art.add(false);
        }
        String w = cs.toString();
        List<Integer> vp = new ArrayList<>();
        for (int i = 0; i < w.length(); i++) if (isVowel(w.charAt(i))) vp.add(i);

        StringBuilder out = new StringBuilder();
        for (int vi = 0; vi < vp.size(); vi++) {
            int p = vp.get(vi);
            char v = w.charAt(p);
            if (vi == vp.size() - 1)          { out.append(ANCEPS); continue; } // brevis in longo
            if (art.get(p) && !isLongVowel(v)) { out.append(ANCEPS); continue; } // a / az
            if (isLongVowel(v))                { out.append(LONG);   continue; }

            // gather consonant phonemes until the next vowel (digraph = 1), with softness
            List<Character> cons = new ArrayList<>();
            List<Boolean> csoft = new ArrayList<>();
            int i = p + 1;
            while (i < w.length()) {
                char c = w.charAt(i);
                if (isVowel(c)) break;
                if (c == ' ') { i++; continue; }
                int adv = 1; char rep = c;
                for (String d : DIGRAPHS) if (w.regionMatches(i, d, 0, d.length())) { adv = d.length(); rep = d.charAt(0); break; }
                cons.add(rep); csoft.add(soft.get(i));
                i += adv;
            }
            int total = cons.size(), hard = 0;
            for (boolean s : csoft) if (!s) hard++;
            boolean mutaLiquida = (total == 2 && isStop(cons.get(0)) && isLiquid(cons.get(1)));

            if (mutaLiquida)            out.append(ANCEPS); // positio debilis
            else if (hard >= 2)         out.append(LONG);
            else if (total >= 2)        out.append(ANCEPS); // reached 2 only via the soft "s"
            else                        out.append(SHORT);
        }
        return out.toString();
    }
    /** Hard-consonant count after a position, within/across text; digraph = 1. */
    private static int hardConsAfter(String w, int from) {
        int count = 0, i = from;
        while (i < w.length()) {
            char c = w.charAt(i);
            if (isVowel(c)) break;
            if (!(Character.isLetter(c) && !isVowel(c))) { i++; continue; }
            int adv = 1;
            for (String d : DIGRAPHS) if (w.regionMatches(i, d, 0, d.length())) { adv = d.length(); break; }
            count++; i += adv;
        }
        return count;
    }
    private static boolean isStop(char c)   { return "ptkbdg".indexOf(c) >= 0; }
    private static boolean isLiquid(char c) { return c == 'r' || c == 'l'; }

    /* ===================================================================== *
     *  RECONSTRUCTED front-end #2 — rhyme-scheme (rímképlet) detector.      *
     *  Approximates Hungarian end-rhyme as "from the last vowel onward";    *
     *  with assonance on, compares vowel skeletons only. Greedy letter      *
     *  assignment (a, b, c…). The binary's exact predicate (and the         *
     *  assonance/anceps tables) were not in the decompile — verify.         *
     * ===================================================================== */
    /* Rhyme-consonant normalization EXTRACTED FROM kalliope.exe (FUN_00468788):
     * collapse phonologically near consonants so near-rhymes match. Devoicing /
     * merges are applied PHONEME-wise (digraphs like gy, ty are one unit and are
     * left intact); the cluster rules run afterwards. This is the substance of
     * the binary's assonance / near-rhyme recognition. */
    private static final Map<String, String> DEVOICE = Map.of(
        "b", "p", "d", "t", "g", "k", "r", "l", "m", "n");     // digraphs are not keys → untouched
    private static final String[][] CLUSTER_NORM = {
        {"mf", "mv"}, {"nt", "nn"}, {"lt", "tt"}, {"nk", "ń"}, {"ól", "ol"}, {"űl", "ül"},
    };
    /** Rhyme key of a line, faithful to the binary: normalize pronunciation,
     *  take the ending from the last vowel; with assonance on keep only the
     *  vowel skeleton, otherwise phoneme-devoice/merge the consonants. */
    static String rhymeKey(String line, boolean assonanceAsRhyme) {
        String w = normalize(line);
        int lastV = -1;
        for (int i = w.length() - 1; i >= 0; i--) if (isVowel(w.charAt(i))) { lastV = i; break; }
        if (lastV < 0) return "";
        // tokenize the ending into phonemes (digraph = one unit)
        List<String> ph = new ArrayList<>();
        for (int i = lastV; i < w.length(); ) {
            char c = w.charAt(i);
            if (isVowel(c)) { ph.add(String.valueOf(c)); i++; continue; }
            if (!isConsonantLetter(c)) { i++; continue; }
            int adv = 1; String rep = String.valueOf(c);
            for (String dg : DIGRAPHS) if (w.regionMatches(i, dg, 0, dg.length())) { adv = dg.length(); rep = dg; break; }
            ph.add(rep); i += adv;
        }
        if (assonanceAsRhyme) {                        // binary blanks consonants → vowels only
            StringBuilder v = new StringBuilder();
            for (String p : ph) if (p.length() == 1 && isVowel(p.charAt(0))) v.append(p);
            return v.toString();
        }
        StringBuilder sb = new StringBuilder();
        for (String p : ph) sb.append(DEVOICE.getOrDefault(p, p)); // digraphs pass through
        String s = sb.toString();
        for (String[] c : CLUSTER_NORM) s = s.replace(c[0], c[1]);
        return s;
    }
    static String rhymeScheme(List<String> lines, boolean assonanceAsRhyme) {
        List<String> keys = new ArrayList<>();
        StringBuilder scheme = new StringBuilder();
        char next = 'a';
        for (String line : lines) {
            String key = rhymeKey(line, assonanceAsRhyme);
            int found = -1;
            for (int j = 0; j < keys.size(); j++) if (keys.get(j).equals(key)) { found = j; break; }
            if (found >= 0) scheme.append(scheme.charAt(found));
            else { scheme.append(next); next++; }
            keys.add(key);
        }
        return scheme.toString();
    }

    /** End-to-end: scan each line, meter-guided match, detect the rhyme scheme. */
    static void analyze(Database db, String poem) {
        List<String> lines = new ArrayList<>();
        for (String l : poem.strip().split("\n")) if (!l.isBlank()) lines.add(l.strip());
        boolean sN = db.boolSetting("az_s_kotoszo_kozombos");
        String scheme = rhymeScheme(lines, db.boolSetting("az_asszonanc_rimkent_valo_kezelese"));
        for (int i = 0; i < lines.size(); i++) {
            String scan = scanLine(lines.get(i), sN);
            List<NamedMeter> hits = matchLine(db, scan); // strict: long<->long, short<->short, ?<->either
            String info = hits.isEmpty() ? "" : "= " + hits.get(0).name();
            System.out.printf("  [%c] %-46s %-20s %s%n", scheme.charAt(i), lines.get(i), scan, info);
        }
        System.out.println("  rímképlet: " + scheme);
    }

    /* ===================================================================== *
     *  Self-test / demo                                                    *
     * ===================================================================== */
    public static void main(String[] args) {
        Database db = parse(DATABASE);
        System.out.println("=== Kalliopé (embedded database) ===");
        System.out.printf("  feet %d | cola %d | line meters %d | stanza meters %d%n",
                db.feet.size(), db.cola.size(), db.lineMeters.size(), db.stanzaMeters.size());
        System.out.printf("  constants %d | complex %d | unstressed %d | stored stanzas %d | settings %d%n",
                db.constants.size(), db.complexes.size(), db.unstressedWords.size(),
                db.storedStanzas.size(), db.settings.size());

        System.out.println("\n=== notation ===");
        for (String p : new String[]{"-=", "-?"})
            System.out.printf("  %-4s -> %s%n", p, expand(p));

        System.out.println("\n=== resolve #Szept_vegen_1212_1212# ===");
        for (String pat : resolveFormula(db, "#Szept_vegen_1212_1212#")) System.out.println("      " + pat);

        System.out.println("\n=== scan + match (hexameter realization) ===");
        String hex = "-UU-UU-UU-UU-UU--";
        List<NamedMeter> h = matchLine(db, hex);
        System.out.printf("  %s -> %s%n", hex, h.isEmpty() ? "(none)" : h.get(0).name());

        System.out.println("\n=== stored stanzas: rhyme readout ===");
        for (StoredStanza st : db.storedStanzas) {
            StringBuilder s = new StringBuilder();
            for (StanzaLine l : st.lines) s.append(l.rhymeTag() == 0 ? '-' : l.rhymeTag());
            System.out.printf("  %-10s (%d) rím: %s%n", st.name, st.lines.size(), s);
        }

        System.out.println("\n=== Szigeti veszedelem (Zrínyi) — rímes, hangsúlyos strófa ===");
        analyze(db, SZIGETI);
        System.out.println("\n=== Iliász (Devecseri ford.) — hexameter ===");
        analyze(db, ILIASZ);

        System.out.println("\n=== rím-felismerés (kalliope.exe normalizáló táblájával) ===");
        for (String[] pr : new String[][]{{"kard","part"},{"hegy","megy"},{"bot","sok"}}) {
            String ka = rhymeKey(pr[0], false), kb = rhymeKey(pr[1], false);
            System.out.printf("  %-6s / %-6s  kulcs %-4s / %-4s  %s%n",
                pr[0], pr[1], ka, kb, ka.equals(kb) ? "RÍMEL" : "nem rímel");
        }

        System.out.println("\n=== normalizálás (kalliope.exe táblájából) ===");
        for (String t : new String[]{"A tv meg a cd", "x y z", "gazság"})
            System.out.printf("  %-16s -> %-22s  %s%n", t, normalize(t), scanLine(t, true));
    }

    // --- Demo texts (user-provided) ---
    /** Zrínyi Miklós: Szigeti veszedelem, I. ének (1651). Felező tizenkettes,
     *  hangsúlyos-magyaros vers, aaaa rím — NEM időmértékes, tehát a klasszikus
     *  metrumillesztő helyesen NEM ad rá találatot; a rímdetektornak aaaa a dolga. */
    static final String SZIGETI = """
            Fegyvert, s vitézt éneklek, török hatalmát,
            Ki meg merte várni, Szulimán haragját,
            Ama nagy Szulimánnak hatalmas karját,
            Az kinek Europa rettegte szablyáját.
            """;
    /** Homérosz: Iliász, I. ének (Devecseri Gábor ford.). Daktilikus hexameter,
     *  rímtelen — a metrumillesztő terepe; a rímdetektornak csupa különböző sor. */
    static final String ILIASZ = """
            Haragot, istennő zengd Péleidész Akhileuszét,
            vészest, mely sokezer kínt szerzett minden akhájnak,
            mert sok hősnek erős lelkét Hádészra vetette,
            míg őket magukat zsákmányul a dögmadaraknak
            és a kutyáknak dobta. Betelt vele Zeusz akaratja,
            attól kezdve, hogy egyszer szétváltak civakodva
            Átreidész, seregek fejedelme s a fényes Akhilleusz.
            """;

    /* ===================================================================== *
     *  EMBEDDED DATABASE (was kalliope.txt) — the metrical canon.          *
     * ===================================================================== */
    static final String DATABASE = """
;                           Kalliope
;              VNP időmérték-matatgató programja            
;                                                           
;  Ennek a fájlnak "kalliope.txt" kell legyen a neve, és a program
;mellett, azzal egy könyvtárban kell legyen.
;  Beta verzió: a program néha csinál nagy hülyeségeket. 
;Ezért nem árt tudni is pár dolgot, és nem 100%-ig megbízni
;Kalliopéban. Bár a múzsák hazudni is tudnak, Kalliope esetleg
;tévedni szokott, néha... szóval beta verzió.
;       
;  U: rövid szótag,  
;  -: hosszú szótag,  
;  ?: hosszú vagy rövid szótag, mondjuk: közönbös,
;  =: "UU"-nek vagy "-"-nek megfelelő rész. Így a 
;          "-="-t a program  "--"-ként vagy "-UU"-ként kezeli.
;  A cezúrát sajnos nem volt értelme jelöljem - szemantika!
;  (a mérték csak emlékül tartalmazza a "||" jelet)
;
;  a "-" vagy "U" kezdetű sorok sormértékek
;  a ":" kezdetű sorok szakaszmértéket jelentenek
;  a ";" kezdetű sorok kiesnek a számításból, kommentár
;  A "." kezdetű sorok mezőmegnevezések.
;  A "!" kezdetű sorok beállítások.
;        Boolean ertekek: "1"=igaz, "0"=hamis
;  A "@" kezdetű sorok kesőbb behelyettesíthető
;        konstansok. Szintaxisa:
;        [konstansnev][szokoz][a konstans erteke]
;        Tulajdonképpen a nagy szakaszok felépítését
;        teszi lehetővé azáltal, hogy tömbösíthetővé
;        teszi a szakasz sorait - rövid, áttekinthető
;        ini-sorok. A konstansok neveivel vigyázni,
;        egyediek legyenek!!!!!!!
;
;        Ilyet, ha nagyon muszáj, deklarálhatsz beállítás-
;        változóként is: ![nev]=[ertek]
;        Vagy ha még hülyébb vagy, C-ben...
;              #define konstansnev ertek 
;
;        #complex[szokoz]"[mertek1][plusz][mertek2]"[szokoz]"[nev]"
;
;
;Forma:
;  sorfajta:
;    [sorkeplet, nincs szokoz][szokoz][nev, tartalmazhat szokozt]
;  szakaszfajta (aminek a használatát kérem kerülni):
;    [kettospont][sorkeplet1/sorkeplet2/.../sorkepletN/][szokoz][nev]
;  szakaszfajta:
;    [kettospont][csillag][sornev1/sornev2/.../sornevN/][szokoz][nev]
;   A [nev] jó ha nem tartalmaz ékezetet, meg egyéb hókuszpókusz-
;karaktereket. Hasonlóan a beállítás-sorok változónév része.
;Ahol mégis ékezet van, az az én bajom.
;   A strófákban a sorok ne szerepeljenek ütemképlettel, 
;mert irtóra lassítja a pogramot! A strófákat előzőleg deklarált 
;sorokból kell összerakni - hogy gyorsabb legyen a program.
;    A "/" normál sortörést jelent.
;    A "#" kezdő és lezáró üres sor - a valódi szakaszt üres sorok
;zárják körül. A disztichon viszont nem szakasz, ezért köré nem kell
;a két diez. Vagyis a ":hexameter/pentameter disztochon" a helyes. 
;Egy sor a strófában az optimalizált sornevet jelenti (osn). Vagyis "osn":
;             az osn a sornévből az első zárójelig tart
;             az osn elején és végén nincs szóköz sem alulvonás
;             az osn minden szóköze alulvonásra változik ("_")
;    Tehát a "nagy alkaioszi (alk. 11) alk. 1,2" osn-je "nagy_alkaioszi"
;
;
;-------------------------------------------------------
.Verslábak (egyedül sorok is lehetnének persze):
-                    ta lab (nem éppen láb ez)
U                    ti lab (nem éppen láb ez) 
U-                   jambus lab
-U                   trocheus lab
-UU                  daktilus lab
UU-                  anapesztus lab
--                   spondeusz lab
UU                   pirrichius lab
---                  molosszus lab
UUU                  tribrachisz lab
UUUU                 proceleuzmatikus lab
;-------------------------------------------------------
.Kolónok (ezek is lehetnének sorok):
;;3szotagos
U--                  bacchius kolon
--U                  palimbacchius kolon
-U-                  kretikus kolon
U-U                  amphibrachisz kolon
;
;;4szotagos
-UUU                 1. paion kolon
U-UU                 2. paion kolon
UU-U                 3. paion kolon
UUU-                 4. paion kolon
;
U---                 1. epitritus kolon
-U--                 2. epitritus kolon
--U-                 3. epitritus kolon
---U                 4. epitritus kolon
; 
UU--                 ionicus a minore kolon
--UU                 ionicus a maiore kolon
-UU-                 choriambus kolon
U--U                 antiszpasztus kolon
;
;;5szotagos
?--U-                dochmius
-UU-?                adoniszi kolon (sapphoi 4.)
-U-U-                hipodochmius kolon (???) 
;;6szotagos
?-UU--               fejetlen pherekrateus kolon (reiziánus)
-U-U--               ithüphallikus kolon (???)
---UU-               Maecenas atavis-kolon
;;7szotagos
-UU-UU-              hémiepesz kolon
-U-U-U-              léküthion kolon
?-UU-U?              téleszilleion kolon (???)
???-UU-              fejetlen wilamovitziánus kolon (???)
?-UU-U-              fejetlen glükóni kolon
??-UU--              pherekrateus kolon
;;8szotagos 
????-UU-             wilamovitziánus kolon (???)
?-UU-UU-             proszodiákus kolon (???)
??-UU-U-             glükóni kolon
---UU-U-             második glükóni kolon
-UU-UU-?             hémiepesz kolon2
?-UU-U-?             fejetlen hipponakteus kolon
;;9szotagos 
?-UU-UU-?            enaszmonideus kolon (???)
?-UU-UU--            enoplion kolon
??-UU-U-?            hipponakteus kolon
;;12szotagos
-UU-UU-||--U-?       Enkomiologikon kolon
;
;;Benji kezirasa olvashatatlan (???)
;-------------------------------------------------------
.Sorfajták:
-=|-=|-=|-=|-UU|-?   hexameter
-=|-=|-||-UU|-UU|?   pentameter
UU-UU-U-U--          phalaikoszi
U--UU-U-U-?          hendecasyllabus/B
-?-UU-U-U-?          hendecasyllabus/A
?-U-?-U-U-U?         choliambus
-U-U-U-U-U-U-U-      4mtr trochaicus
-U--U-||-UU-U-?      francia alexandrin A
U-U---||---UU?       francia alexandrin B
U---U?||U--UU-?      francia alexandrin C
U---U-||U--UU?       francia alexandrin D
?-?-U--              nib.alex.1.fiktiv
-U---||UU-U-?        szapphói sor
U-U-U-U-U-U?         trimeter iambicus
?-U-?-U-?-U-         trimeter iambicus
;;U-U-U-U-U-U?        parti nagy alexandrin (iamb trim?)
;;glykoni...............................................
-?-UU-U-             glykoni1a
U--UU-U-             glykoni1b
-?-UU-U              glykoni2a
U--UU-U              glykoni2b
;;aszklepiadeszi........................................
---UU--UU-U?         asklepiadesi A123 (kis)
---UU-U?             asklepiadesi B4
---UU--              asklepiadesi C3
---UU-?              asklepiadesi D13
---UU--UU--UU-U?     asklepiadesi E1234
;;alkaioszi.............................................
?-U-?||-UU-U?        alkaiosi 12 (alk. 11, nagy alk.)
?-U-U-U-?            alkaiosi 3 (5ötfeles jamb)
-UU-UU-U-?           alkaiosi 4 (kis alk., alk-i 10-es)
;;anakreoni.............................................
UU-U-U-?             anakreoni 8
U-U-U-?              anakreoni 7 (negyedfeles jambikus)
UU--|UU--||UU--|UU-- anakreoni 16 (4db ion. a min.)
;;szeptember vegen......................................
?-UU-UU-UU-?         Szept_vegen1
?-UU-UU-UU?          Szept_vegen2
@Szept_vegen_12        Szept_vegen1/Szept_vegen2
@Szept_vegen_1212      Szept_vegen_12/Szept_vegen_12
@Szept_vegen_1212_1212 Szept_vegen_1212/Szept_vegen_1212
;;igy is lehetne, ;; nelkul persze:
;;#define Szept_vegen_12        Szept_vegen1/Szept_vegen2
;;#define Szept_vegen_1212      Szept_vegen_12/Szept_vegen_12
;;#define Szept_vegen_1212_1212 Szept_vegen_1212/Szept_vegen_1212
;
;;Ket adoniszi:....................................
#complex "adoniszi kolon+adonisi kolon" "bi_adoniszi.fictive"
;;Nibelungizalt alexandrin
#complex "nib.alex.1.fictive+||--|--|U-|?" "nibelungizált alexandrin"
#complex "nib.alex.1.fictive+||--|U-|--|?" "nibelungizált alexandrin"
#complex "nib.alex.1.fictive+||-U|--|U?"   "nibelungizált alexandrin"
#complex "nib.alex.1.fictive+||U-|U-|U?"   "nibelungizált alexandrin"
#complex "nib.alex.1.fictive+||--|U-|U-"   "nibelungizált alexandrin"
#complex "nib.alex.1.fictive+||--|--|U-"   "nibelungizált alexandrin"
;
;-------------------------------------------------------
.VNP sorfajták:
U-UU-UU-?             Gyilkosok
UU-U-UU-?             Éhesek 
-U-U-U-U              Pincsike1.a
-U-U-U?               Pincsike1.b
UU-UU-UU-             melyegi_alom_1
UU-UU-UU-UU-          melyegi_alom_2
--U-UU--U-            utolso mosas 1
U--U--UU-U            utolso mosas 2
---------             utolso mosas 3
?-?-U-U--             létépartja
-UU---UUUU---UUUU---UUUU---UUUU---UU- hal_éji_éneke
#complex "melyegi_alom_1+melyegi_alom_2" "melyegi_alom_nagysor"
;;2. paion+adoniszi = gyilkosok ~ enoplion/enaszmonideus
;;3. paion+adoniszi = éhesek
;(kiegészítés-fitkivek)
-U-U                  bi_trocheus.fictive
U-U-                  bi_jambus.fictive
----                  bi_spondeusz.fictive
-UU-UU                bi_daktilus.fictive
UU-UU-                bi_anapesztus.fictive
;
;-------------------------------------------------------
.Strófák és sortömbök:
#define asklepiadesi_A123_A123   asklepiadesi_A123/asklepiadesi_A123
#define asklepiadesi_D13_A123    asklepiadesi_D13/asklepiadesi_A123
#define asklepiadesi_E1234_E1234 asklepiadesi_E1234/asklepiadesi_E1234
:#asklepiadesi_A123_A123/asklepiadesi_A123_A123# asklepiadesi A
:#asklepiadesi_A123_A123/asklepiadesi_A123/asklepiadesi_B4# asklepiadesi B
:#asklepiadesi_A123_A123/asklepiadesi_C3/asklepiadesi_B4# asklepiadesi C
:#asklepiadesi_D13_A123/asklepiadesi_D13_A123# asklepiadesi D
:#asklepiadesi_E1234_E1234/asklepiadesi_E1234_E1234# asklepiadesi E
:#alkaiosi_12/alkaiosi_12/alkaiosi_3/alkaiosi_4# alkaiosi
:#Szept_vegen_1212_1212# Szeptember végén rimkeplet=ababcdcd
:hexameter/pentameter disztichon
#define 2_ionicus_a_min ionicus_a_minore_kolon/ionicus_a_minore_kolon
:2_ionicus_a_min/2_ionicus_a_min anakreoni 16
:#szapphoi_sor/szapphoi_sor/szapphoi_sor/adoniszi_kolon# sapphoi
;;Christian Morgenstern: Hal éji éneke (ford. Szabó Lőrinc)
;;a formát megverselte Parti Nagy Lajos
#define pnl_1 molosszus_lab/proceleuzmatikus_lab
#define pnl_2 pnl_1/pnl_1
#define pnl_3 pnl_2/pnl_2
#define pnl_4 molosszus_lab/pirrichius_lab
#define pnl_5 pirrichius_lab/pnl_3
#define pnl_6 pnl_5/pnl_4
:#ta_lab/pnl_6/ta_lab# hal éji éneke
;
;-------------------------------------------------------
;;A .valami blokkok nevét ne változtasd
.VNP-strófák és -sortömbök:
@Pincsike1.ab Pincsike1.a/Pincsike1.b
@Pincsike1.bb Pincsike1.b/Pincsike1.b
@Melyegi_alom_ns_ns melyegi_alom_nagysor/melyegi_alom_nagysor
@Melyegi_alom_12 melyegi_alom_1/melyegi_alom_2
@melyegi_alom_1212 melyegi_alom_12/melyegi_alom_12
@Melyegi_alom_ns_ns_ns_ns Melyegi_alom_ns_ns/Melyegi_alom_ns_ns
;;    :#Pincsike1.ab/Pincsike1.ab/Pincsike1.bb# Pincsike1
;;Nem lehet megjegyzéssor az alábbi strófadefinícióban
;
#start_strofa
%name=Pincsike1
%place=".VNP-strófák és -sortömbök:"
.
Pincsike1.a                     rim=x
Pincsike1.b=léküthion_kolon     rim=a
Pincsike1.a                     rim=x
Pincsike1.b=léküthion_kolon     rim=a
Pincsike1.b=léküthion_kolon     rim=b
Pincsike1.b=léküthion_kolon     rim=b
.
#end_strofa
;;
#start_strofa
%name=Komposzt
%place=".VNP-strófák és -sortömbök:"
.
Éhesek     rim=a
Gyilkosok  rim=b
Éhesek     rim=a
Gyilkosok  rim=b
.
#end_strofa
;
:#gyilkosok/gyilkosok/gyilkosok/gyilkosok# Gyilkosok szakasz rimkeplet=xaxa
:#éhesek/éhesek/éhesek/éhesek# Éhesek szakasz rimkeplet=xaxa
:melyegi_alom_1/melyegi_alom_2 melyegi alom: nagysor
:#Melyegi_alom_ns_ns_ns_ns# melyegi alom: nagy szakasz
:#melyegi_alom_1212# melyegi alom: kis szakasz
;-------------------------------------------------------
;;Hangsúlytalan szavak:
$előtt
$után
$mögött
$fölött
$felett
$alatt
$mellett
$és
$a
$az
$helyett
$beli
$nélkül
$között
$mint
$révén
.Beallitasok:
!az_s_kotoszo_kozombos=1
!az_abece_betuinek_kulon_szotag=0
!emberi_nyelvu_mit_tudok=1
!a_jobb_oldali_szoveg_formazott_legyen=1
!az_adatbazis_lezarasanak_datuma=2006. április 23-án
!egynel_tobb_telitalalat_keresese=1
!az_asszonanc_rimkent_valo_kezelese=1
!latszik_az_utemhangsuly_a_gorogon=0
!a_fuggoleges_toszogalos_mutyur_helye=356
!a_beallitasokat_tartalmazo_felulet_elrejtve=0

;-------------------------------------------------------
;;Kiegészítő standard antik sorfajták — verifikált, szisztematikus formák
;;a fenti verslábakból/kolónokból építve. Konzervatív anceps-kódolás.
;;A ritka, hagyományfüggő formák (archilochoszi kombinációk, priapeus,
;;elegiambus) szándékosan kimaradtak: azok mintája nem egyértelmű.
.Kiegészítő antik sorfajták:
?-U-                 iambikus metrum (monométer)
?-U-?-U-             iambikus dimeter
?-U-?-U-?-U-?-U-     iambikus tetrameter (octonarius)
UU-UU-UU-UU-         anapesztikus dimeter (spondeusz-helyettesítés megengedett)
UU--UU--             ión a minore dimeter
--UU--UU             ión a maiore dimeter
-UU-UU-UU-UU         daktilikus tetrameter (spondeusz-helyettesítés megengedett)
""";
}
