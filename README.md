# Kalliopé

Magyar időmértékes (klasszikus) verselés és rímképlet elemzője — egyetlen, önálló Java forrásfájlban.

## Mi ez

A Kalliopé verstani elemző: szöveget **skandál** (rövid/hosszú szótagok), klasszikus **versmértékekre illeszt** (hexameter, disztichon, szapphói, alkaioszi, aszklepiadeszi stb.), és **rímképletet** ismer fel. A teljes metrikai adatbázis — verslábak, kolónok, sor- és szakaszmértékek, konstansok, összetett mértékek, hangsúlytalan szavak, beállítások, tárolt strófák — a forrásba van **ágyazva**; nincs külső adatfájl.

```bash
java Kalliope.java        # JDK 21+; külön fordítás nem kell, forrásból indul
```

## A projekt alakulása

**1. Eredet.** A Kalliopé eredetileg egy ~2006-os Borland Delphi asztali program volt magyar időmértékes verselés és rímképlet elemzésére. A forráskód elveszett; csak a lefordított bináris maradt meg, egy Ghidra reverse-engineering projekt (`.gzf`) formájában.

**2. Visszafejtés.** A logikát a Ghidra dekompilátumából kellett kibányászni: a `.gzf` konténer felbontása (Java-szerializált burok + beágyazott ZIP + a ProgramDB-image), a VCL-alapú architektúra azonosítása (a vers párhuzamos `TStringList`-ekként tárolva: nyers sor / csupasz alak / rímbetű / szótagolás / hangsúlyjelek), és a tényleges verstani logika kihorgonyzása a program magyar string-konstansairól (`rimkeplet`, `strofa`, `utemhangsuly`…).

**3. Az adatbázis.** A program külső metrikai adatbázisa külön került elő, és megadta a teljes formátum-nyelvtant (`;` komment, `.` mezőnév, `!` beállítás, `@`/`#define` konstans, `#complex` összetett mérték, `$` hangsúlytalan szó, `#start_strofa` blokk) és a teljes adatot. Mivel a klasszikus versmérték-kánon gyakorlatilag zárt halmaz, ez a Java változatba be van ágyazva.

**4. Újraépítés Java-ban.** Egyetlen fájlba: a metrikai jelölés (U/-/?/=) és feloldása; az adatbázis-parser; az osn- (optimalizált sornév) feloldás; a formula-resolver (konstansok, `#define`, `#complex`, szakaszmérték-képletek visszavezetése konkrét mintákra); a soríllesztő; és a rímképlet-detektor.

**5. A skandáló megalapozása verstani forrásokból.** A szöveg→U/- skandáló szabályait hiteles magyar verstani munkákból építettük fel (Fazekas Kulturális Enciklopédia – Verstan; Magyartanár / EKF; Pannon Enciklopédia – Kecskés András: A klasszikus időmértékes verselés; Sulinet Tudásbázis; Wikipédia). A beépített szabályok:

- a szótag a következő magánhangzóig tart, **átlépve a szóhatárt**;
- **természeténél fogva hosszú**: hosszú magánhangzó; **helyzeténél fogva hosszú**: rövid magánhangzó után hosszú vagy legalább két mássalhangzó (a digráf — sz, cs, gy, ly, ny, ty, zs, dz, dzs — **egy** mássalhangzó);
- **közös (anceps) szótag** — hosszúnak és rövidnek is számít, jele `?`: mindkét határozott névelő (a, az), a rövid magánhangzós nyílt szótagú kötőszók/névmások (köztük az „s" ← „és"), és a **sorvégi** szótag (brevis in longo);
- **muta cum liquida**: zárhang (p, t, k, b, d, g) + likvida (r, l) kapcsolat nem feltétlenül tesz helyzeti hosszút — közös (pl. „apraja", „atlasz").

