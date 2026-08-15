package hu.porkolab.kalliope;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Rímképlet-felismerés.
 *
 * <p>A rím tartománya magyar verstanban a <b>mag + kóda</b>: többszótagú rímszónál
 * a mag az <i>utolsó előtti</i> szótag magánhangzója (Arany János sémája:
 * b|ar|át – k|ar|át), és a rím tipikus terjedelme egy-két szótag. Ezért a kulcs
 * az utolsó előtti magánhangzótól indul, és csak egyszótagos sorvégnél esik
 * vissza az utolsóra. (A korábbi, egyetlen magánhangzóig érő kulcs miatt
 * <i>haza / soha / béka / anya</i> mind egy kulcsra esett.)
 *
 * <p>Arany kódaszabálya szerint a rím <b>végén</b> kell szigorúnak lenni: a laza
 * rokonságot (zöngésség) csak a rím belsejében engedjük, a záró mássalhangzónak
 * egyeznie kell.
 *
 * <p>A rímtelen sor jelölése <b>x</b> (vaksor) — a félrím képlete így {@code xaxa},
 * nem {@code abcb}.
 *
 * <p>Források: Arany János: Valami az asszonáncról (1854), Simon Gábor
 * ismertetésében (Magyar Nyelvőr); Csehy Zoltán–Polgár Anikó: Gyakorlati magyar
 * verstan; Magyar Néprajzi Lexikon: asszonánc.
 */
public final class RhymeDetector {

    /** A vaksor jele. */
    public static final char BLANK = 'x';

    /** Zöngésségi párok — Arany 1. rokonsági osztálya. Csak a rím belsejében. */
    private static final Map<String, String> VOICING = voicing();

    /** Az {@code ly} és a {@code j} ugyanaz a hang (AkH. 88. §) — mindig egybeesik. */
    private static final Set<String> LY = Set.of("ly");

    private RhymeDetector() {}

