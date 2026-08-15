package hu.porkolab.kalliope;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Szöveg → U/-/? skandálás, szótagszintű indoklással.
 *
 * <p>A szabályok forrása: Fazekas Kulturális Enciklopédia — Verstan; Csehy Zoltán–
 * Polgár Anikó: Gyakorlati magyar verstan; Magyartanár (Kecskés–Szilágyi–Szuromi:
 * Kis magyar verstan alapján); A magyar helyesírás szabályai 12. kiadás.
 *
 * <ul>
 *   <li>a szótag a következő magánhangzóig tart, <b>átlépve a szóhatárt</b>;
 *   <li>hosszú magánhangzó → hosszú (természeténél fogva);
 *   <li>rövid magánhangzó után egy hosszú vagy legalább két rövid mássalhangzó →
 *       hosszú (helyzeténél fogva);
 *   <li>közös (?): a sorvégi szótag, a névelő, a rövid nyílt szótagú kötőszók, a
 *       muta cum liquida (zárhang + likvida), és minden olyan torlódás, amelynek
 *       az olvasata bizonytalan.
 * </ul>
 *
 * <p>A skandáló <b>szigorú</b>: költői licenciát nem feltételez. Ahol a magyar
 * helyesírás valóban kétféle olvasatot enged (görög nevek {@code eu}/{@code au}
 * kapcsolata), ott nem dönt helyettünk, hanem <i>változatot</i> ad — a mérték
 * dönti el, melyik olvasat áll.
 */
public final class Scansion {

    /** Miért ilyen hosszú a szótag — ezt mutatja a felület. */
    public enum Reason {
        NATURAL_LONG("természeténél fogva hosszú"),
        POSITION_LONG("helyzeténél fogva hosszú"),
        SHORT("rövid"),
        LINE_END("sorvégi közös szótag"),
        ARTICLE("a határozott névelő közös"),
        SHORT_WORD("rövid, nyílt szótagú kötőszó vagy névmás — közös"),
        WORD_FINAL_CONSONANT("szóvégi mássalhangzó magánhangzó előtt: zárt és nyílt olvasat is lehet — közös"),
        WORD_INITIAL_STRESS("költői licencia: a szókezdő hangsúly megnyújthatja — közös"),
        MUTA_CUM_LIQUIDA("muta cum liquida: zárhang + likvida nem feltétlenül tesz helyzeti hosszút"),
        AMBIGUOUS_CLUSTER("a torlódás olvasata bizonytalan — közös"),
        SYNIZESIS("összevont kettőshangzó, hosszú"),
        MANUAL("kézi felülbírálás — az olvasó döntése");

        private final String hungarian;

        Reason(String hungarian) {
            this.hungarian = hungarian;
        }

        public String explanation() {
            return hungarian;
        }
    }

    /** Egy szótag: a betűi, a hossza, és hogy miért. */
    public record Syllable(String text, char quantity, Reason reason, int wordIndex) {}

    /** Egy sor egy olvasata. */
    public record Reading(String pattern, List<Syllable> syllables, boolean synizesis) {
        public int syllableCount() {
            return syllables.size();
        }
    }

    /** Névelők — mindkét alakjuk közös. */
    private static final Set<String> ARTICLES = Set.of("a", "az");

    /** Az „s” kötőszó és teljes alakja: mássalhangzójuk elhagyható. */
    private static final Set<String> S_CLITICS = Set.of("s", "és");

    /**
     * Rövid magánhangzós, nyílt szótagú kötőszók és névmások — a Magyartanár és
     * Csehy–Polgár szerint közösek.
     */
    private static final Set<String> SHORT_FUNCTION_WORDS = Set.of("ha", "de", "te", "mi", "ti", "ki", "e", "ma", "ne");

    /** Görög-latin kettőshangzók, amelyek egy szótagba is vonhatók. */
    private static final Set<String> DIPHTHONGS = Set.of("au", "eu", "ei", "oi", "ai", "ou", "ae", "oe", "ui");

    /** Legfeljebb ennyi kettőshangzót vonunk össze változatokban (2^4 olvasat). */
    private static final int MAX_SYNIZESIS_POINTS = 4;

    private Scansion() {}

    /** Egy kézi felülbírálás: hányadik sor hányadik szótagja, és mire. */
    public record Override(int line, int syllable, char quantity) {
        public Override {
            if (quantity != Notation.SHORT && quantity != Notation.LONG && quantity != Notation.ANCEPS) {
                throw new IllegalArgumentException("A hosszúság csak U, - vagy ? lehet: " + quantity);
            }
        }
    }

