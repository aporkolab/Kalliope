package hu.porkolab.kalliope;

import java.util.ArrayList;
import java.util.List;

/**
 * A metrikai jelölés — az eredeti adatbázis fejlécének nyelvtana szerint:
 *
 * <pre>
 *   U  rövid szótag
 *   -  hosszú szótag
 *   ?  közömbös szótaghelyzet (anceps): hosszút és rövidet is elfogad
 *   =  "UU"-nak vagy "-"-nak megfelelő rész (feloldás / összevonás)
 *   |  verslábhatár
 *   || cezúra (csak emlékeztető)
 * </pre>
 *
 * Az illesztés NEM a minta összes realizációjának kifejtésével történik — az a
 * szabad pozíciók számában exponenciális —, hanem pozíciónkénti dinamikus
 * programozással. Lásd {@link #matches(String, String)}.
 */
public final class Notation {

    public static final char SHORT = 'U';
    public static final char LONG = '-';
    public static final char ANCEPS = '?';
    public static final char RESOLVE = '=';
    public static final char FOOT = '|';

    private Notation() {}

    public static boolean isSymbol(char c) {
        return c == SHORT || c == LONG || c == ANCEPS || c == RESOLVE;
    }

    /**
     * Egy mintaelem: a jel, hogy verslábat kezd-e, és hogy cezúra után áll-e.
     * A {@code |} lábhatár, a {@code ||} sormetszet (cezúra).
     */
    public record Symbol(char symbol, boolean footStart, boolean afterCaesura) {}

    /** A nyers mintát elemekre bontja, a '|' és '||' jeleket megkülönböztetve. */
    public static List<Symbol> parse(String pattern) {
        List<Symbol> out = new ArrayList<>(pattern.length());
        boolean footStart = true;
        boolean caesura = false;
        int bars = 0;
        for (int i = 0; i < pattern.length(); i++) {
            char c = pattern.charAt(i);
            if (c == FOOT) {
                footStart = true;
                bars++;
                if (bars >= 2) {
                    caesura = true;
                }
                continue;
            }
            if (!isSymbol(c)) {
                continue;
            }
            out.add(new Symbol(c, footStart, caesura));
            footStart = false;
            caesura = false;
            bars = 0;
        }
        return out;
    }

    /**
     * Mely szótagoknál kezdődik cezúra utáni rész a szkennelt sorban.
     * {@code null}, ha a minta nem illeszkedik.
     */
    public static List<Integer> caesuraSyllables(String scanned, String pattern) {
        int[] consumed = align(scanned, pattern);
        if (consumed == null) {
            return null;
        }
        List<Symbol> syms = parse(pattern);
        List<Integer> out = new ArrayList<>();
        int j = 0;
        for (int i = 0; i < syms.size(); i++) {
            if (syms.get(i).afterCaesura()) {
                out.add(j);
            }
            j += consumed[i];
        }
        return List.copyOf(out);
    }

