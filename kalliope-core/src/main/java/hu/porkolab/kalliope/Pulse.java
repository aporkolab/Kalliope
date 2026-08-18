package hu.porkolab.kalliope;

import java.util.List;

/**
 * Lüktetés: a sor élén álló leghosszabb <b>azonos verslábakból</b> álló sor.
 *
 * <p>Akkor szólal meg, amikor egyetlen kánoni sorfajta sem illeszkedik. Ilyenkor
 * eddig annyit mondtunk, hogy „nincs szabályos rend" — pedig a sor lüktetése
 * gyakran kihallható, csak nem tölt ki egy sorfajtát. Váradi Nagy Pál
 * tesztesete: az „Elmegy a kugli egy este berúgni me’ ő az a kugli ki nincs
 * fából” huszonegy szótag, hat daktilussal indul, aztán megszakad. Egyetlen
 * huszonegy szótagos sorfajta sincs, de a hat daktilus tény.
 *
 * <p><b>Három korlát</b>, hogy ez ne váljon mintakereséssé:
 *
 * <ol>
 *   <li><b>Sorfajtát nem állítunk.</b> A lüktetés nem mérték: nincs neve a
 *       kánonban, és nem is kap. Csak annyit mondunk, miből hány áll a sor élén.
 *   <li><b>Mindig megmondjuk, hol szakad meg.</b> A hallgatás félrevezetne: a
 *       sor nem daktilikus, hanem daktilussal <i>indul</i>.
 *   <li><b>Bizonyíték kell hozzá, nem engedély.</b> A közös szótag bármilyen
 *       lábba beleillik, tehát egy sok közös szótagos sorra minden „ráillene”.
 *       Ezért a lüktetés csak akkor áll, ha a futam pozícióinak legalább a fele
 *       a nyers skandálásban is <b>eldöntött</b>, és úgy egyezik.
 * </ol>
 *
 * <p>A lábak közül csak a <b>váltakozók</b> jönnek szóba (daktilus, anapesztus,
 * trocheus, jambus). A spondeus és a pirrichius ismétlése nem lüktetés, hanem
 * egyenletes hosszú, illetve rövid sor — abból nem hallatszik ritmus.
 */
public final class Pulse {

    /** Legalább ennyi láb kell, hogy lüktetésnek nevezzük. */
    private static final int MIN_FEET = 3;

    private Pulse() {}

    /**
     * Egy váltakozó versláb.
     *
     * @param pattern a láb jelölése, pl. {@code -UU}
     * @param name magyar neve, egyes számban
     * @param plural magyar neve, több lábra
     * @param adjective melléknévi alakja („daktilikus lüktetés")
     */
    private record Foot(String pattern, String name, String plural, String adjective) {}

    /** Sorrendjük egyben a döntetlen esetek eldöntése is. */
    private static final List<Foot> FEET = List.of(
            new Foot("-UU", "daktilus", "daktilus", "daktilikus"),
            new Foot("UU-", "anapesztus", "anapesztus", "anapesztikus"),
            new Foot("-U", "trocheus", "trocheus", "trochaikus"),
            new Foot("U-", "jambus", "jambus", "jambikus"));

    /**
     * A megtalált lüktetés.
     *
     * @param foot a láb jelölése ({@code -UU})
     * @param footName a láb magyar neve
     * @param footAdjective a láb melléknévi alakja — az összegzés ezt használja
     * @param feet hány láb áll egymás után
     * @param syllables hány szótagot fed a futam
     * @param breaksAt az első szótag indexe a futam UTÁN, vagy {@code -1}, ha a
     *     futam a sor végéig tart
     * @param resolved a teljes hosszúságsor: a futamon a láb szerint feloldva, a
     *     futam után a nyers skandálás. Ezt írja ki a felület — a feloldásnak
     *     így megnevezett forrása van, nem néma alapértelmezés.
     * @param summary emberi nyelvű összegzés
     * @param whole igaz, ha a lüktetés a sor végéig tart. Rekordkomponens, nem
     *     származtatott metódus: a szerializáló a metódust nem látja, és a
     *     mező sosem érne ki a felületre. Ugyanez a csapda vitte el korábban a
     *     {@code dualRhythm}-öt és a {@code division}-t.
     */
    public record Result(
            String foot,
            String footName,
            String footAdjective,
            int feet,
            int syllables,
            int breaksAt,
            String resolved,
            String summary,
            boolean whole) {

        static Result of(Foot foot, int feet, int syllables, int breaksAt, String resolved, String summary) {
            return new Result(
                    foot.pattern(),
                    foot.name(),
                    foot.adjective(),
                    feet,
                    syllables,
                    breaksAt,
                    resolved,
                    summary,
                    breaksAt < 0);
        }
    }

    /**
     * A sor élének lüktetése, vagy {@code null}, ha nincs kimutatható.
     *
     * @param scansion a nyers skandálás, közös ({@code ?}) jelekkel
     */
    public static Result detect(String scansion) {
        if (scansion == null || scansion.length() < MIN_FEET * 2) {
            return null;
        }
        Result best = null;
        for (Foot foot : FEET) {
            Result candidate = run(scansion, foot);
            if (candidate != null && (best == null || candidate.syllables() > best.syllables())) {
                best = candidate;
            }
        }
        return best;
    }

    private static Result run(String scansion, Foot foot) {
        int width = foot.pattern().length();
        int feet = 0;
        int decided = 0;
        int at = 0;
        while (at + width <= scansion.length()) {
            boolean fits = true;
            int decidedHere = 0;
            for (int k = 0; k < width; k++) {
                char raw = scansion.charAt(at + k);
                if (raw == Notation.ANCEPS) {
                    continue;
                }
                if (raw != foot.pattern().charAt(k)) {
                    fits = false;
                    break;
                }
                decidedHere++;
            }
            if (!fits) {
                break;
            }
            feet++;
            decided += decidedHere;
            at += width;
        }
        int covered = feet * width;
        if (feet < MIN_FEET) {
            return null;
        }
        // A futam a sor felét fedje: három trocheus egy húsz szótagos sorban nem
        // a sor lüktetése, csak egy szakasza.
        if (covered * 2 < scansion.length()) {
            return null;
        }
        // És legyen bizonyítva: a pozíciók fele a nyers skandálásban is eldőlt.
        if (decided * 2 < covered) {
            return null;
        }
        StringBuilder resolved = new StringBuilder(scansion.length());
        for (int i = 0; i < covered; i++) {
            resolved.append(foot.pattern().charAt(i % width));
        }
        resolved.append(scansion, covered, scansion.length());
        int breaksAt = covered < scansion.length() ? covered : -1;
        return Result.of(foot, feet, covered, breaksAt, resolved.toString(), summary(foot, feet, breaksAt));
    }

    private static String summary(Foot foot, int feet, int breaksAt) {
        String head = feet + " " + foot.plural() + " a sor élén";
        return breaksAt < 0
                ? head + " — a lüktetés végig tart, de egyetlen kánoni sorfajta sem ilyen hosszú"
                : head + " — a " + (breaksAt + 1) + ". szótagnál megszakad";
    }
}
