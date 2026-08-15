package hu.porkolab.kalliope;

import java.util.List;

/**
 * Példatár — valódi, dokumentált formájú magyar versek, lehetőleg teljes
 * egészükben.
 *
 * <p>Ez egyben a motor aranyminta-korpusza: minden darab szövege hiteles
 * forrásból való (Wikiforrás, Magyar Elektronikus Könyvtár), a {@code expected}
 * mező pedig azt mondja meg, mit állít róla a verstan. A gépi elvárásokat a
 * {@code CorpusTest} rögzíti.
 *
 * <p>Szándékosan van köztük olyan, amit a motor <b>nem</b> illeszt: a hangsúlyos-
 * magyaros vers nem időmértékes, és erre a helyes válasz a „nincs találat”.
 *
 * <p><b>Szerzői jog.</b> A közölt szövegek nagy része közkincs (a szerző halála
 * után hetven évvel). Két kivétel Devecseri Gábor (1917–1971) Homérosz-
 * fordítása: ezek szemléltetésként, oktatási célú szabad felhasználás keretében
 * szerepelnek (Szjt. 33–35. §), a forrás és a fordító megjelölésével.
 */
public record Examples(String id, String title, String author, String expected, String text) {

    public static final Examples SZIGETI = new Examples(
            "szigeti-veszedelem",
            "Szigeti veszedelem (I. ének, részlet)",
            "Zrínyi Miklós, 1651",
            "Felező tizenkettes, hangsúlyos-magyaros vers — NEM időmértékes, ezért a klasszikus "
                    + "mértékillesztő helyesen nem ad rá találatot. Rímképlet: aaaa (bokorrím). "
                    + "A metszet Zrínyinél gyakran szóba esik.",
            """
            Fegyvert, s vitézt éneklek, török hatalmát,
            Ki meg merte várni, Szulimán haragját,
            Ama nagy Szulimánnak hatalmas karját,
            Az kinek Europa rettegte szablyáját.""");

    public static final Examples TOLDI = new Examples(
            "toldi",
            "Toldi — Első ének",
            "Arany János, 1846",
            "Felező tizenkettes, páros rím — hangsúlyos vers, klasszikus mérték nélkül.",
            """
            Ég a napmelegtől a kopár szík sarja,
            Tikkadt szöcskenyájak legelésznek rajta;
            Nincs egy árva fűszál a tors közt kelőben,
            Nincs tenyérnyi zöld hely nagy határ mezőben.
            Boglyák hűvösében tíz-tizenkét szolga
            Hortyog, mintha legjobb rendin menne dolga;
            Hej, pedig üresen, vagy félig rakottan,
            Nagy szénás szekerek álldogálnak ottan.

            Ösztövér kútágas, hórihorgas gémmel
            Mélyen néz a kútba s benne vizet kémel:
            Óriás szunyognak képzelné valaki,
            Mely az öreg földnek vérit most szíja ki.
            Válunál az ökrök szomjasan delelnek,
            Bőgölyök hadával háborúra kelnek:
            De felült Lackó a béresek nyakára,[1]
            Nincs, ki vizet merjen hosszu csatornára.

            Egy, csak egy legény van talpon a vidéken,
            Meddig a szem ellát puszta földön, égen;
            Szörnyű vendégoldal reng araszos vállán,
            Pedig még legénytoll sem pehelyzik állán.
            Széles országútra messze, messze bámul,""");

