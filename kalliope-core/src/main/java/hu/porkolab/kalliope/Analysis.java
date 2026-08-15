package hu.porkolab.kalliope;

import java.util.List;

/**
 * Egy vers elemzésének eredménye. Tisztán adat: a motor semmit nem ír ki, a
 * megjelenítés a hívóé (parancssor, REST, felület) — így a CLI és a webes
 * felület ugyanazt az egy implementációt fogyasztja.
 */
public record Analysis(List<Stanza> stanzas, Settings settings, Summary summary, VerseSummary verse) {

    public Analysis {
        stanzas = List.copyOf(stanzas);
    }

    /** Az összegzéssel együtt — ezt hívja az elemző. */
    static Analysis of(List<Stanza> stanzas, Settings settings, Summary summary) {
        Analysis without = new Analysis(stanzas, settings, summary, null);
        return new Analysis(stanzas, settings, summary, VerseSummary.of(without));
    }

    /**
     * Egy szakasz: sorai, rímképlete, a ráilleszkedő szakaszmértékek, és — ha
     * van — az uralkodó ütemhangsúlyos sorfajta.
     *
     * @param accentualForm az a magyaros sorfajta, amely a szakasz sorainak
     *     legalább háromnegyedére illik, és a sorok legalább felében a metszet
     *     valódi szóhatárra esik; {@code null}, ha nincs ilyen
     * @param cleanCaesuraLines hány sorban esik a metszet szóhatárra
     */
    public record Stanza(
            int index,
            List<Line> lines,
            String rhymePattern,
            String rhymePatternName,
            List<MeterMatcher.StanzaMatch> forms,
            AccentualMatcher.Dominant accentual) {

        public Stanza {
            lines = List.copyOf(lines);
            forms = List.copyOf(forms);
        }

        /**
         * Kettős ritmus: a szakasz egyszerre mutat időmértékes és ütemhangsúlyos
         * rendet. Szándékosan NEM állítjuk, hogy „szimultán vers" — az ahhoz kell,
         * hogy mindkét rendnek maradéktalanul megfeleljen, és ezt a szótagszám
         * egybeesése önmagában nem bizonyítja. A két tényt egymás mellé tesszük,
         * az ítélet az olvasóé.
         */
        public boolean dualRhythm() {
            if (accentual == null || accentual.strength() != AccentualMatcher.Strength.TISZTA || lines.isEmpty()) {
                return false;
            }
            long matched = lines.stream().filter(Line::matched).count();
            return matched * 4 >= lines.size() * 3L;
        }
    }

    /**
     * Egy sor teljes elemzése.
     *
     * @param scansion a nyers skandálás, közös ({@code ?}) szótagokkal
     * @param realized a megvalósult hosszúságsor az első illeszkedő mérték
     *     szerint, vagy {@code null}, ha nincs találat. A közös szótag ugyanis
     *     csak addig kérdés, amíg a mérték el nem dönti: a „Még nyílnak a
     *     völgyben” sorban a „nak” önmagában kétféle olvasatú, de az anapesztus
     *     rövidnek követeli. A felület ezt írja ki, nem a nyers {@code ?}-eket.
     */
    public record Line(
            int index,
            String text,
            String scansion,
            String realized,
            List<Scansion.Syllable> syllables,
            boolean synizesis,
            List<MeterMatcher.Match> meters,
            List<AccentualMatcher.Match> accentual,
            NearMiss.Result nearMiss,
            String rhymeLabel,
            String rhymeKey,
            RhymeDetector.Kind rhymeKind,
            List<Caesura.Found> caesurae,
            List<String> unstressedWords,
            String ictusRow) {

        public Line {
            syllables = List.copyOf(syllables);
            meters = List.copyOf(meters);
            accentual = List.copyOf(accentual);
            unstressedWords = List.copyOf(unstressedWords);
            caesurae = List.copyOf(caesurae);
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
            int stanzaCount,
            int lineCount,
            int syllableCount,
            List<String> meters,
            List<String> stanzaForms,
            List<String> accentualForms,
            int simultaneousLines) {
        public Summary {
            meters = List.copyOf(meters);
            stanzaForms = List.copyOf(stanzaForms);
            accentualForms = List.copyOf(accentualForms);
        }
    }
}
