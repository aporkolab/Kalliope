# Kalliopé

Magyar verstani elemző: **skandál**, **versmértéket ismer fel**, és **rímképletet** ad — mindkét
magyar ritmusrendben, időmértékesben és ütemhangsúlyosban egyaránt.

Minden szótagról megmondja, **miért** olyan hosszú; ha egy sor nem illeszkedik, megmondja, **min
múlik**; a végén pedig egy mondatban összegzi, milyen vers ez: *„Időmértékes verselés:
disztichonok."*

```bash
git clone https://github.com/aporkolab/Kalliope.git && cd Kalliope
docker compose up --build          # → http://localhost:8080
```

> A CI a `main` ágról feltölt egy image-et a `ghcr.io/aporkolab/kalliope:latest` címre is, de a
> GitHub a csomagokat alapból **privátra** állítja. Amíg a repó Packages beállításánál nem teszed
> publikussá, a `docker run ghcr.io/...` idegennek nem fog működni — a fenti build viszont mindig.

## Amit tud

| | |
|---|---|
| **Időmértékes** | 53 sorfajta, 38 kolón, 11 versláb, 8 összetett sor, 18 szakaszmérték; szigorú illesztés, kapcsolható licenciákkal |
| **Ütemhangsúlyos** | 20 magyaros sorfajta ütemtagolással; a metszet minősége (tiszta vagy laza) külön látszik |
| **Kettős ritmus** | ha a szakasz mindkét rendnek megfelel, jelezzük — de nem állítjuk, hogy „szimultán vers": az ahhoz kell, hogy *maradéktalanul* megfeleljen mindkettőnek |
| **Rím** | képlet vaksorral (`x`), a képlet neve (keresztrím, bokorrím, félrím…), és soronként a rím fajtája (tiszta rím, ragrím, asszonánc, önrím) |
| **Cezúra** | a mérték jelölt metszete, és a hexameter penthémimerész / kata triton trokhaion / hephthémimerész metszete |
| **Ha nem illeszkedik** | a legközelebbi mérték és a pontos eltérés: *„hexameter lenne, ha — 1. szótag: rövid helyett hosszú kellene"* |
| **Szótagszintű indoklás** | 12-féle ok: természeténél fogva hosszú, helyzeténél fogva hosszú, sorvégi közös, névelő, muta cum liquida, összevont kettőshangzó… |
| **Felülbírálás** | a szótagra kattintva átállítható a hosszúság, és az elemzés újrafut — a verstan értelmezés kérdése, nem orákulumé |
| **Összegzés** | egy mondat + részletek: szerkezet, szakaszmérték, sorfajták, ütemtagolás, rím, licenciák, metszet |
| **Ritmustérkép** | egy negyvensoros eposzrészlet ritmusa egy pillantással befogható; a sorra kattintva odaugrik |
| **Nyomtatás / PDF** | a böngésző nyomtatási párbeszédén át; a lapra az ítélet, a részletek és a skandált sorok kerülnek, a szerkesztő és a kezelőfelület nem |

A felület színrendszere a [Radix Colors](https://www.radix-ui.com/colors) skáláira épül (slate alap,
iris akcentus), mert azok hitelesített kontrasztarányokkal és párosított sötét változattal jönnek.
A szótaghosszúság három jelentése három elkülönülő hue-t kap — hosszú: iris, rövid: jade, közös:
amber —, de a jelentés nem csak a színen múlik: ott a jel is (— ∪ ×). Világos, sötét és
rendszerkövető téma; mobilon a sorok kártyákra tördelődnek, a gombok legalább 44 képpont magasak.

## Felépítés

Három modul, egyetlen deploy-artefaktum:

| Modul | Mi ez | Függőségek |
|---|---|---|
| `kalliope-core` | a verstani motor, a metrikai kánon és a példatár | **nulla** — csak a JDK |
| `kalliope-api` | vékony REST-réteg + a felület kiszolgálása | Spring Boot 4.1 |
| `kalliope-web` | a webes felület | Angular 22 |

Az Angular build a Spring Boot jar `static/` mappájába kerül, tehát **egy image, egy port, nincs
CORS, nincs külön statikus hosting**. A motor nem ismeri se a Springet, se a JSON-t: külön
futtatható és külön tesztelhető.

