package hu.porkolab.kalliope;

import java.util.List;

/**
 * Példatár — rövid, közismert részletek, amelyeken az elemző viselkedése
 * ellenőrizhető. A {@code expected} mező azt mondja meg, mit vár a verstan; ez
 * egyben a golden tesztek forrása.
 */
public record Examples(String id, String title, String author, String expected, String text) {

    public static final Examples SZIGETI = new Examples(
            "szigeti-veszedelem",
            "Szigeti veszedelem (I. ének)",
            "Zrínyi Miklós, 1651",
            "Felező tizenkettes, hangsúlyos-magyaros vers — NEM időmértékes, "
                    + "ezért a klasszikus mértékillesztő helyesen nem ad rá találatot. Rímképlet: aaaa.",
            """
            Fegyvert, s vitézt éneklek, török hatalmát,
            Ki meg merte várni, Szulimán haragját,
            Ama nagy Szulimánnak hatalmas karját,
            Az kinek Europa rettegte szablyáját.""");

    public static final Examples ILIASZ = new Examples(
            "iliasz",
            "Iliász (I. ének, részlet)",
            "Homérosz — Devecseri Gábor fordítása",
            "Daktilikus hexameter, rímtelen.",
            """
            Haragot, istennő zengd Péleidész Akhileuszét,
            vészest, mely sokezer kínt szerzett minden akhájnak,
            mert sok hősnek erős lelkét Hádészra vetette,
            míg őket magukat zsákmányul a dögmadaraknak""");

    public static final Examples HETEDIK_ECLOGA = new Examples(
            "hetedik-ecloga", "Hetedik ecloga (részlet)", "Radnóti Miklós, 1944", "Daktilikus hexameter.", """
            Látod-e, esteledik s a szögesdróttal beszegett, vad
            tölgykerités, barak oly lebegő, felszívja az este.
            Rabságunk keretét elereszti a lassu tekintet
            és csak az ész, csak az ész, az tudja, a drót feszülését.""");

    public static final Examples NAGY_TITOK =
            new Examples("a-nagy-titok", "A nagy titok", "Kazinczy Ferenc", "Disztichon: hexameter + pentameter.", """
            Jót s jól! Ebben áll a nagy titok. Ezt ha nem érted,
            Szánts és vess, s hagyjad másnak az áldozatot.""");

    public static final Examples MAGYAROKHOZ =
            new Examples("a-magyarokhoz", "A magyarokhoz I. (részlet)", "Berzsenyi Dániel", "Alkaioszi strófa.", """
            Romlásnak indult hajdan erős magyar!
            Nem látod, Árpád vére miként fajul?
            Nem látod a bosszús egeknek
            Ostorait nyomorult hazádon?""");

    public static final Examples SZEPTEMBER_VEGEN = new Examples(
            "szeptember-vegen",
            "Szeptember végén (első szakasz)",
            "Petőfi Sándor, 1847",
            "Anapesztikus lejtésű sorok, keresztrím.",
            """
            Még nyílnak a völgyben a kerti virágok,
            Még zöldel a nyárfa az ablak előtt,
            De látod amottan a téli világot?
            Már hó takará el a bérci tetőt.""");

    public static final List<Examples> ALL =
            List.of(SZIGETI, ILIASZ, HETEDIK_ECLOGA, NAGY_TITOK, MAGYAROKHOZ, SZEPTEMBER_VEGEN);

    public static Examples byId(String id) {
        for (Examples e : ALL) {
            if (e.id().equals(id)) {
                return e;
            }
        }
        throw new IllegalArgumentException("Ismeretlen példa: " + id);
    }
}