    /**
     * Homérosz: Íliász, I. ének 1–40. sor — Devecseri Gábor fordítása.
     *
     * <p>Szerzői jogi védelem alatt álló fordítás; itt szemléltetésként, oktatási
     * célú szabad felhasználás keretében (Szjt. 33–35. §).
     * Forrás: Magyar Elektronikus Könyvtár, https://mek.oszk.hu/00400/00406/
     */
    public static final Examples ILIASZ = new Examples(
            "iliasz",
            "Íliász — I. ének, 1–40. sor",
            "Homérosz — Devecseri Gábor fordítása",
            "Daktilikus hexameter, rímtelen. A kezdősor első szótagja csak költői licenciával "
                    + "hosszú („a szókezdő hangsúly nyújtja meg”), ezért alapbeállítással nem illeszkedik.",
            """
            Haragot, istennő zengd Péleidész Akhileuszét,
            vészest, mely sokezer kínt szerzett minden akhájnak,
            mert sok hősnek erős lelkét Hádészra vetette,
            míg őket magukat zsákmányul a dögmadaraknak
            és a kutyáknak dobta. Betelt vele Zeusz akaratja,
            attól kezdve, hogy egyszer szétváltak civakodva
            Átreidész, seregek fejedelme s a fényes Akhilleusz.
            És melyik égilakó uszitotta viszályra a kettőt?
            Létó s Zeusz fia: mert neki gyúlt a királyra haragja,
            s ártó vészt keltett a seregben; hulltak a népek:
            mert ama Khrűszészt megsértette, az ő szent papját,
            Átreidész: odajött az a fürge akháji hajókhoz,
            végtelenül sok váltsággal megváltani lányát,
            messzelövő Phoibosz koszorúját tartva kezében,
            fönt aranyos botján, s kérlelte az összes akháj hőst,
            Átreusz két sereget-tagoló sarját a leginkább:
            "Átreidák s valamennyi remek-lábvértes akháj hős,
            nektek az égilakók adják meg, hogy Priamosznak
            várát feldúlván, haza épen térjetek innen,
            csak szeretett lányom kérem, s ti vegyétek e díjat,
            Zeusz sarját tisztelve, a messzelövő nagy Apollónt."
            Erre helyeslően zúgtak fel az összes akhájok:
            tiszteljék a papot, s tartsák meg a nagyszerű díjat;
            mégsem tetszett így Agamemnón Átreidésznak,
            rútul ráförmedt, elküldte goromba szavakkal:
            "Hallod, öreg, ne találjalak én a nagyöblü hajóknál,
            most se időzz hosszan, később se kerülj ide vissza:
            úgy ne legyen, hogy e bot s isten koszorúja se véd meg.
            Én a leányt nem adom ki, előbb utoléri az aggkor
            messze hazájától, Argoszban, az én palotámban,
            míg a szövőszéken szövöget, s velem ágyamat osztja;
            menj hát, föl ne dühíts, hogy egészségben hazatérhess."
            Így szólt ő; megijedt az öreg s hajlott a szavára;
            hallgatagon haladott zsivajos tenger vize mellett,
            majd, hogy messzevonult az öreg, sok imával esengett
            büszke Apollónhoz, kinek anyja a széphaju Létó:
            "Halld, te Ezüstíjú, aki óvón Khrűsza fölött állsz
            az isteni Killa fölött, Tenedoszban erősen uralkodsz,
            Szmintheusz: hogyha neked kedves szentélyt betetőztem
            bármikor is, ha kövér combját égettem ökörnek""");

