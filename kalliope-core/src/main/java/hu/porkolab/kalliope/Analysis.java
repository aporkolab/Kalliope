package hu.porkolab.kalliope;

import java.util.List;

/**
 * Egy vers elemzésének eredménye. Tisztán adat: a motor semmit nem ír ki, a
 * megjelenítés a hívóé (parancssor, REST, felület) — így a CLI és a webes
 * felület ugyanazt az egy implementációt fogyasztja.
 */
public record Analysis(List<Stanza> stanzas, Settings settings, Summary summary) {

    public Analysis {
        stanzas = List.copyOf(stanzas);
    }

    /** Egy szakasz: sorai, rímképlete és a ráilleszkedő szakaszmértékek. */
    public record Stanza(int index, List<Line> lines, String rhymePattern, List<MeterMatcher.StanzaMatch> forms) {
        public Stanza {
            lines = List.copyOf(lines);
            forms = List.copyOf(forms);
        }
    }

    /** Egy sor teljes elemzése. */
    public record Line(
            int index,
            String text,
            String scansion,
            List<Scansion.Syllable> syllables,
            boolean synizesis,
            List<MeterMatcher.Match> meters,
            String rhymeLabel,
            String rhymeKey,
            List<String> unstressedWords,
            String ictusRow) {

        public Line {
            syllables = List.copyOf(syllables);
            meters = List.copyOf(meters);
            unstressedWords = List.copyOf(unstressedWords);
        }

        public int syllableCount() {
            return syllables.size();
        }

        public boolean matched() {
            return !meters.isEmpty();
        }
    }

    /** Összesítés a felület fejlécéhez. */
    public record Summary(
            int stanzaCount, int lineCount, int syllableCount, List<String> meters, List<String> stanzaForms) {
        public Summary {
            meters = List.copyOf(meters);
            stanzaForms = List.copyOf(stanzaForms);
        }
    }
}
