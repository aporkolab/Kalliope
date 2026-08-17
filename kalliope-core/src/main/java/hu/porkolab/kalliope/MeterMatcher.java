package hu.porkolab.kalliope;

import java.util.ArrayList;
import java.util.List;

/**
 * Sor- és szakaszmérték-illesztés.
 *
 * <p>Szigorú: hosszú↔hosszú, rövid↔rövid, közömbös↔bármelyik. Nincs feltételezett
 * költői licencia — ha egy sor így nem illeszkedik, az a hű válasz.
 *
 * <p>Az illesztés {@link Notation#matches} pozíciónkénti dinamikus programozással
 * történik, nem a minta realizációinak kifejtésével: utóbbi a szabad pozíciók
 * számában exponenciális, és egy sok eldöntetlen szótagot tartalmazó soron
 * kezelhetetlenné vált (a korábbi változat 8192 realizáció fölött CSONKOLT, és a
 * csonkolt előtagokat hasonlította össze, ami hamis találatokat adott).
 */
public final class MeterMatcher {

    /** Egy találat: melyik mérték, milyen realizációval, hol vannak az iktusok. */
    public record Match(Meter meter, String realization, List<Integer> ictusSyllables) {}

    /** Szakaszmérték-találat: hányszor ismétlődik a forma a szakaszban. */
    public record StanzaMatch(StanzaForm form, int repetitions, boolean rhymeSchemeMatches) {}

    private MeterMatcher() {}

    /**
     * A szkennelt sorra illeszkedő mértékek. Elsőként a valódi sorfajták és
     * összetett sorok, utánuk — ha kérik — a kolónok és verslábak, hogy a
     * sornál kisebb egységek is megnevezhetők legyenek (adoniszi, hémiepesz).
     * A fiktív segédmértékek sosem kerülnek a találatok közé.
     */
    public static List<Match> matchLine(String scanned, boolean includeSmallUnits) {
        List<Match> hits = new ArrayList<>();
        collect(scanned, MetricCanon.LINES, hits);
        collect(scanned, MetricCanon.COMPLEXES, hits);
        if (includeSmallUnits) {
            collect(scanned, MetricCanon.COLA, hits);
            collect(scanned, MetricCanon.FEET, hits);
        }
        return List.copyOf(hits);
    }

    private static void collect(String scanned, List<Meter> meters, List<Match> into) {
        for (Meter m : meters) {
            if (m.fictive()) {
                continue;
            }
            String realization = Notation.realize(scanned, m.pattern());
            if (realization == null) {
                continue;
            }
            boolean[] ictus = Notation.ictusPositions(scanned, m.pattern());
            List<Integer> positions = new ArrayList<>();
            if (ictus != null) {
                for (int i = 0; i < ictus.length; i++) {
                    if (ictus[i]) {
                        positions.add(i);
                    }
                }
            }
            into.add(new Match(m, realization, List.copyOf(positions)));
        }
    }

    /**
     * A szakaszra illeszkedő szakaszmértékek.
     *
     * <p>Soronként több <i>olvasat</i> is jöhet (összevont kettőshangzó), és a sor
     * akkor illeszkedik a forma adott sorára, ha bármelyik olvasata illeszkedik.
     *
     * <p>A zárt formák (az eredeti adatban {@code #…#}) pontosan annyi sorból
     * állnak, ahány soruk van. A nem zárt formák — mindenekelőtt a disztichon —
     * <b>ismétlődhetnek</b>: egy hatsoros elégia három disztichon, nem pedig
     * „nincs találat". A korábbi változat kizárólag pontos sorszám-egyezést
     * fogadott el, ezért többstrófás versen sosem talált semmit.
     *
     * @param lineReadings soronként a lehetséges szkennelések
     */
    public static List<StanzaMatch> matchStanza(List<List<String>> lineReadings, String detectedRhyme) {
        List<StanzaMatch> hits = new ArrayList<>();
        for (StanzaForm form : MetricCanon.STANZAS) {
            int n = form.lineCount();
            if (n == 0 || lineReadings.isEmpty() || lineReadings.size() % n != 0) {
                continue;
            }
            int repetitions = lineReadings.size() / n;
            if (repetitions > 1 && form.closed()) {
                continue;
            }
            boolean all = true;
            for (int i = 0; i < lineReadings.size() && all; i++) {
                String expected = form.lines().get(i % n).pattern();
                all = anyMatches(lineReadings.get(i), expected);
            }
            if (!all) {
                continue;
            }
            boolean rhymeOk =
                    !form.hasRhymeScheme() || (detectedRhyme != null && detectedRhyme.equals(form.rhymeScheme()));
            hits.add(new StanzaMatch(form, repetitions, rhymeOk));
        }
        return List.copyOf(hits);
    }

    private static boolean anyMatches(List<String> readings, String pattern) {
        for (String r : readings) {
            if (Notation.matches(r, pattern)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Az ütemhangsúly-sor a bináris jeleivel: {@code Ú}/{@code ÷} az iktust viselő
     * rövid, illetve hosszú szótag. A realizáció az, amelyik ténylegesen
     * illeszkedik a szkennelt sorra — nem az első azonos hosszúságú.
     */
    public static String ictusRow(Match match) {
        String r = match.realization();
        StringBuilder sb = new StringBuilder(r.length());
        for (int i = 0; i < r.length(); i++) {
            boolean ictus = match.ictusSyllables().contains(i);
            char c = r.charAt(i);
            if (c == Notation.LONG) {
                sb.append(ictus ? '÷' : '-');
            } else {
                sb.append(ictus ? 'Ú' : 'U');
            }
        }
        return sb.toString();
    }
}
