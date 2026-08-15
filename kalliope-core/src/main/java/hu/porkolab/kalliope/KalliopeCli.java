package hu.porkolab.kalliope;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Parancssori felület.
 *
 * <pre>
 *   java -jar kalliope-core.jar               a példatár elemzése
 *   java -jar kalliope-core.jar vers.txt      fájl elemzése
 *   java -jar kalliope-core.jar -             a szabvány bemenet elemzése
 *   java -jar kalliope-core.jar --canon       a metrikai kánon kiírása
 * </pre>
 *
 * <p>A kimenetet mindig UTF-8-cal írjuk, függetlenül a rendszer alapértelmezett
 * kódlapjától — különben a {@code ÷}, {@code Ú} és az ékezetek elromlanak, és a
 * {@code ÷} épp a közös szótag {@code ?} jelére esne vissza.
 */
public final class KalliopeCli {

    private KalliopeCli() {}

    public static void main(String[] args) throws IOException {
        PrintStream out = new PrintStream(System.out, true, StandardCharsets.UTF_8);
        run(args, System.in, out);
    }

    /** A tényleges belépési pont; a kimenet és a bemenet átadható, hogy tesztelhető legyen. */
    static void run(String[] args, InputStream in, PrintStream out) throws IOException {
        if (args.length > 0 && "--canon".equals(args[0])) {
            printCanon(out);
            return;
        }
        if (args.length == 0) {
            out.println("Kalliopé — magyar időmértékes verselés és rímképlet elemzője");
            out.println("Eredet: " + MetricCanon.ORIGIN_VERSION + " (kánon lezárva: " + MetricCanon.CANON_CLOSED + ")");
            for (Examples e : Examples.ALL) {
                out.println();
                out.println("=== " + e.title() + " — " + e.author());
                out.println("    várt forma: " + e.expected());
                print(Analyzer.analyze(e.text()), out);
            }
            return;
        }
        String poem = "-".equals(args[0])
                ? new String(in.readAllBytes(), StandardCharsets.UTF_8)
                : Files.readString(Path.of(args[0]), StandardCharsets.UTF_8);
        print(Analyzer.analyze(poem), out);
    }

    static void print(Analysis analysis, PrintStream out) {
        for (Analysis.Stanza stanza : analysis.stanzas()) {
            if (analysis.stanzas().size() > 1) {
                out.printf("  -- %d. szakasz%n", stanza.index() + 1);
            }
            for (Analysis.Line line : stanza.lines()) {
                String meters = line.meters().isEmpty()
                        ? ""
                        : "= "
                                + String.join(
                                        " ~ ",
                                        line.meters().stream()
                                                .map(m -> m.meter().name())
                                                .toList());
                out.printf("  [%-2s] %-52s %-20s %s%n", line.rhymeLabel(), line.text(), line.scansion(), meters);
                if (line.ictusRow() != null) {
                    out.printf("       %-52s %s%n", "", line.ictusRow());
                }
            }
            out.println("  rímképlet: " + stanza.rhymePattern());
            for (MeterMatcher.StanzaMatch f : stanza.forms()) {
                String rep = f.repetitions() > 1 ? " ×" + f.repetitions() : "";
                String rhyme = f.form().hasRhymeScheme()
                        ? (f.rhymeSchemeMatches() ? " (rímképlet egyezik)" : " (a rímképlet eltér)")
                        : "";
                out.println("  szakaszmérték: " + f.form().name() + rep + rhyme);
            }
        }
        Analysis.Summary s = analysis.summary();
        out.printf("  összesen: %d szakasz, %d sor, %d szótag%n", s.stanzaCount(), s.lineCount(), s.syllableCount());
    }

    private static void printCanon(PrintStream out) {
        out.printf(
                "verslábak %d | kolónok %d | sorfajták %d | összetett %d | szakaszmértékek %d%n",
                MetricCanon.FEET.size(),
                MetricCanon.COLA.size(),
                MetricCanon.LINES.size(),
                MetricCanon.COMPLEXES.size(),
                MetricCanon.STANZAS.size());
        List<Meter> all = MetricCanon.ALL_METERS;
        for (Meter m : all) {
            out.printf("  %-10s %-38s %s%n", m.kind(), m.pattern(), m.name());
            if (m.corrected()) {
                out.printf("             javítva, eredeti: %s%n", m.correction().original());
            }
        }
    }
}