```
kalliope/
├─ kalliope-core/    Notation · Phonology · TextNormalizer · Scansion · Caesura
│                    MetricCanon · MeterMatcher · NearMiss · RhymeDetector
│                    AccentualCanon · AccentualMatcher · VerseSummary
│                    Analyzer · Examples · KalliopeCli
├─ kalliope-api/     AnalyzeController · CanonController · SpaConfig
│                    ApiExceptionHandler · RateLimitFilter
├─ kalliope-web/     Angular (standalone, signals, zoneless)
├─ Dockerfile        node build → maven build → rétegelt JRE image, AOT-gyorsítótárral
├─ compose.yaml
└─ .github/workflows/ci.yml
```

Verziók: Java 25 (LTS), Spring Boot 4.1, Angular 22, Node 24, Maven 3.9 (wrapper a repóban).

## Futtatás

**Konténerből** (nem kell se JDK, se Node):

```bash
docker compose up --build
```

**Fejlesztéshez** két terminál:

```bash
./mvnw -pl kalliope-api -am spring-boot:run       # API a 8080-on
cd kalliope-web && npm ci && npm start            # felület a 4200-on, /api proxyzva
```

**Parancssorból**, felület nélkül:

```bash
./mvnw -pl kalliope-core -am package
java -jar kalliope-core/target/kalliope-core-*.jar            # a példatár elemzése
java -jar kalliope-core/target/kalliope-core-*.jar vers.txt   # fájl elemzése
cat vers.txt | java -jar kalliope-core/target/kalliope-core-*.jar -
java -jar kalliope-core/target/kalliope-core-*.jar --canon    # a metrikai kánon
```

**Ellenőrzés** (ezt futtatja a CI is):

```bash
./mvnw verify        # tesztek + Spotless + 80%-os lefedettségi küszöb
cd kalliope-web && npm ci && npx ng test --no-watch && npx prettier --check "src/**/*.{ts,html,css}"
```

| | Teszt | Lefedettség (sor) |
|---|---:|---:|
| `kalliope-core` | 91 | 95% |
| `kalliope-api` | 20 | 84% |
| `kalliope-web` | 33 | 88% |

A 80%-os küszöb mindhárom modulban ki van kényszerítve (JaCoCo `check`, illetve
`vitest-base.config.ts`), tehát a build elbukik, ha valaki lerontja.

## API

| Végpont | Mit ad |
|---|---|
| `POST /api/analyze` | a teljes elemzés szakaszonként, soronként, szótagonként |
| `GET /api/canon` | a mértékek, szakaszmértékek, beállítás-leírások és a hosszúság-indoklások szótára |
| `GET /api/canon/{id}` | egy mérték |
| `GET /api/examples` | a példatár (a korpusz) |

```jsonc
POST /api/analyze
{
  "text": "Jót s jól! Ebben áll a nagy titok. Ezt ha nem érted,\nSzánts és vess, s hagyjad másnak az áldozatot.",
  "settings": { "a_szokezdo_hangsuly_nyujthat": true },   // opcionális
  "overrides": [ { "line": 0, "syllable": 0, "quantity": "-" } ]  // opcionális, kézi hosszúság
}
```

A válasz `verse` mezője az összegzés (`system`, `headline`, `details`), a `stanzas` a szakaszok,
soronként a skandálással (`scansion`), a megvalósult hosszúsággal (`realized`), a szótagokkal és
indoklásukkal, a mértéktalálatokkal, a metszetekkel, a rím fajtájával és — találat híján — a
`nearMiss` magyarázattal.

Hibák RFC 9457 (`application/problem+json`) szerint. Az elemzésre percenkénti kérésszám-korlát él
(alapból 60, `kalliope.rate-limit.requests-per-minute`); 0 kikapcsolja. A felület a `/api/canon`-t
egyszer kéri le induláskor: a magyar feliratok egyetlen forrása a motor, nincs kétszer leírva.

A hash-nevű Angular-fájlok egy évig gyorsítótárazhatók, az `index.html` viszont `no-store` — enélkül
a friss telepítés után is a régi bundle töltődne be.

