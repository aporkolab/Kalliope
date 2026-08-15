package hu.porkolab.kalliope;

import java.util.List;

/**
 * Példatár — valódi, dokumentált formájú magyar versrészletek.
 *
 * <p>Ez egyben a motor aranyminta-korpusza: minden darab szövege hiteles
 * forrásból való (Wikiforrás, MEK, Sulinet szöveggyűjtemény), a {@code expected}
 * mező pedig azt mondja meg, mit állít róla a verstan. A hozzájuk tartozó
 * gépi elvárásokat a {@code CorpusTest} rögzíti.
 *
 * <p>Szándékosan van köztük olyan, amit a motor <b>nem</b> illeszt: a hangsúlyos-
 * magyaros vers nem időmértékes, és erre a helyes válasz a „nincs találat".
 */
public record Examples(String id, String title, String author, String expected, String text) {

    public static final Examples SZIGETI = new Examples(
            "szigeti-veszedelem",
            "Szigeti veszedelem (I. ének)",
            "Zrínyi Miklós, 1651",
            "Felező tizenkettes, hangsúlyos-magyaros vers — NEM időmértékes, ezért a klasszikus "
                    + "mértékillesztő helyesen nem ad rá találatot. Rímképlet: aaaa.",
            """
            Fegyvert, s vitézt éneklek, török hatalmát,
            Ki meg merte várni, Szulimán haragját,
            Ama nagy Szulimánnak hatalmas karját,
            Az kinek Europa rettegte szablyáját.""");

    public static final Examples TOLDI = new Examples(
            "toldi",
            "Toldi (Előhang után, I. ének)",
            "Arany János, 1846",
            "Felező tizenkettes, páros rím (aabb) — hangsúlyos vers, klasszikus mérték nélkül.",
            """
            Ég a napmelegtől a kopár szík sarja,
            Tikkadt szöcskenyájak legelésznek rajta;
            Nincs egy árva fűszál a tors közt kelőben,
            Nincs tenyérnyi zöld hely nagy határ mezőben.""");

    public static final Examples ILIASZ = new Examples(
            "iliasz",
            "Íliász (I. ének, részlet)",
            "Homérosz — Devecseri Gábor fordítása",
            "Daktilikus hexameter, rímtelen. A kezdősor első szótagja csak költői licenciával "
                    + "hosszú („a szókezdő hangsúly nyújtja meg”), ezért alapbeállítással nem illeszkedik.",
            """
            Haragot, istennő zengd Péleidész Akhileuszét,
            vészest, mely sokezer kínt szerzett minden akhájnak,
            mert sok hősnek erős lelkét Hádészra vetette,
            míg őket magukat zsákmányul a dögmadaraknak""");

    public static final Examples ZALAN = new Examples(
            "zalan-futasa",
            "Zalán futása (előhang)",
            "Vörösmarty Mihály, 1825",
            "Daktilikus hexameter, rímtelen.",
            """
            Régi dicsőségünk, hol késel az éji homályban?
            Századok ültenek el, s te alattok mélyen enyésző
            Fénnyel jársz egyedűl. Rajtad sürü fellegek, és a
            Bús feledékenység koszorútlan alakja lebegnek.""");

    public static final Examples HETEDIK_ECLOGA = new Examples(
            "hetedik-ecloga",
            "Hetedik ecloga (részlet)",
            "Radnóti Miklós, 1944",
            "Daktilikus hexameter, rímtelen.",
            """
            Látod-e, esteledik s a szögesdróttal beszegett, vad
            tölgykerités, barak oly lebegő, felszívja az este.
            Rabságunk keretét elereszti a lassu tekintet
            és csak az ész, csak az ész, az tudja, a drót feszülését.""");

    public static final Examples NAGY_TITOK = new Examples(
            "a-nagy-titok",
            "A nagy titok",
            "Kazinczy Ferenc, 1811",
            "Disztichon: hexameter + pentameter. A hexameter tizennégy szótagos, három " + "spondeusszal indul.",
            """
            Jót s jól! Ebben áll a nagy titok. Ezt ha nem érted,
            Szánts és vess, s hagyjad másnak az áldozatot.""");

    public static final Examples MAGYAROKHOZ = new Examples(
            "a-magyarokhoz",
            "A magyarokhoz I. (részlet)",
            "Berzsenyi Dániel",
            "Alkaioszi strófa: két alkaioszi tizenegyes, egy kilences, egy tízes.",
            """
            Romlásnak indult hajdan erős magyar!
            Nem látod, Árpád vére miként fajul?
            Nem látod a bosszús egeknek
            Ostorait nyomorult hazádon?""");

    public static final Examples KOZELITO_TEL = new Examples(
            "a-kozelito-tel",
            "A közelítő tél (első szakasz)",
            "Berzsenyi Dániel, 1804 után",
            "Első aszklepiadeszi strófa: három kis aszklepiadeszi sor és egy glükóni.",
            """
            Hervad már ligetünk, s díszei hullanak,
            Tarlott bokrai közt sárga levél zörög.
            Nincs rózsás labyrinth, s balzsamos illatok
            Közt nem lengedez a Zephyr.""");

    public static final Examples HORAC = new Examples(
            "horac",
            "Horác (első szakasz)",
            "Berzsenyi Dániel, 1799 körül",
            "Első aszklepiadeszi strófa, ugyanaz a forma, mint A közelítő télé.",
            """
            Zúg immár Boreas a Kemenes fölött,
            Zordon fergetegek rejtik el a napot,
            Nézd, a Ság tetejét hófuvatok fedik,
            S minden bús telelésre dőlt.""");

    public static final Examples SZEPTEMBER_VEGEN = new Examples(
            "szeptember-vegen",
            "Szeptember végén (első szakasz)",
            "Petőfi Sándor, 1847",
            "Anapesztikus lejtésű sorok, keresztrím (abab). A rímpárok asszonáncok: " + "virágok–világot, előtt–tetőt.",
            """
            Még nyílnak a völgyben a kerti virágok,
            Még zöldel a nyárfa az ablak előtt,
            De látod amottan a téli világot?
            Már hó takará el a bérci tetőt.""");

    public static final List<Examples> ALL = List.of(
            SZIGETI,
            TOLDI,
            ILIASZ,
            ZALAN,
            HETEDIK_ECLOGA,
            NAGY_TITOK,
            MAGYAROKHOZ,
            KOZELITO_TEL,
            HORAC,
            SZEPTEMBER_VEGEN);

    public static Examples byId(String id) {
        for (Examples e : ALL) {
            if (e.id().equals(id)) {
                return e;
            }
        }
        throw new IllegalArgumentException("Ismeretlen példa: " + id);
    }
}
