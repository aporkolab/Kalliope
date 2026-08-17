import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { WritableSignal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { App } from './app';
import { Analysis, Canon, Line, Override } from './kalliope.models';

const CANON: Canon = {
  originVersion: 'teszt',
  canonClosed: '2006',
  meters: [],
  stanzas: [],
  settings: [],
  reasons: [{ name: 'MANUAL', explanation: 'kézi felülbírálás' }],
  unstressedWords: [],
};

function analysis(over: Partial<Line> = {}): Analysis {
  const line: Line = {
    index: 0,
    text: 'kert alatt',
    scansion: '-U?',
    realized: null,
    syllables: [
      { text: 'kert', quantity: '-', reason: 'POSITION_LONG', wordIndex: 0 },
      { text: 'a', quantity: 'U', reason: 'SHORT', wordIndex: 1 },
      { text: 'latt', quantity: '?', reason: 'LINE_END', wordIndex: 1 },
    ],
    synizesis: false,
    meters: [],
    accentual: [],
    nearMiss: null,
    pulse: null,
    rhymeLabel: 'x',
    rhymeKey: 'att',
    rhymeKind: 'VAKSOR',
    caesurae: [],
    unstressedWords: [],
    ictusRow: null,
    ...over,
  };
  return {
    stanzas: [
      {
        index: 0,
        lines: [line],
        rhymePattern: 'x',
        rhymePatternName: 'rímtelen',
        forms: [],
        accentual: {
          form: {
            id: 'felezo-tizenkettes',
            name: 'felező tizenkettes',
            measures: [6, 6],
            caesuraAfter: 1,
            note: null,
            division: '6 || 6',
          },
          strength: 'LAZA',
          cleanLines: 1,
        },
        dualRhythm: true,
      },
    ],
    settings: {},
    summary: {
      stanzaCount: 1,
      lineCount: 1,
      syllableCount: 3,
      meters: [],
      stanzaForms: [],
      accentualForms: ['felező tizenkettes'],
      simultaneousLines: 1,
    },
    verse: {
      system: 'IDOMERTEKES',
      headline: 'Időmértékes verselés: hexameterek.',
      details: ['Egyetlen, 1 soros szakasz.'],
    },
  };
}

interface Internals {
  poem: WritableSignal<string>;
  overrides: WritableSignal<Override[]>;
  analyze: () => void;
  clearOverrides: () => void;
  rhymeKindLabel: (k: string) => string;
  strengthLabel: (s: string) => string;
}

describe('App — új verstani funkciók', () => {
  let fixture: ComponentFixture<App>;
  let http: HttpTestingController;
  let app: Internals;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    fixture = TestBed.createComponent(App);
    http = TestBed.inject(HttpTestingController);
    app = fixture.componentInstance as unknown as Internals;
    fixture.detectChanges();
    http.expectOne('/api/canon').flush(CANON);
    http.expectOne('/api/examples').flush([]);
    fixture.detectChanges();
  });

  function run(result: Analysis): void {
    app.poem.set('kert alatt');
    app.analyze();
    http.expectOne('/api/analyze').flush(result);
    fixture.detectChanges();
  }

  it('kiírja az ütemhangsúlyos formát és a kettős ritmust', () => {
    run(analysis());
    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('felező tizenkettes');
    expect(text).toContain('6 || 6');
    expect(text).toContain('laza metszet');
    expect(text).toContain('kettős ritmus');
    expect(text).toContain('rímtelen');
  });

  it('találat híján megmondja, min múlik', () => {
    run(
      analysis({
        nearMiss: {
          meter: {
            id: 'hexameter',
            name: 'hexameter',
            pattern: '-=|-=',
            kind: 'LINE',
            fictive: false,
            note: null,
            correction: null,
          },
          differences: [
            {
              syllable: 0,
              actual: 'U',
              expected: '-',
              explanation: '1. szótag: rövid helyett hosszú',
            },
          ],
          summary: 'hexameter lenne, ha — 1. szótag: rövid helyett hosszú kellene',
        },
      }),
    );
    expect(fixture.nativeElement.querySelector('.nearmiss').textContent).toContain(
      '1. szótag: rövid helyett hosszú kellene',
    );
  });

  it('megjeleníti a cezúrát és a rímfajtát', () => {
    run(
      analysis({ caesurae: [{ afterSyllable: 5, name: 'penthémimerész' }], rhymeKind: 'RAGRIM' }),
    );
    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('penthémimerész');
    expect(text).toContain('ragrím');
  });

  it('a szótagra kattintva körbejár a hosszúság, és újraelemez', () => {
    run(analysis());
    const first = fixture.nativeElement.querySelector('.syllable') as HTMLButtonElement;

    first.click();
    let request = http.expectOne('/api/analyze');
    expect(request.request.body.overrides).toEqual([{ line: 0, syllable: 0, quantity: '-' }]);
    request.flush(analysis());
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.syllable').classList.contains('overridden')).toBe(
      true,
    );

    first.click();
    request = http.expectOne('/api/analyze');
    expect(request.request.body.overrides[0].quantity).toBe('U');
    request.flush(analysis());

    first.click();
    request = http.expectOne('/api/analyze');
    expect(request.request.body.overrides[0].quantity).toBe('?');
    request.flush(analysis());

    // negyedik kattintás: vissza az automatikus olvasathoz
    first.click();
    request = http.expectOne('/api/analyze');
    expect(request.request.body.overrides).toEqual([]);
    request.flush(analysis());
  });

  it('a felülbírálások egyben törölhetők', () => {
    run(analysis());
    app.overrides.set([{ line: 0, syllable: 1, quantity: '-' }]);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Felülbírálás törlése (1)');
    app.clearOverrides();
    http.expectOne('/api/analyze').flush(analysis());
    expect(app.overrides()).toEqual([]);
  });

  it('kirajzolja a lábhatárokat és a sormetszetet', () => {
    run(
      analysis({
        meters: [
          {
            meter: {
              id: 'hexameter',
              name: 'hexameter',
              pattern: '-=|-=',
              kind: 'LINE',
              fictive: false,
              note: null,
              correction: null,
            },
            realization: '-UU',
            ictusSyllables: [0, 2],
          },
        ],
        realized: '-UU',
        caesurae: [{ afterSyllable: 1, name: 'penthémimerész' }],
      }),
    );
    const dividers = fixture.nativeElement.querySelectorAll('.divider');
    expect(dividers.length).toBe(2);
    // az 1. szótag előtt sormetszet, a 2. előtt lábhatár
    // a jelet a CSS rajzolja, ezért az osztály hordozza a jelentést, nem a szöveg
    expect(dividers[0].classList.contains('caesura')).toBe(true);
    expect(dividers[0].getAttribute('title')).toBe('sormetszet');
    expect(dividers[1].classList.contains('caesura')).toBe(false);
    expect(dividers[1].getAttribute('title')).toBe('lábhatár');
  });

  it('mérték híján az ütemhatárokat rajzolja ki', () => {
    run(
      analysis({
        accentual: [
          {
            form: {
              id: 'x',
              name: 'próba',
              measures: [1, 2],
              caesuraAfter: 1,
              note: null,
              division: '1 || 2',
            },
            wordBoundaryMeasures: 1,
            caesuraOnWordBoundary: true,
            pure: true,
            quality: 'tiszta ütemtagolás',
          },
        ],
      }),
    );
    const dividers = fixture.nativeElement.querySelectorAll('.divider');
    expect(dividers.length).toBe(1);
    expect(dividers[0].classList.contains('caesura')).toBe(true);
  });

  it('az összegző ablak kiírja az ítéletet és a részleteket', () => {
    run(analysis());
    expect(fixture.nativeElement.querySelector('.modal')).toBeNull();

    const button = [...fixture.nativeElement.querySelectorAll('button')].find(
      (b: HTMLButtonElement) => b.textContent?.trim() === 'Összegzés',
    ) as HTMLButtonElement;
    button.click();
    fixture.detectChanges();

    const modal = fixture.nativeElement.querySelector('.modal');
    expect(modal).not.toBeNull();
    expect(modal.getAttribute('aria-modal')).toBe('true');
    expect(modal.textContent).toContain('Időmértékes verselés: hexameterek.');
    expect(modal.querySelectorAll('li').length).toBe(1);

    (
      [...modal.querySelectorAll('button')].find(
        (b: HTMLButtonElement) => b.textContent?.trim() === 'Bezárás',
      ) as HTMLButtonElement
    ).click();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.modal')).toBeNull();
  });

  it('elemzés nélkül nem nyílik meg az összegzés', () => {
    const internals = fixture.componentInstance as unknown as {
      openSummary: () => void;
      showSummary: () => boolean;
    };
    internals.openSummary();
    expect(internals.showSummary()).toBe(false);
  });

  it('a nyomtatás a böngésző párbeszédét hívja, és bezárja az összegzőt', () => {
    run(analysis());
    const internals = fixture.componentInstance as unknown as {
      showSummary: { set: (v: boolean) => void; (): boolean };
    };
    internals.showSummary.set(true);
    let printed = false;
    const real = window.print;
    window.print = () => {
      printed = true;
    };

    const button = [...fixture.nativeElement.querySelectorAll('button')].find(
      (b: HTMLButtonElement) => b.textContent?.trim() === 'Nyomtatás',
    ) as HTMLButtonElement;
    button.click();
    window.print = real;

    expect(printed).toBe(true);
    expect(internals.showSummary()).toBe(false);
  });

  it('az összegzés részletei a lapra is rákerülnek nyomtatáshoz', () => {
    run(analysis());
    // képernyőn rejtve, de a DOM-ban ott van, hogy a print stíluslap megmutassa
    const details = fixture.nativeElement.querySelectorAll('.print-details li');
    expect(details.length).toBe(1);
    expect(fixture.nativeElement.querySelector('.print-title').textContent).toContain('Kalliopé');
  });

  it('a rímfajták és az ütemtagolás magyar nevet kapnak', () => {
    expect(app.rhymeKindLabel('ONRIM')).toBe('önrím');
    expect(app.rhymeKindLabel('TISZTA')).toBe('tiszta rím');
    expect(app.rhymeKindLabel('RAGRIM')).toBe('ragrím');
    expect(app.rhymeKindLabel('ROKONHANGZOS')).toBe('rokonhangzós rím');
    expect(app.rhymeKindLabel('ASSZONANC')).toBe('asszonánc');
    expect(app.rhymeKindLabel('VAKSOR')).toBe('vaksor');
    expect(app.strengthLabel('TISZTA')).toBe('tiszta ütemtagolás');
    expect(app.strengthLabel('LAZA')).toBe('laza metszet');
  });

  afterEach(() => http.verify());
});