### Nyomtatás

A „Nyomtatás" gomb a böngésző saját párbeszédét nyitja, ahonnan PDF-be is menthető. Nincs mögötte
PDF-könyvtár: egy `@media print` stíluslap alakítja a lapot, így nincs külön renderelő, ami
elcsúszhatna a képernyős változattól. A lapra az ítélet, az összegzés részletei és a skandált sorok
kerülnek — a szerkesztő, a fejléc, a ritmustérkép és a gombok nem. A hosszúságot nyomtatásban a jel
hordozza (— ∪ ×), mert fekete-fehér nyomtatón a szín eltűnik; egy sor pedig sosem törik ketté
lapok között.

## Hogyan skandál

A szabályok hiteles magyar verstani forrásokból valók (Fazekas Kulturális Enciklopédia — Verstan;
Csehy Zoltán–Polgár Anikó: *Gyakorlati magyar verstan*; Magyartanár / Kecskés–Szilágyi–Szuromi:
*Kis magyar verstan*; A magyar helyesírás szabályai 12. kiadás):

- a szótag a következő magánhangzóig tart, **átlépve a szóhatárt**;
- **természeténél fogva hosszú**: hosszú magánhangzó;
- **helyzeténél fogva hosszú**: rövid magánhangzó után egy hosszú vagy legalább két rövid
  mássalhangzó. A kétjegyű betű (`cs, dz, gy, ly, ny, sz, ty, zs`, `dzs`) **egy** mássalhangzó; a
  kettőzött kétjegyű (`ssz, ggy, nny`…) **egy hosszú**, tehát két pozíció; az `x` **két** hang
  (`ksz`); a `dz`/`dzs` kettőzés nélkül is hosszú (AkH. 87. §);
- **közös (anceps, `?`)**: a sorvégi szótag, a határozott névelő, a rövid nyílt szótagú kötőszók és
  névmások, a *muta cum liquida* (zárhang + likvida — de csak szón **belül**), és minden torlódás,
  amelynek az olvasata bizonytalan.

**A skandáló szigorú.** Költői licenciát alapból nem feltételez: ha egy sor így nem illeszkedik, az
a hű válasz, nem hiba. Ahol viszont a hagyomány valóban kétféle olvasatot enged, ott nem dönt
helyettünk: a görög-latin **kettőshangzókra** (`Európa`, `Zeusz`, `Péleidész`) *változatokat* állít
elő, és a mérték választ — így lesz az Íliász kezdősora hexameter.

A **megjelenítés a döntést mutatja**, nem a nyers `?`-eket: a „Még nyílnak a völgyben" sorban a
„nak" önmagában kétféle olvasatú, de amint az anapesztus illeszkedik, eldőlt, hogy rövid. A közös
eredetet pontozott aláhúzás jelzi.

### Beállítások

Az első hat az eredeti 2006-os adatbázis kapcsolója, az utolsó négy ennek a változatnak a
dokumentált kiegészítése:

| Kulcs | Alap | Mit tesz |
|---|:--:|---|
| `az_s_kotoszo_kozombos` | be | az „s" kötőszó mássalhangzója elhagyható |
| `az_abece_betuinek_kulon_szotag` | ki | a magában álló betű betűnévvé bomlik (`b` → `bé`) |
| `emberi_nyelvu_mit_tudok` | be | jelöli a hangsúlytalan szavakat |
| `egynel_tobb_telitalalat_keresese` | be | egynél több teljes találatot is felsorol |
| `az_asszonanc_rimkent_valo_kezelese` | be | az asszonánc is rímnek számít |
| `latszik_az_utemhangsuly_a_gorogon` | ki | kiírja az iktussort (`÷`/`Ú`) |
| `a_rovid_kotoszok_kozombosek` | be | a rövid, nyílt szótagú kötőszók közösek (*ha, de, te, mi*…) |
| `a_gorog_diftongusok_osszevonhatok` | be | az `eu`/`au`/`ei` egy szótagnak is vehető |
| `a_szovegi_massalhangzo_kozosse_tesz` | be | latinos hagyomány: a szóvégi mássalhangzó zárhatja a szótagot |
| `a_szokezdo_hangsuly_nyujthat` | **ki** | költői licencia: a szókezdő hangsúly megnyújthat (ettől lesz az Íliász kezdősora hexameter) |

