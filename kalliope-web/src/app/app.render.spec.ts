import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { WritableSignal } from '@angular/core';
import { App } from './app';
import { Analysis, Canon } from './kalliope.models';

const CANON: Canon = {
  originVersion: "VNP's Kalliope 1.71 beta",
  canonClosed: '2006. április 23.',
  meters: [
    {
      id: 'hexameter',
      name: 'hexameter',
      pattern: '-=|-=|-=|-=|-UU|-?',
      kind: 'LINE',
      fictive: false,
      note: null,
      correction: null,
    },
    {
      id: 'choliambus',
      name: 'choliambus',
      pattern: '?-U-?-U-U--?',
      kind: 'LINE',
      fictive: false,
      note: 'sánta jambus',
      correction: {
        original: '?-U-?-U-U-U?',
        reason: 'A sánta jambust az utolsó előtti hosszú definiálja.',
        source: 'https://en.wikipedia.org/wiki/Choliamb',
      },
    },
  ],
  stanzas: [],
  settings: [{ key: 'az_s_kotoszo_kozombos', label: 'Az s kötőszó közömbös', defaultValue: true }],
  reasons: [
    { name: 'POSITION_LONG', explanation: 'helyzeténél fogva hosszú' },
    { name: 'SHORT', explanation: 'rövid' },
  ],
  unstressedWords: ['a', 'az'],
};

const ANALYSIS: Analysis = {
  stanzas: [
    {
      index: 0,
      rhymePattern: 'xx',
      rhymePatternName: 'rímtelen',
      accentual: { form: null, strength: 'NINCS', cleanLines: 0 },
      dualRhythm: false,
      forms: [
        {
          form: { id: 'disztichon', name: 'disztichon', rhymeScheme: null, closed: false },
          repetitions: 1,
          rhymeSchemeMatches: true,
        },
      ],
      lines: [
        {
          index: 0,
          text: 'kert alatt',
          scansion: '-U?',
          realized: '-UU',
          syllables: [
            { text: 'kert', quantity: '-', reason: 'POSITION_LONG', wordIndex: 0 },
            { text: 'a', quantity: 'U', reason: 'SHORT', wordIndex: 1 },
            { text: 'latt', quantity: '?', reason: 'LINE_END', wordIndex: 1 },
          ],
          synizesis: false,
          meters: [
            {
              meter: CANON.meters[0],
              // A realizáció EGYEZIK a sor realized mezőjével: a motor is így
              // adja (a realized az első találat realizációja). Amíg itt '-U-'
              // állt, a fixture maga volt inkonzisztens.
              realization: '-UU',
              ictusSyllables: [0],
            },
          ],
          accentual: [],
          nearMiss: null,
          pulse: null,
          rhymeLabel: 'x',
          rhymeKey: 'att',
          rhymeKind: 'VAKSOR',
          caesurae: [],
          unstressedWords: [],
          ictusRow: null,
        },
      ],
    },
  ],
  settings: { az_s_kotoszo_kozombos: true },
  summary: {
    stanzaCount: 1,
    lineCount: 1,
    syllableCount: 3,
    meters: ['hexameter'],
    stanzaForms: ['disztichon'],
    accentualForms: [],
    simultaneousLines: 0,
  },
  verse: {
    system: 'IDOMERTEKES',
    headline: 'Időmértékes verselés: hexameterek.',
    details: ['Egyetlen, 1 soros szakasz.'],
  },
};

