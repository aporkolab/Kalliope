# Kalliopé v.2.0

Magyar verstani elemző: **skandál**, **versmértéket ismer fel**, és **rímképletet** ad — mindkét magyar ritmusrendben, időmértékesben és ütemhangsúlyosban egyaránt.

Minden szótagról megmondja, **miért** olyan hosszú; ha egy sor nem illeszkedik, megmutatja, **min múlik**; a végén pedig egy mondatban összegzi, milyen vers ez: *„Időmértékes verselés: disztichonok."*

> A Kalliopét **Váradi Nagy Pál** (vnp85) írta 2004–2006 táján. Ez a modernizált változat az ő munkájára épül; **közösen tesszük közzé**, társszerzőként, MIT licenc alatt. → [Eredet és fejlesztés](#eredet-és-fejlesztés)

**Kipróbálni telepítés nélkül:** <https://aporkolab.github.io/Kalliope/> — teljes elemzés a böngészőben, backend nélkül.

Saját gépen konténerből:

```bash
docker run -p 8080:8080 ghcr.io/aporkolab/kalliope:latest   # → http://localhost:8080

```

A kész image publikus, `linux/amd64` és `linux/arm64` alatt is fut, és **0,36 másodperc** alatt indul. Ha inkább forrásból építenéd:

```bash
git clone https://github.com/aporkolab/Kalliope.git && cd Kalliope
docker compose up --build          # → http://localhost:8080

```

## Eredet és fejlesztés

A Kalliopé eredetileg Váradi Nagy Pál egyetemi munkájaként született, **Borland Delphiben** (a bináris `DVCLAL` és `PACKAGEINFO` resource-a alapján). A szerző szerint a forrás lényegében változtatás nélkül fordul Lazarus/FreePascal alatt is, amire később állt át. A saját webes változata itt érhető el: [https://csillagtura.ro/projektek/kalliope/](https://csillagtura.ro/projektek/kalliope/).

Mivel a forráskód nem volt nyilvános, ezt a verziót a lefordított `kalliope.exe` binárisából, visszafejtéssel (reverse-engineering) készítettem el. A motort Java nyelven újraírtam, de **az adatbázis magja az eredeti**.

**Váradi Nagy Pál munkája:**

* A metrikai kánon (verslábak, kolónok, sorfajták, szakaszmértékek), beleértve saját formáit is.
* A kiejtési normalizáló tábla, a név-alias tábla, a digráf-lista és a *muta cum liquida* halmaz.

**Az én kiegészítéseim a jelenlegi verzióban:**

* Teljes kód-újraírás és architekturális modernizáció (típusos Java adatszerkezetek a saját parserek helyett).
* Ütemhangsúlyos elemzési ág és szakaszmérték-illesztés.
* REST API és modern (Angular) webes felület.
* A skandáló algoritmus és a rímdetektor finomhangolása (pl. Arany János szabályai szerinti rímfelismerés, közös szótagok dinamikus értékelése).

## Amit tud

| Funkció | Leírás |
| --- | --- |
| **Időmértékes** | 56 sorfajta, 38 kolón, 11 versláb, 8 összetett sor, 20 szakaszmérték; szigorú illesztés, kapcsolható licenciákkal. |
| **Ütemhangsúlyos** | 20 magyaros sorfajta ütemtagolással; a metszet minősége (tiszta vagy laza) külön látszik. |
| **Kettős ritmus** | Ha a szakasz mindkét rendnek megfelel, jelzi — de nem mondja rá, hogy „szimultán vers". |
| **Rím** | Képlet vaksorral (`x`), a képlet neve, és soronként a rím fajtája (tiszta rím, ragrím, asszonánc, önrím). |
| **Cezúra** | A mérték jelölt metszete, valamint a hexameter klasszikus metszeteinek felismerése. |
| **Szótagszintű indoklás** | 12-féle ok (pl. természeténél fogva hosszú, *muta cum liquida*, összevont kettőshangzó). |
| **Interaktivitás** | A felületen a szótagra kattintva a hosszúság felülbírálható, az elemzés azonnal újrafut. |
| **Megosztás & Export** | A „Link” gomb paraméterbe kódolja a verset (adatbázis nélkül osztható). A JSON export letölti az API nyers válaszát. |
| **Nyomtatás** | Tiszta, zavaró UI-elemek nélküli nyomtatási/PDF nézet, ahol a hosszúságot a jelek hordozzák a színek helyett. |
| **Lüktetés** | Ha egyetlen sorfajta sem illeszkedik, kimondja a sor élének lábsorát: *„6 daktilus a sor élén — a 19. szótagnál megszakad"*. Sorfajtát nem állít. |
| **Ritmustérkép** | Az ítélet mellett, soronként a szótaghosszak — egy negyvensoros eposzrészlet ritmusa egy pillantással befogható; a sorra kattintva odaugrik. |

### A jelölés és a felület

| Jel | Mit jelent |
| --- | --- |
| `—` | hosszú szótag |
| `∪` | rövid szótag |
| `×` | eldöntetlen: **közös szótag** (a hangtani minősítés ambivalens) vagy **közömbös szótaghelyzet** (a mérték nem kér számot róla — ilyen a sorvég) |
| `\|` | lábhatár |
| `‖` | sormetszet, cezúra |
| pontozott aláhúzás | a szótag eredetileg eldöntetlen volt, a mérték döntötte el |

A felület színrendszere vizuálisan is elkülöníti a szótagokat (hosszú, rövid, eldöntetlen), támogatja a sötét/világos témát, és mobilon is kényelmesen használható (kártyás tördelés).

## Felépítés

Négy modul, egyetlen futtatható artefaktum (a negyedik csak a statikus változathoz kell):

| Modul | Funkció | Függőségek |
| --- | --- | --- |
| `kalliope-core` | A verstani motor, a metrikai kánon és a példatár. | **Csak a JDK** |
| `kalliope-api` | REST-réteg + a felület kiszolgálása. | Spring Boot 4.1 |
| `kalliope-web` | A webes felület. | Angular 22 |
| `kalliope-js` | A motor JavaScriptre fordítva (TeaVM), a statikus változathoz. | TeaVM 0.12 |

Az Angular build a Spring Boot `static/` mappájába kerül, így **nincs CORS probléma, és nem kell külön statikus hosting**.

### Két futtatókörnyezet, egy motor

| | GitHub Pages | Docker-image |
| --- | --- | --- |
| a motor | JavaScriptre fordítva a lapban (239 kB) | JVM-en, a REST API mögött |
| hálózat | nem kell, minden helyben fut | `/api` hívások |
| felület | **ugyanaz a bundle** | ugyanaz a bundle |

A felület nem tud a különbségről: a `KalliopeService` futásidőben megnézi, ott van-e a lapon a lefordított motor, és ha igen, azt hívja. A motor forrása egyetlen helyen van (`kalliope-core`), a JSON-t is ugyanaz az osztály írja mindkét oldalon — és három teszt köti le, hogy ne csúszhassanak el:

| Teszt | Mit köt össze |
| --- | --- |
| `JsonTest` (mag) | a kézi JSON-kiírás == a Jackson rekord-leképezése |
| `JsonEquivalenceTest` (API) | a kézi JSON-kiírás == a valódi HTTP-válasz |
| `js-diff.mjs` (js) | a **böngészőbe fordított** motor kimenete == a JVM-motor kimenete, a teljes korpuszon, **bájtra** |

Az utolsó a legfontosabb: a TeaVM más futtatókörnyezetre képezi le a Java szemantikát, és ha bármi elcsúszik, a webes változat csendben adna más skandálást. A Pages-deploy elbukik, ha nem egyezik.

A motor ezért 17-es bytecode-ra fordul (a TeaVM ASM-je nem olvas 25-öst), és nincs benne se regex, se `Integer::sum` — a `String.split` a `Character.UnicodeScript`-en át olyan JDK-belsőket ér el, amiket a TeaVM nem emulál. A kézi vágás egyenértékűségét a `StringsTest` a JDK kimenetéhez méri.

## Futtatás és Fejlesztés

**Konténerből:**

```bash
docker run -p 8080:8080 ghcr.io/aporkolab/kalliope:latest   # kész image a GHCR-ből
docker compose up --build                                   # vagy forrásból

```

| | |
| --- | --- |
| platformok | `linux/amd64`, `linux/arm64` |
| méret | 418 MB (ebből 56 MB az AOT-gyorsítótár) |
| indulás | 0,36 s |
| felhasználó | nem root (uid 10001) |

Az image tartalmaz egy **AOT-gyorsítótárat**: a build végén egy tanítófutás elmenti a felépített JVM-állapotot, ettől indul 0,36 s alatt 0,87 helyett. A `--build-arg AOT_CACHE=false` kikapcsolja, ilyenkor az image 346 MB, cserébe lassabban indul.

A build **kereszt-fordít**: az Angular- és a Maven-szakasz a build gép architektúráján fut (a kimenetük architektúrafüggetlen), és csak a záró JRE-réteg készül platformonként. Enélkül az arm64 változat emulált Maven-buildben készülne.

**Fejlesztéshez (két terminál):**

```bash
./mvnw -pl kalliope-api -am spring-boot:run           # API (8080-as port)
cd kalliope-web && npm ci && npm start                # Web (4200-as port, /api proxyzva)

```

**Parancssorból (CLI), felület nélkül:**

```bash
./mvnw -pl kalliope-core -am package
java -jar kalliope-core/target/kalliope-core-*.jar vers.txt           # Fájl elemzése
java -jar kalliope-core/target/kalliope-core-*.jar --json vers.txt    # Gépi kimenet
java -jar kalliope-core/target/kalliope-core-*.jar --canon            # A metrikai kánon

```

**A statikus változat építése** (ehhez JDK 21 kell a TeaVM-nek — a szokásos `verify` nem igényli):

```bash
./mvnw -Pjs -pl kalliope-js -am package     # a motor JavaScriptre
node kalliope-js/tools/js-diff.mjs          # összevetés a JVM-motorral
cd kalliope-web && npx ng build --base-href=/Kalliope/ && node tools/build-pages.mjs

```

### Konfiguráció

A főbb beállítások környezeti változóként is megadhatók:

| Kulcs | Alapérték | Funkció |
| --- | --- | --- |
| `server.port` | `8080` | HTTP port |
| `kalliope.rate-limit.requests-per-minute` | `60` | `/api/analyze` kérésszám-korlát (`0` kikapcsolja) |
| `JAVA_TOOL_OPTIONS` | `-XX:MaxRAMPercentage=75.0` | a compose ezt adja a JVM-nek |
| `AOT_CACHE` (build arg) | `true` | az AOT-gyorsítótár építése; `false` esetén 346 MB az image |

### CI és Ellenőrzés

A projekt szigorú minőségi kapukkal rendelkezik (80%-os tesztlefedettségi küszöb a motorban, az API-ban és a felületen, Spotless és Prettier kódformázás). Helyi ellenőrzéshez:

```bash
./mvnw verify
cd kalliope-web && npm ci && npx ng test --no-watch && npx prettier --check "src/**/*.{ts,html,css}"

```

Jelenleg **135 Java teszt** (112 motor + 23 API) és **45 frontend teszt** fut; a sorlefedettség 95% / 85% / 93%. A CI ugyanezt futtatja, majd `main`-re pusholva megépíti és felteszi a kétplatformos image-et a GHCR-be (az `org.opencontainers.image.source` label köti a csomagot ehhez a repóhoz), egy külön workflow pedig a motort JavaScriptre fordítja és kiteszi a GitHub Pages-re — de csak akkor, ha a `js-diff.mjs` szerint bájtra ugyanazt adja, mint a JVM.

## API Referencia

| Végpont | Leírás |
| --- | --- |
| `POST /api/analyze` | A teljes elemzés elvégzése. |
| `GET /api/canon` | A kánon (mértékek, beállítások, indoklások) lekérése. |
| `GET /api/canon/{id}` | Egy konkrét mérték lekérése. |
| `GET /api/examples` | A beépített verskorpusz lekérése. |

## Hogyan skandál

Az algoritmus hiteles magyar verstani forrásokra épül (Fazekas Enciklopédia, Csehy–Polgár: Gyakorlati magyar verstan, AkH. 12. kiadás). Főbb szabályok:

* A szótag a következő magánhangzóig tart, **átlépve a szóhatárt**.
* A kétjegyű betűk (`cs, sz, gy` stb.) egy, az `x` két mássalhangzónak számít. A kettőzött kétjegyűek (`ssz, ggy`) egy hosszú hangot jelölnek.
* **Közös szótag** (*syllaba communis*, `?`): a határozott névelő, a rövid nyílt szótagú kötőszók, a *muta cum liquida* szón belül, a görög aspiráták (`kh`, `th`) és az eldönthetetlen torlódások — itt a **szótag** hangtani minősítése ambivalens.
* **Közömbös szótaghelyzet** (*syllaba anceps*, `?`): a **sorvég**. Itt nem a szótag kétértékű, hanem a *mérték* nem kér számot a hosszúságáról; a sorvégi szünet kitölti az időt. A két fogalmat a jelölés nem, az indoklás viszont megkülönbözteti.
* **Kettőshangzók:** A görög-latin diftongusokra (`eu`, `au`) a motor változatokat állít elő, és a minta alapján dönti el az optimális olvasatot.

A skandáló alapértelmezetten **szigorú**: költői licenciát nem feltételez, de pontosan megmutatja, hol tér el a szöveg a mértéktől.

**Ha egy sor több mértékre is illeszkedik**, és nem ugyanúgy oldják fel a közös szótagokat, akkor a *vers* mértéke dönt — az egyértelmű sorok tanúsága szerint. A többi olvasat nem tűnik el: a sor mellett gombként ott van, és váltásra a skandálás, a mértéknév és a lábhatárok együtt változnak.

**Ha egyetlen mérték sem illeszkedik**, még mindig lehet mit mondani. A *lüktetés* kimutatja a sor élén álló leghosszabb azonos lábsort, három korláttal: **sorfajtát nem állít**, **mindig megmondja, hol szakad meg**, és **bizonyítékot kér, nem engedélyt** — a közös szótag bármely lábba beleillik, ezért a futam pozícióinak legalább a fele a nyers skandálásban is eldöntött kell legyen. A korpusz 34 találat nélküli során egyszer sem szólal meg (Zrínyi és Arany magyaros sorai), Váradi Nagy Pál 21 szótagos tesztesetére viszont igen: *„6 daktilus a sor élén — a 19. szótagnál megszakad"*.

Az **ítélet is ehhez igazodik**: a lüktetés időmértékes rend, csak nem tölt ki sorfajtát, ezért az összegzés nem „szabadverset" mond, hanem *„Időmértékes verselés: daktilikus lüktetés, kánoni sorfajta nélkül."* Ütemhangsúlyos ítéletet nem ír felül, csak kiegészíti.

### Beállítások

A motor számos elemzési beállítást támogat (pl. `a_rovid_kotoszok_kozombosek`, `a_szokezdo_hangsuly_nyujthat`).
*Megjegyzés:* Az eredeti Delphi-kliens három, tisztán ablakkezelési kapcsolója (`a_jobb_oldali_szoveg_formazott_legyen`, `a_fuggoleges_toszogalos_mutyur_helye`, `a_beallitasokat_tartalmazo_felulet_elrejtve`) **nincs** a motorban: itt nem volna értelmük, és nem is teszek úgy, mintha lenne. A tíz megmaradt beállításból hat az eredeti adatbázisé, négy az én kiegészítésem.

## Korpusz-riport és Tesztelés

A rendszer stabilitását és pontosságát egy 245 sorból álló, klasszikus verseket tartalmazó korpusz (Zrínyi, Arany, Homérosz, Radnóti, Berzsenyi, Petőfi) garantálja. Az automatizált `CorpusTest` elbukik, ha az algoritmus felismerési aránya (alapbeállításokkal 86% a teljes korpuszon, versenként külön küszöbbel) romlana egy kódmódosítás során.

## Döntések

Amit egy kódolvasásból nem lehet kitalálni: miért így van.

**A fejlesztőkörnyezetet méréssel állapítottam meg, nem emlékezetből.** A szerző maga jelezte, hogy húsz év után nem tudja biztosan, Delphi volt-e vagy Lazarus. A bináris viszont eldönti: a `.rsrc` szekcióban ott a `DVCLAL` (Delphi VCL Application License) és a `PACKAGEINFO` resource, a `Delphi%.8X` ablakosztály-string és a `Borland` cégnév, a szekciónevek pedig a Delphi sémája (`CODE`/`DATA`/`BSS`, nem `.text`/`.data`). FreePascal-nyom nulla. Egy ideig Lazarus szerepelt itt — az emlékre hallgattam a mérés helyett, és tévedtem.

**Az eredeti adathoz csak bizonyíték mellett nyúltam.** Öt mintát módosítottam, mindenhol ott az eredeti minta és a hivatkozott forrás, a felület `Kánon` nézetében kinyitva. Több saját javítási javaslatomat pedig az ellenőrzés **megcáfolta** — azok a szerző védhető kódolásai maradtak. Húszéves adatnál az eltérés normális, a döntés vitatható, ezért nyomon követhető.

**A skandáló szigorú, a licenciák kapcsolók.** Ha egy sor nem illeszkedik, az a hű válasz, nem hiba: a motor megmondja, min múlik. A költői licenciák (szókezdő hangsúly, kettőshangzó-összevonás) külön beállítások, és az egyik alapból ki van kapcsolva — így látszik, hol kell a licencia.

**Ami eldöntetlen, az eldöntetlen marad, és a mérték dönt.** Ahol a hagyomány kétféle olvasatot enged, ott nem döntök előre: a motor változatokat állít elő, és az illeszkedő mérték választ. Ez a döntés fogta meg a görög aspiráta hibáját is — `kap+hat` és *A-khil-leusz* ugyanazt a `kh`-t írja le, kétféle olvasattal.

**A közös szótagot és a közömbös szótaghelyzetet külön tartom.** Ugyanaz a `?` áll mindkettőre, de nem ugyanaz a kettő: a *syllaba communis* a **szótag** hangtani ambivalenciája, a *syllaba anceps* a **mértéknek** a helye, amely hosszút és rövidet is elfogad — ilyen a sorvég. Egy ideig a sorvéget is „közös szótagnak" írtam; ez tévedés volt, mert a sorvégi szótag hosszúsága nem kétértékű, csak érdektelen. Az indoklás most a kettőt külön nevezi meg.

**A rímfelismerés Arany rendszerére épül, nem a binárisból portolt táblára.** A portolt tábla a szó **végén** is egyesített, ami épp az ellenkezője Arany kódaszabályának. Az asszonáncnál a magánhangzó-hosszúságot megtartom, mert Aranynál a felcserélése külön, gyengébb fokozat — összemosva a `dögmadaraknak` is rímelne az `akhájnak`-ra.

**A „kettős ritmus" nem „szimultán vers".** Ha egy szakasz mindkét rendnek megfelel, azt jelzem, de nem minősítem: a szimultán vershez *maradéktalan* megfelelés kell, és ezt a szótagszám egybeesése nem bizonyítja. A két tényt egymás mellé teszem, az ítélet az olvasóé.

**A motornak nulla futásidejű függősége van.** Ez elvi döntés volt, és utólag ez tette lehetővé a böngészőbe fordítást: a TeaVM csak azért eszi meg, mert a motor `java.util` kollekciókon és stringeken kívül semmit nem használ. Ezért került ki belőle a regex és az `Integer::sum` is — mindkettő viselkedés-semlegesen, tesztekkel bizonyítva.

**Egy szerializáló van, nem kettő.** A böngészőben nincs reflexió, tehát Jackson sincs. Ha az API Jacksont használna, a webes változat pedig egy külön kézi kiírót, akkor két JSON-alak volna, és elcsúsznának. Így egyetlen kézi szerializáló van a magban, három ponton lehorgonyozva — a legerősebb kötés az, ami a **lefordított motor kimenetét a JVM-éhez** méri, bájtra, a teljes korpuszon.

**LTS-fegyelem.** Java 25 LTS és Node 24 LTS. Négy Dependabot-PR-t ezért zártam le: a Java 26 nem LTS (jövő hónapban kifut), a Node 26 csak októbertől lesz az, a Maven-image bumpja pedig a pinjét lebegő `3`-ra oldotta volna, amivel a build nem reprodukálható. A TypeScript 7-et nem az elv, hanem az Angular zárta ki (`@angular/build` peer igénye `typescript >=6.0 <6.1`); a csoportból a jsdom emelését átvettem.

**A korpuszban a nulla százalék is helyes válasz.** Zrínyi és Arany verse ütemhangsúlyos, nem időmértékes — ezekre a „nincs időmértékes találat" a hű felelet, és a motor a másik ágon ismeri fel őket. A küszöbök ezért versenkéntiek, nem globálisak.

**Két futtatókörnyezet, egy motorforrás.** A GitHub Pages-változat és a Docker-image ugyanabból a `kalliope-core`-ból él, és a felület **ugyanaz a bundle** — futásidőben derül ki, van-e backend. Nincs kettős karbantartás.

## Ismert korlátok

* **Ragrímek:** Mivel a rímdetektor a sorvégeket fonetikailag vizsgálja, a magyar ragrímek (pl. két `-nak` végződés) rímként jelennek meg.
* **Íráskép vs. Kiejtés:** A szóösszetételi határon álló találkozások (pl. `község` -> `z+s`) íráskép alapján egy hangnak tűnhetnek, a kiejtési kivételszótár csak a leggyakoribb eseteket (pl. `igazság`) fedi le.
* **Ütemhangsúly:** Az illesztés szótagszámon és szóhatáron alapul, nem végez mély, kontextuális hangsúlyelemzést.

## Szövegek és Jogi helyzet

A kód **MIT licenc** alatt áll, Váradi Nagy Pál és Porkoláb Ádám közös szerzőségével (lásd a [`LICENSE`](LICENSE) fájlt). Hogy ehhez a kiadáshoz pontosan mi változott, és mi maradt érintetlenül az eredetiből: [`CHANGELOG.md`](CHANGELOG.md).

A példatárban (`Examples.java`) szereplő versek nagyrészt közkincsek. A két kivétel Devecseri Gábor Homérosz-fordítása, amelyek oktatási célú szabad felhasználás keretében (Szjt. 33–35. §) szerepelnek a tesztkorpuszban, a forrás pontos megjelölésével. Más célú felhasználás esetén ezeket a szövegeket cserélni szükséges.
