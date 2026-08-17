# Kalliopé

Magyar verstani elemző: **skandál**, **versmértéket ismer fel**, és **rímképletet** ad — mindkét
magyar ritmusrendben, időmértékesben és ütemhangsúlyosban egyaránt.

Minden szótagról megmondja, **miért** olyan hosszú; ha egy sor nem illeszkedik, megmondja, **min
múlik**; a végén pedig egy mondatban összegzi, milyen vers ez: *„Időmértékes verselés:
disztichonok."*

> A Kalliopét **Váradi Nagy Pál** (vnp85) írta 2004–2006 táján. Ez a változat az ő munkájára épül;
> **közösen tesszük közzé**, társszerzőként, MIT licenc alatt. → [Eredet és
> szerzőség](#eredet-és-szerzőség)

```bash
git clone https://github.com/aporkolab/Kalliope.git && cd Kalliope
docker compose up --build          # → http://localhost:8080
```

> A CI a `main` ágról feltölt egy image-et a `ghcr.io/aporkolab/kalliope:latest` címre is, de a
> GitHub a csomagokat alapból **privátra** állítja. Amíg a repó Packages beállításánál nem teszed
> publikussá, a `docker run ghcr.io/...` idegennek nem fog működni — a fenti build viszont mindig.

## Eredet és szerzőség

A Kalliopét **Váradi Nagy Pál** (vnp85) írta 2004–2006 táján, egyetemistaként, **Lazarus /
FreePascal** alatt. Ugyanennek a programnak él egy webes változata is, a szerző saját oldalán:
<https://csillagtura.ro/projektek/kalliope/>.

Ez a repó az ő munkájára épül, az ő engedélyével. **Közösen tesszük közzé**, MIT licenc alatt,
társszerzőként.

**Mi alapján dolgoztam, és hogyan.** A forráskód nem volt nyilvános, ezért a lefordított
`kalliope.exe` binárisából indultam: Ghidrával visszafejtettem, majd kibontottam az adatszekciót. A
motort Java nyelven újraírtam, de **az adat az eredeti**. Váradi Nagy Pál munkája:

- a **metrikai kánon** — verslábak, kolónok, sorfajták, összetett sorok, szakaszmértékek;
- a **kiejtési normalizáló tábla** (`tv→tévé`, `w→v`, betűnevek);
- a **név-alias tábla** és a **központozás-lista**;
- a rímfelismerés **mássalhangzó-normalizáló táblája** (ezt végül nem használom — lásd lentebb);
- az **ütemhangsúly-jelek** (`U`/`Ú`/`-`/`÷`) és a verziószöveg (`VNP's Kalliope 1.71 beta`);
- a **digráf-lista** és a *muta cum liquida* halmaz, amelyekkel a rekonstruált szkennert
  hitelesítettem.

A kánon `VNP sorfajták` és `VNP-strófák` szekciója — *Gyilkosok, Éhesek, Pincsike1, mélyégi álom,
utolsó mosás, létépartja* — a szerző **saját versformái**, a saját verseiből.

Az eredeti felületet nem portoltam (utoljára Windows XP alatt futott); helyette Angular felület
készült. Amit én tettem hozzá: a szakaszmérték-illesztés, az ütemhangsúlyos ág, néhány kiegészítő
antik sorfajta, a REST API és a webes felület.

Így a kánon ma **kettőnk közös munkája**: az adat az övé, a javítások és a kiegészítések az enyémek,
és mindkettő nyomon követhető — minden eltérésnél ott az eredeti minta és a forrás.

## Amit tud

| | |
|---|---|
| **Időmértékes** | 56 sorfajta, 38 kolón, 11 versláb, 8 összetett sor, 20 szakaszmérték; szigorú illesztés, kapcsolható licenciákkal |
| **Ütemhangsúlyos** | 20 magyaros sorfajta ütemtagolással; a metszet minősége (tiszta vagy laza) külön látszik |
| **Kettős ritmus** | ha a szakasz mindkét rendnek megfelel, jelzi — de nem állítja, hogy „szimultán vers": az ahhoz kell, hogy *maradéktalanul* megfeleljen mindkettőnek |
| **Rím** | képlet vaksorral (`x`), a képlet neve (keresztrím, bokorrím, félrím…), és soronként a rím fajtája (tiszta rím, ragrím, asszonánc, önrím) |
| **Cezúra** | a mérték jelölt metszete, és a hexameter penthémimerész / kata triton trokhaion / hephthémimerész metszete |
| **Ha nem illeszkedik** | a legközelebbi mérték és a pontos eltérés: *„hexameter lenne, ha — 1. szótag: rövid helyett hosszú kellene"* |
| **Szótagszintű indoklás** | 12-féle ok: természeténél fogva hosszú, helyzeténél fogva hosszú, sorvégi közös, névelő, muta cum liquida, összevont kettőshangzó… |
| **Felülbírálás** | a szótagra kattintva átállítható a hosszúság, és az elemzés újrafut — a verstan értelmezés kérdése, nem orákulumé |
| **Összegzés** | egy mondat + részletek: szerkezet, szakaszmérték, sorfajták, ütemtagolás, rím, licenciák, metszet |
| **Ritmustérkép** | egy negyvensoros eposzrészlet ritmusa egy pillantással befogható; a sorra kattintva odaugrik |
| **Nyomtatás / PDF** | a böngésző nyomtatási párbeszédén át; a lapra az ítélet, a részletek és a skandált sorok kerülnek, a szerkesztő és a kezelőfelület nem |
| **Megosztható link** | a „Link" gomb a verset a címsor törtrészébe kódolja (`#v=…`) és vágólapra teszi — szerver és adatbázis nélkül |
| **JSON export** | a teljes elemzés letölthető (`kalliope-elemzes.json`), ugyanaz a szerkezet, amit az API ad |
| **Téma** | világos / sötét / rendszerkövető, a választás megmarad |

### A jelölés

| Jel | Mit jelent |
|:--:|---|
| `—` | hosszú szótag |
| `∪` | rövid szótag |
| `×` | közös (anceps) — a mérték nem dönti el |
| `\|` | **lábhatár** (vékony elválasztó a felületen) |
| `‖` | **sormetszet, cezúra** (vastag kettős vonal a felületen) |
| pontozott aláhúzás | a szótag eredetileg közös volt, a mérték döntötte el |

A mintákban ugyanez a `-` (hosszú), `U` (rövid), `?` (közös) jelekkel szerepel, a lábhatár egy, a
cezúra két függőleges vonal; a `=` pedig olyan helyet jelöl, ahol spondeusz és daktilus is állhat
(`-` vagy `UU`) — így lesz a hexameter mintája `-=|-=|-=|-=|-UU|-?`.

A felület színrendszerét a [Radix Colors](https://www.radix-ui.com/colors) skáláira építettem (slate
alap, iris akcentus), mert azok hitelesített kontrasztarányokkal és párosított sötét változattal
jönnek. A szótaghosszúság három jelentése három elkülönülő hue-t kap — hosszú: iris, rövid: jade,
közös: amber —, de a jelentés nem csak a színen múlik: ott a jel is (— ∪ ×). Világos, sötét és
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
| `kalliope-core` | 92 | 95% |
| `kalliope-api` | 20 | 84% |
| `kalliope-web` | 33 | 88% |

A 80%-os küszöböt mindhárom modulban kikényszerítettem (JaCoCo `check`, illetve
`vitest-base.config.ts`), tehát a build elbukik, ha valaki lerontja.

### Konfiguráció

Minden beállításnak van működő alapértéke; a lenti kulcsok környezeti változóként is megadhatók
(`SERVER_PORT=9090`, `KALLIOPE_RATE_LIMIT_REQUESTS_PER_MINUTE=0`).

| Kulcs | Alap | Mit tesz |
|---|:--:|---|
| `server.port` | `8080` | a HTTP-port |
| `kalliope.rate-limit.requests-per-minute` | `60` | a `/api/analyze` percenkénti kérésszám-korlátja; `0` kikapcsolja |
| `JAVA_TOOL_OPTIONS` | `-XX:MaxRAMPercentage=75.0` | a compose ezt állítja be; konténerben ennyi memóriát használhat a JVM |

A konténer `346 MB`, és ~0,8 másodperc alatt indul (rétegelt JRE image, AOT-gyorsítótárral). A
compose-ban van healthcheck is: a `/api/canon`-t hívja.

### CI

A [`ci.yml`](.github/workflows/ci.yml) minden pusholásra és minden PR-re három párhuzamos jobot
futtat:

| Job | Mit csinál |
|---|---|
| **Java** | `./mvnw -B -ntp verify` — teszt + Spotless + JaCoCo-küszöb egyetlen parancsban |
| **Angular** | `prettier --check`, majd `ng test --no-watch` a lefedettségi küszöbbel |
| **Docker image** | megépíti; `main`-re pusholva fel is tölti a `ghcr.io/aporkolab/kalliope:latest` címre |

A CI-nek nincs külön kapulistája: amit helyben futtatsz, azt futtatja ő is.

### Ha hozzá akarsz nyúlni

- A kód formázását a **Spotless** (palantir-java-format) és a **Prettier** tartja karban; a
  `./mvnw spotless:apply`, illetve az `npx prettier --write` mindent helyretesz. A `verify` elbukik
  formázatlan kódon.
- Az olvasást az `Analyzer.analyze()` felől érdemes kezdeni: az fűzi össze a normalizálót, a
  skandálót, a mértékillesztőt, a rímdetektort és az összegzőt.
- Ha a verstani viselkedésen változtatsz, előbb a [korpuszt](#korpusz-riport) nézd: a `CorpusTest`
  valódi verseken méri az arányt, és elbukik, ha romlik. Új verstani szabályhoz **forrás kell**, és
  lehetőleg egy korpuszsor, amin látszik a különbség.

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

A szabályokat hiteles magyar verstani forrásokból vettem (Fazekas Kulturális Enciklopédia — Verstan;
Csehy Zoltán–Polgár Anikó: *Gyakorlati magyar verstan*; Magyartanár / Kecskés–Szilágyi–Szuromi:
*Kis magyar verstan*; A magyar helyesírás szabályai 12. kiadás):

- a szótag a következő magánhangzóig tart, **átlépve a szóhatárt**;
- **természeténél fogva hosszú**: hosszú magánhangzó;
- **helyzeténél fogva hosszú**: rövid magánhangzó után egy hosszú vagy legalább két rövid
  mássalhangzó. A kétjegyű betű (`cs, dz, gy, ly, ny, sz, ty, zs`, `dzs`) **egy** mássalhangzó; a
  kettőzött kétjegyű (`ssz, ggy, nny`…) **egy hosszú**, tehát két pozíció; az `x` **két** hang
  (`ksz`); a `dz`/`dzs` kettőzés nélkül is hosszú (AkH. 87. §);
- **közös (anceps, `?`)**: a sorvégi szótag, a határozott névelő, a rövid nyílt szótagú kötőszók és
  névmások, a *muta cum liquida* (zárhang + likvida — de csak szón **belül**), a görög aspiráta
  (`kh`, `th`, `ph`), és minden torlódás, amelynek az olvasata bizonytalan.

**A skandáló szigorú.** Költői licenciát alapból nem feltételez: ha egy sor így nem illeszkedik, az
a hű válasz, nem hiba. Ahol viszont a hagyomány valóban kétféle olvasatot enged, ott nem dönt
előre: a görög-latin **kettőshangzókra** (`Európa`, `Zeusz`, `Péleidész`) *változatokat* állít elő,
és a mérték választ — így lesz az Íliász kezdősora hexameter.

A **megjelenítés a döntést mutatja**, nem a nyers `?`-eket: a „Még nyílnak a völgyben" sorban a
„nak" önmagában kétféle olvasatú, de amint az anapesztus illeszkedik, eldőlt, hogy rövid. A közös
eredetet pontozott aláhúzás jelzi.

### Beállítások

Az első hat az eredeti adatbázis kapcsolója, az utolsó négy az én dokumentált kiegészítésem:

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
Lazarus-felülethez tartozott; itt nincs értelmük, és nem is teszek úgy, mintha lenne.

## Korpusz-riport

A példatárba tizenegy valódi verset tettem, lehetőleg teljes egészében, hiteles forrásból. Ez
egyben a motor regressziós hálója: a `CorpusTest` elbukik, ha az arány romlik.

| Vers | Sor | Illeszkedik | Az összegzés ítélete |
|---|---:|---:|---|
| Zrínyi: Szigeti veszedelem (részlet) | 4 | 0% | ütemhangsúlyos: felező tizenkettes |
| Arany: Toldi, Első ének | 21 | 0% | ütemhangsúlyos: felező tizenkettes |
| Homérosz–Devecseri: Íliász I. 1–40. | 40 | 92% | időmértékes: hexameterek |
| Homérosz–Devecseri: Odüsszeia I. 1–40. | 40 | 98% | időmértékes: hexameterek |
| Vörösmarty: Zalán futása, előhang | 34 | 97% | időmértékes: hexameterek |
| Radnóti: Hetedik ecloga (teljes) | 36 | 94% | időmértékes: hexameterek |
| Kazinczy: A nagy titok | 2 | 100% | időmértékes: disztichonok |
| Berzsenyi: A magyarokhoz I. (részlet) | 4 | 100% | időmértékes: alkaioszi strófa |
| Berzsenyi: A közelítő tél (teljes) | 24 | 100% | szimultán: aszklepiadeszi + felező tizenkettes |
| Berzsenyi: Horác (teljes) | 16 | 94% | szimultán: aszklepiadeszi + felező tizenkettes |
| Petőfi: Szeptember végén (teljes) | 24 | 96% | időmértékes |
| **Összesen** | **245** | **86%** | |

A két nulla százalék nem hiba, hanem a helyes válasz: Zrínyi és Arany verse ütemhangsúlyos, nem
időmértékes — a motor ezeket a másik ágon ismeri fel, és Zrínyinél külön kimondja, hogy a metszet
gyakran szóba esik. A hiányzó néhány százalék a költői licencia: azoknál a soroknál a „miért nem
illeszkedik?" megmondja, min múlik.

## Összevetés a webes változattal

A szerző webes Kalliopéja ([csillagtura.ro](https://csillagtura.ro/projektek/kalliope/)) teljes
egészében kliensoldali JavaScript, tehát **kinyerhető és futtatható**. Kiszedtem a lapból, Node
alatt futtattam, és a fenti 245 soros korpuszt mindkét motorral végigskandáltattam, majd
szótagonként összevetettem. Ez váltja ki a `kalliope.exe` futásidejű összevetését, amihez Wine
kellett volna.

| | |
|---|---:|
| azonos szótagszámú sor | **236 / 245** (96,3%) |
| ezeken belül: az én nyers olvasatom megfér az övével | **100%** (3266/3266 szótag) |
| betű szerint azonos kimenet | 48,3% |

A „betű szerint azonos" alacsony szám nem eltérés, hanem **különböző felbontás**: az ő változata
minden szótagot hosszúra vagy rövidre dönt, az enyémben van **közös (`?`)** szótag is. Ahol ő dönt,
nálam kérdés marad, és a mérték dönt. Fajtánként:

| Eltérés | Db | Mi ez |
|---|---:|---|
| sorvégi szótag: nála rövid, nálam közös → a mérték hosszúvá teszi | 91 | **brevis in longo** — nálam van, nála nincs |
| névelő és rövid kötőszó: nála rövid, nálam közös | 63 | az én kiegészítésem (kapcsolható) |
| *muta cum liquida*: nála hosszú, nálam közös | 30 | nálam van, nála nincs |
| görög aspiráta (`kh`, `th`): nála hosszú, nálam rövid | 12 | **valódi hiba volt — mindkét oldalon**, lásd lentebb |

**A 9 eltérő szótagszámú sor mind a webes változat hibája**, három okból:

- 4 sor: a `y` betűt feltétel nélkül törli, ezért a görög eredetű szavakban **elnyeli a
  magánhangzót** — `labyrinth` → *labrinth*, `Zephyr` → *Zephr*. Berzsenyi négy sora így egy
  szótaggal rövidebb lesz;
- 3 sor: nincs kettőshangzó-kezelés, tehát `Poszeidáón`, `Aigiszthoszra`, `aithiopokhoz` eggyel
  több szótag;
- 2 sor: a szóvégi `eusz` → `evsz` összevonás **csak szóköz és sorvég előtt fut, írásjel előtt nem**,
  ezért `Akhilleusz.` és `Szmintheusz:` nem vonódik össze.

### Amit ebből átvettem

**A görög aspiráta kétértelműsége.** A `kh`, `th`, `ph`, `rh`, `ch` írásképe két olvasatot fed:
**egy** hang a görög névben (*A-khil-leusz* = χ, *I-tha-ka* = θ), de **kettő** a magyar
morfémahatáron (*csak+hogy*, *halandó+k+hoz*, *kap+hat*, *át+hat*). Eddig **mindketten** egy-egy
irányban döntöttünk, és mindketten hibáztunk: az ő változata az Íliász `akháj`-os sorait rontja el,
én az Odüsszeia `-okhoz` ragos sorait rontottam. Most **közös** a szótag, és a mérték választ. Ez
az egyetlen javítás:

- az Odüsszeia illeszkedési aránya **90% → 98%**, a korpuszé **84% → 86%**;
- az én nyers olvasatom a maradék 236 soron **minden egyes szótagon** megfér az övével;
- a `kaphat`, `áthat`, `csakhogy` típusú **magyar** szavak első szótaga eddig tévesen rövid volt.

**Három hiányzó sorfajta és két szakaszmérték.** A webes kánon 104 formájából 97-et már lefedtem;
átvettem a `Mozdonyszonett a`/`b` és az „anakreóni-féle sor" (`valami_anakreon`) mintát, valamint
két további Horatius-féle aszklepiadeszi strófát (F és G). A maradék négy eltérés a
[dokumentált javításom](#eltérések-az-eredeti-kánontól): `asklepiadesi_D13`, `glykoni2a`,
`glykoni2b` — ezeknél megtartom a saját döntést, forrással.

**Amit nem vettem át.** Az ő rímkulcsa mindig az utolsó **két** magánhangzót veszi; nálam a
terjedelem Arany szabályát követi (zárt sorvég → egy szótag, nyílt → kettő). A
mássalhangzó-normalizálása `r`-t, `l`-t és `j`-t egyetlen hangba olvasztja, ami a tiszta rímhez túl
megengedő. Az asszonáncnál ő elhagyja a magánhangzó-hosszúságot, nálam megmarad — Aranynál ez
külön, gyengébb fokozat, és összemosva a `dögmadaraknak` is rímelne az `akhájnak`-ra.

**Amit a szerzőnek jelezni érdemes.** Az `anyegin8` mintája `U-U-U-UU`; négy jambus sora nem
végződhet két rövidre, a hímrímes anyegin-sor `U-U-U-U-`. Az utolsó jel valószínűleg elgépelés.

## Mi változott ehhez a kiadáshoz

Ez a szakasz az **én Java portomról** szól, nem az eredeti programról: a felsorolt hibákat én
követtem el, a visszafejtett motor első Java változatában.

Az akkori változat egyetlen Java fájl volt, a metrikai adatbázissal beágyazott **szövegként**, saját
szintaxissal (`;` komment, `.` mezőnév, `!` beállítás, `@` konstans, `#complex`, `$` szó). A
mélyaudit 137 megerősített hibát talált (25 további állítást az ellenőrzés megcáfolt); a többségük
két forrásból jött:

**1. A saját szövegformátum és a hozzá írt parser.** Ezt a réteget kidobtam: a kánon **típusos Java
adat**, a hivatkozás objektumhivatkozás. Ezzel egy csapásra megszűnt az elgépelt hivatkozás
(`nib.alex.1.fiktiv` ↔ `.fictive` — emiatt mind a 6 „nibelungizált alexandrin" némán csonka volt),
a némán elnyelt feloldási hiba, a kis/nagybetű-érzékeny konstansnév, a névalias-tábla láncolt
csere miatti önrontása (`adoniszi` → `adonisziizi`), a körkörös hivatkozás okozta `OutOfMemoryError`,
és a dokumentált, de sosem megvalósított nyelvtani formák.

**2. Valódi verstani hibák a skandálóban és a rímdetektorban.** A javítottak közül:

- a kétjegyű betűket az első betűjükre csonkoltam, ezért a `gy`/`ty`/`dz` zárhangnak, az `ly`
  likvidának látszott → hamis *muta cum liquida*: `hegyre`, `szablyáját` közös lett hosszú helyett;
- a *muta cum liquida* szóhatáron is elsült (`vak róka`), ahol nem szabad;
- az `x` egy hangnak számított, a `dz`/`dzs` rövidnek;
- az illesztő a minta összes realizációját kifejtette — ez a szabad pozíciók számában exponenciális
  —, és 8192 fölött **csonkolt**, majd a csonkolt előtagokat hasonlította: egy negyven szótagos sor
  „hexameter" lett. Helyette pozíciónkénti dinamikus programozást írtam;
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

A valódi verskorpusz utólag még két hibát fogott, amit szintetikus teszttel nem találtam volna meg:
a magánhangzó nélküli „s" kötőszó kiesett a megjelenítésből (a felület „Fegyvert, vitézt…"-et írt
volna), a kánon-kereső pedig lekisbetűsítve kereste a mintát is, így `-UU-?`-re sosem talált.

### Eltérések az eredeti kánontól

Öt mintán módosítottam az eredeti adathoz képest. Csak ott nyúltam hozzá, ahol a minta
bizonyíthatóan **más formát ír le, mint a neve** — húszéves adatnál ez normális, és a döntés
vitatható. Ezért minden eltérés megőrzi az eredeti mintát és a hivatkozott forrást; a felület
`Kánon` nézetében kinyithatók, tehát bárki felülbírálhatja:

| Mérték | Eredeti | Javítva | Miért |
|---|---|---|---|
| `choliambus` | `?-U-?-U-U-U?` | `?-U-?-U-U--?` | hiányzott az utolsó előtti hosszú, ami *definiálja* a sánta jambust — a minta közönséges jambikus trimeter volt |
| `alkaioszi 3` | `?-U-U-U-?` | `?-U-?-U-?` | az 5. pozíció közös; fix rövidként a teljes alkaioszi strófa illeszthetetlen volt |
| `aszklepiadeszi D13` | `---UU-?` (7) | `---UU-U?` (8) | a 4. aszklepiadeszi strófa rövid sora glükóni, nem pherekrateus |
| `4mtr trochaicus` | `-U-U-U-U-U-U-U-` | `-U-?-U-?-U-?-U-` | a trochaikus metrum második eleme közös |
| `anapesztikus dimeter`, `daktilikus tetrameter` | tiszta lábak | `=-\|=-\|…`, `-=\|-=\|…` | a nevük spondeusz-helyettesítést ígért, a mintájuk tiltotta |
| `glykoni2a/2b` | `-?-UU-U`, `U--UU-U` | törölve | hét pozíciós, rövidre végződő „glükóni" nem létezik, viszont minden valódi glükóni első hét szótagjára ráillett |

Újként felvettem: `versus spondiacus` (spondeuszi ötödik lábú hexameter).

**Amihez nem nyúltam.** Az ellenőrzés több „javítási" javaslatomat megcáfolta: a `szapphói sor` 4.
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
- A `kalliope.exe` futásidejű, bit-pontos összevetését nem végeztem el (ahhoz Wine kellene).
  Helyette a szerző [webes változatával](https://csillagtura.ro/projektek/kalliope/) vetettem össze
  a korpuszt — lásd az [Összevetés a webes változattal](#összevetés-a-webes-változattal) szakaszt.

## A projekt története

**1. Eredet.** A Kalliopé eredetileg egy ~2004–2006-os, **Lazarus / FreePascal** alatt írt asztali
program: Váradi Nagy Pál egyetemi munkája. A forráskód sosem került nyilvánosságra, így nekem csak
a lefordított bináris állt rendelkezésre, egy Ghidra reverse-engineering projekt formájában.

**2. Visszafejtés.** A logikát a dekompilátumból kellett kibányásznom: felbontottam a `.gzf`
konténert, azonosítottam a VCL-alapú architektúrát (a vers párhuzamos `TStringList`-ekként
tárolva), és a verstani logikát a program magyar string-konstansairól horgonyoztam ki
(`rimkeplet`, `strofa`, `utemhangsuly`…).

**3. Az adatbázis.** A program külső metrikai adatbázisa külön került elő, és megadta a teljes
formátum-nyelvtant és az adatot. Mivel a klasszikus versmérték-kánon gyakorlatilag zárt halmaz,
nem tettem mögé adatbázist: a kánon a forrásban él, típusos adatként.

**4. Hitelesítés a binárisból.** Végigvizsgáltam a `kalliope.exe` saját címtartományának összes
string-literálját. Ez igazolta a rekonstruált szkenner magját (a **digráf-lista** és a **muta cum
liquida** halmaz pontosan egyezik), és innen való a **normalizáló előfeldolgozó** (`tv→tévé`,
`w→v`, betűnevek), a **központozás-lista**, a **név-alias tábla**, az **ütemhangsúly-jelek**
(`U`/`Ú`/`-`/`÷`) és a **verzió** (`VNP's Kalliope 1.71 beta`).

**5. Ez a kiadás.** Mélyaudit, a talált hibák javítása, modularizálás, REST API, webes felület,
konténerezés, majd az elemzés kiterjesztése az ütemhangsúlyos verselésre. A rímdetektort a
binárisból portolt tábla helyett Arany János rokonsági rendszerére és a mai verstani szakirodalomra
építettem — a portolt tábla ugyanis a szó végén is egyesített, ami épp az ellenkezője Arany
kódaszabályának.

## Szövegek és jogi helyzet

A példatár szövegeit hiteles forrásból vettem (Wikiforrás, Magyar Elektronikus Könyvtár, Sulinet
szöveggyűjtemény). Nagy részük közkincs — a szerző halála után hetven évvel. **Két kivétel**
Devecseri Gábor (1917–1971) Homérosz-fordítása: az [Íliász](https://mek.oszk.hu/00400/00406/) és az
[Odüsszeia](https://mek.oszk.hu/00400/00408/) negyven-negyven sora szemléltetésként, oktatási célú
szabad felhasználás keretében szerepel (Szjt. 33–35. §), a forrás és a fordító megjelölésével. Ha a
projektet más célra használod, ezt a két szöveget cseréld le — a példatár az `Examples.java`-ban
van, minden darabnál ott a forrás és az `expected` mező, ami a `CorpusTest`-be köt.

A kód **MIT licenc** alatt áll, Váradi Nagy Pál és Porkoláb Ádám közös szerzőségével — lásd a
[`LICENSE`](LICENSE) fájlt. A licenc a kódra és a metrikai adatra vonatkozik, a példatár
szövegeire **nem**.
