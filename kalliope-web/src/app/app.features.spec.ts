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
