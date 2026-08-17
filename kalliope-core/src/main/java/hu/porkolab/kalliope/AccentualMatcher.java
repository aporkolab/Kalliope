package hu.porkolab.kalliope;

import java.util.ArrayList;
import java.util.List;

/**
 * Ütemhangsúlyos sorillesztés.
 *
 * <p>Két dolgot mérünk, és a kettőt nem keverjük össze:
 *
 * <ul>
 *   <li><b>szótagszám</b> — ez dönti el, milyen sorfajta jöhet szóba;
 *   <li><b>a metszet és az ütemhatárok helye</b> — az ütem eleje a magyarban
 *       szóhatár, mert a hangsúly a szó első szótagjára esik.
 * </ul>
 *
 * <p>A szóhatár-egyezés nem feltétel, hanem <b>minőség</b>. Zrínyi felező
 * tizenkettesei híresen átvágják a metszetet („Fegyvert, s vitézt éne|klek”),
 * és attól még felező tizenkettesek. Aki emiatt elutasítaná a sort, az téved —
 * de aki elhallgatja, hogy a metszet szóba esik, az is.
 */
public final class AccentualMatcher {

    /**
     * @param wordBoundaryMeasures hány ütemhatár esik valódi szóhatárra
     * @param caesuraOnWordBoundary a FŐ sormetszet szóhatárra esik-e
     */
    public record Match(
            AccentualForm form, int wordBoundaryMeasures, boolean caesuraOnWordBoundary, boolean pure, String quality) {

        /** Az emberi nyelvű minősítést is eltároljuk, hogy a REST-válaszban is ott legyen. */
        static Match of(AccentualForm form, int wordBoundaryMeasures, boolean caesuraOnWordBoundary, boolean pure) {
            String quality;
            if (pure) {
                quality = "tiszta ütemtagolás";
            } else if (caesuraOnWordBoundary) {
                quality = "a metszet szóhatáron van, de nem minden ütemhatár";
            } else {
                quality = "a metszet szóba esik";
            }
            return new Match(form, wordBoundaryMeasures, caesuraOnWordBoundary, pure, quality);
        }
    }

    private AccentualMatcher() {}

    /**
     * A sorra illeszkedő ütemhangsúlyos formák, legjobb minőség elöl.
     *
     * @param syllables a sor szótagjai (a szóindexük adja a szóhatárokat)
     */
    public static List<Match> match(List<Scansion.Syllable> syllables) {
        if (syllables.isEmpty()) {
            return List.of();
        }
        boolean[] wordStart = wordStarts(syllables);
        List<Match> hits = new ArrayList<>();
        for (AccentualForm form : AccentualCanon.bySyllableCount(syllables.size())) {
            int onBoundary = 0;
            for (int start : form.measureStarts()) {
                if (start > 0 && start < wordStart.length && wordStart[start]) {
                    onBoundary++;
                }
            }
            int caesura = form.caesuraSyllable();
            boolean caesuraOk = caesura > 0 && caesura < wordStart.length && wordStart[caesura];
            boolean pure = onBoundary == form.measures().size() - 1;
            hits.add(Match.of(form, onBoundary, caesuraOk, pure));
        }
        hits.sort((a, b) -> {
            if (a.pure() != b.pure()) {
                return a.pure() ? -1 : 1;
            }
            if (a.caesuraOnWordBoundary() != b.caesuraOnWordBoundary()) {
                return a.caesuraOnWordBoundary() ? -1 : 1;
            }
            if (a.wordBoundaryMeasures() != b.wordBoundaryMeasures()) {
                return Integer.compare(b.wordBoundaryMeasures(), a.wordBoundaryMeasures());
            }
            // döntetlennél a kánonban előbb álló, idiomatikusabb forma nyer:
            // egy tizenkét szótagos magyar sor elsősorban felező tizenkettes
            return Integer.compare(AccentualCanon.ALL.indexOf(a.form()), AccentualCanon.ALL.indexOf(b.form()));
        });
        return List.copyOf(hits);
    }

