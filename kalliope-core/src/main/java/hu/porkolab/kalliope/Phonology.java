package hu.porkolab.kalliope;

import java.util.List;
import java.util.Locale;

/**
 * Magyar hangtan a skandáláshoz.
 *
 * <p>A verstani hosszúság két forrása (Fazekas Kulturális Enciklopédia — Verstan;
 * Csehy–Polgár: Gyakorlati magyar verstan):
 *
 * <ul>
 *   <li><b>természeténél fogva hosszú</b>: a szótag magánhangzója hosszú;
 *   <li><b>helyzeténél fogva hosszú</b>: rövid magánhangzó után <i>vagy</i> egy
 *       hosszú mássalhangzó, <i>vagy</i> legalább két rövid mássalhangzó áll.
 * </ul>
 *
 * <p>A mássalhangzókat ezért nem betűnként, hanem <i>fonémánként</i> és
 * <i>pozíciószámmal</i> tartjuk nyilván: a kétjegyű betű (AkH. 7. §) egy
 * mássalhangzó, a kettőzött kétjegyű (AkH. 7. § b: {@code ssz, ggy, nny}…) egy
 * hosszú mássalhangzó, tehát két pozíció, az {@code x} két hang ({@code ksz}),
 * a {@code dz}/{@code dzs} pedig kettőzés nélkül is hosszú (AkH. 87. §: bodza,
 * madzag, edz).
 */
public final class Phonology {

    private Phonology() {}

    static final String LONG_VOWELS = "áéíóőúű";
    static final String SHORT_VOWELS = "aeiouöü";

    /** Zárhangok a muta cum liquida vizsgálatához — csak az egyjegyűek. */
    private static final String STOPS = "ptkbdg";
    /** Likvidák — az {@code ly} NEM az, az [j] hangot jelöli (AkH. 88. §). */
    private static final String LIQUIDS = "lr";

    /**
     * Többjegyű grafémák, HOSSZABB ELŐSZÖR. A {@code positions} a mássalhangzó-
     * pozíciók száma, az {@code ambiguous} azt jelöli, hogy a graféma olvasata
     * bizonytalan (szóösszetételi határon két külön hang is lehet).
     */
    record Grapheme(String text, int positions, boolean ambiguous) {}

    private static final List<Grapheme> GRAPHEMES = List.of(
            // kettőzött háromjegyű / kétjegyűek: egy hosszú mássalhangzó = 2 pozíció
            new Grapheme("ddzs", 2, false),
            new Grapheme("ccs", 2, false),
            new Grapheme("ddz", 2, false),
            new Grapheme("ggy", 2, false),
            new Grapheme("lly", 2, false),
            new Grapheme("nny", 2, false),
            new Grapheme("ssz", 2, false),
            new Grapheme("tty", 2, false),
            new Grapheme("zzs", 2, false),
            // dzs és dz: kettőzés nélkül is hosszú (AkH. 87. §)
            new Grapheme("dzs", 2, false),
            new Grapheme("dz", 2, false),
            // a többi kétjegyű: egy rövid mássalhangzó
            new Grapheme("cs", 1, false),
            new Grapheme("gy", 1, false),
            new Grapheme("ly", 1, false),
            new Grapheme("ny", 1, false),
            new Grapheme("sz", 1, false),
            new Grapheme("ty", 1, false),
            new Grapheme("zs", 1, false),
            // görög/latin átírási digráfok: egy hehezetes hang — DE szóösszetételi
            // határon (hat|hatós, orr|hang) két hang, ezért bizonytalan
            new Grapheme("kh", 1, true),
            new Grapheme("th", 1, true),
            new Grapheme("ph", 1, true),
            new Grapheme("rh", 1, true),
            new Grapheme("ch", 1, true));

    /** A magyar ábécé kétjegyű betűi (AkH. 7. §) — a rímkulcs fonémákra bontásához. */
    static final List<String> DIGRAPHS = List.of("dzs", "cs", "dz", "gy", "ly", "ny", "sz", "ty", "zs");

    static boolean isLongVowel(char c) {
        return LONG_VOWELS.indexOf(Character.toLowerCase(c)) >= 0;
    }

    static boolean isShortVowel(char c) {
        return SHORT_VOWELS.indexOf(Character.toLowerCase(c)) >= 0;
    }

    /**
     * Magánhangzó-e a betű. Az {@code y} csak akkor, ha nem kétjegyű betű része —
     * idegen szavakban önálló magánhangzó (Charybdis, Zephyr, thyrsus).
     */
    static boolean isVowelAt(String word, int i) {
        char c = Character.toLowerCase(word.charAt(i));
        if (isLongVowel(c) || isShortVowel(c)) {
            return true;
        }
        if (c != 'y') {
            return false;
        }
        return i == 0 || !isDigraphSecondLetter(word, i);
    }

    private static boolean isDigraphSecondLetter(String word, int i) {
        char prev = Character.toLowerCase(word.charAt(i - 1));
        return prev == 'g' || prev == 'l' || prev == 'n' || prev == 't';
    }

    static boolean isConsonantLetter(String word, int i) {
        char c = word.charAt(i);
        return Character.isLetter(c) && !isVowelAt(word, i);
    }

    /** Egy mássalhangzó-fonéma a szövegben: hossza betűkben, súlya pozíciókban. */
    record Consonant(String text, int letters, int positions, boolean ambiguous) {
        boolean isStop() {
            return text.length() == 1 && STOPS.indexOf(text.charAt(0)) >= 0;
        }

        boolean isLiquid() {
            return text.length() == 1 && LIQUIDS.indexOf(text.charAt(0)) >= 0;
        }
    }

    /**
     * A {@code i} pozíción kezdődő mássalhangzó-fonéma. A hívó garantálja, hogy
     * ott mássalhangzó áll.
     */
    static Consonant consonantAt(String word, int i) {
        for (Grapheme g : GRAPHEMES) {
            if (word.regionMatches(i, g.text(), 0, g.text().length())) {
                // az 'y'-ra végződő digráf csak akkor digráf, ha az y nem magánhangzó
                return new Consonant(g.text(), g.text().length(), g.positions(), g.ambiguous());
            }
        }
        char c = word.charAt(i);
        // x = [ksz], két hang (PTE: „a mixer tá ti, hiszen az x két hangot jelöl")
        if (c == 'x') {
            return new Consonant("x", 1, 2, false);
        }
        return new Consonant(String.valueOf(c), 1, 1, false);
    }

    static String lower(String s) {
        return s.toLowerCase(Locale.ROOT);
    }
}
