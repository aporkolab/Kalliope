package hu.porkolab.kalliope;

import java.util.ArrayList;
import java.util.List;

/**
 * A magyaros (ütemhangsúlyos) sorfajták kánona.
 *
 * <p>A sorrend számít: az illesztő az elsőként felsorolt, idiomatikusabb formát
 * adja elöl. Ugyanarra a szótagszámra több tagolás is illeszkedhet — a
 * nyolcas lehet 4|4, 4|2|2 vagy 2|2|2|2 —, ezért a szóhatárokkal való egyezés
 * dönt köztük.
 *
 * <p>Források: Sulinet Tudásbázis — Az ütemhangsúlyos sorfajok; Wikipédia —
 * Ütemhangsúlyos verselés; Sulinet — A reneszánsz magyar verselés (Balassi-sor).
 */
public final class AccentualCanon {

    private AccentualCanon() {}

    // --- kétütemű sorok ---
    public static final AccentualForm OTOS_32 = form("ötös (3|2)", List.of(3, 2), 1, null);
    public static final AccentualForm OTOS_23 = form("ötös (2|3)", List.of(2, 3), 1, null);
    public static final AccentualForm FELEZO_HATOS = form("felező hatos", List.of(3, 3), 1, null);
    public static final AccentualForm HATOS_42 = form("hatos (4|2)", List.of(4, 2), 1, null);
    public static final AccentualForm HETES = form("kétütemű hetes", List.of(4, 3), 1, null);
    public static final AccentualForm FELEZO_NYOLCAS =
            form("felező nyolcas", List.of(4, 4), 1, "ősi nyolcas — a magyar népdal alapformája");
    public static final AccentualForm KILENCES_54 = form("kilences (5|4)", List.of(5, 4), 1, null);
    public static final AccentualForm FELEZO_TIZES = form("felező tízes", List.of(5, 5), 1, null);
    public static final AccentualForm FELEZO_TIZENKETTES =
            form("felező tizenkettes", List.of(6, 6), 1, "magyar alexandrin — Zrínyi, Arany");
    public static final AccentualForm FELEZO_TIZENNEGYES = form("felező tizennégyes", List.of(7, 7), 1, null);

    // --- háromütemű sorok ---
    public static final AccentualForm HAROMUTEMU_NYOLCAS = form("háromütemű nyolcas", List.of(4, 2, 2), 1, null);
    public static final AccentualForm HAROMUTEMU_KILENCES = form("háromütemű kilences", List.of(3, 3, 3), 2, null);
    public static final AccentualForm HAROMUTEMU_TIZES = form("háromütemű tízes", List.of(4, 4, 2), 1, null);
    public static final AccentualForm KANASZTANC =
            form("háromütemű tizenegyes", List.of(4, 4, 3), 1, "kanásztánc-ritmus");
    public static final AccentualForm BALASSI_SOR =
            form("Balassi-sor", List.of(6, 6, 7), 2, "a Balassi-strófa hosszú sora, belső rímekkel");

    // --- négyütemű sorok ---
    public static final AccentualForm NEGYUTEMU_NYOLCAS = form("négyütemű nyolcas", List.of(2, 2, 2, 2), 2, null);
    public static final AccentualForm NEGYUTEMU_TIZES = form("négyütemű tízes", List.of(4, 2, 2, 2), 1, null);
    public static final AccentualForm NEGYUTEMU_TIZENKETTES_3333 =
            form("négyütemű tizenkettes (3|3||3|3)", List.of(3, 3, 3, 3), 2, null);
    public static final AccentualForm NEGYUTEMU_TIZENKETTES_4242 =
            form("négyütemű tizenkettes (4|2||4|2)", List.of(4, 2, 4, 2), 2, null);
    public static final AccentualForm NEGYUTEMU_TIZENOTOS = form("négyütemű tizenötös", List.of(4, 4, 4, 3), 2, null);

    public static final List<AccentualForm> ALL = List.of(
            FELEZO_TIZENKETTES,
            NEGYUTEMU_TIZENKETTES_4242,
            NEGYUTEMU_TIZENKETTES_3333,
            FELEZO_NYOLCAS,
            HAROMUTEMU_NYOLCAS,
            NEGYUTEMU_NYOLCAS,
            KANASZTANC,
            HETES,
            FELEZO_HATOS,
            HATOS_42,
            OTOS_32,
            OTOS_23,
            HAROMUTEMU_KILENCES,
            KILENCES_54,
            FELEZO_TIZES,
            HAROMUTEMU_TIZES,
            NEGYUTEMU_TIZES,
            FELEZO_TIZENNEGYES,
            NEGYUTEMU_TIZENOTOS,
            BALASSI_SOR);

    /** Az adott szótagszámhoz tartozó formák, a kánon sorrendjében. */
    public static List<AccentualForm> bySyllableCount(int syllables) {
        List<AccentualForm> out = new ArrayList<>();
        for (AccentualForm f : ALL) {
            if (f.syllableCount() == syllables) {
                out.add(f);
            }
        }
        return List.copyOf(out);
    }

    private static AccentualForm form(String name, List<Integer> measures, int caesuraAfter, String note) {
        return AccentualForm.of(MetricCanon.slugOf(name), name, measures, caesuraAfter, note);
    }
}
