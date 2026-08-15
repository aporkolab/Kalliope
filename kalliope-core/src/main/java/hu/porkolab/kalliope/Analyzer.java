package hu.porkolab.kalliope;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * A teljes elemzés: szakaszokra bontás, soronkénti skandálás, mérték- és
 * rímfelismerés.
 *
 * <p>A verset <b>üres sorok mentén</b> bontjuk szakaszokra, és a rímképletet is
 * szakaszonként számoljuk. (A korábbi változat eldobta az üres sorokat, ezért a
 * szakaszmérték-illesztés valódi, többstrófás versen sosem tudott megszólalni,
 * és a rímbetűk végigfutottak az egész versen.)
 */
public final class Analyzer {

    /** Ennél hosszabb verset nem elemzünk — a felület így nem tud megfagyasztani. */
    public static final int MAX_LINES = 2000;

    public static final int MAX_LINE_LENGTH = 2000;

    private Analyzer() {}

    public static Analysis analyze(String poem) {
        return analyze(poem, MetricCanon.DEFAULT_SETTINGS);
    }

    public static Analysis analyze(String poem, Settings settings) {
        return analyze(poem, settings, List.of());
    }

    /**
     * @param overrides kézi szótaghosszúság-felülbírálások; a sorindex a vers
     *     egészére nézve értendő, ahogy az elemzés visszaadja
     */
    public static Analysis analyze(String poem, Settings settings, List<Scansion.Override> overrides) {
        java.util.Map<Integer, java.util.Map<Integer, Character>> byLine = new java.util.HashMap<>();
        for (Scansion.Override o : overrides == null ? List.<Scansion.Override>of() : overrides) {
            byLine.computeIfAbsent(o.line(), k -> new java.util.HashMap<>()).put(o.syllable(), o.quantity());
        }
        List<List<String>> blocks = split(poem == null ? "" : poem);
        List<Analysis.Stanza> stanzas = new ArrayList<>(blocks.size());
        Set<String> meterNames = new LinkedHashSet<>();
        Set<String> formNames = new LinkedHashSet<>();
        Set<String> accentualNames = new LinkedHashSet<>();
        int simultaneous = 0;
        int lineCounter = 0;
        int syllableCounter = 0;

        for (int s = 0; s < blocks.size(); s++) {
            List<String> texts = blocks.get(s);
            List<List<Scansion.Reading>> readings = new ArrayList<>(texts.size());
            for (String text : texts) {
                readings.add(Scansion.readings(text, settings));
            }
            // a kézi felülbírálás a sor MINDEN olvasatára vonatkozik
            for (int i = 0; i < readings.size(); i++) {
                java.util.Map<Integer, Character> over = byLine.get(lineCounter + i);
                if (over == null) {
                    continue;
                }
                List<Scansion.Reading> applied = new ArrayList<>(readings.get(i).size());
                for (Scansion.Reading r : readings.get(i)) {
                    applied.add(Scansion.withOverrides(r, over));
                }
                readings.set(i, applied);
            }
            RhymeDetector.Scheme scheme =
                    RhymeDetector.scheme(texts, settings.assonanceAsRhyme(), settings.letterSyllables());

            List<Analysis.Line> lines = new ArrayList<>(texts.size());
            List<String> chosenScansions = new ArrayList<>(texts.size());
            for (int i = 0; i < texts.size(); i++) {
                Chosen chosen = choose(readings.get(i));
                chosenScansions.add(chosen.reading.pattern());
                syllableCounter += chosen.reading.syllableCount();
                List<MeterMatcher.Match> matches = settings.multipleMatches() || chosen.matches.isEmpty()
                        ? chosen.matches
                        : List.of(chosen.matches.get(0));
                for (MeterMatcher.Match m : matches) {
                    meterNames.add(m.meter().name());
                }
                String ictus =
                        settings.showIctus() && !matches.isEmpty() ? MeterMatcher.ictusRow(matches.get(0)) : null;
                List<String> unstressed =
                        settings.explainUnstressed() ? unstressedWords(texts.get(i), settings) : List.of();
                // minden olvasatot megnézünk: a „Europa" összevont olvasata teszi
                // Zrínyi negyedik sorát tizenkét szótagossá
                List<AccentualMatcher.Match> accentual = bestAccentual(readings.get(i));
                String realized = matches.isEmpty() ? null : matches.get(0).realization();
                // ha nincs találat, mondjuk meg, mi hiányzott hozzá — minden olvasatot végignézve
                NearMiss.Result nearMiss = matches.isEmpty() ? closestOfReadings(readings.get(i)) : null;
                // hol van ténylegesen sormetszet a sorban
                List<Caesura.Found> caesurae = matches.isEmpty()
                        ? List.of()
                        : Caesura.detect(matches.get(0).meter(), chosen.reading.pattern(), chosen.reading.syllables());
                lines.add(new Analysis.Line(
                        lineCounter++,
                        texts.get(i),
                        chosen.reading.pattern(),
                        realized,
                        chosen.reading.syllables(),
                        chosen.reading.synizesis(),
                        matches,
                        accentual,
                        nearMiss,
                        scheme.labels().get(i),
                        scheme.keys().get(i),
                        scheme.kinds().get(i),
                        caesurae,
                        unstressed,
                        ictus));
            }

            List<List<String>> readingPatterns = new ArrayList<>(readings.size());
            for (List<Scansion.Reading> perLine : readings) {
                List<String> patterns = new ArrayList<>(perLine.size());
                for (Scansion.Reading r : perLine) {
                    patterns.add(r.pattern());
                }
                readingPatterns.add(patterns);
            }
            List<MeterMatcher.StanzaMatch> forms = MeterMatcher.matchStanza(readingPatterns, scheme.pattern());
            for (MeterMatcher.StanzaMatch f : forms) {
                formNames.add(f.form().name());
            }
            List<List<AccentualMatcher.Match>> perLine =
                    lines.stream().map(Analysis.Line::accentual).toList();
            AccentualMatcher.Dominant dominant = AccentualMatcher.dominant(perLine);
            if (dominant.form() != null) {
                accentualNames.add(dominant.form().name());
            }
            Analysis.Stanza stanza =
                    new Analysis.Stanza(s, lines, scheme.pattern(), scheme.patternName(), forms, dominant);
            if (stanza.dualRhythm()) {
                simultaneous += lines.size();
            }
            stanzas.add(stanza);
        }

        Analysis.Summary summary = new Analysis.Summary(
                stanzas.size(),
                lineCounter,
                syllableCounter,
                List.copyOf(meterNames),
                List.copyOf(formNames),
                List.copyOf(accentualNames),
                simultaneous);
        return new Analysis(stanzas, settings, summary);
    }

