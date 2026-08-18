// Deploy utáni füstpróba: a KIÉLESÍTETT lapot töltjük le, futtatjuk valódi
// DOM-ban, és tényleg begépelünk egy verset.
//
// Miért kell, ha van unit teszt és js-diff? Mert egyik sem látja az
// összeszerelést. A js-diff a motort méri a JVM-hez, a unit tesztek a
// komponenst stubolt motorral — de ha a build-pages.mjs nem fűzi be a motort a
// lapba, vagy a bundle el sem indul, mindkettő zöld marad, és a lap mégis üres.
// Ez a próba pont azt a rést fedi le: a böngésző szemszögéből nézi.
//
// Használat: node tools/smoke-pages.mjs [url]
import { JSDOM, VirtualConsole } from 'jsdom';

const base = (process.argv[2] ?? 'https://aporkolab.github.io/Kalliope/').replace(/\/?$/, '/');
const PROBE = 'Még nyílnak a völgyben a kerti virágok';
const EXPECTED = '——∪∪—∪∪—∪∪——';
const ATTEMPTS = 6;

const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

async function fetchText(url) {
  const res = await fetch(url, { cache: 'no-store' });
  if (!res.ok) {
    throw new Error(`${url} → HTTP ${res.status}`);
  }
  return res.text();
}

async function smoke() {
  const html = await fetchText(base);
  const errors = [];
  const vc = new VirtualConsole();
  vc.on('jsdomError', (e) => errors.push(e.message));

  const dom = new JSDOM(html, {
    url: base,
    runScripts: 'outside-only',
    pretendToBeVisual: true,
    virtualConsole: vc,
  });
  const { window } = dom;
  window.matchMedia ??= () => ({ matches: false, addEventListener() {}, removeEventListener() {} });

  // Ahogy a böngésző: minden script, sorrendben. A külsőt letöltjük, a
  // beágyazottat úgy futtatjuk, ahogy a lapon áll — így a motor bootstrapja is
  // pontosan úgy fut le, mint élesben.
  for (const script of window.document.querySelectorAll('script')) {
    const code = script.src ? await fetchText(new URL(script.src, base).href) : script.textContent;
    // A hibát összegyűjtjük, de nem itt dobjuk el: az alábbi ellenőrzések
    // beszédesebb okot adnak (a hiányzó motortól a bootstrap „main is not
    // defined”-dal száll el, ami önmagában semmit nem mond).
    try {
      window.eval(code);
    } catch (e) {
      errors.push((script.src || 'beágyazott script') + ': ' + e.message);
    }
  }

  if (typeof window.kalliope !== 'object') {
    throw new Error(
      'a motor nem került a lapra (window.kalliope hiányzik)' +
        (errors.length ? ' — ' + errors[0] : ''),
    );
  }

  await sleep(500);
  const textarea = window.document.querySelector('textarea');
  if (!textarea) {
    throw new Error('az Angular alkalmazás nem indult el (nincs textarea)');
  }

  const verdict = () => window.document.querySelector('.verdict h2')?.textContent.trim() ?? '';
  const marks = () =>
    [...window.document.querySelectorAll('.syllable .mark')]
      .map((e) => e.textContent.trim())
      .join('');

  if (verdict()) {
    throw new Error('gépelés előtt már van elemzés: ' + verdict());
  }

  // Karakterenként, ahogy egy ember gépel — és GOMBNYOMÁS NÉLKÜL.
  for (const ch of PROBE) {
    textarea.value += ch;
    textarea.dispatchEvent(new window.Event('input', { bubbles: true }));
    await sleep(10);
  }
  if (verdict()) {
    throw new Error('a várakozás nem tartja vissza: azonnal elemzett');
  }

  await sleep(1500);
  if (!verdict()) {
    throw new Error('gépelés után nem indult el az elemzés');
  }
  if (marks() !== EXPECTED) {
    throw new Error(`rossz skandálás:\n  várt:  ${EXPECTED}\n  kapott: ${marks()}`);
  }
  const credits = window.document.querySelector('.credits')?.textContent ?? '';
  for (const name of ['Váradi Nagy Pál', 'Porkoláb Ádám']) {
    if (!credits.includes(name)) {
      throw new Error('hiányzik a szerzőség a lapról: ' + name);
    }
  }
  if (errors.length) {
    throw new Error('hiba a lapon: ' + errors[0]);
  }
  return { verdict: verdict(), marks: marks() };
}

// A frissen kiélesített lap nem azonnal áll rendelkezésre, ezért újrapróbáljuk.
for (let attempt = 1; attempt <= ATTEMPTS; attempt++) {
  try {
    const { verdict, marks } = await smoke();
    console.log(`Füstpróba rendben (${base})`);
    console.log(`  motor a lapon, alkalmazás elindult`);
    console.log(`  gépelésre magától elemzett, gombnyomás nélkül`);
    console.log(`  skandálás: ${marks}`);
    console.log(`  ítélet:    ${verdict}`);
    process.exit(0);
  } catch (e) {
    console.log(`  ${attempt}/${ATTEMPTS}. próba: ${e.message}`);
    if (attempt === ATTEMPTS) {
      console.error(`\nA füstpróba elbukott: ${base}`);
      process.exit(1);
    }
    await sleep(15000);
  }
}
