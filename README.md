# Kalliopé

Magyar időmértékes (klasszikus) verselés és rímképlet elemzője — motor, REST API és webes felület.

A Kalliopé **skandál** (rövid / hosszú / közös szótagok), klasszikus **versmértékekre illeszt**
(hexameter, disztichon, szapphói, alkaioszi, aszklepiadeszi…), felismeri az **ütemhangsúlyos
(magyaros)** sorfajtákat is, és **rímképletet** ad, a rím fajtájának megnevezésével. Minden
szótagról megmondja, **miért** olyan hosszú — és ha egy sor nem illeszkedik, megmondja, **min
múlik**.

Amit tud:

| | |
|---|---|
| **Időmértékes** | 53 sorfajta, 38 kolón, 11 versláb, 18 szakaszmérték; szigorú illesztés, kapcsolható licenciákkal |
| **Ütemhangsúlyos** | 20 magyaros sorfajta ütemtagolással; a metszet minősége (tiszta vagy laza) külön látszik |
| **Kettős ritmus** | ha a szakasz mindkét rendnek megfelel, jelezzük — de nem állítjuk, hogy „szimultán vers" |
| **Rím** | képlet vaksorral (x), a képlet neve (keresztrím, bokorrím…), és soronként a rím fajtája (tiszta rím, ragrím, asszonánc, önrím) |
| **Cezúra** | a mérték jelölt metszete, és a hexameter penthémimerész / hephthémimerész / kata triton trokhaion metszete |
| **Ha nem illeszkedik** | a legközelebbi mérték és a pontos eltérés: „hexameter lenne, ha az 1. szótag hosszú volna" |
| **Felülbírálás** | a szótagra kattintva átállítható a hosszúság, és az elemzés újrafut — a verstan értelmezés kérdése |

