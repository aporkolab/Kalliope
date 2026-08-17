import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { KalliopeService } from './kalliope.service';

/**
 * A két üzemmód: beágyazott motor (GitHub Pages) és REST API (Docker-image).
 *
 * A beágyazott ág azért érdemel tesztet, mert a felület nem tud a
 * különbségről: ha a szolgáltatás csendben mégis hálózatra menne, a statikus
 * változat üres lappal indulna, és ez csak a deploy után derülne ki.
 */
describe('KalliopeService', () => {
  let service: KalliopeService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(KalliopeService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    delete window.kalliope;
  });

  describe('beágyazott motorral', () => {
    let calls: string[][];

    beforeEach(() => {
      calls = [];
      window.kalliope = {
        analyze: (text: string, settings: string, overrides: string) => {
          calls.push([text, settings, overrides]);
          return '{"stanzas":[],"settings":{},"summary":{},"verse":{"headline":"kész"}}';
        },
        canon: () => '{"meters":[{"id":"hexameter"}]}',
        examples: () => '[{"id":"toldi"}]',
      };
    });

    it('felismeri, hogy nincs szükség hálózatra', () => {
      expect(service.offline).toBe(true);
    });

    it('a motort hívja, nem az API-t', () => {
      let headline = '';
      service.analyze('vers', { a: true }, []).subscribe((r) => (headline = r.verse.headline));
      expect(headline).toBe('kész');
      http.verify(); // egyetlen HTTP-kérés sem indult
    });

    it('a beállításokat kulcs=1;kulcs=0 alakban adja át', () => {
      service.analyze('vers', { egy: true, ketto: false }, []).subscribe();
      expect(calls[0][1]).toBe('egy=1;ketto=0');
    });

    it('a felülbírálásokat sor:szótag:jel hármasként adja át', () => {
      service
        .analyze('vers', {}, [
          { line: 0, syllable: 3, quantity: '-' },
          { line: 2, syllable: 1, quantity: 'U' },
        ])
        .subscribe();
      expect(calls[0][2]).toBe('0:3:-,2:1:U');
    });

    it('a kánont és a példatárat is helyben oldja meg', () => {
      let meters = 0;
      let examples = 0;
      service.canon().subscribe((c) => (meters = c.meters.length));
      service.examples().subscribe((e) => (examples = e.length));
      expect(meters).toBe(1);
      expect(examples).toBe(1);
      http.verify();
    });
  });

  describe('beágyazott motor nélkül', () => {
    it('az API-t hívja', () => {
      expect(service.offline).toBe(false);
      service.analyze('vers', { a: true }, []).subscribe();
      const req = http.expectOne('/api/analyze');
      expect(req.request.body).toEqual({ text: 'vers', settings: { a: true }, overrides: [] });
      req.flush({});
      http.verify();
    });
  });
});
