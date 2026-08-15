import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { KalliopeService } from './kalliope.service';
import {
  Analysis,
  Canon,
  Example,
  Line,
  Meter,
  Override,
  Quantity,
  Syllable,
} from './kalliope.models';

type Tab = 'elemzes' | 'adatbazis';

@Component({
  selector: 'app-root',
  imports: [FormsModule],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App {
  private readonly api = inject(KalliopeService);

  protected readonly tab = signal<Tab>('elemzes');
  protected readonly poem = signal('');
  protected readonly analysis = signal<Analysis | null>(null);
  protected readonly canon = signal<Canon | null>(null);
  protected readonly examples = signal<Example[]>([]);
  protected readonly settings = signal<Record<string, boolean>>({});
  protected readonly error = signal<string | null>(null);
  protected readonly busy = signal(false);
  protected readonly showSettings = signal(false);
  protected readonly canonQuery = signal('');
  protected readonly copied = signal(false);
  protected readonly showSummary = signal(false);

  /** Kézi szótaghosszúság-felülbírálások — kattintásra körbejárnak. */
  protected readonly overrides = signal<Override[]>([]);

  /** A hoverelt szótag magyarázata — a felület fő tanító eszköze. */
  protected readonly hovered = signal<Syllable | null>(null);

  protected readonly reasonText = computed(() => {
    const s = this.hovered();
    if (!s) {
      return '';
    }
    const reasons = this.canon()?.reasons ?? [];
    const found = reasons.find((r) => r.name === s.reason);
    return `${s.text} — ${found?.explanation ?? s.reason}`;
  });

  protected readonly filteredMeters = computed<Meter[]>(() => {
    const all = this.canon()?.meters ?? [];
    // A mintát NEM kisbetűsítjük: a jelölésben az 'U' a rövid szótag jele,
    // lekisbetűzve a mintakeresés sosem találna.
    const raw = this.canonQuery().trim();
    const q = raw.toLowerCase();
    if (!raw) {
      return all;
    }
    const fold = (t: string) =>
      t
        .toLowerCase()
        .replace(/[áàâ]/g, 'a')
        .replace(/[éè]/g, 'e')
        .replace(/[íî]/g, 'i')
        .replace(/[óöő]/g, 'o')
        .replace(/[úüű]/g, 'u');
    const needle = fold(q);
    return all.filter(
      (m) => fold(m.name).includes(needle) || m.id.includes(needle) || m.pattern.includes(raw),
    );
  });

  constructor() {
    this.api.canon().subscribe({
      next: (c) => {
        this.canon.set(c);
        const defaults: Record<string, boolean> = {};
        for (const s of c.settings) {
          defaults[s.key] = s.defaultValue;
        }
        this.settings.set(defaults);
        const shared = this.readShared();
        if (shared) {
          this.poem.set(shared);
          this.analyze();
        }
      },
      error: () => this.error.set('Nem sikerült betölteni a metrikai kánont.'),
    });
    this.api.examples().subscribe({ next: (e) => this.examples.set(e) });
  }

  protected analyze(): void {
    const text = this.poem().trim();
    this.error.set(null);
    if (!text) {
      this.analysis.set(null);
      return;
    }
    this.busy.set(true);
    this.api.analyze(text, this.settings(), this.overrides()).subscribe({
      next: (a) => {
        this.analysis.set(a);
        this.busy.set(false);
      },
      error: (err) => {
        this.error.set(err?.error?.detail ?? 'Az elemzés nem sikerült.');
        this.busy.set(false);
      },
    });
  }

  protected toggleSetting(key: string): void {
    const next = { ...this.settings() };
    next[key] = !next[key];
    this.settings.set(next);
    if (this.analysis()) {
      this.analyze();
    }
  }

  protected loadExample(example: Example): void {
    this.poem.set(example.text);
    this.analyze();
  }

  /** Az összegző ablak megnyitása; elemzés nélkül nincs mit mutatni. */
  protected openSummary(): void {
    if (this.analysis()) {
      this.showSummary.set(true);
    }
  }

  protected closeSummary(): void {
    this.showSummary.set(false);
  }

  protected clear(): void {
    this.poem.set('');
    this.analysis.set(null);
    this.error.set(null);
    this.overrides.set([]);
    this.showSummary.set(false);
  }

  /**
   * Kattintásra körbejárja a hosszúságot: hosszú → rövid → közös → automatikus.
   * A verstan értelmezés kérdése; az olvasónak joga van más olvasathoz.
   */
  protected cycleQuantity(line: Line, index: number): void {
    const current = this.overrides().find((o) => o.line === line.index && o.syllable === index);
    const order: (Quantity | null)[] = ['-', 'U', '?', null];
    const next = order[(order.indexOf(current?.quantity ?? null) + 1) % order.length];
    const rest = this.overrides().filter((o) => !(o.line === line.index && o.syllable === index));
    this.overrides.set(
      next ? [...rest, { line: line.index, syllable: index, quantity: next }] : rest,
    );
    this.analyze();
  }

  protected isOverridden(line: Line, index: number): boolean {
    return this.overrides().some((o) => o.line === line.index && o.syllable === index);
  }

  protected clearOverrides(): void {
    this.overrides.set([]);
    this.analyze();
  }

  /** Osztható link: a vers a címsor törtrészében, szerver nélkül. */
  protected share(): void {
    const encoded = encodeURIComponent(this.poem());
    const url = `${location.origin}${location.pathname}#v=${encoded}`;
    void navigator.clipboard.writeText(url).then(() => {
      this.copied.set(true);
      setTimeout(() => this.copied.set(false), 2000);
    });
  }

  protected exportJson(): void {
    const data = this.analysis();
    if (!data) {
      return;
    }
    const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'kalliope-elemzes.json';
    a.click();
    URL.revokeObjectURL(url);
  }

  private readShared(): string | null {
    const hash = location.hash;
    if (!hash.startsWith('#v=')) {
      return null;
    }
    try {
      return decodeURIComponent(hash.slice(3));
    } catch {
      return null;
    }
  }

  /**
   * A kiírandó hosszúság: ha egy mérték illeszkedik, a MEGVALÓSULT hosszúság,
   * mert a közös szótag kérdése ilyenkor eldőlt. Ha nincs találat, a nyers
   * skandálás marad, közös jellel.
   */
  protected shownQuantity(line: Line, index: number): string {
    const realized = line.realized;
    if (realized && index < realized.length) {
      return realized.charAt(index);
    }
    return line.syllables[index].quantity;
  }

  /**
   * Verslábat kezd-e ez a szótag. Az illeszkedő mérték lábhatárait az illesztés
   * adja (a minta '|' jelei a szkennelt sorra vetítve). Ha nincs időmértékes
   * találat, de van ütemhangsúlyos forma, annak az ütemhatárait rajzoljuk.
   */
  protected startsFoot(line: Line, index: number): boolean {
    if (index === 0) {
      return false;
    }
    if (line.meters.length) {
      return line.meters[0].ictusSyllables.includes(index);
    }
    const measures = line.accentual[0]?.form.measures;
    if (!measures) {
      return false;
    }
    let at = 0;
    for (const m of measures) {
      at += m;
      if (at === index) {
        return true;
      }
    }
    return false;
  }

  /** Sormetszet (cezúra) van-e a szótag előtt — vastagabb elválasztó. */
  protected startsCaesura(line: Line, index: number): boolean {
    if (index === 0) {
      return false;
    }
    if (line.caesurae.some((c) => c.afterSyllable === index)) {
      return true;
    }
    if (line.meters.length) {
      return false;
    }
    const acc = line.accentual[0];
    if (!acc || !acc.form.caesuraAfter) {
      return false;
    }
    let at = 0;
    for (let i = 0; i < acc.form.caesuraAfter; i++) {
      at += acc.form.measures[i];
    }
    return at === index;
  }

  /** Közös volt-e a szótag, mielőtt a mérték eldöntötte. */
  protected wasAnceps(line: Line, index: number): boolean {
    return line.syllables[index].quantity === '?';
  }

  protected quantityClass(q: string): string {
    return q === '-' ? 'long' : q === 'U' ? 'short' : 'anceps';
  }

  protected quantityMark(q: string): string {
    return q === '-' ? '—' : q === 'U' ? '∪' : '×';
  }

  protected meterNames(line: Line): string {
    return line.meters.map((m) => m.meter.name).join(' ~ ');
  }

  protected rhymeKindLabel(kind: string): string {
    switch (kind) {
      case 'ONRIM':
        return 'önrím';
      case 'TISZTA':
        return 'tiszta rím';
      case 'RAGRIM':
        return 'ragrím';
      case 'ROKONHANGZOS':
        return 'rokonhangzós rím';
      case 'ASSZONANC':
        return 'asszonánc';
      default:
        return 'vaksor';
    }
  }

  protected strengthLabel(strength: string): string {
    return strength === 'TISZTA' ? 'tiszta ütemtagolás' : 'laza metszet';
  }

  protected kindLabel(kind: string): string {
    switch (kind) {
      case 'FOOT':
        return 'versláb';
      case 'COLON':
        return 'kolón';
      case 'LINE':
        return 'sorfajta';
      default:
        return 'összetett';
    }
  }
}