    /**
     * Homérosz: Odüsszeia, I. ének 1–40. sor — Devecseri Gábor fordítása.
     * Ugyanaz a jogi helyzet, mint az Íliásznál.
     * Forrás: Magyar Elektronikus Könyvtár, https://mek.oszk.hu/00400/00408/
     */
    public static final Examples ODUSSZEIA = new Examples(
            "odusszeia",
            "Odüsszeia — I. ének, 1–40. sor",
            "Homérosz — Devecseri Gábor fordítása",
            "Daktilikus hexameter, rímtelen.",
            """
            Férfiuról szólj nékem, Múzsa, ki sokfele bolygott
            s hosszan hányódott, földúlván szentfalu Tróját,
            sok nép városait, s eszejárását kitanulta,
            s tengeren is sok erős gyötrelmet tűrt a szivében,
            menteni vágyva saját lelkét, társak hazatértét.
            Csakhogy nem tarthatta meg őket, akárhogy akarta:
            mert önnön buta vétkeikért odavesztek a társak,
            balgák: fölfalták Hüperíón Éeliosznak
            barmait, és hazatértük napját ő elorozta.
            Istennő, Zeusz lánya, beszélj minekünk is ezekből.
            Hát aki megmenekült meredek vészből, valamennyi
            otthon volt, túl háborun és a vizek veszedelmén;
            őt egyedül, hitvesre, hazára hiába sovárgót
            tartóztatta Kalüpszó nimfa, az isteni úrnő
            barlang öblös ölén, mivel áhította urául.
            És hogy az esztendők perdültén jött az az év is,
            melyhez az istenek azt szőtték, hogy visszakerüljön
            már Ithakába, a küzdelmektől nem menekült meg
            még a szerettei közt sem. Az isten mind könyörült már
            rajta; Poszeidáón egyedül gyűlölte szünetlen
            isteni hős Odüsszeuszt, valameddig csak haza nem tért.
            Csakhogy az elment éppen a távoli aithiopokhoz
            - kik két részre oszoltan a szélső népe a földnek
            s hulló napra tekint egy részük, más a kelőre -,
            hogy bárány s bikaáldozatukból kapja a részét.
            Ott ült ő, lakomának örülve, s a többiek ekkor
            mind az olümposzi Zeusz palotájában gyülekeztek.
            S köztük az emberek, istenek apja fogott a beszédbe;
            mert gondolt a szivében a gáncstalan Aigiszthoszra,
            kit ledöfött Agamemnón sarja, a híres Oresztész;
            őrá emlékezve beszélt a haláltalanokhoz:
            "Jaj, csak örökkön az isteneket vádolja az ember:
            azt mondják, a csapás mind tőlünk jön, de bizony hogy
            ostoba vétkeikért szenvednek a végzeten is túl;
            lám, Aigiszthosz is Átreidész megkért feleségét
            sorsa fölött elvette, s a visszajövőt meg is ölte,
            tudta pedig meredek veszedelmét, hisz megüzentük,
            Argoszölő Hermészt küldtük ki, a messzirelátót,
            hogy le ne döfje a hőst, s feleségét meg sose kérje:
            mert hiszen Átreidészt majd megbosszúlja Oresztész,""");

    public static final Examples ZALAN = new Examples(
            "zalan-futasa",
            "Zalán futása — előhang",
            "Vörösmarty Mihály, 1825",
            "Daktilikus hexameter, rímtelen.",
            """
            Régi dicsőségünk, hol késel az éji homályban?
            Századok ültenek el, s te alattok mélyen enyésző
            Fénnyel jársz egyedűl. Rajtad sürü fellegek, és a
            Bús feledékenység koszorútlan alakja lebegnek.
            Hol vagyon, aki merész ajakát hadi dalnak eresztvén,
            A riadó vak mélységet fölverje szavával,
            S késő százak után, méltán láttassa vezérlő
            Párducos Árpádot, s hadrontó népe hatalmát?
            Hol vagyon? Ah ezeren némán fordulnak el: álom
            Öldösi szíveiket, s velök alszik az ősi dicsőség.
            A tehetetlen kor jött el, puhaságra serényebb
            Gyermekek álltak elő az erősebb jámbor apáktól.
            Engem is, a nyugalom napján, ily év hoza fényre
            Már késő unokát, ki előbb a lányka mulandó
            Szépségén függtem gondatlan gyermeki szemmel,
            S rajta veszett örömem dalait panaszosra cserélvén.
            Hasztalanúl eget és földet kérlelve betölték.
            Mégis az ifjúság háborgó napjai múlván,
            Biztos erőt érzek: kebelemben nagyra kelendő
            Képzeletek villannak meg, diadalmas Ügekről,
            S a deli Álmosról, s Álmosnak büszke fiáról,
            Párducos Árpádról... Óh hon! meghallasz-e engem,
            S nagyra törő tehetősb fiaid hallgatnak-e szómra?
            Megjön az éj, szomorún feketednek az ormok, az élet
            Elnyugszik, s a fél föld lesz nyoszolyája; de engem
            Fölver az elmúlt szép tetteknek gondja. Derengő
            Lelkem előtt lobogós kopják és kardok acéli
            Szegdelik a levegőt: villog, dörög a hadi környék.
            Látom, elől kacagányos apák, s heves ifju leventék
            Száguldó lovakon mint törnek halni, vagy ölni,
            Zászlódat látom, Bulcsú, s szemem árja megindúl.
            Óh hát halljátok, ti hazának gyermeki! szómat,
            Későn hangzik már; de magában hordja halálos
            Harcok fergetegét, s hű a haladékony időhöz.""");