**6. Elvi döntés: szigorú hűség.** A skandáló a szabályokat **szigorúan** alkalmazza. A **helyzeti hosszúság valódi hosszúság** — nem „gyengíthető" rövidre azért, hogy egy sor kényszerből mértékre illeszkedjen (pl. „istennő" = `— — —`, három hosszú). Az illesztés is szigorú: hosszú↔hosszú, rövid↔rövid, közös↔bármelyik; **nincs feltételezett költői licencia**. Ha egy sor így nem illeszkedik egy mértékre, az a helyes, hű válasz — nem hiba.

**7. Kiegészítések.** A beágyazott kánon kiegészült néhány standard, szisztematikus klasszikus mértékkel (jambikus mono-/di-/tetrameter, anapesztikus dimeter, ión a minore/maiore dimeter, daktilikus tetrameter), konzervatív anceps-kódolással, külön `.Kiegészítő antik sorfajták:` szekcióban. A ritka, hagyományfüggő formák (archilochoszi kombinációk, priapeus, elegiambus) szándékosan kimaradtak, mert mintájuk nem egyértelmű.

**8. Hitelesítés az eredeti binárisból.** Utóbb előkerült maga a `kalliope.exe`. Ebből statikusan kihúzhatóvá vált az az adat, ami a Ghidra-exportból hiányzott, és ez **igazolta** a rekonstruált szkenner magját: a program tényleges **digráf-listája** (`ty, gy, ny, ly, dzs, dz, sz, zs, cs`, `cc`) és **muta cum liquida** halmaza (zárhangok `p, b, t, d, k, g` + likvidák `l, r`) pontosan egyezik ezzel a változattal, és az „s" tényleg kötőszóként kezelt (nincs a betűnév-táblában). Egy dolog került be újként, a bináris valódi adatából: a **normalizáló előfeldolgozó** (rövidítés-kiejtés `tv→tévé`, `cd→cédé`, `vc→vécé`, `w→v`; magában álló mássalhangzók betűnévvé bontása `b→bé`, `f→eff`, `x→iksz`…). A bináris **futtatásához** (bit-pontos diff a kimenethez) Wine kellene; a jelen hitelesítés az adat és a szabályok egyezésén alapul, nem a futásidejű kimenet összevetésén.

## Felépítés (egy fájlon belül)

| Rész | Felelős |
|---|---|
| Jelölés | `expand` — a `?`/`=` pozíciók kibontása konkrét U/- realizációkra |
| Adatmodell | `NamedMeter`, `ComplexMeter`, `Constant`, `StanzaMeter`, `StoredStanza`, `PoemModel` |
| Parser | a beágyazott `DATABASE` feldolgozása szekciónként |
| Feloldás | `osnOf`, `resolveFormula` (konstansok/`#define`/képletek → minták) |
| Skandáló | `scanLine` — a fenti verstani szabályokkal, U/-/? kimenettel |
| Illesztő | `matchLine` — szigorú, közös-kezeléssel |
| Rímdetektor | `rhymeScheme` — sorvégi kulcs, mohó betűkiosztás |
| Beállítások | `Settings` — a valódi (adatbázisból vett) default-okkal |

## Hitelesség (állapot)

- **Metrikai adatbázis:** teljes, szó szerinti, tesztelt.
- **Szkenner magja** (természetes + helyzeti hossz, digráfok, muta cum liquida): a binárisból **igazolt** — az adat egyezik.
- **Normalizáló tábla:** a bináris valódi adata.
- **Rímdetektor és összetett/strófa-szintű illesztés:** még rekonstrukció, nem a bináris pontos algoritmusa.
- **Futásidejű bit-pontos egyezés:** nem mérve (nincs Wine); a hitelesség az adat/szabály egyezésén nyugszik.

## Ismert korlátok

- A skandáló **szigorú**: a teljesítmény-szintű költői licenciát (pl. egy nem-közös nyílt rövid szótag nyújtása az iktuson) nem feltételezi. Ezért egyes, valóban hexameteres sorok, amelyek ilyen licenciára támaszkodnak, szigorúan nem illeszkednek. Ez tudatos, hű viselkedés, nem hiányosság.
- A rímdetektor a sorvégeket veszi (közelítés); rímtelen szövegen előfordulhat vél-klaszterezés.
