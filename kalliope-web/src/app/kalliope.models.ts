/** A motor JSON-alakja. A mezőnevek a Java rekordokból jönnek — ne térjenek el. */

export type Quantity = '-' | 'U' | '?';

export interface Correction {
  original: string;
  reason: string;
  source: string;
}

export interface Meter {
  id: string;
  name: string;
  pattern: string;
  kind: 'FOOT' | 'COLON' | 'LINE' | 'COMPLEX';
  fictive: boolean;
  note: string | null;
  correction: Correction | null;
}

export interface Syllable {
  text: string;
  quantity: Quantity;
  reason: string;
  wordIndex: number;
}

export interface MeterMatch {
  meter: Meter;
  realization: string;
  ictusSyllables: number[];
}

export interface StanzaFormRef {
  id: string;
  name: string;
  rhymeScheme: string | null;
  closed: boolean;
}

export interface StanzaMatch {
  form: StanzaFormRef;
  repetitions: number;
  rhymeSchemeMatches: boolean;
}

export interface Line {
  index: number;
  text: string;
  scansion: string;
  syllables: Syllable[];
  synizesis: boolean;
  meters: MeterMatch[];
  rhymeLabel: string;
  rhymeKey: string;
  unstressedWords: string[];
  ictusRow: string | null;
}

export interface Stanza {
  index: number;
  lines: Line[];
  rhymePattern: string;
  forms: StanzaMatch[];
}

export interface Summary {
  stanzaCount: number;
  lineCount: number;
  syllableCount: number;
  meters: string[];
  stanzaForms: string[];
}

export interface Analysis {
  stanzas: Stanza[];
  settings: Record<string, boolean>;
  summary: Summary;
}

export interface SettingInfo {
  key: string;
  label: string;
  defaultValue: boolean;
}

export interface ReasonInfo {
  name: string;
  explanation: string;
}

export interface StanzaFormInfo {
  id: string;
  name: string;
  lineMeterIds: string[];
  rhymeScheme: string | null;
  closed: boolean;
}

export interface Canon {
  originVersion: string;
  canonClosed: string;
  meters: Meter[];
  stanzas: StanzaFormInfo[];
  settings: SettingInfo[];
  reasons: ReasonInfo[];
  unstressedWords: string[];
}

export interface Example {
  id: string;
  title: string;
  author: string;
  expected: string;
  text: string;
}
