package hu.porkolab.kalliope;

import java.util.ArrayList;
import java.util.List;

/**
 * „Miért nem illeszkedik?” — a legközelebbi mérték és a pontos eltérés.
 *
 * <p>Ha egy sor egyetlen mértékre sem illik, az önmagában hű, de haszontalan
 * válasz. Sokkal többet mond, ha megnevezzük a legközelebbi formát, és
 * megmondjuk, <b>hányadik szótagon</b> és <b>mit</b> kellene másképp olvasni.
 *
 * <p>A távolságot ugyanaz a dinamikus programozás számolja, mint az illesztést,
 * csak nem elutasítja az eltérést, hanem gyűjti: minden pozíció, ahol a
 * szkennelt szótag és a minta összeegyeztethetetlen, egy pont.
 */
public final class NearMiss {

    /** Egy eltérés: hányadik szótagon, mi van ott, és mi kellene. */
    public record Difference(int syllable, char actual, char expected, String explanation) {

        static Difference of(int syllable, char actual, char expected) {
            return new Difference(
                    syllable,
                    actual,
                    expected,
                    (syllable + 1) + ". szótag: " + quantity(actual) + " helyett " + quantity(expected) + " kellene");
        }

        private static String quantity(char c) {
            return switch (c) {
                case Notation.LONG -> "hosszú";
                case Notation.SHORT -> "rövid";
                default -> "közös";
            };
        }
    }

    /** A legközelebbi mérték és az eltérések. */
    public record Result(Meter meter, List<Difference> differences, String summary) {

        public Result {
            differences = List.copyOf(differences);
        }

        static Result of(Meter meter, List<Difference> differences) {
            List<String> parts =
                    differences.stream().map(Difference::explanation).toList();
            String summary =
                    differences.isEmpty() ? meter.name() : meter.name() + " lenne, ha — " + String.join("; ", parts);
            return new Result(meter, differences, summary);
        }
    }

    /**
     * Ennél több eltérésnél már nem „majdnem" az a mérték. Kettőnél a javaslat
     * még tanulságos („ezen a két szótagon múlik"), háromnál viszont már csak
     * zaj: bármely tizenkét szótagos sor „majdnem" akármi.
     */
    private static final int MAX_DIFFERENCES = 2;

    private NearMiss() {}

    /**
     * A szkennelt sorhoz legközelebbi mérték, vagy {@code null}, ha egyik sincs
     * elég közel. Csak azonos szótagszámú mértékeket vizsgálunk: aminek más a
     * hossza, az nem „majdnem" ugyanaz.
     */
    public static Result closest(String scanned) {
        if (scanned == null || scanned.isEmpty()) {
            return null;
        }
        Result best = null;
        for (Meter meter : MetricCanon.LINES) {
            if (meter.fictive()) {
                continue;
            }
            List<Difference> diff = differences(scanned, meter.pattern());
            if (diff == null || diff.isEmpty() || diff.size() > MAX_DIFFERENCES) {
                continue;
            }
            // döntetlennél a kánonban előbb álló, közismertebb mérték nyer:
            // a hexameter hasznosabb javaslat, mint egy VNP-magánforma
            if (best == null
                    || diff.size() < best.differences().size()
                    || (diff.size() == best.differences().size()
                            && MetricCanon.LINES.indexOf(meter) < MetricCanon.LINES.indexOf(best.meter()))) {
                best = Result.of(meter, diff);
            }
        }
        return best;
    }

    /**
     * Az eltérések a minta ahhoz a realizációjához képest, amelyik a legkevesebb
     * ponton tér el. {@code null}, ha a hosszak eleve nem hozhatók össze.
     */
    static List<Difference> differences(String scanned, String pattern) {
        List<Notation.Symbol> symbols = Notation.parse(pattern);
        Integer[][] cost = new Integer[symbols.size() + 1][scanned.length() + 1];
        int[][] choice = new int[symbols.size() + 1][scanned.length() + 1];
        int total = cost(symbols, scanned, 0, 0, cost, choice);
        if (total >= INFEASIBLE) {
            return null;
        }
        List<Difference> out = new ArrayList<>();
        int i = 0;
        int j = 0;
        while (i < symbols.size()) {
            char sym = symbols.get(i).symbol();
            int consumed = choice[i][j];
            if (sym == Notation.RESOLVE) {
                char expected = consumed == 1 ? Notation.LONG : Notation.SHORT;
                for (int k = 0; k < consumed; k++) {
                    if (!fits(expected, scanned.charAt(j + k))) {
                        out.add(Difference.of(j + k, scanned.charAt(j + k), expected));
                    }
                }
            } else if (!fits(sym, scanned.charAt(j))) {
                out.add(Difference.of(j, scanned.charAt(j), sym));
            }
            j += consumed;
            i++;
        }
        return out;
    }

    private static final int INFEASIBLE = 1 << 20;

    private static boolean fits(char patternSymbol, char scannedSymbol) {
        return scannedSymbol == Notation.ANCEPS || patternSymbol == Notation.ANCEPS || patternSymbol == scannedSymbol;
    }

    private static int penalty(char patternSymbol, char scannedSymbol) {
        return fits(patternSymbol, scannedSymbol) ? 0 : 1;
    }

    private static int cost(
            List<Notation.Symbol> symbols, String scanned, int i, int j, Integer[][] memo, int[][] choice) {
        if (i == symbols.size()) {
            return j == scanned.length() ? 0 : INFEASIBLE;
        }
        if (j >= scanned.length()) {
            return INFEASIBLE;
        }
        Integer cached = memo[i][j];
        if (cached != null) {
            return cached;
        }
        char sym = symbols.get(i).symbol();
        int best;
        int taken;
        if (sym == Notation.RESOLVE) {
            int asLong = penalty(Notation.LONG, scanned.charAt(j)) + cost(symbols, scanned, i + 1, j + 1, memo, choice);
            int asShort = INFEASIBLE;
            if (j + 1 < scanned.length()) {
                asShort = penalty(Notation.SHORT, scanned.charAt(j))
                        + penalty(Notation.SHORT, scanned.charAt(j + 1))
                        + cost(symbols, scanned, i + 1, j + 2, memo, choice);
            }
            if (asLong <= asShort) {
                best = asLong;
                taken = 1;
            } else {
                best = asShort;
                taken = 2;
            }
        } else {
            best = penalty(sym, scanned.charAt(j)) + cost(symbols, scanned, i + 1, j + 1, memo, choice);
            taken = 1;
        }
        memo[i][j] = best;
        choice[i][j] = taken;
        return best;
    }
}