describe('App megjelenítés', () => {
  let fixture: ComponentFixture<App>;
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    fixture = TestBed.createComponent(App);
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
    http.expectOne('/api/canon').flush(CANON);
    http.expectOne('/api/examples').flush([]);
    fixture.detectChanges();
  });

  interface Internals {
    poem: WritableSignal<string>;
    tab: WritableSignal<string>;
    canonQuery: WritableSignal<string>;
    analysis: WritableSignal<Analysis | null>;
    analyze: () => void;
  }

  function internals(): Internals {
    return fixture.componentInstance as unknown as Internals;
  }

  function analyze(): void {
    const component = internals();
    component.poem.set('kert alatt');
    component.analyze();
    http.expectOne('/api/analyze').flush(ANALYSIS);
    fixture.detectChanges();
  }

  it('szótagonként jeleníti meg a sort, hosszúságjellel', () => {
    analyze();
    const syllables = fixture.nativeElement.querySelectorAll('.syllable');
    expect(syllables.length).toBe(3);
    expect(syllables[0].textContent).toContain('kert');
    expect(syllables[0].classList.contains('long')).toBe(true);
    expect(syllables[1].classList.contains('short')).toBe(true);
    expect(syllables[0].textContent).toContain('—');
  });

  it('a közös szótagot a mérték dönti el — a megvalósult hosszúság látszik', () => {
    // A harmadik szótag nyersen közös ('?'), de az illeszkedő mérték
    // megvalósulása ('-UU') rövidnek követeli. A felület a döntést mutatja,
    // a közös eredetet pedig pontozott aláhúzás jelzi.
    analyze();
    const third = fixture.nativeElement.querySelectorAll('.syllable')[2];
    expect(third.classList.contains('short')).toBe(true);
    expect(third.classList.contains('anceps')).toBe(false);
    expect(third.classList.contains('resolved')).toBe(true);
    expect(third.textContent).toContain('∪');
  });

  it('kiírja a mértéket, a rímképletet és a szakaszmértéket', () => {
    analyze();
    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('hexameter');
    expect(text).toContain('rímképlet:');
    expect(text).toContain('disztichon');
  });

  it('a szótagra mutatva megmondja, miért olyan hosszú', () => {
    analyze();
    const syllable = fixture.nativeElement.querySelector('.syllable') as HTMLElement;
    syllable.dispatchEvent(new MouseEvent('mouseenter'));
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.inspector').textContent).toContain(
      'helyzeténél fogva hosszú',
    );
  });

  it('a kánon nézet mutatja a javításokat az eredeti mintával és forrással', () => {
    internals().tab.set('adatbazis');
    fixture.detectChanges();
    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('choliambus');
    expect(text).toContain('javítva');
    expect(fixture.nativeElement.querySelector('details p code').textContent).toContain(
      '?-U-?-U-U-U?',
    );
  });

  it('a kánon kereshető', () => {
    internals().tab.set('adatbazis');
    internals().canonQuery.set('cholia');
    fixture.detectChanges();
    const rows = fixture.nativeElement.querySelectorAll('tbody tr');
    expect(rows.length).toBe(1);
    expect(rows[0].textContent).toContain('choliambus');
  });

  it('a szerzőség mindkét nevet kiírja, a licenccel együtt', () => {
    const credits = fixture.nativeElement.querySelector('.credits');
    expect(credits).toBeTruthy();
    const text = credits.textContent as string;
    expect(text).toContain('Váradi Nagy Pál');
    expect(text).toContain('Porkoláb Ádám');
    expect(text).toContain('MIT');
    const links = [...credits.querySelectorAll('a')].map((a: HTMLAnchorElement) => a.href);
    expect(links.some((h) => h.includes('github.com/aporkolab/Kalliope'))).toBe(true);
    expect(links.some((h) => h.includes('csillagtura.ro'))).toBe(true);
  });

  it('a ritmustérkép az ítélet MELLETT van, nem alatta', () => {
    analyze();
    // Négy sor fölött jelenik meg; a példaelemzés egysoros, ezért felnagyítjuk.
    internals().analysis.set({
      ...ANALYSIS,
      summary: { ...ANALYSIS.summary, lineCount: 12 },
    });
    fixture.detectChanges();
    const row = fixture.nativeElement.querySelector('.verdict-row');
    expect(row.classList.contains('with-rhythm')).toBe(true);
    // Mindkettő a sor KÖZVETLEN gyereke — így kerülnek egy vonalba.
    expect(row.querySelector(':scope > .verdict')).toBeTruthy();
    expect(row.querySelector(':scope > .rhythm')).toBeTruthy();
  });

  it('rövid versnél nincs ritmustérkép, és a sor sem kap két hasábot', () => {
    analyze(); // a példaelemzés egysoros
    const row = fixture.nativeElement.querySelector('.verdict-row');
    expect(row.classList.contains('with-rhythm')).toBe(false);
    expect(row.querySelector('.rhythm')).toBeNull();
  });

  it('regresszió: a kiírt skandálás a MEGNEVEZETT mértékhez tartozik', () => {
    // Váradi Nagy Pál jelentette: az „Elmegy a kugli…” sorra két mérték
    // illeszkedik, KÜLÖNBÖZŐ feloldással, és a felület mindkét nevet kiírta a
    // skandálás mellé — amelyik viszont csak az elsőé volt. A kiírt hosszúságok
    // így ellentmondtak a mellettük álló névnek.
    analyze();
    const line = ANALYSIS.stanzas[0].lines[0];
    internals().analysis.set({
      ...ANALYSIS,
      stanzas: [
        {
          ...ANALYSIS.stanzas[0],
          lines: [
            {
              ...line,
              realized: '-UU',
              meters: [
                {
                  meter: { ...CANON.meters[0], name: 'alfa' },
                  realization: '-UU',
                  ictusSyllables: [0],
                },
                {
                  meter: { ...CANON.meters[0], name: 'béta' },
                  realization: '---',
                  ictusSyllables: [0, 1],
                },
              ],
            },
          ],
        },
      ],
    });
    fixture.detectChanges();

    const shown = () =>
      [...fixture.nativeElement.querySelectorAll('.syllable .mark')]
        .map((e: Element) => e.textContent?.trim())
        .join('');
    const meterName = () => fixture.nativeElement.querySelector('.meter').textContent.trim();

    // Alapból az első olvasat: a név és a skandálás összetartozik.
    expect(meterName()).toBe('alfa');
    expect(shown()).toBe('—∪∪');

    // A másik olvasatra váltva MINDKETTŐ változik, együtt.
    const alt = fixture.nativeElement.querySelector('.alt');
    expect(alt.textContent.trim()).toBe('béta');
    alt.click();
    fixture.detectChanges();
    expect(meterName()).toBe('béta');
    expect(shown()).toBe('———');
  });

  it('egyetlen olvasatnál nincs váltógomb', () => {
    analyze();
    expect(fixture.nativeElement.querySelector('.alt')).toBeNull();
    expect(fixture.nativeElement.querySelector('.alt-readings')).toBeNull();
  });

  it('mérték nélkül a lüktetés adja a kiírt hosszúságokat, és megmondja hol szakad meg', () => {
    // Váradi Nagy Pál egy soros tesztesete: huszonegy szótag, hat daktilussal
    // indul. Nincs ilyen hosszú kánoni sorfajta, de a lüktetés tény — és a
    // feloldásnak megnevezett forrása van, nem néma alapértelmezés.
    analyze();
    const line = ANALYSIS.stanzas[0].lines[0];
    internals().analysis.set({
      ...ANALYSIS,
      stanzas: [
        {
          ...ANALYSIS.stanzas[0],
          lines: [
            {
              ...line,
              realized: null,
              meters: [],
              scansion: '-?U',
              pulse: {
                foot: '-UU',
                footName: 'daktilus',
                footAdjective: 'daktilikus',
                feet: 1,
                syllables: 3,
                breaksAt: -1,
                resolved: '-UU',
                summary: '6 daktilus a sor élén — a 19. szótagnál megszakad',
                whole: true,
              },
            },
          ],
        },
      ],
    });
    fixture.detectChanges();

    const marks = [...fixture.nativeElement.querySelectorAll('.syllable .mark')]
      .map((e: Element) => e.textContent?.trim())
      .join('');
    expect(marks).toBe('—∪∪');
    expect(fixture.nativeElement.querySelector('.pulse').textContent).toContain('6 daktilus');
    expect(fixture.nativeElement.querySelector('.pulse').textContent).toContain('megszakad');
    // sorfajtát NEM állít — a SORNÁL nincs mértéknév (a szakaszmérték a
    // lábléchez tartozik, azt nem szabad ide keverni)
    expect(fixture.nativeElement.querySelector('.verdict-line .meter')).toBeNull();
  });

  afterEach(() => http.verify());
});
