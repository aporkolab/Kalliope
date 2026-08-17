// A böngészőbe fordított motor és a JVM-motor összevetése.
//
// Ez a teszt a lefordítás értelmét őrzi: a TeaVM egy másik futtatókörnyezetre
// képezi le a Java szemantikát (String, kollekciók, egészosztás, kettős
// idézőjel escape-elése), és ha bármi elcsúszik, a webes változat CSENDBEN ad
// más skandálást, mint a szerveres. Ezért nem elég, hogy lefordul: a teljes
// 245 soros korpuszon bájtra egyeznie kell.
//
// Hivatkozási alap a `--json` CLI-kimenet, amit ugyanaz a Json írja, mint amit
// a REST API ad — azt viszont a JsonEquivalenceTest a valódi HTTP-válaszhoz
// méri. A lánc így zárt: HTTP-válasz == JVM JSON == böngésző JSON.
import { execFileSync } from 'node:child_process';
import { existsSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const here = dirname(fileURLToPath(import.meta.url));
const root = resolve(here, '../..');
const bundle = resolve(root, 'kalliope-js/target/js/kalliope.js');
const classes = resolve(root, 'kalliope-core/target/classes');

for (const [what, path] of [
  ['a lefordított motor', bundle],
  ['a motor osztályai', classes],
]) {
  if (!existsSync(path)) {
    console.error(`Hiányzik ${what}: ${path}`);
    console.error('Előbb: ./mvnw -Pjs -pl kalliope-js -am package');
    process.exit(1);
  }
}

// A TeaVM kimenete böngészőre készül: `self`-re teszi a globálisokat.
globalThis.self = globalThis;
const { createRequire } = await import('node:module');
createRequire(import.meta.url)(bundle).main([]);
const engine = globalThis.kalliope;
if (!engine) {
  console.error('A motor nem regisztrálta magát a window.kalliope alá.');
  process.exit(1);
}

const jvm = JSON.parse(
  execFileSync('java', ['-cp', classes, 'hu.porkolab.kalliope.KalliopeCli', '--json'], {
    encoding: 'utf8',
    maxBuffer: 64 * 1024 * 1024,
  }),
);
const examples = JSON.parse(engine.examples());

let failed = 0;
const check = (name, mine, reference) => {
  if (mine === reference) {
    console.log(`  ✓ ${name}`);
    return;
  }
  failed++;
  const sameTree = JSON.stringify(JSON.parse(mine)) === JSON.stringify(JSON.parse(reference));
  console.log(
    `  ✗ ${name}: eltér (JVM ${reference.length} B, JS ${mine.length} B` +
      `${sameTree ? ', a fastruktúra egyezik — csak a sorrend/formázás' : ', a TARTALOM is más'})`,
  );
  if (!sameTree) {
    for (let i = 0; i < Math.min(mine.length, reference.length); i++) {
      if (mine[i] !== reference[i]) {
        console.log(`      első eltérés a ${i}. bájton:`);
        console.log(`      JVM: …${reference.slice(Math.max(0, i - 60), i + 60)}…`);
        console.log(`      JS:  …${mine.slice(Math.max(0, i - 60), i + 60)}…`);
        break;
      }
    }
  }
};

console.log(`Összevetés: ${examples.length} vers, ${JSON.parse(engine.canon()).meters.length} mérték`);
for (const e of examples) {
  check(e.title, engine.analyze(e.text, '', ''), JSON.stringify(jvm[e.id]));
}

// A beállítások és a felülbírálások átadása is számít: ha a tömör kódolást
// félreolvasná, az elemzés némán az alapértékekkel futna.
const licence = 'a_szokezdo_hangsuly_nyujthat=1';
const iliasz = examples.find((e) => e.id === 'iliasz');
const strict = JSON.parse(engine.analyze(iliasz.text, '', ''));
const loose = JSON.parse(engine.analyze(iliasz.text, licence, ''));
const matched = (a) => a.stanzas.flatMap((s) => s.lines).filter((l) => l.meters.length).length;
if (matched(loose) > matched(strict)) {
  console.log(`  ✓ beállítás átmegy (Íliász ${matched(strict)} → ${matched(loose)} illeszkedő sor)`);
} else {
  failed++;
  console.log(`  ✗ a beállítás nem érvényesült (${matched(strict)} → ${matched(loose)})`);
}

const plain = JSON.parse(engine.analyze('Még nyílnak a völgyben', '', ''));
const forced = JSON.parse(engine.analyze('Még nyílnak a völgyben', '', '0:2:-'));
if (plain.stanzas[0].lines[0].scansion !== forced.stanzas[0].lines[0].scansion) {
  console.log('  ✓ felülbírálás átmegy');
} else {
  failed++;
  console.log('  ✗ a felülbírálás nem érvényesült');
}

console.log(failed ? `\n${failed} eltérés` : '\nMinden egyezik, bájtra.');
process.exit(failed ? 1 : 0);