    public static final Examples HETEDIK_ECLOGA = new Examples(
            "hetedik-ecloga",
            "Hetedik ecloga",
            "Radnóti Miklós, 1944",
            "Daktilikus hexameter, rímtelen. Teljes vers.",
            """
            Látod-e, esteledik s a szögesdróttal beszegett, vad
            tölgykerités, barakk oly lebegő, felszívja az este.
            Rabságunk keretét elereszti a lassu tekintet
            és csak az ész, csak az ész, az tudja, a drót feszülését.
            Látod-e drága, a képzelet itt, az is így szabadul csak,
            megtöretett testünket az álom, a szép szabadító
            oldja fel és a fogolytábor hazaindul ilyenkor.

            Rongyosan és kopaszon, horkolva repülnek a foglyok,
            Szerbia vak tetejéről búvó otthoni tájra.
            Búvó otthoni táj! Ó, megvan-e még az az otthon?
            Bomba sem érte talán? s van, mint amikor bevonultunk?
            És aki jobbra nyöszörg, aki balra hever, hazatér-e?
            Mondd, van-e ott haza még, ahol értik e hexametert is?

            Ékezetek nélkül, csak sort sor alá tapogatva,
            úgy irom itt a homályban a verset, mint ahogy élek,
            vaksin, hernyóként araszolgatván a papíron;
            zseblámpát, könyvet, mindent elvettek a Lager
            őrei s posta se jön, köd száll le csupán barakunkra.

            Rémhirek és férgek közt él itt francia, lengyel,
            hangos olasz, szakadár szerb, méla zsidó a hegyekben,
            szétdarabolt lázas test s mégis egy életet él itt, -
            jóhírt vár, szép asszonyi szót, szabad emberi sorsot,
            s várja a véget, a sűrü homályba bukót, a csodákat.

            Fekszem a deszkán, férgek közt fogoly állat, a bolhák
            ostroma meg-megujúl, de a légysereg elnyugodott már.
            Este van, egy nappal rövidebb, lásd, ujra a fogság
            és egy nappal az élet is. Alszik a tábor. A tájra
            rásüt a hold s fényében a drótok ujra feszülnek,
            s látni az ablakon át, hogy a fegyveres őrszemek árnya
            lépdel a falra vetődve az éjszaka hangjai közben.

            Alszik a tábor, látod-e drága, suhognak az álmok,
            horkan a felriadó, megfordul a szűk helyen és már
            ujra elalszik s fénylik az arca. Csak én ülök ébren,
            féligszítt cigarettát érzek a számban a csókod
            íze helyett és nem jön az álom, az enyhetadó, mert
            nem tudok én meghalni se, élni se nélküled immár.""");

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
            "A közelítő tél",
            "Berzsenyi Dániel, 1804 után",
            "Első aszklepiadeszi strófa: három kis aszklepiadeszi sor és egy glükóni. Teljes vers, hat szakasz.",
            """
            Hervad már ligetünk, s díszei hullanak,
            Tarlott bokrai közt sárga levél zörög.
            Nincs rózsás labyrinth, s balzsamos illatok
            Közt nem lengedez a Zephyr.

            Nincs már symphonia, s zöld lugasok között
            Nem búg gerlice, és a füzes ernyein
            A csermely violás völgye nem illatoz,
            S tükrét durva csalét fedi.

            A hegy boltozatin néma homály borong.
            Bíbor thyrsusain nem mosolyog gerezd.
            Itt nemrég az öröm víg dala harsogott:
            S most minden szomorú s kiholt.

            Oh, a szárnyas idő hirtelen elrepül,
            S minden míve tünő szárnya körül lebeg!
            Minden csak jelenés; minden az ég alatt,
            Mint a kis nefelejcs, enyész.

            Lassanként koszorúm bimbaja elvirít,
            Itt hágy szép tavaszom: még alig ízleli
            Nektárját ajakam, még alig illetem
            Egy-két zsenge virágait.

            Itt hágy, s vissza se tér majd gyönyörű korom.
            Nem hozhatja fel azt több kikelet soha!
            Sem béhunyt szememet fel nem igézheti
            Lollim barna szemöldöke!""");

    public static final Examples HORAC = new Examples(
            "horac",
            "Horác",
            "Berzsenyi Dániel, 1799 körül",
            "Első aszklepiadeszi strófa, ugyanaz a forma, mint A közelítő télé. Teljes vers.",
            """
            Zúg immár Boreas a Kemenes fölött,
            Zordon fergetegek rejtik el a napot,
            Nézd, a Ság tetejét hófuvatok fedik,
            S minden bús telelésre dőlt.

            Halljad, Flaccus arany lantja mit énekel:
            Gerjeszd a szenelőt, tölts poharadba bort,
            Villogjon fejeden balzsamomos kenet,
            Mellyet Bengala napja főz.

            Használd a napokat, s ami jelen vagyon,
            Forró szívvel öleld, s a szerelem szelíd
            Érzésit ki ne zárd, míg fiatal korod
            Boldog csillaga tündököl.

            Holnappal ne törődj, messze ne álmodozz,
            Légy víg, légy te okos, míg lehet, élj s örülj.
            Míg szólunk, az idő hirtelen elrepül,
            Mint a nyíl s zuhogó patak.""");

    public static final Examples SZEPTEMBER_VEGEN = new Examples(
            "szeptember-vegen",
            "Szeptember végén",
            "Petőfi Sándor, 1847",
            "Anapesztikus lejtésű sorok, keresztrím. A rímpárok asszonáncok: virágok–világot. Teljes vers.",
            """
            Még nyílnak a völgyben a kerti virágok,
            Még zöldel a nyárfa az ablak előtt,
            De látod amottan a téli világot?
            Már hó takará el a bérczi tetőt.
            Még ifju szivemben a lángsugarú nyár
            S még benne virít az egész kikelet,
            De íme sötét hajam őszbe vegyűl már,
            A tél dere már megüté fejemet.

            Elhull a virág, eliramlik az élet...
            Űlj, hitvesem, űlj az ölembe ide!
            Ki most fejedet kebelemre tevéd le,
            Holnap nem omolsz-e sirom fölibe?
            Oh mondd: ha előbb halok el, tetemimre
            Könnyezve borítasz-e szemfödelet?
            S rábírhat-e majdan egy ifju szerelme,
            Hogy elhagyod érte az én nevemet?

            Ha eldobod egykor az özvegyi fátyolt,
            Fejfámra sötét lobogóul akaszd,
            Én feljövök érte a síri világból
            Az éj közepén, s oda leviszem azt,
            Letörleni véle könyűimet érted,
            Ki könnyeden elfeledéd hivedet,
            S e szív sebeit bekötözni, ki téged
            Még akkor is, ott is, örökre szeret!""");

    public static final List<Examples> ALL = List.of(
            SZIGETI,
            TOLDI,
            ILIASZ,
            ODUSSZEIA,
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