```bash
docker run --rm -p 8080:8080 ghcr.io/aporkolab/kalliope:latest
# → http://localhost:8080
```

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
│                    AccentualCanon · AccentualMatcher · Analyzer · KalliopeCli
├─ kalliope-api/     AnalyzeController · CanonController · SpaConfig
│                    ApiExceptionHandler · RateLimitFilter
├─ kalliope-web/     Angular (standalone, signals, zoneless)
├─ Dockerfile        node build → maven build → rétegelt JRE image, AOT-gyorsítótárral
└─ .github/workflows/ci.yml
```

## Futtatás

**Konténerből** (nem kell se JDK, se Node):

```bash
docker compose up --build      # vagy: docker run --rm -p 8080:8080 ghcr.io/aporkolab/kalliope
```

**Fejlesztéshez** két terminál:

```bash
./mvnw -pl kalliope-api -am spring-boot:run       # API a 8080-on
cd kalliope-web && npm start                      # felület a 4200-on, /api proxyzva
```

**Parancssorból**, felület nélkül:

```bash
./mvnw -pl kalliope-core -am package
java -jar kalliope-core/target/kalliope-core-*.jar            # a példatár elemzése
java -jar kalliope-core/target/kalliope-core-*.jar vers.txt   # fájl elemzése
java -jar kalliope-core/target/kalliope-core-*.jar --canon    # a metrikai kánon
```

**Ellenőrzés** (ezt futtatja a CI is):

```bash
./mvnw verify                                   # tesztek + Spotless + lefedettségi küszöb
cd kalliope-web && npm ci && npx ng test --no-watch && npx prettier --check "src/**/*.{ts,html,css}"
```

## API

| Végpont | Mit ad |
|---|---|
| `POST /api/analyze` | `{ text, settings? }` → a teljes elemzés szakaszonként, soronként, szótagonként |
| `GET /api/canon` | a mértékek, szakaszmértékek, beállítás-leírások és a hosszúság-indoklások szótára |
| `GET /api/canon/{id}` | egy mérték |
| `GET /api/examples` | példatár |

Hibák RFC 9457 (`application/problem+json`) szerint. A felület a `/api/canon`-t egyszer kéri le
induláskor: a magyar feliratok egyetlen forrása a motor, nincs kétszer leírva.

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
helyettünk:

- a görög-latin **kettőshangzókra** (`Európa`, `Zeusz`, `Péleidész`) *változatokat* állít elő, és a
  mérték választ — így lesz az Íliász kezdősora hexameter;
- két kapcsolható licencia — a *szóvégi mássalhangzó zárhatja a szótagot* (latinos hagyomány) és a
  *szókezdő hangsúly nyújthat* — külön beállítás, hogy látszódjon, mikor kell hozzá engedmény.

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
kimenetet" — négy közülük sehol nem volt beolvasva. A három tisztán ablakkezelési beállítás
(`a_jobb_oldali_szoveg_formazott_legyen`, `a_fuggoleges_toszogalos_mutyur_helye`,
`a_beallitasokat_tartalmazo_felulet_elrejtve`) az eredeti Delphi-felülethez tartozott; itt nincs
értelmük, és nem is teszünk úgy, mintha lenne.

### Javítások a 2006-os kánonban

Csak ott nyúltunk az adathoz, ahol a minta bizonyíthatóan **más formát ír le, mint a neve**. Minden
javítás megőrzi az eredeti mintát és a forrást — a felület `Metrikai kánon` nézetében kinyithatók:

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

## Korpusz-riport

A példatár tizenegy valódi vers, lehetőleg teljes egészében, hiteles forrásból. Ez egyben a motor
regressziós hálója: a `CorpusTest` elbukik, ha az arány romlik.

| Vers | Sor | Illeszkedik |
|---|---:|---:|
| Zrínyi: Szigeti veszedelem (részlet) | 4 | 0% — helyesen: hangsúlyos vers |
| Arany: Toldi, Első ének | 21 | 0% — helyesen: hangsúlyos vers |
| Homérosz–Devecseri: Íliász I. 1–40. | 40 | 92% |
| Homérosz–Devecseri: Odüsszeia I. 1–40. | 40 | 90% |
| Vörösmarty: Zalán futása, előhang | 34 | 97% |
| Radnóti: Hetedik ecloga (teljes) | 36 | 94% |
| Kazinczy: A nagy titok | 2 | 100% |
| Berzsenyi: A magyarokhoz I. (részlet) | 4 | 100% |
| Berzsenyi: A közelítő tél (teljes) | 24 | 100% |
| Berzsenyi: Horác (teljes) | 16 | 93% |
| Petőfi: Szeptember végén (teljes) | 24 | 95% |
| **Összesen** | **245** | **84%** |

A két nulla százalék nem hiba, hanem a helyes válasz: Zrínyi és Arany verse ütemhangsúlyos, nem
időmértékes — a motor ezeket felező tizenkettesként ismeri fel a másik ágon. A hiányzó néhány
százalék a költői licencia: azoknál a soroknál a „miért nem illeszkedik?" megmondja, min múlik.

## Ismert korlátok

- A rímdetektor a sorvégeket veszi. A magyar **ragrím** ezért összecseng: egy rímtelen hexameteres
  szövegben két `-nak` végű sor rímelőnek látszik. Ez nem hiba, hanem a jelenség — de érdemes tudni.
- A kétjegyű betűk felismerése írásképi: a szóösszetételi határon álló `z+s`, `d+z`, `c+s`
  (`község`, `vadzab`) egy hangnak látszik. A binárisból örökölt kiejtési tábla ezt csak az
  `igazság` típusra kezeli.
- A szótagszintű indoklás az elsődleges olvasatra vonatkozik; ha a sor csak összevont
  kettőshangzóval illeszkedik, azt a felület külön jelzi („összevonással").
- Az ütemhangsúlyos illesztés szótagszámon és szóhatáron alapul, nem valódi hangsúlyelemzésen. Egy
  hosszabb rímtelen hexametersorozat sorvégei között is akadnak véletlen egybecsengések — a
  detektor ezeket ragrímként meg is nevezi, de nem tudja, hogy a költő nem így gondolta.
- A `kalliope.exe` futásidejű, bit-pontos összevetése továbbra sem történt meg (ahhoz Wine kellene);
  a hitelesség az adat és a szabályok egyezésén nyugszik.

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
(`U`/`Ú`/`-`/`÷`) és a **verzió** (`VNP's Kalliope 1.71 beta`). Futásidejű bit-pontos összevetés
nem történt (ahhoz Wine kellene); a hitelesség az adat és a szabályok egyezésén nyugszik.

**5. Ez a kiadás.** Mélyaudit, a talált hibák javítása, modularizálás, REST API, webes felület,
konténerezés. A rímdetektor a binárisból portolt tábla helyett Arany János rokonsági rendszerére és
a mai verstani szakirodalomra épül — a portolt tábla ugyanis a szó végén is egyesített, ami épp az
ellenkezője Arany kódaszabályának.