    /** Csak a metrikai jelek, lábhatárok nélkül. */
    public static String symbolsOnly(String pattern) {
        StringBuilder sb = new StringBuilder(pattern.length());
        for (int i = 0; i < pattern.length(); i++) {
            char c = pattern.charAt(i);
            if (isSymbol(c)) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /** Hány szótag lehet a minta legrövidebb, illetve leghosszabb realizációja. */
    public static int minSyllables(String pattern) {
        return symbolsOnly(pattern).length();
    }

    public static int maxSyllables(String pattern) {
        int n = 0;
        for (Notation.Symbol s : parse(pattern)) {
            n += s.symbol() == RESOLVE ? 2 : 1;
        }
        return n;
    }

    /** Illeszkedik-e a jel a szkennelt szótaghoz. A közömbös helyzet (?) mindkét irányban elfogadó. */
    private static boolean fits(char patternSymbol, char scanned) {
        if (scanned == ANCEPS || patternSymbol == ANCEPS) {
            return true;
        }
        return patternSymbol == scanned;
    }

    /**
     * Szigorú illesztés: hosszú↔hosszú, rövid↔rövid, közömbös↔bármelyik, a '=' pedig
     * vagy egy hosszúra, vagy két rövidre. Nincs feltételezett költői licencia.
     */
    public static boolean matches(String scanned, String pattern) {
        return align(scanned, pattern) != null;
    }

    /**
     * Az illesztés konkrét megvalósulása: mintaelemenként hány szótagot fogyaszt
     * (1 vagy 2). {@code null}, ha nincs illeszkedés. Ebből áll elő a UI
     * verslábtagolása és az ütemhangsúly-sor is.
     */
    public static int[] align(String scanned, String pattern) {
        List<Symbol> syms = parse(pattern);
        int n = syms.size();
        int m = scanned.length();
        // reach[i][j] = az i. mintaelemtől a j. szótagtól kezdve végig lehet-e menni
        Boolean[][] memo = new Boolean[n + 1][m + 1];
        if (!reachable(syms, scanned, 0, 0, memo)) {
            return null;
        }
        int[] consumed = new int[n];
        int i = 0;
        int j = 0;
        while (i < n) {
            char sym = syms.get(i).symbol();
            if (sym == RESOLVE) {
                // A hosszú olvasat az elsődleges (a "-=" alapértelmezetten "--").
                if (j < m && fits(LONG, scanned.charAt(j)) && reachable(syms, scanned, i + 1, j + 1, memo)) {
                    consumed[i] = 1;
                    j += 1;
                } else {
                    consumed[i] = 2;
                    j += 2;
                }
            } else {
                consumed[i] = 1;
                j += 1;
            }
            i++;
        }
        return consumed;
    }

    private static boolean reachable(List<Symbol> syms, String scanned, int i, int j, Boolean[][] memo) {
        if (i == syms.size()) {
            return j == scanned.length();
        }
        if (j > scanned.length()) {
            return false;
        }
        Boolean cached = memo[i][j];
        if (cached != null) {
            return cached;
        }
        boolean ok = false;
        char sym = syms.get(i).symbol();
        if (sym == RESOLVE) {
            if (j < scanned.length() && fits(LONG, scanned.charAt(j))) {
                ok = reachable(syms, scanned, i + 1, j + 1, memo);
            }
            if (!ok
                    && j + 1 < scanned.length()
                    && fits(SHORT, scanned.charAt(j))
                    && fits(SHORT, scanned.charAt(j + 1))) {
                ok = reachable(syms, scanned, i + 1, j + 2, memo);
            }
        } else if (j < scanned.length() && fits(sym, scanned.charAt(j))) {
            ok = reachable(syms, scanned, i + 1, j + 1, memo);
        }
        memo[i][j] = ok;
        return ok;
    }

    /**
     * A minta egy konkrét realizációja a szkennelt sorra vetítve (U/- sorozat).
     * {@code null}, ha nem illeszkedik.
     */
    public static String realize(String scanned, String pattern) {
        int[] consumed = align(scanned, pattern);
        if (consumed == null) {
            return null;
        }
        List<Symbol> syms = parse(pattern);
        StringBuilder sb = new StringBuilder(scanned.length());
        for (int i = 0; i < syms.size(); i++) {
            char sym = syms.get(i).symbol();
            if (sym == RESOLVE) {
                sb.append(consumed[i] == 1 ? LONG : "UU");
            } else if (sym == ANCEPS) {
                // a szkennelt sor dönt; ha az is eldöntetlen, hosszúnak írjuk ki
                char actual = scanned.charAt(sb.length());
                sb.append(actual == ANCEPS ? LONG : actual);
            } else {
                sb.append(sym);
            }
        }
        return sb.toString();
    }

    /**
     * Az iktust (verslábkezdetet) hordozó szótagok a szkennelt sorban.
     * {@code null}, ha a minta nem illeszkedik.
     */
    public static boolean[] ictusPositions(String scanned, String pattern) {
        int[] consumed = align(scanned, pattern);
        if (consumed == null) {
            return null;
        }
        List<Symbol> syms = parse(pattern);
        boolean[] ictus = new boolean[scanned.length()];
        int j = 0;
        for (int i = 0; i < syms.size(); i++) {
            if (syms.get(i).footStart() && j < ictus.length) {
                ictus[j] = true;
            }
            j += consumed[i];
        }
        return ictus;
    }
}
