import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, of } from 'rxjs';
import { Analysis, Canon, Example, Override } from './kalliope.models';

/**
 * A böngészőbe fordított motor, ha jelen van.
 *
 * A `kalliope-js` modul TeaVM-mel JavaScriptre fordítja a Java motort, és a
 * `window.kalliope` alá teszi. A visszatérési érték mindenhol ugyanaz a JSON
 * string, amit a REST API ad — ezt Java oldalon a `JsonEquivalenceTest` a
 * valódi HTTP-válaszhoz méri, a `js-diff.mjs` pedig a JVM kimenetéhez.
 */
interface EmbeddedEngine {
  analyze(text: string, settings: string, overrides: string): string;
  canon(): string;
  examples(): string;
}

declare global {
  interface Window {
    kalliope?: EmbeddedEngine;
  }
}

/**
 * A motor elérése. Két üzemmód van, és a felület nem tud róluk:
 *
 * - **beágyazott**: ha a lapon ott a lefordított motor (GitHub Pages), akkor
 *   helyben fut, hálózat nélkül;
 * - **API**: egyébként a Spring backend `/api` végpontjai (Docker-image).
 *
 * Ugyanaz a bundle mindkettőben — a döntés futásidejű, egyetlen `if`.
 */
@Injectable({ providedIn: 'root' })
export class KalliopeService {
  private readonly http = inject(HttpClient);

  /** A beágyazott motor, ha a lap betöltötte. */
  private get embedded(): EmbeddedEngine | undefined {
    return typeof window === 'undefined' ? undefined : window.kalliope;
  }

  /** Igaz, ha nincs backend — a felület ezt kiírja a láblécben. */
  get offline(): boolean {
    return this.embedded !== undefined;
  }

  analyze(
    text: string,
    settings: Record<string, boolean>,
    overrides: Override[] = [],
  ): Observable<Analysis> {
    const engine = this.embedded;
    if (engine) {
      return of(
        JSON.parse(
          engine.analyze(text, encodeSettings(settings), encodeOverrides(overrides)),
        ) as Analysis,
      );
    }
    return this.http.post<Analysis>('/api/analyze', { text, settings, overrides });
  }

  canon(): Observable<Canon> {
    const engine = this.embedded;
    return engine ? of(JSON.parse(engine.canon()) as Canon) : this.http.get<Canon>('/api/canon');
  }

  examples(): Observable<Example[]> {
    const engine = this.embedded;
    return engine
      ? of(JSON.parse(engine.examples()) as Example[])
      : this.http.get<Example[]>('/api/examples');
  }
}

/** `kulcs=1;kulcs=0` — a beágyazott motor ebben a tömör alakban kéri. */
function encodeSettings(settings: Record<string, boolean>): string {
  return Object.entries(settings)
    .map(([key, value]) => `${key}=${value ? 1 : 0}`)
    .join(';');
}

/** `sor:szótag:jel` hármasok vesszővel. */
function encodeOverrides(overrides: Override[]): string {
  return overrides.map((o) => `${o.line}:${o.syllable}:${o.quantity}`).join(',');
}