    /** Milyen erős a szakasz ütemhangsúlyos rendje. */
    public enum Strength {
        /** A sorok legalább háromnegyedében a metszet valódi szóhatárra esik. */
        TISZTA,
        /** A szótagszám stimmel, de a metszet gyakran szóba esik — így verselt Zrínyi. */
        LAZA,
        /** Nincs uralkodó ütemhangsúlyos forma. */
        NINCS
    }

    /** A szakasz uralkodó ütemhangsúlyos sorfajtája és annak erőssége. */
    public record Dominant(AccentualForm form, Strength strength, int cleanLines) {}

    /**
     * A szakasz uralkodó ütemhangsúlyos sorfajtája.
     *
     * <p>A szótagszámnak a sorok legalább háromnegyedére illenie kell. Ha ezen
     * felül a metszet is a sorok háromnegyedében szóhatárra esik, a rend
     * <b>tiszta</b>; ha nem, <b>laza</b> — ez utóbbi nem hiba, hanem stílus:
     * Zrínyi felező tizenkettesei híresen átvágják a metszetet.
     *
     * <p>A puszta szótagszám-egyezésre azért nem hagyatkozunk, mert egy
     * tizennégy szótagos hexametersor is „kiadna" felező tizennégyest.
     *
     * @param perLine soronként a lehetséges illesztések (az első a legjobb)
     */
    public static Dominant dominant(List<List<Match>> perLine) {
        if (perLine.isEmpty()) {
            return new Dominant(null, Strength.NINCS, 0);
        }
        java.util.Map<AccentualForm, Integer> hits = new java.util.LinkedHashMap<>();
        java.util.Map<AccentualForm, Integer> clean = new java.util.LinkedHashMap<>();
        for (List<Match> line : perLine) {
            java.util.Set<AccentualForm> seen = new java.util.HashSet<>();
            for (Match m : line) {
                if (!seen.add(m.form())) {
                    continue;
                }
                hits.merge(m.form(), 1, (a, b) -> a + b);
                if (m.caesuraOnWordBoundary() || m.pure()) {
                    clean.merge(m.form(), 1, (a, b) -> a + b);
                }
            }
        }
        AccentualForm best = null;
        for (var entry : hits.entrySet()) {
            AccentualForm form = entry.getKey();
            if (entry.getValue() * 4 < perLine.size() * 3) {
                continue;
            }
            if (best == null
                    || entry.getValue() > hits.get(best)
                    || (entry.getValue().equals(hits.get(best))
                            && AccentualCanon.ALL.indexOf(form) < AccentualCanon.ALL.indexOf(best))) {
                best = form;
            }
        }
        if (best == null) {
            return new Dominant(null, Strength.NINCS, 0);
        }
        int cleanCount = clean.getOrDefault(best, 0);
        Strength strength = cleanCount * 4 >= perLine.size() * 3 ? Strength.TISZTA : Strength.LAZA;
        return new Dominant(best, strength, cleanCount);
    }

    /** Hány sorban esik az adott forma metszete valódi szóhatárra. */
    public static int cleanCaesuraLines(List<List<Match>> perLine, AccentualForm form) {
        if (form == null) {
            return 0;
        }
        int n = 0;
        for (List<Match> line : perLine) {
            for (Match m : line) {
                if (m.form().equals(form) && (m.caesuraOnWordBoundary() || m.pure())) {
                    n++;
                    break;
                }
            }
        }
        return n;
    }

    /** Mely szótagindexeken kezdődik új szó. */
    static boolean[] wordStarts(List<Scansion.Syllable> syllables) {
        boolean[] starts = new boolean[syllables.size()];
        for (int i = 0; i < syllables.size(); i++) {
            starts[i] = i == 0
                    || syllables.get(i).wordIndex() != syllables.get(i - 1).wordIndex();
        }
        return starts;
    }
}
