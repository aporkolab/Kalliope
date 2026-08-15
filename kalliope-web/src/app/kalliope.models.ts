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

export interface AccentualFormRef {
  id: string;
  name: string;
  measures: number[];
  caesuraAfter: number;
  note: string | null;
}

export interface AccentualMatch {
  form: AccentualFormRef;
  wordBoundaryMeasures: number;
  caesuraOnWordBoundary: boolean;
  pure: boolean;
  quality: string;
}

export interface Dominant {
  form: AccentualFormRef | null;
  strength: 'TISZTA' | 'LAZA' | 'NINCS';
  cleanLines: number;
}

export interface Difference {
  syllable: number;
  actual: string;
  expected: string;
  explanation: string;
}

export interface NearMiss {
  meter: Meter;
  differences: Difference[];
  summary: string;
}

export interface CaesuraFound {
  afterSyllable: number;
  name: string;
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
  realized: string | null;
  syllables: Syllable[];
  synizesis: boolean;
  meters: MeterMatch[];
  accentual: AccentualMatch[];
  nearMiss: NearMiss | null;
  rhymeLabel: string;
  rhymeKey: string;
  rhymeKind: string;
  caesurae: CaesuraFound[];
  unstressedWords: string[];
  ictusRow: string | null;
}

export interface Stanza {
  index: number;
  lines: Line[];
  rhymePattern: string;
  rhymePatternName: string | null;
  forms: StanzaMatch[];
  accentual: Dominant;
  dualRhythm: boolean;
}

export interface Summary {
  stanzaCount: number;
  lineCount: number;
  syllableCount: number;
  meters: string[];
  stanzaForms: string[];
  accentualForms: string[];
  simultaneousLines: number;
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

export interface Override {
  line: number;
  syllable: number;
  quantity: Quantity;
}

export interface Example {
  id: string;
  title: string;
  author: string;
  expected: string;
  text: string;
}