    private static Map<String, String> voicing() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("b", "p");
        m.put("d", "t");
        m.put("g", "k");
        m.put("v", "f");
        m.put("z", "sz");
        m.put("zs", "s");
        m.put("gy", "ty");
        m.put("dzs", "cs");
        m.put("dz", "c");
        return Map.copyOf(m);
    }

    /**
     * Egy sor rímkulcsa.
     *
     * @param assonance ha igaz, csak a magánhangzóváz számít, a hosszúságot is
     *     elhagyva (Arany harmadik, leggyengébb fokozata)
     */
    public static String key(String line, boolean assonance, boolean letterSyllables) {
        String w = TextNormalizer.normalized(line, letterSyllables).replace(" ", "");
        if (w.isEmpty()) {
            return "";
        }
        List<Integer> vowels = new ArrayList<>();
        for (int i = 0; i < w.length(); i++) {
            if (Phonology.isVowelAt(w, i)) {
                vowels.add(i);
            }
        }
        if (vowels.isEmpty()) {
            return "";
        }
        // A rím terjedelme: ha az utolsó szótag ZÁRT (mássalhangzóra végződik), a
        // rím maga az utolsó szótag — a magyar rím tipikusan ilyen (hatalmát /
        // szablyáját = „át"). Ha NYÍLT (magánhangzóra végződik), egyetlen
        // magánhangzó túl kevés volna (haza / soha / béka / anya mind rímelne),
        // ezért ilyenkor az utolsó előtti magánhangzótól számolunk.
        int lastVowel = vowels.get(vowels.size() - 1);
        boolean closedEnding = !assonance && lastVowel < w.length() - 1;
        int start = closedEnding ? lastVowel : vowels.get(Math.max(0, vowels.size() - 2));
        List<String> phonemes = phonemes(w, start);
        if (assonance) {
            // Csak a magánhangzóváz — de a hosszúságot MEGTARTVA. A hosszú/rövid
            // felcserélése Aranynál külön, gyengébb fokozat, nem automatizmus;
            // összemosva „akhájnak" és „dögmadaraknak" is rímelne.
            StringBuilder sb = new StringBuilder();
            for (String p : phonemes) {
                if (p.length() == 1 && Phonology.isVowelAt(p, 0)) {
                    sb.append(p);
                }
            }
            return sb.toString();
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < phonemes.size(); i++) {
            String p = phonemes.get(i);
            boolean isVowel = p.length() == 1 && Phonology.isVowelAt(p, 0);
            if (isVowel) {
                sb.append(p);
                continue;
            }
            String normalized = LY.contains(p) ? "j" : p;
            boolean last = i == phonemes.size() - 1;
            if (!last) {
                normalized = VOICING.getOrDefault(normalized, normalized);
            }
            sb.append(normalized);
        }
        return sb.toString();
    }

    private static List<String> phonemes(String w, int from) {
        List<String> out = new ArrayList<>();
        int i = from;
        while (i < w.length()) {
            if (Phonology.isVowelAt(w, i)) {
                out.add(String.valueOf(w.charAt(i)));
                i++;
                continue;
            }
            if (!Character.isLetter(w.charAt(i))) {
                i++;
                continue;
            }
            String matched = null;
            for (String d : Phonology.DIGRAPHS) {
                if (w.regionMatches(i, d, 0, d.length())) {
                    matched = d;
                    break;
                }
            }
            if (matched == null) {
                matched = String.valueOf(w.charAt(i));
            }
            out.add(matched);
            i += matched.length();
        }
        return out;
    }

    /** A rím minősége — a tiszta rímtől az asszonáncig. */
    public enum Kind {
        ONRIM("önrím — ugyanaz a szó"),
        TISZTA("tiszta rím — a magánhangzótól minden hang egyezik"),
        RAGRIM("ragrím — a toldalék ismétlődik"),
        ROKONHANGZOS("rokonhangzós rím — a mássalhangzók rokonok"),
        ASSZONANC("asszonánc — a magánhangzóváz egyezik"),
        VAKSOR("vaksor — nincs rímpárja");

        private final String hungarian;

        Kind(String hungarian) {
            this.hungarian = hungarian;
        }

        public String explanation() {
            return hungarian;
        }
    }

    /** Gyakori magyar toldalékok — a ragrím felismeréséhez (Arany: „rag ismétlésből rím nem ered”). */
    private static final List<String> SUFFIXES = List.of(
            "nak", "nek", "ban", "ben", "bol", "bőt", "ból", "ből", "val", "vel", "ra", "re", "tol", "tól", "től",
            "hoz", "hez", "höz", "ok", "ök", "ak", "ek", "ja", "je", "om", "em", "am", "ni", "va", "ve", "an", "en",
            "on", "ön", "at", "et", "ot", "öt", "ut", "üt", "ig", "ul", "ül", "kor", "ként", "int");

    /** Egy szakasz rímelemzése: soronkénti kulcs, betű és minőség. */
    public record Scheme(List<String> keys, List<String> labels, List<Kind> kinds) {
        /** A képlet olvasható alakja, pl. {@code xaxa}. */
        public String pattern() {
            return String.join("", labels);
        }

        /** A képlet neve, ha ismert forma. */
        public String patternName() {
            return schemeName(pattern());
        }
    }

    /** A rímképlet magyar neve, vagy {@code null}, ha nem szokványos forma. */
    public static String schemeName(String pattern) {
        if (!pattern.isEmpty() && pattern.chars().allMatch(c -> c == BLANK)) {
            return "rímtelen";
        }
        return switch (pattern) {
            case "aabb", "aabbcc", "aabbccdd" -> "páros rím";
            case "abab", "ababcdcd" -> "keresztrím";
            case "abba", "abbacddc" -> "ölelkező rím";
            case "aaaa", "aaa", "aaaaa" -> "bokorrím";
            case "xaxa", "axax", "xaxaxbxb" -> "félrím";
            case "aab", "aabccb" -> "ráütő rím";
            case "xxxx", "xxx", "xx", "x" -> "rímtelen";
            default -> null;
        };
    }

    /**
     * Egy szakasz rímképlete.
     *
     * <p>Két sor akkor rímel, ha a <b>szigorú</b> kulcsuk egyezik (a rím a
     * sorvégi szótag, mássalhangzóstul: <i>hatalmát – szablyáját</i>), <b>vagy</b>
     * — ha az asszonánc rímnek számít — a magánhangzóvázuk egyezik
     * (<i>virágok – világot</i>). A kettő uniója kell: külön-külön egyik sem
     * ismeri fel a magyar rímgyakorlat mindkét felét.
     *
     * <p>Az egyedül maradó sorvég vaksor ({@code x}). Huszonhat különböző rím
     * fölött a jelölés {@code aa}, {@code ab}… — sosem fut ki az ábécéből.
     */
    public static Scheme scheme(List<String> lines, boolean assonance, boolean letterSyllables) {
        int n = lines.size();
        List<String> strict = new ArrayList<>(n);
        List<String> loose = new ArrayList<>(n);
        for (String line : lines) {
            strict.add(key(line, false, letterSyllables));
            loose.add(assonance ? key(line, true, letterSyllables) : "");
        }
        int[] parent = new int[n];
        for (int i = 0; i < n; i++) {
            parent[i] = i;
        }
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                boolean same = (!strict.get(i).isEmpty() && strict.get(i).equals(strict.get(j)))
                        || (!loose.get(i).isEmpty() && loose.get(i).equals(loose.get(j)));
                if (same) {
                    union(parent, i, j);
                }
            }
        }
        Map<Integer, Integer> sizes = new LinkedHashMap<>();
        for (int i = 0; i < n; i++) {
            sizes.merge(find(parent, i), 1, Integer::sum);
        }
        Map<Integer, String> assigned = new LinkedHashMap<>();
        List<String> labels = new ArrayList<>(n);
        List<Kind> kinds = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            int root = find(parent, i);
            if (strict.get(i).isEmpty() || sizes.getOrDefault(root, 0) < 2) {
                labels.add(String.valueOf(BLANK));
                kinds.add(Kind.VAKSOR);
                continue;
            }
            String existing = assigned.get(root);
            if (existing == null) {
                existing = label(assigned.size());
                assigned.put(root, existing);
            }
            labels.add(existing);
            kinds.add(kindOf(lines, strict, loose, parent, i));
        }
        return new Scheme(List.copyOf(strict), List.copyOf(labels), List.copyOf(kinds));
    }

    /** A sor rímének minősége a rímtársaihoz képest. */
    private static Kind kindOf(List<String> lines, List<String> strict, List<String> loose, int[] parent, int i) {
        int root = find(parent, i);
        String myWord = lastWord(lines.get(i));
        Kind best = Kind.ASSZONANC;
        for (int j = 0; j < lines.size(); j++) {
            if (j == i || find(parent, j) != root) {
                continue;
            }
            if (!myWord.isEmpty() && myWord.equals(lastWord(lines.get(j)))) {
                return Kind.ONRIM;
            }
            if (strict.get(i).equals(strict.get(j))) {
                Kind candidate = SUFFIXES.contains(strict.get(i)) ? Kind.RAGRIM : Kind.TISZTA;
                if (candidate.ordinal() < best.ordinal()) {
                    best = candidate;
                }
            } else if (!loose.get(i).isEmpty() && loose.get(i).equals(loose.get(j))) {
                if (Kind.ASSZONANC.ordinal() < best.ordinal()) {
                    best = Kind.ASSZONANC;
                }
            }
        }
        return best;
    }

    private static String lastWord(String line) {
        List<String> words = TextNormalizer.words(line, false);
        return words.isEmpty() ? "" : words.get(words.size() - 1);
    }

    private static int find(int[] parent, int i) {
        while (parent[i] != i) {
            parent[i] = parent[parent[i]];
            i = parent[i];
        }
        return i;
    }

    private static void union(int[] parent, int a, int b) {
        int ra = find(parent, a);
        int rb = find(parent, b);
        if (ra != rb) {
            parent[Math.max(ra, rb)] = Math.min(ra, rb);
        }
    }

    /** 0→„a", 25→„z", 26→„aa", 27→„ab"… — sosem fut ki az ábécéből. */
    static String label(int index) {
        StringBuilder sb = new StringBuilder();
        int n = index;
        while (true) {
            sb.insert(0, (char) ('a' + (n % 26)));
            n = n / 26 - 1;
            if (n < 0) {
                break;
            }
        }
        return sb.toString();
    }
}
