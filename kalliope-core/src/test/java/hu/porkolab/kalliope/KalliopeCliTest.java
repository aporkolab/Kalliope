package hu.porkolab.kalliope;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class KalliopeCliTest {

    private static final InputStream NO_INPUT = new ByteArrayInputStream(new byte[0]);

    private static String run(String... args) throws IOException {
        return run(NO_INPUT, args);
    }

    private static String run(InputStream in, String... args) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        KalliopeCli.run(args, in, new PrintStream(buffer, true, StandardCharsets.UTF_8));
        return buffer.toString(StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("argumentum nélkül a teljes példatárat elemzi")
    void noArgsAnalysesTheCorpus() throws Exception {
        String out = run();
        assertThat(out).contains("Kalliopé", MetricCanon.ORIGIN_VERSION);
        for (Examples e : Examples.ALL) {
            assertThat(out).as("%s a kimenetben", e.title()).contains(e.title());
        }
        assertThat(out).contains("szakaszmérték: alkaioszi strófa");
        assertThat(out).contains("rímképlet: aaaa");
    }

    @Test
    @DisplayName("--canon kiírja a teljes kánont a javításokkal")
    void canonIsPrinted() throws Exception {
        String out = run("--canon");
        assertThat(out).contains("verslábak 11", "kolónok 38");
        assertThat(out).contains("hexameter", "choliambus");
        assertThat(out).contains("javítva, eredeti: ?-U-?-U-U-U?");
    }

    @Test
    @DisplayName("fájlból olvas")
    void readsFromFile(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("vers.txt");
        Files.writeString(file, Examples.MAGYAROKHOZ.text(), StandardCharsets.UTF_8);
        String out = run(file.toString());
        assertThat(out).contains("szakaszmérték: alkaioszi strófa");
        assertThat(out).contains("összesen: 1 szakasz, 4 sor");
    }

    @Test
    @DisplayName("a '-' a szabvány bemenetet olvassa, UTF-8-cal")
    void readsFromStandardInput() throws Exception {
        InputStream in = new ByteArrayInputStream(Examples.NAGY_TITOK.text().getBytes(StandardCharsets.UTF_8));
        String out = run(in, "-");
        assertThat(out).contains("szakaszmérték: disztichon");
        assertThat(out).contains("Jót s jól!");
    }

    @Test
    @DisplayName("többszakaszos verset szakaszonként ír ki")
    void multiStanzaOutputIsNumbered(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("ket-szakasz.txt");
        Files.writeString(file, Examples.MAGYAROKHOZ.text() + "\n\n" + Examples.HORAC.text(), StandardCharsets.UTF_8);
        String out = run(file.toString());
        assertThat(out).contains("-- 1. szakasz", "-- 2. szakasz");
        assertThat(out).contains("összesen: 5 szakasz, 20 sor");
    }

    @Test
    @DisplayName("bekapcsolt ütemhangsúllyal kiírja az iktussort")
    void ictusRowIsPrinted() {
        Settings ictus = MetricCanon.DEFAULT_SETTINGS.with(Map.of(Settings.SHOW_ICTUS, true));
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        KalliopeCli.print(
                Analyzer.analyze(Examples.ZALAN.text(), ictus), new PrintStream(buffer, true, StandardCharsets.UTF_8));
        assertThat(buffer.toString(StandardCharsets.UTF_8)).containsAnyOf("÷", "Ú");
    }

    @Test
    @DisplayName("nemlétező fájlra beszédes hibát ad")
    void missingFileFails() {
        assertThrows(IOException.class, () -> run("nincs-ilyen-fajl.txt"));
    }
}
