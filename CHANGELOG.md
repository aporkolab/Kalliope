# Változásjegyzék

## 2.0.0

A kiindulás egy `Kalliope.java` volt: egyetlen fájl, a metrikai adatbázissal
beágyazott szövegként, saját szintaxissal. Ez a kiadás abból lett motor + API +
felület, közben pedig kijavítottam, amit a mélyaudit és a valódi verskorpusz
kimutatott.

Ami **nem** változott: a metrikai kánon adata, a kiejtési normalizáló tábla, a
név-alias tábla és a digráf-lista Váradi Nagy Pál eredeti munkája. Ahol az
adathoz mégis hozzányúltam, ott a régi minta és a forrás is ott van (lásd
[Eltérések az eredeti kánontól](#eltérések-az-eredeti-kánontól)).

### Szerzőség és licenc

- Megnevezve a szerző: **Váradi Nagy Pál** (vnp85), és linkelve az élő webes
  változata. A kánon `VNP sorfajták` / `VNP-strófák` szekciója az ő saját
  versformái — eddig névtelen rövidítés volt a kódban.
- **MIT licenc**, társszerzőként, a szerző engedélyével. Korábban nem volt
  licencfájl, tehát a GitHub alapértelmezése szerint minden jog fenntartva volt.
- A fejlesztőkörnyezet megállapítása **méréssel**: a bináris `.rsrc` szekciójában
  ott a `DVCLAL` (Delphi VCL Application License) és a `PACKAGEINFO` resource, a
  `Delphi%.8X` ablakosztály-string és a `Borland` cégnév; a szekciónevek is a
  Delphi sémája (`CODE`/`DATA`/`BSS`). FreePascal-nyom nulla. **Borland Delphi**
  tehát, nem Lazarus — a szerző maga jelezte, hogy húsz év után nem emlékszik
  biztosan, a bináris viszont eldönti. (A forrás alighanem fordul Lazarus alatt
  is, de az exe nem úgy készült.)

### Architektúra

- Három modulra bontva: **`kalliope-core`** (a motor — nulla futásidejű
  függőség), **`kalliope-api`** (Spring Boot REST + a felület kiszolgálása),
  **`kalliope-web`** (Angular). Egy image, egy port, nincs CORS.
- **`kalliope-js`**: a motor TeaVM-mel JavaScriptre fordítva, hogy backend nélkül
  is fusson (GitHub Pages). Ugyanaz a felület-bundle mindkét üzemmódban; a
  szolgáltatás futásidőben dönt.
- A saját szövegformátum és a hozzá írt parser **megszűnt**: a kánon típusos Java
  adat, a hivatkozás objektumhivatkozás. Ezzel egy csapásra eltűnt egy egész
  hibaosztály — elgépelt hivatkozás, némán elnyelt feloldási hiba,
  kis/nagybetű-érzékeny konstansnév, körkörös hivatkozás okozta
  `OutOfMemoryError`.
- A JSON-t kézi szerializáló írja a magban, nem a Jackson: a böngészőbe fordított
  változatban nincs reflexió. **Egyetlen** implementáció van, három ponton
  lehorgonyozva (lásd [Tesztelés](#tesztelés)).

### Verstani javítások

A mélyaudit 137 megerősített hibát talált (25 további állítást az ellenőrzés
megcáfolt). A lényegesebbek:

- a kétjegyű betűket az első betűjükre csonkoltam, ezért a `gy`/`ty`/`dz`
  zárhangnak, az `ly` likvidának látszott → hamis *muta cum liquida*: `hegyre`,
  `szablyáját` közös lett hosszú helyett;
- a *muta cum liquida* szóhatáron is elsült (`vak róka`), ahol nem szabad;
- az `x` egy hangnak számított, a `dz`/`dzs` rövidnek (AkH. 87. §);
- **a görög aspiráta (`kh`, `th`, `ph`) kétértelműsége.** Egy hang a görög névben
  (*A-khil-leusz* = χ), de kettő a magyar morfémahatáron (*csak+hogy*,
  *halandó+k+hoz*, **kap+hat**, **át+hat**). Eddig mindig egynek vettem, ezért az
  Odüsszeia `-okhoz` ragos sorai nem illeszkedtek, és a `kaphat` első szótaga is
  tévesen rövid volt. Mostantól közös, és a mérték dönt: **Odüsszeia 90% → 98%,
  a korpusz 84% → 86%**;
- az illesztő a minta összes realizációját kifejtette — ez a szabad pozíciók
  számában exponenciális —, és 8192 fölött **csonkolt**, majd a csonkolt
  előtagokat hasonlította: egy negyven szótagos sor „hexameter" lett. Helyette
  pozíciónkénti dinamikus programozás;
- az elemző eldobta az üres sorokat, ezért többstrófás versen **soha egyetlen
  szakaszmértéket sem** talált, és a rímbetűk végigfutottak az egész versen;
- a disztichon csak pontosan kétsoros versre illett — egy hatsoros elégia nem
  volt három disztichon;
- a rímkulcs az utolsó magánhangzótól indult, ezért `haza`, `soha`, `béka`,
  `anya` mind rímelt; a zöngétlenítés a szó **végén** is futott, és láncba
  fűződött, így `kard`, `part`, `halt` egy kulcsra esett. A rímdetektor most Arany
  János rokonsági rendszerére és a mai szakirodalomra épül, nem a binárisból
  portolt táblára — az ugyanis a szó végén is egyesített, ami épp az ellenkezője
  Arany kódaszabályának;
- a rímbetűk `z` után `{`, `|`, `}` karaktereket írtak; a rímtelen sor nem kapta
  meg a szabályos `x` jelet (vaksor), így a félrím `xaxa` helyett `abcb` lett;
- a `.fictive` segédmértékek valódi találatként jelentek meg;
- `toLowerCase()` locale nélkül (török `I`), négyzetes normalizálás (egy hosszú
  sor 21 másodperc), nem törhető szóközök, `null` bemenet.

**A számított szövegek nem jutottak el a felületig.** A `division`, a `summary`,
a `quality`, az `explanation` és a `dualRhythm` származtatott *metódus* volt, nem
rekordkomponens — a szerializáló ezért nem látta őket. A „kettős ritmus" jelzés
emiatt **még soha nem jelent meg** a felületen. Mind komponens lett, és
tesztek őrzik.

**A valódi verskorpusz utólag még két hibát fogott**, amit szintetikus teszttel
nem találtam volna meg: a magánhangzó nélküli „s" kötőszó kiesett a
megjelenítésből (a felület „Fegyvert, vitézt…"-et írt volna), a kánon-kereső
pedig lekisbetűsítve kereste a mintát is, így `-UU-?`-re sosem talált.

### Új képességek

- **Ütemhangsúlyos ág**: 20 magyaros sorfajta ütemtagolással, a metszet
  minőségével (tiszta vagy laza), és a szakasz domináns formájával.
- **Szakaszmérték-illesztés** 20 formára, opcionálisan kötött rímképlettel.
- **„Miért nem illeszkedik?"** — a legközelebbi mérték és a pontos eltérés.
- **Szótagszintű indoklás**, 12-féle okkal.
- **Kézi felülbírálás**: a szótagra kattintva átállítható a hosszúság.
- **Egymondatos összegzés** a vers verseléséről, részletekkel.
- **Ritmustérkép**, **megosztható link** (`#v=`, szerver nélkül), **JSON
  export**, **nyomtatás/PDF**, **világos/sötét/rendszerkövető téma**, mobil
  nézet.
- **Lábhatárok és sormetszet** kirajzolva.
- A CLI kapott `--json` és `--canon` kapcsolót.

### Eltérések az eredeti kánontól

Öt mintán módosítottam, mindenhol ott az eredeti minta és a forrás; a felület
`Kánon` nézetében kinyithatók.

| Mérték | Miért |
| --- | --- |
| `choliambus` | hiányzott az utolsó előtti hosszú, ami *definiálja* a sánta jambust |
| `alkaioszi 3` | az 5. pozíció közös; fix rövidként a teljes strófa illeszthetetlen volt |
| `aszklepiadeszi D13` | a 4. aszklepiadeszi strófa rövid sora glükóni, nem pherekrateus |
| `4mtr trochaicus` | a trochaikus metrum második eleme közös |
| `anapesztikus dimeter`, `daktilikus tetrameter` | a nevük spondeusz-helyettesítést ígért, a mintájuk tiltotta |
| `glykoni2a/2b` | hét pozíciós, rövidre végződő „glükóni" nem létezik — törölve |

Új: `versus spondiacus`, `Mozdonyszonett a`/`b`, „anakreóni-féle sor", és két
további Horatius-féle aszklepiadeszi strófa (F, G).

**Amihez nem nyúltam:** az ellenőrzés több javítási javaslatomat megcáfolta — a
`szapphói sor` 4. pozíciója, a `léküthion`, a `dochmius`, a `phalaikoszi`
bázisa, a `wilamovitziánus`, a `téleszilleion` és az anakreóni sorok a szerző
saját, védhető kódolásai maradtak.

### Összevetés a szerző webes változatával

A [csillagtura.ro](https://csillagtura.ro/projektek/kalliope/) változata
kliensoldali JavaScript, tehát kinyerhető és futtatható — ez váltotta ki a
`kalliope.exe` futásidejű összevetését, amihez Wine kellett volna. A 245 soros
korpuszt mindkét motorral végigskandáltattam:

- **236 / 245 sor** azonos szótagszámú (96,3%), és ezeken a nyers olvasatom
  **minden egyes szótagon** megfér az övével;
- a 9 eltérő szótagszámú sor mind a webes változat hibája: a `y` betű feltétel
  nélküli törlése elnyeli a magánhangzót (`Zephyr` → *Zephr*), nincs
  kettőshangzó-kezelés, és az `eusz` → `evsz` összevonás csak szóköz előtt fut,
  írásjel előtt nem;
- a görög aspiráta ügyében **mindketten hibáztunk**, egymással ellentétes
  irányban — ez lett a fenti javítás.

### Tesztelés és minőségi kapuk

- **123 Java teszt** (100 motor + 23 API) és **39 frontend teszt**;
  sorlefedettség 95% / 85% / 89%, mindhárom modulban **80%-os küszöb
  kikényszerítve** (JaCoCo, illetve Vitest).
- **Korpusz**: 11 valódi vers, lehetőleg teljes egészében, hiteles forrásból,
  versenként külön küszöbbel. A `CorpusTest` elbukik, ha az arány romlik.
- A JSON-alak három ponton le van kötve: `JsonTest` (kézi kiírás == Jackson
  rekord-leképezése), `JsonEquivalenceTest` (== a valódi HTTP-válasz),
  `js-diff.mjs` (a **böngészőbe fordított** motor == a JVM-motor, a teljes
  korpuszon, **bájtra**). A Pages-deploy elbukik, ha nem egyezik.
- `StringsTest`: a regexet kiváltó kézi szövegvágás egyenértékű a JDK
  `String.split`-jével.
- CI: Java + Angular + kétplatformos Docker image + Pages, minden pusholásra.

### Csomagolás és üzemeltetés

- **Docker image** a GHCR-en, `linux/amd64` **és** `linux/arm64` alatt. Korábban
  csak amd64 készült, mert a build-push-action a runner architektúráján épít —
  Apple Siliconon az image el sem indult (`no matching manifest`). Az 1–3. build
  szakasz `$BUILDPLATFORM`-on fut (a kimenetük architektúrafüggetlen), tehát csak
  a JRE-réteg emulálódik.
- **Az AOT-gyorsítótár sosem készült el.** A `WORKDIR /application` root
  tulajdonú maradt, a tanítófutás viszont a 10001-es userrel futott, tehát nem
  tudta kiírni az `app.aot`-ot — a záró `|| true` pedig elnyelte a hibát. A
  konténer emiatt minden induláskor három AOT-hibasort írt ki, és gyorsítótár
  nélkül indult. Javítva: **indulás 0,87 s → 0,36 s**, hibasorok **3 → 0**, image
  346 MB → 418 MB. A `test -s app.aot` gondoskodik róla, hogy ez ne tudjon még
  egyszer némán elromlani.
- OCI labelek (`image.source`, `licenses`, szerzők) — enélkül a GHCR-csomag nem
  kötődött a repóhoz.
- A **GitHub Pages** változat backend nélkül fut:
  <https://aporkolab.github.io/Kalliope/>.

### Amit a portoláshoz ki kellett venni a motorból

Mindhárom változás viselkedés-semleges, és teszt bizonyítja:

- **`String.split`** (három hívás): a regex a `Character.UnicodeScript`-en át
  olyan JDK-belsőket ér el, amiket a TeaVM nem emulál. Kézi vágás lett belőle —
  mellékhaszon, hogy nincs több mintafordítás minden hívásnál.
- **`Integer::sum`** metódusreferenciák → lambda (a TeaVM nem emulálja).
- **A bytecode szintje**: a motor 17-re fordul, nem 25-re. Nyelvi elemet nem
  veszít, és egy könyvtárnak amúgy sem dolga futásidejű verziót kényszeríteni a
  hívójára.

### Ismert korlátok

- A rímdetektor a sorvégeket veszi, ezért a magyar **ragrím** összecseng.
- A kétjegyű betűk felismerése írásképi: a szóösszetételi határon álló `z+s`,
  `d+z`, `c+s` (`község`, `vadzab`) egy hangnak látszik; a binárisból örökölt
  kiejtési tábla csak az `igazság` típust kezeli.
- Az ütemhangsúlyos illesztés szótagszámon és szóhatáron alapul, nem valódi
  hangsúlyelemzésen.
- A `kalliope.exe` futásidejű, bit-pontos összevetése nem történt meg; helyette a
  szerző webes változatával vetettem össze a korpuszt.