A három tisztán ablakkezelési beállítás (`a_jobb_oldali_szoveg_formazott_legyen`,
`a_fuggoleges_toszogalos_mutyur_helye`, `a_beallitasokat_tartalmazo_felulet_elrejtve`) az eredeti
Delphi-felülethez tartozott; itt nincs értelmük, és nem is teszünk úgy, mintha lenne.

## Korpusz-riport

A példatár tizenegy valódi vers, lehetőleg teljes egészében, hiteles forrásból. Ez egyben a motor
regressziós hálója: a `CorpusTest` elbukik, ha az arány romlik.

| Vers | Sor | Illeszkedik | Az összegzés ítélete |
|---|---:|---:|---|
| Zrínyi: Szigeti veszedelem (részlet) | 4 | 0% | ütemhangsúlyos: felező tizenkettes |
| Arany: Toldi, Első ének | 21 | 0% | ütemhangsúlyos: felező tizenkettes |
| Homérosz–Devecseri: Íliász I. 1–40. | 40 | 92% | időmértékes: hexameterek |
| Homérosz–Devecseri: Odüsszeia I. 1–40. | 40 | 90% | időmértékes: hexameterek |
| Vörösmarty: Zalán futása, előhang | 34 | 97% | időmértékes: hexameterek |
| Radnóti: Hetedik ecloga (teljes) | 36 | 94% | időmértékes: hexameterek |
| Kazinczy: A nagy titok | 2 | 100% | időmértékes: disztichonok |
| Berzsenyi: A magyarokhoz I. (részlet) | 4 | 100% | időmértékes: alkaioszi strófa |
| Berzsenyi: A közelítő tél (teljes) | 24 | 100% | szimultán: aszklepiadeszi + felező tizenkettes |
| Berzsenyi: Horác (teljes) | 16 | 93% | szimultán: aszklepiadeszi + felező tizenkettes |
| Petőfi: Szeptember végén (teljes) | 24 | 95% | időmértékes |
| **Összesen** | **245** | **84%** | |

A két nulla százalék nem hiba, hanem a helyes válasz: Zrínyi és Arany verse ütemhangsúlyos, nem
időmértékes — a motor ezeket a másik ágon ismeri fel, és Zrínyinél külön kimondja, hogy a metszet
gyakran szóba esik. A hiányzó néhány százalék a költői licencia: azoknál a soroknál a „miért nem
illeszkedik?" megmondja, min múlik.

## Mi változott ehhez a kiadáshoz

Az előző változat egyetlen Java fájl volt, a metrikai adatbázissal beágyazott **szövegként**, saját
szintaxissal (`;` komment, `.` mezőnév, `!` beállítás, `@` konstans, `#complex`, `$` szó). A
mélyaudit 137 megerősített hibát talált (25 további állítást az ellenőrzés megcáfolt); a többségük
két forrásból jött:

