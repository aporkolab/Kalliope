import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { WritableSignal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { App } from './app';
import { Analysis, Canon, Example } from './kalliope.models';

const CANON: Canon = {
  originVersion: 'teszt',
  canonClosed: '2006',
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
      id: 'adoniszi-kolon',
      name: 'adoniszi kolón',
      pattern: '-UU-?',
      kind: 'COLON',
      fictive: false,
      note: null,
      correction: null,
    },
    {
      id: 'jambus',
      name: 'jambus',
      pattern: 'U-',
      kind: 'FOOT',
      fictive: false,
      note: null,
      correction: null,
    },
    {
      id: 'ket-adoniszi',
      name: 'két adoniszi',
      pattern: '-UU-?-UU-?',
      kind: 'COMPLEX',
      fictive: true,
      note: null,
      correction: null,
    },
  ],
  stanzas: [],
  settings: [
    { key: 'az_s_kotoszo_kozombos', label: 'Az s kötőszó közömbös', defaultValue: true },
    { key: 'latszik_az_utemhangsuly_a_gorogon', label: 'Ütemhangsúly', defaultValue: false },
  ],
  reasons: [{ name: 'SHORT', explanation: 'rövid' }],
  unstressedWords: [],
};

const EXAMPLE: Example = {
  id: 'proba',
  title: 'Próbavers',
  author: 'Senki',
  expected: 'semmi',
  text: 'kert alatt',
};

const ANALYSIS: Analysis = {
  stanzas: [
    {
      index: 0,
      rhymePattern: 'x',
      rhymePatternName: null,
      accentual: { form: null, strength: 'NINCS', cleanLines: 0 },
      dualRhythm: false,
      forms: [],
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
          synizesis: true,
          meters: [],
          accentual: [],
          nearMiss: null,
          pulse: null,
          rhymeLabel: 'x',
          rhymeKey: 'att',
          rhymeKind: 'VAKSOR',
          caesurae: [],
          unstressedWords: [],
          ictusRow: '÷UU',
        },
      ],
    },
  ],
  settings: {},
  summary: {
    stanzaCount: 1,
    lineCount: 1,
    syllableCount: 3,
    meters: [],
    stanzaForms: [],
    accentualForms: [],
    simultaneousLines: 0,
  },
  verse: {
    system: 'IDOMERTEKES',
    headline: 'Időmértékes verselés: hexameterek.',
    details: ['Egyetlen, 1 soros szakasz.'],
  },
};

interface Internals {
  poem: WritableSignal<string>;
  settings: WritableSignal<Record<string, boolean>>;
  analysis: WritableSignal<Analysis | null>;
  error: WritableSignal<string | null>;
  copied: WritableSignal<boolean>;
  canonQuery: WritableSignal<string>;
  tab: WritableSignal<string>;
  analyze: () => void;
  clear: () => void;
  share: () => void;
  exportJson: () => void;
  loadExample: (e: Example) => void;
  toggleSetting: (key: string) => void;
  quantityMark: (q: string) => string;
  kindLabel: (kind: string) => string;
}

