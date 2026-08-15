import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { KalliopeService } from './kalliope.service';
import { Analysis, Canon, Example, Line, Meter, Syllable } from './kalliope.models';

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

  /** A hoverelt szótag magyarázata — a felület fő tanító eszköze. */
  protected readonly hovered = signal<Syllable | null>(null);

  protected readonly reasonText = computed(() => {
    const s = this.hovered();
    if (!s) {
      return null;
    }
    const reasons = this.canon()?.reasons ?? [];
    const found = reasons.find((r) => r.name === s.reason);
    return `${s.text} — ${found?.explanation ?? s.reason}`;
  });

  protected readonly filteredMeters = computed<Meter[]>(() => {
    const all = this.canon()?.meters ?? [];
    const q = this.canonQuery().trim().toLowerCase();
    if (!q) {
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
      (m) => fold(m.name).includes(needle) || m.id.includes(needle) || m.pattern.includes(q),
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
    this.api.analyze(text, this.settings()).subscribe({
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

  protected clear(): void {
    this.poem.set('');
    this.analysis.set(null);
    this.error.set(null);
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

  protected quantityClass(q: string): string {
    return q === '-' ? 'long' : q === 'U' ? 'short' : 'anceps';
  }

  protected quantityMark(q: string): string {
    return q === '-' ? '—' : q === 'U' ? '∪' : '×';
  }

  protected meterNames(line: Line): string {
    return line.meters.map((m) => m.meter.name).join(' ~ ');
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
