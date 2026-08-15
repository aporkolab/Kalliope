import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { App } from './app';

describe('App', () => {
  let fixture: ComponentFixture<App>;
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    fixture = TestBed.createComponent(App);
    http = TestBed.inject(HttpTestingController);
  });

  it('induláskor lekéri a kánont és a példatárat', () => {
    fixture.detectChanges();
    const canon = http.expectOne('/api/canon');
    const examples = http.expectOne('/api/examples');
    expect(canon.request.method).toBe('GET');
    canon.flush({
      originVersion: 'teszt',
      canonClosed: '2006',
      meters: [],
      stanzas: [],
      settings: [
        { key: 'az_s_kotoszo_kozombos', label: 'Az s kötőszó közömbös', defaultValue: true },
      ],
      reasons: [{ name: 'SHORT', explanation: 'rövid' }],
      unstressedWords: [],
    });
    examples.flush([]);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('h1').textContent).toContain('Kalliopé');
    http.verify();
  });

  it('a beállítások alapértékei a kánonból jönnek', () => {
    fixture.detectChanges();
    http.expectOne('/api/canon').flush({
      originVersion: 't',
      canonClosed: '2006',
      meters: [],
      stanzas: [],
      settings: [
        { key: 'a', label: 'A', defaultValue: true },
        { key: 'b', label: 'B', defaultValue: false },
      ],
      reasons: [],
      unstressedWords: [],
    });
    http.expectOne('/api/examples').flush([]);
    fixture.detectChanges();
    const settings = (
      fixture.componentInstance as unknown as { settings: () => Record<string, boolean> }
    ).settings();
    expect(settings).toEqual({ a: true, b: false });
    http.verify();
  });
});
