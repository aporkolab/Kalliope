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

        // ELŐFUTAM: minden sor olvasata és mértéktalálatai, mielőtt bármit
        // kiírnánk. Kell, mert egy sor több mértékre is illeszkedhet KÜLÖNBÖZŐ
        // feloldással, és ilyenkor a VERS mértéke dönti el, melyik az olvasat.
        List<List<Chosen>> chosenByStanza = new ArrayList<>(blocks.size());
        List<List<List<Scansion.Reading>>> readingsByStanza = new ArrayList<>(blocks.size());

        List<Analysis.Stanza> stanzas = new ArrayList<>(blocks.size());
        Set<String> meterNames = new LinkedHashSet<>();
        Set<String> formNames = new LinkedHashSet<>();
        Set<String> accentualNames = new LinkedHashSet<>();
        int simultaneous = 0;
        int lineCounter = 0;
        int syllableCounter = 0;

        int scanned = 0;
        for (List<String> texts : blocks) {
            List<List<Scansion.Reading>> readings = new ArrayList<>(texts.size());
            for (String text : texts) {
                readings.add(Scansion.readings(text, settings));
            }
            // a kézi felülbírálás a sor MINDEN olvasatára vonatkozik
            for (int i = 0; i < readings.size(); i++) {
                java.util.Map<Integer, Character> over = byLine.get(scanned + i);
                if (over == null) {
                    continue;
                }
                List<Scansion.Reading> applied = new ArrayList<>(readings.get(i).size());
                for (Scansion.Reading r : readings.get(i)) {
                    applied.add(Scansion.withOverrides(r, over));
                }
                readings.set(i, applied);
            }
            List<Chosen> chosen = new ArrayList<>(texts.size());
            for (List<Scansion.Reading> perLine : readings) {
                chosen.add(choose(perLine));
            }
            readingsByStanza.add(readings);
            chosenByStanza.add(chosen);
            scanned += texts.size();
        }

        // A vers mértéke, az EGYÉRTELMŰ sorok tanúsága szerint. Ez dönti el az
        // olvasatot ott, ahol egy sor többféleképp is skandálható.
        String dominantMeter = dominantMeter(chosenByStanza);

        for (int s = 0; s < blocks.size(); s++) {
            List<String> texts = blocks.get(s);
            List<List<Scansion.Reading>> readings = readingsByStanza.get(s);
            RhymeDetector.Scheme scheme =
                    RhymeDetector.scheme(texts, settings.assonanceAsRhyme(), settings.letterSyllables());

            List<Analysis.Line> lines = new ArrayList<>(texts.size());
            List<String> chosenScansions = new ArrayList<>(texts.size());
            for (int i = 0; i < texts.size(); i++) {
                Chosen chosen = preferDominant(chosenByStanza.get(s).get(i), dominantMeter);
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
                    Analysis.Stanza.of(s, lines, scheme.pattern(), scheme.patternName(), forms, dominant);
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
        return Analysis.of(stanzas, settings, summary);
    }

    // ------------------------------------------------------------------ //

    private record Chosen(Scansion.Reading reading, List<MeterMatcher.Match> matches) {}

    /**
     * A vers domináns mértéke — az egyértelmű sorok tanúsága szerint.
     *
     * <p>Csak azokat a sorokat számoljuk, amelyeknek EGYETLEN feloldásuk van:
     * azok mondanak valamit biztosan. A kétértelmű sor önmagáról nem tanú, hisz
     * épp azt akarjuk eldönteni róla, melyik olvasata áll.
     */
    private static String dominantMeter(List<List<Chosen>> chosenByStanza) {
        java.util.Map<String, Integer> votes = new java.util.LinkedHashMap<>();
        for (List<Chosen> stanza : chosenByStanza) {
            for (Chosen c : stanza) {
                if (c.matches.isEmpty() || distinctRealizations(c.matches) > 1) {
                    continue;
                }
                for (MeterMatcher.Match m : c.matches) {
                    votes.merge(m.meter().name(), 1, (a, b) -> a + b);
                }
            }
        }
        String best = null;
        int bestCount = 0;
        for (java.util.Map.Entry<String, Integer> e : votes.entrySet()) {
            if (e.getValue() > bestCount) {
                best = e.getKey();
                bestCount = e.getValue();
            }
        }
        return best;
    }

    private static int distinctRealizations(List<MeterMatcher.Match> matches) {
        Set<String> seen = new LinkedHashSet<>();
        for (MeterMatcher.Match m : matches) {
            seen.add(m.realization());
        }
        return seen.size();
    }

    /**
     * Ha a sor többféleképp is skandálható, a vers mértékének megfelelő olvasat
     * kerül előre — a kiírt hosszúságok, a lábhatárok és a metszet is ebből
     * jönnek.
     *
     * <p>Ez verstan, nem kényelem: az egyes sort a vers mértéke felől olvassuk.
     * Az „Elmegy a kugli egy este berúgni, mer’” egyszerre illeszkedik
     * aszklepiadeszi A123-ra és daktilikus tetraméterre; a szomszédos sorok
     * viszont CSAK daktilikus tetraméterre, tehát a vers daktilikus, és a sort
     * is úgy kell skandálni. Váradi Nagy Pál jelentette, hogy az aszklepiadeszi
     * feloldás állt ott.
     */
    private static Chosen preferDominant(Chosen chosen, String dominantMeter) {
        if (dominantMeter == null || chosen.matches.size() < 2 || distinctRealizations(chosen.matches) < 2) {
            return chosen;
        }
        String wanted = null;
        for (MeterMatcher.Match m : chosen.matches) {
            if (m.meter().name().equals(dominantMeter)) {
                wanted = m.realization();
                break;
            }
        }
        if (wanted == null) {
            return chosen;
        }
        List<MeterMatcher.Match> reordered = new ArrayList<>(chosen.matches.size());
        for (MeterMatcher.Match m : chosen.matches) {
            if (m.realization().equals(wanted)) {
                reordered.add(m);
            }
        }
        for (MeterMatcher.Match m : chosen.matches) {
            if (!m.realization().equals(wanted)) {
                reordered.add(m);
            }
        }
        return new Chosen(chosen.reading, List.copyOf(reordered));
    }

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
        for (String raw : Strings.lines(poem)) {
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