describe('App interakciók', () => {
  let fixture: ComponentFixture<App>;
  let http: HttpTestingController;
  let app: Internals;

  function bootstrap(): void {
    fixture.detectChanges();
    http.expectOne('/api/canon').flush(CANON);
    http.expectOne('/api/examples').flush([EXAMPLE]);
    fixture.detectChanges();
  }

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    fixture = TestBed.createComponent(App);
    http = TestBed.inject(HttpTestingController);
    app = fixture.componentInstance as unknown as Internals;
  });

  it('a példa betöltése azonnal elemez', () => {
    bootstrap();
    app.loadExample(EXAMPLE);
    const request = http.expectOne('/api/analyze');
    expect(request.request.body.text).toBe('kert alatt');
    request.flush(ANALYSIS);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('kert');
    // az összevont olvasatot külön jelzi
    expect(fixture.nativeElement.textContent).toContain('összevonással');
    // és az iktussort is kiírja
    expect(fixture.nativeElement.querySelector('.ictus').textContent).toContain('÷');
  });

  it('a törlés kiüríti a szöveget és az eredményt', () => {
    bootstrap();
    app.loadExample(EXAMPLE);
    http.expectOne('/api/analyze').flush(ANALYSIS);
    app.clear();
    fixture.detectChanges();
    expect(app.poem()).toBe('');
    expect(app.analysis()).toBeNull();
    expect(fixture.nativeElement.textContent).toContain('Írj be egy verset');
  });

  it('üres szövegre nem küld kérést', () => {
    bootstrap();
    app.poem.set('   ');
    app.analyze();
    http.expectNone('/api/analyze');
    expect(app.analysis()).toBeNull();
  });

  it('a szerverhibát olvashatóan mutatja', () => {
    bootstrap();
    app.poem.set('vers');
    app.analyze();
    http
      .expectOne('/api/analyze')
      .flush(
        { detail: 'Ismeretlen beállítás: nincs_ilyen' },
        { status: 400, statusText: 'Bad Request' },
      );
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.error').textContent).toContain(
      'Ismeretlen beállítás',
    );
  });

  it('a beállítás átkapcsolása újraelemez', () => {
    bootstrap();
    app.poem.set('vers');
    app.analyze();
    http.expectOne('/api/analyze').flush(ANALYSIS);

    app.toggleSetting('latszik_az_utemhangsuly_a_gorogon');
    const request = http.expectOne('/api/analyze');
    expect(request.request.body.settings['latszik_az_utemhangsuly_a_gorogon']).toBe(true);
    request.flush(ANALYSIS);
  });

  it('elemzés nélkül a beállítás nem küld kérést', () => {
    bootstrap();
    app.toggleSetting('az_s_kotoszo_kozombos');
    http.expectNone('/api/analyze');
    expect(app.settings()['az_s_kotoszo_kozombos']).toBe(false);
  });

  it('a megosztható link a vágólapra kerül', async () => {
    bootstrap();
    const written: string[] = [];
    Object.defineProperty(navigator, 'clipboard', {
      value: { writeText: (t: string) => (written.push(t), Promise.resolve()) },
      configurable: true,
    });
    app.poem.set('kert alatt');
    app.share();
    await Promise.resolve();
    expect(written[0]).toContain('#v=kert%20alatt');
    expect(app.copied()).toBe(true);
  });

  it('a JSON-export letöltést indít', () => {
    bootstrap();
    app.analysis.set(ANALYSIS);
    const urls: string[] = [];
    URL.createObjectURL = () => 'blob:teszt';
    URL.revokeObjectURL = (u: string) => urls.push(u);
    let clicked = false;
    const realCreate = document.createElement.bind(document);
    document.createElement = ((tag: string) => {
      const el = realCreate(tag) as HTMLAnchorElement;
      if (tag === 'a') {
        el.click = () => {
          clicked = true;
        };
      }
      return el;
    }) as typeof document.createElement;

    app.exportJson();
    document.createElement = realCreate;
    expect(clicked).toBe(true);
    expect(urls).toContain('blob:teszt');
  });

  it('elemzés nélkül az export nem csinál semmit', () => {
    bootstrap();
    let called = false;
    URL.createObjectURL = () => ((called = true), 'blob:x');
    app.exportJson();
    expect(called).toBe(false);
  });

  it('a megosztott linkből induláskor betölti a verset', () => {
    location.hash = '#v=' + encodeURIComponent('kert alatt');
    fixture.detectChanges();
    http.expectOne('/api/canon').flush(CANON);
    http.expectOne('/api/examples').flush([]);
    const request = http.expectOne('/api/analyze');
    expect(request.request.body.text).toBe('kert alatt');
    request.flush(ANALYSIS);
    location.hash = '';
  });

  it('hibás kánon-lekérésre hibaüzenetet ad', () => {
    fixture.detectChanges();
    http.expectOne('/api/canon').error(new ProgressEvent('hiba'));
    http.expectOne('/api/examples').flush([]);
    fixture.detectChanges();
    expect(app.error()).toContain('metrikai kánont');
  });

  it('a hosszúságjelek és a fajtanevek magyarul jelennek meg', () => {
    bootstrap();
    expect(app.quantityMark('-')).toBe('—');
    expect(app.quantityMark('U')).toBe('∪');
    expect(app.quantityMark('?')).toBe('×');
    expect(app.kindLabel('FOOT')).toBe('versláb');
    expect(app.kindLabel('COLON')).toBe('kolón');
    expect(app.kindLabel('LINE')).toBe('sorfajta');
    expect(app.kindLabel('COMPLEX')).toBe('összetett');
  });

  it('a kánon keresése ékezet- és mintatűrő', () => {
    bootstrap();
    app.tab.set('adatbazis');
    app.canonQuery.set('adoniszi');
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelectorAll('tbody tr').length).toBe(2);

    app.canonQuery.set('-UU-?');
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelectorAll('tbody tr').length).toBe(2);

    app.canonQuery.set('');
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelectorAll('tbody tr').length).toBe(CANON.meters.length);
  });

  afterEach(() => http.verify());
});
