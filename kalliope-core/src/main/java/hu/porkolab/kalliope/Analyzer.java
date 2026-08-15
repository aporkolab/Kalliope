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
        List<List<String>> blocks = split(poem == null ? "" : poem);
        List<Analysis.Stanza> stanzas = new ArrayList<>(blocks.size());
        Set<String> meterNames = new LinkedHashSet<>();
        Set<String> formNames = new LinkedHashSet<>();
        int lineCounter = 0;
        int syllableCounter = 0;

        for (int s = 0; s < blocks.size(); s++) {
            List<String> texts = blocks.get(s);
            List<List<Scansion.Reading>> readings = new ArrayList<>(texts.size());
            for (String text : texts) {
                readings.add(Scansion.readings(text, settings));
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
                lines.add(new Analysis.Line(
                        lineCounter++,
                        texts.get(i),
                        chosen.reading.pattern(),
                        chosen.reading.syllables(),
                        chosen.reading.synizesis(),
                        matches,
                        scheme.labels().get(i),
                        scheme.keys().get(i),
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
            stanzas.add(new Analysis.Stanza(s, lines, scheme.pattern(), forms));
        }

        Analysis.Summary summary = new Analysis.Summary(
                stanzas.size(), lineCounter, syllableCounter, List.copyOf(meterNames), List.copyOf(formNames));
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
