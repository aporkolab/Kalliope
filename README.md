# Kalliopé v.2.0

Magyar verstani elemző: **skandál**, **versmértéket ismer fel**, és **rímképletet** ad — mindkét magyar ritmusrendben, időmértékesben és ütemhangsúlyosban egyaránt.

Minden szótagról megmondja, **miért** olyan hosszú; ha egy sor nem illeszkedik, megmutatja, **min múlik**; a végén pedig egy mondatban összegzi, milyen vers ez: *„Időmértékes verselés: disztichonok."*

> A Kalliopét **Váradi Nagy Pál** (vnp85) írta 2004–2006 táján. Ez a modernizált változat az ő munkájára épül; **közösen tesszük közzé**, társszerzőként, MIT licenc alatt. → [Eredet és
> szerzőség](#eredet-és-fejlesztés)

```bash
git clone https://github.com/aporkolab/Kalliope.git && cd Kalliope
docker compose up --build          # → http://localhost:8080

```

> **Megjegyzés:** A CI a `main` ágról feltölt egy Docker image-et a GitHub Container Registry-be is (`ghcr.io/aporkolab/kalliope:latest`). Mivel a GitHub ezeket a csomagokat alapértelmezetten privátként kezeli, idegen felhasználóknak a fenti lokális build (`docker compose up --build`) a legbiztosabb módszer a futtatásra.

## Eredet és fejlesztés

A Kalliopé eredetileg Váradi Nagy Pál egyetemi munkájaként született, Lazarus/FreePascal környezetben. A szerző saját webes változata itt érhető el: [https://csillagtura.ro/projektek/kalliope/](https://csillagtura.ro/projektek/kalliope/).

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
| **Kettős ritmus** | Ha a szakasz mindkét rendnek megfelel, jelzi. |
| **Rím** | Képlet vaksorral (`x`), a képlet neve, és soronként a rím fajtája (tiszta rím, ragrím, asszonánc, önrím). |
| **Cezúra** | A mérték jelölt metszete, valamint a hexameter klasszikus metszeteinek felismerése. |
| **Szótagszintű indoklás** | 12-féle ok (pl. természeténél fogva hosszú, *muta cum liquida*, összevont kettőshangzó). |
| **Interaktivitás** | A felületen a szótagra kattintva a hosszúság felülbírálható, az elemzés azonnal újrafut. |
| **Megosztás & Export** | A „Link” gomb paraméterbe kódolja a verset (adatbázis nélkül osztható). A JSON export letölti az API nyers válaszát. |
| **Nyomtatás** | Tiszta, zavaró UI-elemek nélküli nyomtatási/PDF nézet, ahol a hosszúságot a jelek hordozzák a színek helyett. |

### A jelölés és a felület

| Jel | Mit jelent |
| --- | --- |
| `—` | hosszú szótag |
| `∪` | rövid szótag |
| `×` | közös (anceps) — a mérték dönti el |
| `\|` | lábhatár |
| `‖` | sormetszet, cezúra |
| pontozott aláhúzás | a szótag eredetileg közös volt, a mérték döntötte el |

A felület színrendszere vizuálisan is elkülöníti a szótagokat (hosszú, rövid, közös), támogatja a sötét/világos témát, és mobilon is kényelmesen használható (kártyás tördelés).

## Felépítés

Három modul, egyetlen futtatható artefaktum:

| Modul | Funkció | Függőségek |
| --- | --- | --- |
| `kalliope-core` | A verstani motor, a metrikai kánon és a példatár. | **Csak a JDK** |
| `kalliope-api` | REST-réteg + a felület kiszolgálása. | Spring Boot 4.1 |
| `kalliope-web` | A webes felület. | Angular 22 |

Az Angular build a Spring Boot `static/` mappájába kerül, így **nincs CORS probléma, és nem kell külön statikus hosting**.

## Futtatás és Fejlesztés

**Konténerből:**

```bash
docker compose up --build

```

**Fejlesztéshez (két terminál):**

```bash
./mvnw -pl kalliope-api -am spring-boot:run           # API (8080-as port)
cd kalliope-web && npm ci && npm start                # Web (4200-as port, /api proxyzva)

```

**Parancssorból (CLI), felület nélkül:**

```bash
./mvnw -pl kalliope-core -am package
java -jar kalliope-core/target/kalliope-core-*.jar vers.txt    # Fájl elemzése

```

### Konfiguráció

A főbb beállítások környezeti változóként is megadhatók:

| Kulcs | Alapérték | Funkció |
| --- | --- | --- |
| `server.port` | `8080` | HTTP port |
| `kalliope.rate-limit.requests-per-minute` | `60` | `/api/analyze` kérésszám-korlát (`0` kikapcsolja) |

### CI és Ellenőrzés

A projekt szigorú minőségi kapukkal rendelkezik (80%-os tesztlefedettségi küszöb mindhárom modulban, Spotless és Prettier kódformázás). Helyi ellenőrzéshez:

```bash
./mvnw verify
cd kalliope-web && npm ci && npx ng test --no-watch && npx prettier --check "src/**/*.{ts,html,css}"

```

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
* **Közös (anceps, `?`) szótagok:** Sorvégi szótagok, határozott névelők, rövid kötőszók, *muta cum liquida* szón belül, görög aspiráták (`kh`, `th`), és az eldönthetetlen torlódások.
* **Kettőshangzók:** A görög-latin diftongusokra (`eu`, `au`) a motor változatokat állít elő, és a minta alapján dönti el az optimális olvasatot.

A skandáló alapértelmezetten **szigorú**: költői licenciát nem feltételez, de pontosan megmutatja, hol tér el a szöveg a mértéktől.

### Beállítások

A motor számos elemzési beállítást támogat (pl. `a_rovid_kotoszok_kozombosek`, `a_szokezdo_hangsuly_nyujthat`).
*Megjegyzés:* Az eredeti Lazarus-kliens három, tisztán ablakkezelési kapcsolója (`a_jobb_oldali_szoveg_formazott_legyen`, `a_fuggoleges_toszogalos_mutyur_helye`, `a_beallitasokat_tartalmazo_felulet_elrejtve`) **nincs** a motorban: itt nem volna értelmük, és nem is teszek úgy, mintha lenne. A tíz megmaradt beállításból hat az eredeti adatbázisé, négy az én kiegészítésem.

## Korpusz-riport és Tesztelés

A rendszer stabilitását és pontosságát egy 245 sorból álló, klasszikus verseket tartalmazó korpusz (Zrínyi, Arany, Homérosz, Radnóti, Berzsenyi, Petőfi) garantálja. Az automatizált `CorpusTest` elbukik, ha az algoritmus felismerési aránya (alapbeállításokkal 86% a teljes korpuszon, versenként külön küszöbbel) romlana egy kódmódosítás során.

## Ismert korlátok

* **Ragrímek:** Mivel a rímdetektor a sorvégeket fonetikailag vizsgálja, a magyar ragrímek (pl. két `-nak` végződés) rímként jelennek meg.
* **Íráskép vs. Kiejtés:** A szóösszetételi határon álló találkozások (pl. `község` -> `z+s`) íráskép alapján egy hangnak tűnhetnek, a kiejtési kivételszótár csak a leggyakoribb eseteket (pl. `igazság`) fedi le.
* **Ütemhangsúly:** Az illesztés szótagszámon és szóhatáron alapul, nem végez mély, kontextuális hangsúlyelemzést.

## Szövegek és Jogi helyzet

A kód **MIT licenc** alatt áll, Váradi Nagy Pál és Porkoláb Ádám közös szerzőségével (lásd a `LICENSE` fájlt).

A példatárban (`Examples.java`) szereplő versek nagyrészt közkincsek. A két kivétel Devecseri Gábor Homérosz-fordítása, amelyek oktatási célú szabad felhasználás keretében (Szjt. 33–35. §) szerepelnek a tesztkorpuszban, a forrás pontos megjelölésével. Más célú felhasználás esetén ezeket a szövegeket cserélni szükséges.