    // ------------------------------------------------------------------ //

    private record Chosen(Scansion.Reading reading, List<MeterMatcher.Match> matches) {}

    /**
     * A sor olvasata: az elsődleges (minden magánhangzó külön szótag), kivéve ha
     * csak egy összevont kettőshangzós olvasat illeszkedik valamely mértékre.
     */
    private static Chosen choose(List<Scansion.Reading> readings) {
        Chosen fallback = null;
        for (Scansion.Reading r : readings) {
            List<MeterMatcher.Match> m = MeterMatcher.matchLine(r.pattern(), false);
            if (!m.isEmpty()) {
                return new Chosen(r, m);
            }
            if (fallback == null) {
                fallback = new Chosen(r, List.of());
            }
        }
        if (fallback == null) {
            return new Chosen(new Scansion.Reading("", List.of(), false), List.of());
        }
        // sorfajta nincs — hátha megnevezhető kolónként vagy verslábként
        List<MeterMatcher.Match> small = MeterMatcher.matchLine(fallback.reading.pattern(), true);
        return new Chosen(fallback.reading, small);
    }

    /** A legkevesebb eltérést adó közeli mérték, minden olvasatot végignézve. */
    private static NearMiss.Result closestOfReadings(List<Scansion.Reading> readings) {
        NearMiss.Result best = null;
        for (Scansion.Reading r : readings) {
            NearMiss.Result candidate = NearMiss.closest(r.pattern());
            if (candidate != null
                    && (best == null
                            || candidate.differences().size()
                                    < best.differences().size())) {
                best = candidate;
            }
        }
        return best;
    }

    /** A sor legjobb ütemhangsúlyos illesztései — minden olvasatot végignézve. */
    private static List<AccentualMatcher.Match> bestAccentual(List<Scansion.Reading> readings) {
        List<AccentualMatcher.Match> best = List.of();
        for (Scansion.Reading r : readings) {
            List<AccentualMatcher.Match> m = AccentualMatcher.match(r.syllables());
            if (m.isEmpty()) {
                continue;
            }
            if (best.isEmpty() || (!best.get(0).pure() && m.get(0).pure())) {
                best = m;
            }
        }
        return best;
    }

    private static List<String> unstressedWords(String line, Settings settings) {
        List<String> out = new ArrayList<>();
        for (String w : TextNormalizer.words(line, settings.letterSyllables())) {
            if (MetricCanon.UNSTRESSED_WORDS.contains(w)) {
                out.add(w);
            }
        }
        return out;
    }

    /** A verset üres sorok mentén szakaszokra bontja. */
    static List<List<String>> split(String poem) {
        List<List<String>> blocks = new ArrayList<>();
        List<String> current = new ArrayList<>();
        int lineCount = 0;
        for (String raw : poem.split("\\R", -1)) {
            String line = raw.strip();
            if (line.isEmpty()) {
                if (!current.isEmpty()) {
                    blocks.add(List.copyOf(current));
                    current.clear();
                }
                continue;
            }
            if (lineCount++ >= MAX_LINES) {
                break;
            }
            current.add(line.length() > MAX_LINE_LENGTH ? line.substring(0, MAX_LINE_LENGTH) : line);
        }
        if (!current.isEmpty()) {
            blocks.add(List.copyOf(current));
        }
        return blocks;
    }
}