**1. A saját szövegformátum és a hozzá írt parser.** Ez a réteg most nincs: a kánon **típusos Java
adat**, a hivatkozás objektumhivatkozás. Ezzel egy csapásra megszűnt az elgépelt hivatkozás
(`nib.alex.1.fiktiv` ↔ `.fictive` — emiatt mind a 6 „nibelungizált alexandrin" némán csonka volt),
a némán elnyelt feloldási hiba, a kis/nagybetű-érzékeny konstansnév, a névalias-tábla láncolt
csere miatti önrontása (`adoniszi` → `adonisziizi`), a körkörös hivatkozás okozta `OutOfMemoryError`,
és a dokumentált, de sosem megvalósított nyelvtani formák.

**2. Valódi verstani hibák a skandálóban és a rímdetektorban.** A javítottak közül:

- a kétjegyű betűket az első betűjükre csonkolta, ezért a `gy`/`ty`/`dz` zárhangnak, az `ly`
  likvidának látszott → hamis *muta cum liquida*: `hegyre`, `szablyáját` közös lett hosszú helyett;
- a *muta cum liquida* szóhatáron is elsült (`vak róka`), ahol nem szabad;
- az `x` egy hangnak számított, a `dz`/`dzs` rövidnek;
- az illesztő a minta összes realizációját kifejtette — ez a szabad pozíciók számában exponenciális
  —, és 8192 fölött **csonkolt**, majd a csonkolt előtagokat hasonlította: egy negyven szótagos sor
  „hexameter" lett. Helyette pozíciónkénti dinamikus programozás;
- az elemző eldobta az üres sorokat, ezért többstrófás versen **soha egyetlen szakaszmértéket sem**
  talált, és a rímbetűk végigfutottak az egész versen;
- a disztichon csak pontosan kétsoros versre illett — egy hatsoros elégia nem volt három disztichon;
- a rímkulcs az utolsó magánhangzótól indult, ezért `haza`, `soha`, `béka`, `anya` mind rímelt;
  a zöngétlenítés a szó **végén** is futott, és láncba fűződött, így `kard`, `part`, `halt` egy
  kulcsra esett;
- a rímbetűk `z` után `{`, `|`, `}` karaktereket írtak; a rímtelen sor nem kapta meg a szabályos
  `x` jelet (vaksor), így a félrím `xaxa` helyett `abcb` lett;
- a `.fictive` segédmértékek valódi találatként jelentek meg;
- az ütemhangsúly-sor nem a ténylegesen illeszkedő realizációt írta ki, hanem az első azonos
  hosszúságút — ellentmondott a mellette álló skandálásnak;
- `toLowerCase()` locale nélkül (török `I`), négyzetes normalizálás (egy hosszú sor 21 másodperc),
  nem törhető szóközök, `null` bemenet.

**3. A README állításai.** A korábbi szöveg azt írta, „minden beállítás ténylegesen befolyásolja a
kimenetet" — négy közülük sehol nem volt beolvasva.

A valódi verskorpusz utólag még két hibát fogott, amit szintetikus teszt nem talált volna meg: a
magánhangzó nélküli „s" kötőszó kiesett a megjelenítésből (a felület „Fegyvert, vitézt…"-et írt
volna), a kánon-kereső pedig lekisbetűsítve kereste a mintát is, így `-UU-?`-re sosem talált.

### Javítások a 2006-os kánonban

Csak ott nyúltunk az adathoz, ahol a minta bizonyíthatóan **más formát ír le, mint a neve**. Minden
javítás megőrzi az eredeti mintát és a forrást — a felület `Kánon` nézetében kinyithatók:

| Mérték | Eredeti | Javítva | Miért |
|---|---|---|---|
| `choliambus` | `?-U-?-U-U-U?` | `?-U-?-U-U--?` | hiányzott az utolsó előtti hosszú, ami *definiálja* a sánta jambust — a minta közönséges jambikus trimeter volt |
| `alkaioszi 3` | `?-U-U-U-?` | `?-U-?-U-?` | az 5. pozíció közös; fix rövidként a teljes alkaioszi strófa illeszthetetlen volt |
| `aszklepiadeszi D13` | `---UU-?` (7) | `---UU-U?` (8) | a 4. aszklepiadeszi strófa rövid sora glükóni, nem pherekrateus |
| `4mtr trochaicus` | `-U-U-U-U-U-U-U-` | `-U-?-U-?-U-?-U-` | a trochaikus metrum második eleme közös |
| `anapesztikus dimeter`, `daktilikus tetrameter` | tiszta lábak | `=-\|=-\|…`, `-=\|-=\|…` | a nevük spondeusz-helyettesítést ígért, a mintájuk tiltotta |
| `glykoni2a/2b` | `-?-UU-U`, `U--UU-U` | törölve | hét pozíciós, rövidre végződő „glükóni" nem létezik, viszont minden valódi glükóni első hét szótagjára ráillett |

Új: `versus spondiacus` (spondeuszi ötödik lábú hexameter).

**Amihez nem nyúltunk.** Az ellenőrzés több „javítási" javaslatot megcáfolt: a `szapphói sor` 4.
pozíciója, a `léküthion`, a `dochmius`, a `phalaikoszi` bázisa, a `wilamovitziánus`, a
`téleszilleion` és az anakreóni sorok az eredeti szerző saját, védhető kódolásai maradtak.

