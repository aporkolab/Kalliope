package hu.porkolab.kalliope;

import java.util.ArrayList;
import java.util.List;

/**
 * Sormetszet (cezúra) felismerése.
 *
 * <p>Két külön dolgot mérünk. Az egyik: a mérték mintája tartalmazhat
 * {@code ||} jelet — ilyenkor csak azt kell ellenőrizni, hogy ott valódi
 * szóhatár van-e. A másik: a hexameternél a metszet nincs a mintában, mert
 * <i>hol</i> van, az a sortól függ — a hagyomány több helyet is elfogad, és
 * épp az a kérdés, melyiket használja a költő.
 *
 * <p>A hexameter szokásos metszetei (West: Introduction to Greek Metre;
 * Fazekas Kulturális Enciklopédia — Verstan):
 *
 * <ul>
 *   <li><b>penthémimerész</b> — az ötödik félláb után, azaz a harmadik versláb
 *       hosszúja után; ez a leggyakoribb;
 *   <li><b>kata triton trokhaion</b> — a harmadik versláb első rövidje után;
 *   <li><b>hephthémimerész</b> — a hetedik félláb után, a negyedik versláb
 *       hosszúja után.
 * </ul>
 */
public final class Caesura {

    /** Egy megtalált metszet: hányadik szótag után, és mi a neve. */
    public record Found(int afterSyllable, String name) {}

    private Caesura() {}

    /**
     * A sorban ténylegesen meglévő metszetek.
     *
     * @param meter az illeszkedő mérték
     * @param scanned a szkennelt sor
     * @param syllables a sor szótagjai (a szóhatárokhoz)
     */
    public static List<Found> detect(Meter meter, String scanned, List<Scansion.Syllable> syllables) {
        boolean[] wordStart = AccentualMatcher.wordStarts(syllables);
        List<Found> out = new ArrayList<>();

        // 1. a mintában jelölt cezúrák
        List<Integer> marked = Notation.caesuraSyllables(scanned, meter.pattern());
        if (marked != null) {
            for (int at : marked) {
                if (at > 0 && at < wordStart.length && wordStart[at]) {
                    out.add(new Found(at, "a mérték jelölt metszete"));
                }
            }
        }

        // 2. a hexameter hagyományos metszetei
        if (meter.equals(MetricCanon.HEXAMETER) || meter.equals(MetricCanon.VERSUS_SPONDIACUS)) {
            int[] footStarts = footStarts(scanned, meter.pattern());
            if (footStarts != null && footStarts.length >= 5) {
                addIfWordBoundary(out, wordStart, footStarts[2] + 1, "penthémimerész");
                addIfWordBoundary(out, wordStart, footStarts[2] + 2, "kata triton trokhaion");
                addIfWordBoundary(out, wordStart, footStarts[3] + 1, "hephthémimerész");
            }
        }
        return List.copyOf(out);
    }

    private static void addIfWordBoundary(List<Found> out, boolean[] wordStart, int at, String name) {
        if (at > 0 && at < wordStart.length && wordStart[at]) {
            out.add(new Found(at, name));
        }
    }

    /** A verslábak kezdő szótagindexei a szkennelt sorban. */
    static int[] footStarts(String scanned, String pattern) {
        int[] consumed = Notation.align(scanned, pattern);
        if (consumed == null) {
            return null;
        }
        List<Notation.Symbol> symbols = Notation.parse(pattern);
        List<Integer> starts = new ArrayList<>();
        int at = 0;
        for (int i = 0; i < symbols.size(); i++) {
            if (symbols.get(i).footStart()) {
                starts.add(at);
            }
            at += consumed[i];
        }
        int[] result = new int[starts.size()];
        for (int i = 0; i < starts.size(); i++) {
            result[i] = starts.get(i);
        }
        return result;
    }
}
