package hu.porkolab.kalliope;

import java.util.List;

/**
 * Egy versmérték: versláb, kolón, sorfajta vagy összetett sor.
 *
 * <p>A minta {@link Notation} jelöléssel: {@code U} rövid, {@code -} hosszú,
 * {@code ?} közömbös szótaghelyzet, {@code =} feloldás, {@code |} lábhatár,
 * {@code ||} cezúra.
 */
public record Meter(
        String id, String name, String pattern, Kind kind, boolean fictive, String note, Correction correction) {

    public enum Kind {
        /** Versláb — önmagában is lehetne sor. */
        FOOT,
        /** Kolón — sornál kisebb, zárt egység. */
        COLON,
        /** Sorfajta. */
        LINE,
        /** Összetett sor: több mérték egymás után fűzve. */
        COMPLEX
    }

    /**
     * Egy javítás a 2006-os eredetihez képest. A felület ezt kiírja, hogy
     * látszódjon: hol tértünk el az eredeti adattól, és min alapul.
     */
    public record Correction(String original, String reason, String source) {}

    public Meter {
        if (Notation.symbolsOnly(pattern).isEmpty()) {
            throw new IllegalArgumentException("Üres minta: " + name);
        }
    }

    /** A minta legrövidebb, illetve leghosszabb realizációjának szótagszáma. */
    public int minSyllables() {
        return Notation.minSyllables(pattern);
    }

    public int maxSyllables() {
        return Notation.maxSyllables(pattern);
    }

    /** Verslábakra tagolt minta a megjelenítéshez. */
    public List<String> feet() {
        return List.copyOf(Strings.splitRuns(pattern, '|'));
    }

    public boolean corrected() {
        return correction != null;
    }
}