    /**
     * A megadott szótagok hosszúságát felülírja.
     *
     * <p>A verstan értelmezés kérdése: az olvasó dönthet úgy, hogy egy szótagot
     * másképp olvas, és a motornak ezt tiszteletben kell tartania. Enélkül a
     * program orákulum volna, nem eszköz.
     */
    public static Reading withOverrides(Reading reading, java.util.Map<Integer, Character> overrides) {
        if (overrides == null || overrides.isEmpty()) {
            return reading;
        }
        List<Syllable> syllables = new ArrayList<>(reading.syllables());
        StringBuilder pattern = new StringBuilder(reading.pattern());
        for (var e : overrides.entrySet()) {
            int at = e.getKey();
            if (at < 0 || at >= syllables.size()) {
                continue;
            }
            Syllable old = syllables.get(at);
            syllables.set(at, new Syllable(old.text(), e.getValue(), Reason.MANUAL, old.wordIndex()));
            pattern.setCharAt(at, e.getValue());
        }
        return new Reading(pattern.toString(), List.copyOf(syllables), reading.synizesis());
    }

    /** A sor elsődleges olvasata. */
    public static Reading scan(String line, Settings settings) {
        return readings(line, settings).get(0);
    }

    /**
     * A sor összes olvasata; az első az elsődleges (minden magánhangzó külön
     * szótag), a többiben egy-egy görög-latin kettőshangzó össze van vonva.
     */
    public static List<Reading> readings(String line, Settings settings) {
        List<String> words = TextNormalizer.words(line, settings.letterSyllables());
        Stream stream = Stream.of(words);
        if (stream.nuclei.isEmpty()) {
            return List.of(new Reading("", List.of(), false));
        }
        List<Integer> candidates = settings.allowSynizesis() ? stream.synizesisCandidates() : List.<Integer>of();
        List<Reading> out = new ArrayList<>();
        out.add(stream.read(Set.of(), settings));
        int n = Math.min(candidates.size(), MAX_SYNIZESIS_POINTS);
        for (int mask = 1; mask < (1 << n); mask++) {
            Set<Integer> merged = new LinkedHashSet<>();
            for (int b = 0; b < n; b++) {
                if ((mask & (1 << b)) != 0) {
                    merged.add(candidates.get(b));
                }
            }
            out.add(stream.read(merged, settings));
        }
        return out;
    }

    // ------------------------------------------------------------------ //

    /** A sor betűfolyama szóhatárokkal, magánhangzó-magokkal. */
    private static final class Stream {
        private final String text;
        private final int[] wordOf;
        private final List<String> words;
        private final List<Integer> nuclei = new ArrayList<>();

        private Stream(String text, int[] wordOf, List<String> words) {
            this.text = text;
            this.wordOf = wordOf;
            this.words = words;
            for (int i = 0; i < text.length(); i++) {
                if (text.charAt(i) != ' ' && Phonology.isVowelAt(text, i)) {
                    nuclei.add(i);
                }
            }
        }

        static Stream of(List<String> words) {
            StringBuilder sb = new StringBuilder();
            List<Integer> owner = new ArrayList<>();
            for (int w = 0; w < words.size(); w++) {
                if (w > 0) {
                    sb.append(' ');
                    owner.add(-1);
                }
                String word = words.get(w);
                for (int i = 0; i < word.length(); i++) {
                    sb.append(word.charAt(i));
                    owner.add(w);
                }
            }
            int[] wordOf = new int[owner.size()];
            for (int i = 0; i < owner.size(); i++) {
                wordOf[i] = owner.get(i);
            }
            return new Stream(sb.toString(), wordOf, words);
        }

        /**
         * Az összevonható kettőshangzók: szón belül közvetlenül egymás mellett álló
         * görög-latin diftongusok (Európa, Zeusz, Akhilleusz, Péleidész, Auróra,
         * Aiász, Oidipusz). A magyar a diftongusokat nem ismeri, ezért ezek
         * alapból két szótagot alkotnak — de a versmérték a görög olvasatot is
         * kérheti, és Fazekas szerint „általában egy hosszú szótagot alkotnak, de
         * két rövid magánhangzónak is tekinthetők". Nem döntünk helyette: a
         * lehetséges olvasatokat előállítjuk, és a mérték választ.
         *
         * @return a pár ELSŐ magánhangzójának indexe a magok listájában
         */
        List<Integer> synizesisCandidates() {
            List<Integer> out = new ArrayList<>();
            for (int k = 0; k + 1 < nuclei.size(); k++) {
                int a = nuclei.get(k);
                int b = nuclei.get(k + 1);
                if (b != a + 1 || wordOf[a] != wordOf[b]) {
                    continue;
                }
                String pair = text.substring(a, b + 1);
                if (DIPHTHONGS.contains(pair)) {
                    out.add(k);
                }
            }
            return out;
        }

