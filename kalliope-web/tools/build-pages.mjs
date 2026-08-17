// A GitHub Pages-változat összeállítása: a lefordított motor bekerül a lapba,
// és a felület hálózat nélkül fut.
//
// Miért kell külön lépés? Mert a motor nem npm-csomag: a kalliope-js modul
// TeaVM-mel fordítja Java bytecode-ból, tehát a Maven build kimenetét kell
// bemásolni, és egy klasszikus <script>-tel betölteni MÉG az Angular bundle
// előtt — az App konstruktora azonnal kéri a kánont, addigra a
// window.kalliope-nak ott kell lennie. Az Angular belépője module típusú,
// vagyis halasztott, a klasszikus script pedig nem: a sorrend így garantált.
import { copyFileSync, existsSync, readFileSync, statSync, writeFileSync } from 'node:fs';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const here = dirname(fileURLToPath(import.meta.url));
const web = resolve(here, '..');
const engine = resolve(web, '../kalliope-js/target/js/kalliope.js');
const dist = resolve(web, 'dist/kalliope-web/browser');
const index = join(dist, 'index.html');
const ENGINE_FILE = 'kalliope-engine.js';

for (const [what, path] of [
  ['a lefordított motor', engine],
  ['az Angular build', index],
]) {
  if (!existsSync(path)) {
    console.error(`Hiányzik ${what}: ${path}`);
    console.error(
      'Sorrend: ./mvnw -Pjs -pl kalliope-js -am package  majd  npx ng build --base-href=/Kalliope/',
    );
    process.exit(1);
  }
}

copyFileSync(engine, join(dist, ENGINE_FILE));

// A TeaVM kimenete UMD-szerű: böngészőben a globálisra teszi a main-t. Azt
// nem hagyjuk ott lógni — egy IIFE meghívja, majd letörli, és utána már csak
// a window.kalliope marad.
const bootstrap = `<script src="${ENGINE_FILE}"></script><script>(function(){main([]);try{delete self.main;}catch(e){self.main=undefined;}})();</script>`;

let html = readFileSync(index, 'utf8');
if (html.includes(ENGINE_FILE)) {
  console.log('A motor már be van fűzve, nem nyúlok hozzá.');
} else if (html.includes('</head>')) {
  html = html.replace('</head>', `${bootstrap}</head>`);
  writeFileSync(index, html);
} else {
  console.error('Nem találom a </head>-et az index.html-ben.');
  process.exit(1);
}

// A Pages nem tud 404-et SPA-útvonalra átirányítani; a 404.html ugyanaz a lap.
copyFileSync(index, join(dist, '404.html'));
// A Jekyll alapból elrejti az aláhúzással kezdődő fájlokat.
writeFileSync(join(dist, '.nojekyll'), '');

const kb = (p) => Math.round(statSync(p).size / 1024);
console.log(`Kész: ${dist}`);
console.log(`  motor:      ${kb(join(dist, ENGINE_FILE))} kB`);
console.log(`  index.html: ${kb(index)} kB (+ 404.html, .nojekyll)`);