## Ismert korlátok

- A rímdetektor a sorvégeket veszi. A magyar **ragrím** ezért összecseng: egy rímtelen hexameteres
  szövegben két `-nak` végű sor rímelőnek látszik. A motor ezt ragrímnek is nevezi, és hosszú,
  szakaszra nem tagolt szövegben nem is állít rímképletet — de nem tudja, mit gondolt a költő.
- A kétjegyű betűk felismerése írásképi: a szóösszetételi határon álló `z+s`, `d+z`, `c+s`
  (`község`, `vadzab`) egy hangnak látszik. A binárisból örökölt kiejtési tábla ezt csak az
  `igazság` típusra kezeli.
- Az ütemhangsúlyos illesztés szótagszámon és szóhatáron alapul, nem valódi hangsúlyelemzésen.
- A szótagszintű indoklás az elsődleges olvasatra vonatkozik; ha a sor csak összevont
  kettőshangzóval illeszkedik, azt a felület külön jelzi („összevonással").
- A `kalliope.exe` futásidejű, bit-pontos összevetése nem történt meg (ahhoz Wine kellene); a
  hitelesség az adat és a szabályok egyezésén nyugszik.

## A projekt története

**1. Eredet.** A Kalliopé eredetileg egy ~2006-os Borland Delphi asztali program volt. A forráskód
elveszett; csak a lefordított bináris maradt meg, egy Ghidra reverse-engineering projekt formájában.

**2. Visszafejtés.** A logikát a dekompilátumból kellett kibányászni: a `.gzf` konténer felbontása,
a VCL-alapú architektúra azonosítása (a vers párhuzamos `TStringList`-ekként tárolva), és a
verstani logika kihorgonyzása a program magyar string-konstansairól (`rimkeplet`, `strofa`,
`utemhangsuly`…).

**3. Az adatbázis.** A program külső metrikai adatbázisa külön került elő, és megadta a teljes
formátum-nyelvtant és az adatot. Mivel a klasszikus versmérték-kánon gyakorlatilag zárt halmaz,
nincs mögötte adatbázis: a kánon a forrásban él, típusos adatként.

**4. Hitelesítés a binárisból.** A `kalliope.exe` saját címtartományának összes string-literálja
végigvizsgálva. Ez igazolta a rekonstruált szkenner magját (a **digráf-lista** és a **muta cum
liquida** halmaz pontosan egyezik), és innen való a **normalizáló előfeldolgozó** (`tv→tévé`,
`w→v`, betűnevek), a **központozás-lista**, a **név-alias tábla**, az **ütemhangsúly-jelek**
(`U`/`Ú`/`-`/`÷`) és a **verzió** (`VNP's Kalliope 1.71 beta`).

**5. Ez a kiadás.** Mélyaudit, a talált hibák javítása, modularizálás, REST API, webes felület,
konténerezés, majd az elemzés kiterjesztése az ütemhangsúlyos verselésre. A rímdetektor a binárisból
portolt tábla helyett Arany János rokonsági rendszerére és a mai verstani szakirodalomra épül — a
portolt tábla ugyanis a szó végén is egyesített, ami épp az ellenkezője Arany kódaszabályának.

## Szövegek és jogi helyzet

A példatár szövegei hiteles forrásból valók (Wikiforrás, Magyar Elektronikus Könyvtár, Sulinet
szöveggyűjtemény). Nagy részük közkincs — a szerző halála után hetven évvel. **Két kivétel**
Devecseri Gábor (1917–1971) Homérosz-fordítása: az Íliász és az Odüsszeia negyven-negyven sora
szemléltetésként, oktatási célú szabad felhasználás keretében szerepel (Szjt. 33–35. §), a forrás és
a fordító megjelölésével. Ha a projektet más célra használod, ezt a két szöveget cseréld le.

A kódnak jelenleg **nincs licencfájlja**; amíg nincs, a GitHub alapértelmezése szerint minden jog
fenntartva. Ha nyílt forrásúvá tennéd, tegyél a repóba egy `LICENSE` fájlt.