        Reading read(Set<Integer> merged, Settings settings) {
            // 1. magok kijelölése (az összevont párok egy magot alkotnak)
            List<int[]> groups = new ArrayList<>(); // {kezdet, vég} betűindex, zárt intervallum
            for (int k = 0; k < nuclei.size(); k++) {
                int start = nuclei.get(k);
                if (merged.contains(k) && k + 1 < nuclei.size()) {
                    groups.add(new int[] {start, nuclei.get(k + 1)});
                    k++;
                } else {
                    groups.add(new int[] {start, start});
                }
            }
            // 2. szótagszövegek szavanként
            List<String> texts = syllableTexts(groups);
            // 3. hosszúság magonként
            StringBuilder pattern = new StringBuilder(groups.size());
            List<Syllable> syllables = new ArrayList<>(groups.size());
            for (int k = 0; k < groups.size(); k++) {
                int[] g = groups.get(k);
                boolean last = k == groups.size() - 1;
                int word = wordOf[g[0]];
                Quantified q = quantify(g, k, groups, last, settings);
                pattern.append(q.quantity);
                syllables.add(new Syllable(texts.get(k), q.quantity, q.reason, word));
            }
            return new Reading(pattern.toString(), List.copyOf(syllables), !merged.isEmpty());
        }

        private record Quantified(char quantity, Reason reason) {}

        private Quantified quantify(int[] group, int index, List<int[]> groups, boolean last, Settings settings) {
            if (last) {
                return new Quantified(Notation.ANCEPS, Reason.LINE_END);
            }
            if (group[0] != group[1]) {
                return new Quantified(Notation.LONG, Reason.SYNIZESIS);
            }
            char vowel = text.charAt(group[0]);
            String word = wordOf[group[0]] >= 0 ? words.get(wordOf[group[0]]) : "";
            boolean shortVowel = !Phonology.isLongVowel(vowel);
            if (shortVowel && ARTICLES.contains(word)) {
                return new Quantified(Notation.ANCEPS, Reason.ARTICLE);
            }
            if (shortVowel && settings.shortWordsAnceps() && SHORT_FUNCTION_WORDS.contains(word)) {
                return new Quantified(Notation.ANCEPS, Reason.SHORT_WORD);
            }
            if (!shortVowel) {
                return new Quantified(Notation.LONG, Reason.NATURAL_LONG);
            }
            return byPosition(group, index, groups, settings);
        }

        private Quantified byPosition(int[] group, int index, List<int[]> groups, Settings settings) {
            int from = group[1] + 1;
            int to = groups.get(index + 1)[0];
            List<Phonology.Consonant> cluster = new ArrayList<>();
            List<Boolean> optional = new ArrayList<>();
            int nucleusWord = wordOf[group[1]];
            int nextVowelWord = wordOf[to];
            boolean singleWordFinal = false;
            boolean clusterSplitByWordBoundary = false;
            int firstConsonantWord = -1;
            int i = from;
            while (i < to) {
                if (text.charAt(i) == ' ') {
                    i++;
                    continue;
                }
                Phonology.Consonant c = Phonology.consonantAt(text, i);
                int owner = wordOf[i];
                if (cluster.isEmpty()) {
                    singleWordFinal = owner == nucleusWord && nextVowelWord != nucleusWord;
                    firstConsonantWord = owner;
                } else if (owner != firstConsonantWord) {
                    clusterSplitByWordBoundary = true;
                }
                cluster.add(c);
                boolean soft = settings.sConjunctionAnceps() && owner >= 0 && S_CLITICS.contains(words.get(owner));
                optional.add(soft);
                i += c.letters();
            }
            singleWordFinal = singleWordFinal && cluster.size() == 1;
            int min = 0;
            int max = 0;
            for (int k = 0; k < cluster.size(); k++) {
                Phonology.Consonant c = cluster.get(k);
                max += c.positions();
                if (!optional.get(k) && !c.ambiguous()) {
                    min += c.positions();
                }
            }
            // Muta cum liquida csak szón BELÜL: a licencia oka, hogy a zárhang +
            // likvida egyetlen szótagkezdetet alkothat (a-pra-ja). Szóhatáron
            // (vak | róka) a zárhang az előző szótag zárója, tehát valódi
            // helyzeti hosszúságot ad.
            if (cluster.size() == 2
                    && max == 2
                    && !clusterSplitByWordBoundary
                    && cluster.get(0).isStop()
                    && cluster.get(1).isLiquid()) {
                return new Quantified(Notation.ANCEPS, Reason.MUTA_CUM_LIQUIDA);
            }
            if (min >= 2) {
                return new Quantified(Notation.LONG, Reason.POSITION_LONG);
            }
            if (max >= 2) {
                return new Quantified(Notation.ANCEPS, Reason.AMBIGUOUS_CLUSTER);
            }
            // Egyetlen, SZÓVÉGI mássalhangzó magánhangzóval kezdődő szó előtt: a
            // mai magyar szótagolás átviszi a következő szótagba (nyílt, rövid),
            // a latinos hagyomány a szótag zárójának veszi (hosszú). A kapott
            // skandálások az utóbbit is használják — pl. Kazinczy „Ebben áll” —,
            // ezért itt nem döntünk: közös.
            if (singleWordFinal && settings.wordFinalConsonantAnceps() && !cluster.isEmpty()) {
                return new Quantified(Notation.ANCEPS, Reason.WORD_FINAL_CONSONANT);
            }
            boolean firstSyllableOfWord = index == 0 || wordOf[groups.get(index - 1)[0]] != nucleusWord;
            if (settings.wordInitialStressLengthens() && firstSyllableOfWord) {
                return new Quantified(Notation.ANCEPS, Reason.WORD_INITIAL_STRESS);
            }
            return new Quantified(Notation.SHORT, Reason.SHORT);
        }

