package hu.porkolab.kalliope;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * A metrikai kánon — verslábak, kolónok, sorfajták, összetett sorok és
 * szakaszmértékek, típusos adatként.
 *
 * <p>Az adat Váradi Nagy Pál (vnp85) munkája; a {@code VNP sorfajták} és
 * {@code VNP-strófák} szekció az ő saját versformái.
 *
 * <p>Az eredeti, 2004–2006-os Delphi-program ezt egy saját szintaxisú szövegfájlból
 * ({@code kalliope.txt}) olvasta: {@code ;} komment, {@code .} mezőnév,
 * {@code !} beállítás, {@code @}/{@code #define} konstans, {@code #complex},
 * {@code $} hangsúlytalan szó, {@code #start_strofa} blokk. Az a formátum
 * futásidejű elemzést és névfeloldást igényelt, és épp ebből származott a
 * hibák többsége: elgépelt hivatkozás ({@code nib.alex.1.fiktiv} ↔
 * {@code .fictive}), némán elnyelt feloldási hiba, kis/nagybetű-érzékeny
 * konstansnév, körkörös hivatkozás. Itt a kánon Java-adat: a hivatkozás
 * <b>objektumhivatkozás</b>, tehát elgépelni nem lehet, és nincs parser, amiben
 * hiba lehetne.
 *
 * <p>A klasszikus versmérték-kánon gyakorlatilag zárt halmaz, ezért nem
 * változik futásidőben — nincs mögötte adatbázis, és nincs is rá szükség.
 */
public final class MetricCanon {

    private MetricCanon() {}

    // ================================================================== //
    //  Verslábak                                                         //
    // ================================================================== //

    public static final Meter TA = foot("-", "ta", "nem éppen láb ez");
    public static final Meter TI = foot("U", "ti", "nem éppen láb ez");
    public static final Meter JAMBUS = foot("U-", "jambus");
    public static final Meter TROCHEUS = foot("-U", "trocheus");
    public static final Meter DAKTILUS = foot("-UU", "daktilus");
    public static final Meter ANAPESZTUS = foot("UU-", "anapesztus");
    public static final Meter SPONDEUSZ = foot("--", "spondeusz");
    public static final Meter PIRRICHIUS = foot("UU", "pirrichius");
    public static final Meter MOLOSSZUS = foot("---", "molosszus");
    public static final Meter TRIBRACHISZ = foot("UUU", "tribrachisz");
    public static final Meter PROCELEUZMATIKUS = foot("UUUU", "proceleuzmatikus");

    public static final List<Meter> FEET = List.of(
            TA,
            TI,
            JAMBUS,
            TROCHEUS,
            DAKTILUS,
            ANAPESZTUS,
            SPONDEUSZ,
            PIRRICHIUS,
            MOLOSSZUS,
            TRIBRACHISZ,
            PROCELEUZMATIKUS);

    // ================================================================== //
    //  Kolónok                                                           //
    // ================================================================== //

    public static final Meter BACCHIUS = colon("U--", "bacchius kolón");
    public static final Meter PALIMBACCHIUS = colon("--U", "palimbacchius kolón");
    public static final Meter KRETIKUS = colon("-U-", "krétikus kolón");
    public static final Meter AMPHIBRACHISZ = colon("U-U", "amphibrachisz kolón");

    public static final Meter PAION_1 = colon("-UUU", "1. paión kolón");
    public static final Meter PAION_2 = colon("U-UU", "2. paión kolón");
    public static final Meter PAION_3 = colon("UU-U", "3. paión kolón");
    public static final Meter PAION_4 = colon("UUU-", "4. paión kolón");

    public static final Meter EPITRITUS_1 = colon("U---", "1. epitritus kolón");
    public static final Meter EPITRITUS_2 = colon("-U--", "2. epitritus kolón");
    public static final Meter EPITRITUS_3 = colon("--U-", "3. epitritus kolón");
    public static final Meter EPITRITUS_4 = colon("---U", "4. epitritus kolón");

    public static final Meter IONICUS_A_MINORE = colon("UU--", "ionicus a minore kolón");
    public static final Meter IONICUS_A_MAIORE = colon("--UU", "ionicus a maiore kolón");
    public static final Meter CHORIAMBUS = colon("-UU-", "choriambus kolón");
    public static final Meter ANTISZPASZTUS = colon("U--U", "antiszpasztus kolón");

    public static final Meter DOCHMIUS = colon("?--U-", "dochmius");
    public static final Meter ADONISZI = colon("-UU-?", "adoniszi kolón", "sapphoi 4.");
    public static final Meter HIPODOCHMIUS =
            colon("-U-U-", "hipodochmius kolón", "az eredeti adatban bizonytalan (???)");

    public static final Meter REIZIANUS = colon("?-UU--", "fejetlen pherekrateus kolón", "reiziánus");
    public static final Meter ITHÜPHALLIKUS =
            colon("-U-U--", "ithüphallikus kolón", "az eredeti adatban bizonytalan (???)");
    public static final Meter MAECENAS = colon("---UU-", "Maecenas atavis-kolón");

    public static final Meter HEMIEPESZ = colon("-UU-UU-", "hémiepesz kolón");
    public static final Meter LEKÜTHION = colon("-U-U-U-", "léküthion kolón");
    public static final Meter TELESZILLEION =
            colon("?-UU-U?", "téleszilleion kolón", "az eredeti adatban bizonytalan (???)");
    public static final Meter FEJETLEN_WILAMOVITZIANUS =
            colon("???-UU-", "fejetlen wilamovitziánus kolón", "az eredeti adatban bizonytalan (???)");
    public static final Meter FEJETLEN_GLÜKONI = colon("?-UU-U-", "fejetlen glükóni kolón");
    public static final Meter PHEREKRATEUS = colon("??-UU--", "pherekrateus kolón");

    public static final Meter WILAMOVITZIANUS =
            colon("????-UU-", "wilamovitziánus kolón", "az eredeti adatban bizonytalan (???)");
    public static final Meter PROSZODIAKUS =
            colon("?-UU-UU-", "proszodiákus kolón", "az eredeti adatban bizonytalan (???)");
    public static final Meter GLÜKONI = colon("??-UU-U-", "glükóni kolón");
    public static final Meter MASODIK_GLÜKONI = colon("---UU-U-", "második glükóni kolón");
    public static final Meter HEMIEPESZ_2 = colon("-UU-UU-?", "hémiepesz kolón 2");
    public static final Meter FEJETLEN_HIPPONAKTEUS = colon("?-UU-U-?", "fejetlen hipponakteus kolón");

    public static final Meter ENASZMONIDEUS =
            colon("?-UU-UU-?", "enaszmonideus kolón", "az eredeti adatban bizonytalan (???)");
    public static final Meter ENOPLION = colon("?-UU-UU--", "enoplion kolón");
    public static final Meter HIPPONAKTEUS = colon("??-UU-U-?", "hipponakteus kolón");

    public static final Meter ENKOMIOLOGIKON = colon("-UU-UU-||--U-?", "Enkomiologikon kolón");

    public static final List<Meter> COLA = List.of(
            BACCHIUS,
            PALIMBACCHIUS,
            KRETIKUS,
            AMPHIBRACHISZ,
            PAION_1,
            PAION_2,
            PAION_3,
            PAION_4,
            EPITRITUS_1,
            EPITRITUS_2,
            EPITRITUS_3,
            EPITRITUS_4,
            IONICUS_A_MINORE,
            IONICUS_A_MAIORE,
            CHORIAMBUS,
            ANTISZPASZTUS,
            DOCHMIUS,
            ADONISZI,
            HIPODOCHMIUS,
            REIZIANUS,
            ITHÜPHALLIKUS,
            MAECENAS,
            HEMIEPESZ,
            LEKÜTHION,
            TELESZILLEION,
            FEJETLEN_WILAMOVITZIANUS,
            FEJETLEN_GLÜKONI,
            PHEREKRATEUS,
            WILAMOVITZIANUS,
            PROSZODIAKUS,
            GLÜKONI,
            MASODIK_GLÜKONI,
            HEMIEPESZ_2,
            FEJETLEN_HIPPONAKTEUS,
            ENASZMONIDEUS,
            ENOPLION,
            HIPPONAKTEUS,
            ENKOMIOLOGIKON);

    // ================================================================== //
    //  Sorfajták                                                         //
    // ================================================================== //

    public static final Meter HEXAMETER = line("-=|-=|-=|-=|-UU|-?", "hexameter");
    public static final Meter VERSUS_SPONDIACUS = line(
            "-=|-=|-=|-=|--|-?",
            "versus spondiacus",
            "spondeuszi ötödik lábú hexameter — az eredeti adatban nem szerepelt");
    public static final Meter PENTAMETER = line("-=|-=|-||-UU|-UU|?", "pentameter");
    public static final Meter PHALAIKOSZI = line("UU-UU-U-U--", "phalaikoszi");
    public static final Meter HENDECASYLLABUS_B = line("U--UU-U-U-?", "hendecasyllabus B");
    public static final Meter HENDECASYLLABUS_A = line("-?-UU-U-U-?", "hendecasyllabus A");

    public static final Meter CHOLIAMBUS = corrected(
            line("?-U-?-U-U--?", "choliambus", "sánta jambus, szkázón"),
            "?-U-?-U-U-U?",
            "A sánta jambust az utolsó előtti HOSSZÚ pozíció definiálja; e nélkül a minta "
                    + "közönséges jambikus trimeter volt, és egyetlen valódi szkázónt sem ismert fel.",
            "https://en.wikipedia.org/wiki/Choliamb");

    public static final Meter TROCHAICUS_4MTR = corrected(
            line("-U-?-U-?-U-?-U-", "trochaikus tetrameter (katalektikus)"),
            "-U-U-U-U-U-U-U-",
            "A trochaikus metrum második eleme közömbös. Fix rövidként a minta csak a teljesen "
                    + "„tiszta” sorokra illett, a valódi görög-latin gyakorlat szinte minden sorát elutasította.",
            "https://en.wikipedia.org/wiki/Trochaic_septenarius");

    public static final Meter ALEXANDRIN_A = line("-U--U-||-UU-U-?", "francia alexandrin A");
    public static final Meter ALEXANDRIN_B = line("U-U---||---UU?", "francia alexandrin B");
    public static final Meter ALEXANDRIN_C = line("U---U?||U--UU-?", "francia alexandrin C");
    public static final Meter ALEXANDRIN_D = line("U---U-||U--UU?", "francia alexandrin D");
    public static final Meter NIB_ALEX_1 = fictiveLine("?-?-U--", "nibelungizált alexandrin első fele");

    public static final Meter SZAPPHOI = line("-U---||UU-U-?", "szapphói sor");
    public static final Meter TRIMETER_IAMBICUS_TISZTA =
            line("U-U-U-U-U-U?", "trimeter iambicus (tiszta)", "az eredeti adatban ugyanazon a néven állt");
    public static final Meter TRIMETER_IAMBICUS = line("?-U-?-U-?-U-", "trimeter iambicus");

    public static final Meter GLYKONI_1A = line("-?-UU-U-", "glykoni 1a");
    public static final Meter GLYKONI_1B = line("U--UU-U-", "glykoni 1b");

    public static final Meter ASZKLEPIADESZI_A123 = line("---UU--UU-U?", "aszklepiadeszi A123", "kis aszklepiadeszi");
    public static final Meter ASZKLEPIADESZI_B4 = line("---UU-U?", "aszklepiadeszi B4", "glükóni; ez a D13 sor is");
    public static final Meter ASZKLEPIADESZI_C3 = line("---UU--", "aszklepiadeszi C3", "pherekrateus");
    public static final Meter ASZKLEPIADESZI_E1234 =
            line("---UU--UU--UU-U?", "aszklepiadeszi E1234", "nagy aszklepiadeszi");

    /**
     * A negyedik aszklepiadeszi strófa rövid sora. Az eredeti adat itt hét
     * pozíciós pherekrateust írt; a forma valójában nyolc pozíciós glükóni,
     * tehát azonos a B4 sorral — ezért ugyanaz az objektum.
     */
    public static final Meter ASZKLEPIADESZI_D13 = ASZKLEPIADESZI_B4;

    public static final Meter ALKAIOSZI_12 = line("?-U-?||-UU-U?", "alkaioszi 12", "alk. 11, nagy alkaioszi");
    public static final Meter ALKAIOSZI_3 = corrected(
            line("?-U-?-U-?", "alkaioszi 3", "ötödfeles jambus, alkaioszi kilences"),
            "?-U-U-U-?",
            "Az alkaioszi kilences ötödik pozíciója közömbös — Horatiusnál rendre hosszú. "
                    + "Fix rövidként a teljes alkaioszi strófa illeszthetetlen volt.",
            "https://en.wikipedia.org/wiki/Alcaic_stanza");
    public static final Meter ALKAIOSZI_4 = line("-UU-UU-U-?", "alkaioszi 4", "kis alkaioszi, alkaioszi tízes");

    public static final Meter ANAKREONI_8 = line("UU-U-U-?", "anakreóni 8");
    public static final Meter ANAKREONI_7 = line("U-U-U-?", "anakreóni 7", "negyedfeles jambikus");
    public static final Meter ANAKREONI_16 = line("UU--|UU--||UU--|UU--", "anakreóni 16", "négy ión a minore");

    /**
     * A szerző webes változatában {@code valami_anakreon} néven szerepel; ión a
     * minore nyitás után jambikus folytatás. A bizonytalan nevet megtartjuk,
     * mert a besorolás maga is bizonytalan.
     */
    public static final Meter VALAMI_ANAKREON = line("UU--UU-U-U-U-?", "anakreóni-féle sor", "valami_anakreon");

    public static final Meter SZEPT_VEGEN_1 = line("?-UU-UU-UU-?", "Szeptember végén 1");
    public static final Meter SZEPT_VEGEN_2 = line("?-UU-UU-UU?", "Szeptember végén 2");

    // --- VNP sorfajták ---
    public static final Meter GYILKOSOK = line("U-UU-UU-?", "Gyilkosok");
    public static final Meter EHESEK = line("UU-U-UU-?", "Éhesek");
    public static final Meter PINCSIKE_1A = line("-U-U-U-U", "Pincsike1.a");
    public static final Meter PINCSIKE_1B = line("-U-U-U?", "Pincsike1.b");
    public static final Meter MELYEGI_ALOM_1 = line("UU-UU-UU-", "mélyégi álom 1");
    public static final Meter MELYEGI_ALOM_2 = line("UU-UU-UU-UU-", "mélyégi álom 2");
    public static final Meter UTOLSO_MOSAS_1 = line("--U-UU--U-", "utolsó mosás 1");
    public static final Meter UTOLSO_MOSAS_2 = line("U--U--UU-U", "utolsó mosás 2");
    public static final Meter UTOLSO_MOSAS_3 = line("---------", "utolsó mosás 3");
    public static final Meter LETEPARTJA = line("?-?-U-U--", "létépartja");
    public static final Meter MOZDONYSZONETT_A = line("UUUU-UU-U-?", "Mozdonyszonett a");
    public static final Meter MOZDONYSZONETT_B = line("UUUU-U-U-?", "Mozdonyszonett b");
    public static final Meter HAL_EJI_ENEKE =
            line("-UU---UUUU---UUUU---UUUU---UUUU---UU-", "hal éji éneke", "Morgenstern formája, Parti Nagy nyomán");

    public static final Meter BI_TROCHEUS = fictiveLine("-U-U", "két trocheus");
    public static final Meter BI_JAMBUS = fictiveLine("U-U-", "két jambus");
    public static final Meter BI_SPONDEUSZ = fictiveLine("----", "két spondeusz");
    public static final Meter BI_DAKTILUS = fictiveLine("-UU-UU", "két daktilus");
    public static final Meter BI_ANAPESZTUS = fictiveLine("UU-UU-", "két anapesztus");

    // --- Kiegészítő standard antik sorfajták ---
    public static final Meter IAMBIKUS_MONOMETER = line("?-U-", "iambikus metrum (monométer)");
    public static final Meter IAMBIKUS_DIMETER = line("?-U-?-U-", "iambikus dimeter");
    public static final Meter IAMBIKUS_TETRAMETER = line("?-U-?-U-?-U-?-U-", "iambikus tetrameter (octonarius)");
    public static final Meter ANAPESZTIKUS_DIMETER = corrected(
            line("=-|=-|=-|=-", "anapesztikus dimeter", "spondeusz-helyettesítés megengedett"),
            "UU-UU-UU-UU-",
            "A név spondeusz-helyettesítést ígért, a minta viszont tiszta anapesztusokat "
                    + "követelt. A '=' jel épp erre való.",
            "https://antigonejournal.com/wp-content/uploads/2021/05/Metre-X.pdf");
    public static final Meter ION_A_MINORE_DIMETER = line("UU--UU--", "ión a minore dimeter");
    public static final Meter ION_A_MAIORE_DIMETER = line("--UU--UU", "ión a maiore dimeter");
    public static final Meter DAKTILIKUS_TETRAMETER = corrected(
            line("-=|-=|-=|-=", "daktilikus tetrameter", "spondeusz-helyettesítés megengedett"),
            "-UU-UU-UU-UU",
            "Ugyanaz, mint az anapesztikus dimeternél: a megjegyzés és a minta ellentmondott egymásnak.",
            "https://antigonejournal.com/wp-content/uploads/2021/05/Metre-X.pdf");

    public static final List<Meter> LINES = List.of(
            HEXAMETER,
            VERSUS_SPONDIACUS,
            PENTAMETER,
            PHALAIKOSZI,
            HENDECASYLLABUS_B,
            HENDECASYLLABUS_A,
            CHOLIAMBUS,
            TROCHAICUS_4MTR,
            ALEXANDRIN_A,
            ALEXANDRIN_B,
            ALEXANDRIN_C,
            ALEXANDRIN_D,
            NIB_ALEX_1,
            SZAPPHOI,
            TRIMETER_IAMBICUS_TISZTA,
            TRIMETER_IAMBICUS,
            GLYKONI_1A,
            GLYKONI_1B,
            ASZKLEPIADESZI_A123,
            ASZKLEPIADESZI_B4,
            ASZKLEPIADESZI_C3,
            ASZKLEPIADESZI_E1234,
            ALKAIOSZI_12,
            ALKAIOSZI_3,
            ALKAIOSZI_4,
            ANAKREONI_8,
            ANAKREONI_7,
            ANAKREONI_16,
            VALAMI_ANAKREON,
            SZEPT_VEGEN_1,
            SZEPT_VEGEN_2,
            GYILKOSOK,
            EHESEK,
            PINCSIKE_1A,
            PINCSIKE_1B,
            MELYEGI_ALOM_1,
            MELYEGI_ALOM_2,
            UTOLSO_MOSAS_1,
            UTOLSO_MOSAS_2,
            UTOLSO_MOSAS_3,
            LETEPARTJA,
            MOZDONYSZONETT_A,
            MOZDONYSZONETT_B,
            HAL_EJI_ENEKE,
            BI_TROCHEUS,
            BI_JAMBUS,
            BI_SPONDEUSZ,
            BI_DAKTILUS,
            BI_ANAPESZTUS,
            IAMBIKUS_MONOMETER,
            IAMBIKUS_DIMETER,
            IAMBIKUS_TETRAMETER,
            ANAPESZTIKUS_DIMETER,
            ION_A_MINORE_DIMETER,
            ION_A_MAIORE_DIMETER,
            DAKTILIKUS_TETRAMETER);

    // ================================================================== //
    //  Összetett sorok                                                   //
    // ================================================================== //

    public static final Meter BI_ADONISZI = complex("két adoniszi", true, ADONISZI, ADONISZI);
    public static final Meter MELYEGI_ALOM_NAGYSOR =
            complex("mélyégi álom nagysor", false, MELYEGI_ALOM_1, MELYEGI_ALOM_2);
    public static final Meter NIB_ALEX_A = complex("nibelungizált alexandrin 1", false, NIB_ALEX_1, "||--|--|U-|?");
    public static final Meter NIB_ALEX_B = complex("nibelungizált alexandrin 2", false, NIB_ALEX_1, "||--|U-|--|?");
    public static final Meter NIB_ALEX_C = complex("nibelungizált alexandrin 3", false, NIB_ALEX_1, "||-U|--|U?");
    public static final Meter NIB_ALEX_D = complex("nibelungizált alexandrin 4", false, NIB_ALEX_1, "||U-|U-|U?");
    public static final Meter NIB_ALEX_E = complex("nibelungizált alexandrin 5", false, NIB_ALEX_1, "||--|U-|U-");
    public static final Meter NIB_ALEX_F = complex("nibelungizált alexandrin 6", false, NIB_ALEX_1, "||--|--|U-");

    public static final List<Meter> COMPLEXES = List.of(
            BI_ADONISZI, MELYEGI_ALOM_NAGYSOR, NIB_ALEX_A, NIB_ALEX_B, NIB_ALEX_C, NIB_ALEX_D, NIB_ALEX_E, NIB_ALEX_F);

    // ================================================================== //
    //  Szakaszmértékek                                                   //
    // ================================================================== //

    public static final List<StanzaForm> STANZAS = List.of(
            closed("aszklepiadeszi A", repeat(ASZKLEPIADESZI_A123, 4), null),
            closed(
                    "aszklepiadeszi B",
                    List.of(ASZKLEPIADESZI_A123, ASZKLEPIADESZI_A123, ASZKLEPIADESZI_A123, ASZKLEPIADESZI_B4),
                    null),
            closed(
                    "aszklepiadeszi C",
                    List.of(ASZKLEPIADESZI_A123, ASZKLEPIADESZI_A123, ASZKLEPIADESZI_C3, ASZKLEPIADESZI_B4),
                    null),
            closed(
                    "aszklepiadeszi D",
                    List.of(ASZKLEPIADESZI_D13, ASZKLEPIADESZI_A123, ASZKLEPIADESZI_D13, ASZKLEPIADESZI_A123),
                    null),
            closed("aszklepiadeszi E", repeat(ASZKLEPIADESZI_E1234, 4), null),
            // Két további Horatius-strófa, a szerző webes változatából. Nem
            // önálló forma: az A–E sorainak más sorrendje, de a versek, amelyeken
            // Horatius használja, e nélkül nem kapnának szakaszmértéket.
            // F: „Míg én voltam a kedvesed…”, G: „Úgy futsz, félve, Chloé…”
            closed(
                    "aszklepiadeszi F",
                    List.of(ASZKLEPIADESZI_B4, ASZKLEPIADESZI_A123, ASZKLEPIADESZI_B4, ASZKLEPIADESZI_A123),
                    null),
            closed(
                    "aszklepiadeszi G",
                    List.of(ASZKLEPIADESZI_A123, ASZKLEPIADESZI_A123, ASZKLEPIADESZI_D13, ASZKLEPIADESZI_B4),
                    null),
            closed("alkaioszi strófa", List.of(ALKAIOSZI_12, ALKAIOSZI_12, ALKAIOSZI_3, ALKAIOSZI_4), null),
            closed("szapphói strófa", List.of(SZAPPHOI, SZAPPHOI, SZAPPHOI, ADONISZI), null),
            closed(
                    "Szeptember végén",
                    List.of(
                            SZEPT_VEGEN_1,
                            SZEPT_VEGEN_2,
                            SZEPT_VEGEN_1,
                            SZEPT_VEGEN_2,
                            SZEPT_VEGEN_1,
                            SZEPT_VEGEN_2,
                            SZEPT_VEGEN_1,
                            SZEPT_VEGEN_2),
                    "ababcdcd"),
            open("disztichon", List.of(HEXAMETER, PENTAMETER), null),
            open("anakreóni 16 szakasz", repeat(IONICUS_A_MINORE, 4), null),
            closed("hal éji éneke", halEjiEneke(), null),
            closed(
                    "Pincsike1",
                    List.of(PINCSIKE_1A, PINCSIKE_1B, PINCSIKE_1A, PINCSIKE_1B, PINCSIKE_1B, PINCSIKE_1B),
                    "xaxabb"),
            closed("Komposzt", List.of(EHESEK, GYILKOSOK, EHESEK, GYILKOSOK), "abab"),
            closed("Gyilkosok szakasz", repeat(GYILKOSOK, 4), "xaxa"),
            closed("Éhesek szakasz", repeat(EHESEK, 4), "xaxa"),
            open("mélyégi álom: nagysor", List.of(MELYEGI_ALOM_1, MELYEGI_ALOM_2), null),
            closed("mélyégi álom: nagy szakasz", repeat(MELYEGI_ALOM_NAGYSOR, 4), null),
            closed(
                    "mélyégi álom: kis szakasz",
                    List.of(MELYEGI_ALOM_1, MELYEGI_ALOM_2, MELYEGI_ALOM_1, MELYEGI_ALOM_2),
                    null));

    // ================================================================== //
    //  Hangsúlytalan szavak és beállítások                               //
    // ================================================================== //

    /** Névutók, névelők, kötőszók, amelyek sosem viselik az ütemhangsúlyt. */
    public static final Set<String> UNSTRESSED_WORDS = Set.of(
            "előtt", "után", "mögött", "fölött", "felett", "alatt", "mellett", "és", "a", "az", "helyett", "beli",
            "nélkül", "között", "mint", "révén");

    /** Az eredeti adatbázis 2006-os lezárási dátuma. */
    public static final String CANON_CLOSED = "2006. április 23.";

    /** A megőrzött termékazonosító a binárisból. */
    public static final String ORIGIN_VERSION = "VNP's Kalliope 1.71 beta";

    /** Az eredeti adatbázis {@code !} sorainak alapállapota. */
    public static final Settings DEFAULT_SETTINGS = Settings.fromDatabase(Map.ofEntries(
            Map.entry(Settings.S_CONJUNCTION_ANCEPS, "1"),
            Map.entry(Settings.LETTER_SYLLABLES, "0"),
            Map.entry(Settings.EXPLAIN_UNSTRESSED, "1"),
            Map.entry(Settings.MULTIPLE_MATCHES, "1"),
            Map.entry(Settings.ASSONANCE_AS_RHYME, "1"),
            Map.entry(Settings.SHOW_ICTUS, "0"),
            Map.entry(Settings.SHORT_WORDS_ANCEPS, "1"),
            Map.entry(Settings.ALLOW_SYNIZESIS, "1"),
            Map.entry(Settings.WORD_FINAL_CONSONANT_ANCEPS, "1"),
            Map.entry(Settings.WORD_INITIAL_STRESS, "0")));

    /** Minden mérték, típus szerint sorrendben. */
    public static final List<Meter> ALL_METERS = allMeters();

    private static final Map<String, Meter> BY_ID = indexById(ALL_METERS);
    private static final Map<String, StanzaForm> STANZA_BY_ID = indexStanzas(STANZAS);

    public static Meter meter(String id) {
        Meter m = BY_ID.get(id);
        if (m == null) {
            throw new IllegalArgumentException("Ismeretlen mérték: " + id);
        }
        return m;
    }

    public static StanzaForm stanza(String id) {
        StanzaForm s = STANZA_BY_ID.get(id);
        if (s == null) {
            throw new IllegalArgumentException("Ismeretlen szakaszmérték: " + id);
        }
        return s;
    }

    /** Ékezet- és kisbetű-tűrő keresés névre és mintára. */
    public static List<Meter> search(String query) {
        if (query == null || query.isBlank()) {
            return ALL_METERS;
        }
        String needle = fold(query);
        List<Meter> out = new ArrayList<>();
        for (Meter m : ALL_METERS) {
            if (fold(m.name()).contains(needle)
                    || m.id().contains(needle)
                    || m.pattern().contains(query)) {
                out.add(m);
            }
        }
        return List.copyOf(out);
    }

    /** Ékezetek elhagyása a kereséshez. */
    public static String fold(String s) {
        String lower = s.toLowerCase(Locale.ROOT);
        StringBuilder sb = new StringBuilder(lower.length());
        for (int i = 0; i < lower.length(); i++) {
            char c = lower.charAt(i);
            int idx = "áéíóöőúüű".indexOf(c);
            sb.append(idx >= 0 ? "aeiooouuu".charAt(idx) : c);
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------ //

    private static Meter foot(String pattern, String name) {
        return foot(pattern, name, null);
    }

    private static Meter foot(String pattern, String name, String note) {
        return new Meter(slug(name), name, pattern, Meter.Kind.FOOT, false, note, null);
    }

    private static Meter colon(String pattern, String name) {
        return colon(pattern, name, null);
    }

    private static Meter colon(String pattern, String name, String note) {
        return new Meter(slug(name), name, pattern, Meter.Kind.COLON, false, note, null);
    }

    private static Meter line(String pattern, String name) {
        return line(pattern, name, null);
    }

    private static Meter line(String pattern, String name, String note) {
        return new Meter(slug(name), name, pattern, Meter.Kind.LINE, false, note, null);
    }

    private static Meter fictiveLine(String pattern, String name) {
        return new Meter(slug(name), name, pattern, Meter.Kind.LINE, true, "segédmérték, nem önálló sorfajta", null);
    }

    private static Meter corrected(Meter base, String original, String reason, String source) {
        return new Meter(
                base.id(),
                base.name(),
                base.pattern(),
                base.kind(),
                base.fictive(),
                base.note(),
                new Meter.Correction(original, reason, source));
    }

    /** Összetett sor: a részek mintáit fűzi össze. A rész lehet {@link Meter} vagy nyers minta. */
    private static Meter complex(String name, boolean fictive, Object... parts) {
        StringBuilder sb = new StringBuilder();
        for (Object part : parts) {
            if (part instanceof Meter m) {
                sb.append(m.pattern());
            } else if (part instanceof String s) {
                if (Notation.symbolsOnly(s).isEmpty()) {
                    throw new IllegalArgumentException("Az összetett mérték nyers része nem minta: " + s);
                }
                sb.append(s);
            } else {
                throw new IllegalArgumentException("Ismeretlen komponens: " + part);
            }
        }
        return new Meter(slug(name), name, sb.toString(), Meter.Kind.COMPLEX, fictive, null, null);
    }

    private static List<Meter> repeat(Meter m, int times) {
        List<Meter> out = new ArrayList<>(times);
        for (int i = 0; i < times; i++) {
            out.add(m);
        }
        return out;
    }

    private static List<Meter> halEjiEneke() {
        List<Meter> out = new ArrayList<>();
        out.add(TA);
        out.add(PIRRICHIUS);
        for (int i = 0; i < 4; i++) {
            out.add(MOLOSSZUS);
            out.add(PROCELEUZMATIKUS);
        }
        out.add(MOLOSSZUS);
        out.add(PIRRICHIUS);
        out.add(TA);
        return out;
    }

    private static StanzaForm closed(String name, List<Meter> lines, String rhyme) {
        return new StanzaForm(slug(name), name, lines, rhyme, true);
    }

    private static StanzaForm open(String name, List<Meter> lines, String rhyme) {
        return new StanzaForm(slug(name), name, lines, rhyme, false);
    }

    /** Azonosító képzése névből — az ütemhangsúlyos kánon is ezt használja. */
    static String slugOf(String name) {
        return slug(name);
    }

    private static String slug(String name) {
        String folded = fold(name);
        StringBuilder sb = new StringBuilder(folded.length());
        boolean lastDash = false;
        for (int i = 0; i < folded.length(); i++) {
            char c = folded.charAt(i);
            if (Character.isLetterOrDigit(c)) {
                sb.append(c);
                lastDash = false;
            } else if (!lastDash && sb.length() > 0) {
                sb.append('-');
                lastDash = true;
            }
        }
        while (sb.length() > 0 && sb.charAt(sb.length() - 1) == '-') {
            sb.setLength(sb.length() - 1);
        }
        return sb.toString();
    }

    private static List<Meter> allMeters() {
        List<Meter> out = new ArrayList<>(FEET.size() + COLA.size() + LINES.size() + COMPLEXES.size());
        out.addAll(FEET);
        out.addAll(COLA);
        out.addAll(LINES);
        out.addAll(COMPLEXES);
        return List.copyOf(out);
    }

    private static Map<String, Meter> indexById(List<Meter> meters) {
        Map<String, Meter> m = new LinkedHashMap<>();
        for (Meter meter : meters) {
            Meter clash = m.putIfAbsent(meter.id(), meter);
            if (clash != null && clash != meter) {
                throw new IllegalStateException("Ütköző azonosító: " + meter.id() + " (" + meter.name() + ")");
            }
        }
        return Map.copyOf(m);
    }

    private static Map<String, StanzaForm> indexStanzas(List<StanzaForm> forms) {
        Map<String, StanzaForm> m = new LinkedHashMap<>();
        for (StanzaForm f : forms) {
            if (m.putIfAbsent(f.id(), f) != null) {
                throw new IllegalStateException("Ütköző szakaszmérték-azonosító: " + f.id());
            }
        }
        return Map.copyOf(m);
    }
}