        /**
         * Megjelenítendő szótagszöveg: szón belüli, hagyományos elválasztás szerint
         * (egy mássalhangzó a következő szótaghoz, kettő vagy több esetén az első az
         * előzőhöz tapad). A metrikai hosszúság ettől független — az átlépi a szóhatárt.
         */
        private List<String> syllableTexts(List<int[]> groups) {
            List<String> out = new ArrayList<>(groups.size());
            for (int k = 0; k < groups.size(); k++) {
                int[] g = groups.get(k);
                int word = wordOf[g[0]];
                int wordStart = wordStart(g[0]);
                int wordEnd = wordEnd(g[0]);
                int start = k == 0 || wordOf[groups.get(k - 1)[0]] != word
                        ? wordStart
                        : splitPoint(groups.get(k - 1)[1], g[0], wordEnd);
                boolean lastInWord = k + 1 >= groups.size() || wordOf[groups.get(k + 1)[0]] != word;
                int end = lastInWord ? wordEnd : splitPoint(g[1], groups.get(k + 1)[0], wordEnd);
                out.add(text.substring(Math.max(start, wordStart), Math.min(end, wordEnd)));
            }
            attachVowellessWords(groups, out);
            return out;
        }

        /**
         * A magánhangzó nélküli szó — mindenekelőtt az „s” kötőszó — egyetlen
         * szótagot sem alkot, mássalhangzója az előző szótag zárójához tartozik.
         * A megjelenítésben ezért oda tapasztjuk: enélkül a sor szövege csorbulna
         * („Fegyvert, s vitézt” → „Fegyvert vitézt”).
         */
        private void attachVowellessWords(List<int[]> groups, List<String> out) {
            if (out.isEmpty()) {
                return;
            }
            for (int w = 0; w < words.size(); w++) {
                if (hasNucleus(w)) {
                    continue;
                }
                int target = -1;
                for (int k = 0; k < groups.size(); k++) {
                    if (wordOf[groups.get(k)[0]] < w) {
                        target = k;
                    }
                }
                if (target >= 0) {
                    out.set(target, out.get(target) + " " + words.get(w));
                } else {
                    out.set(0, words.get(w) + " " + out.get(0));
                }
            }
        }

        private boolean hasNucleus(int word) {
            for (int nucleus : nuclei) {
                if (wordOf[nucleus] == word) {
                    return true;
                }
            }
            return false;
        }

        /** Hol vágjuk el a két mag közti mássalhangzó-torlódást (szón belül). */
        private int splitPoint(int nucleusEnd, int nextNucleus, int wordEnd) {
            int limit = Math.min(nextNucleus, wordEnd);
            List<Integer> starts = new ArrayList<>();
            int i = nucleusEnd + 1;
            while (i < limit) {
                starts.add(i);
                i += Phonology.consonantAt(text, i).letters();
            }
            if (starts.size() >= 2) {
                return starts.get(1);
            }
            return starts.isEmpty() ? limit : starts.get(0);
        }

        private int wordStart(int i) {
            int j = i;
            while (j > 0 && wordOf[j - 1] == wordOf[i]) {
                j--;
            }
            return j;
        }

        private int wordEnd(int i) {
            int j = i;
            while (j + 1 < text.length() && wordOf[j + 1] == wordOf[i]) {
                j++;
            }
            return j + 1;
        }
    }
}
