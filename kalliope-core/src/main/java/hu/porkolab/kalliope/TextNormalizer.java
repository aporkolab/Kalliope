package hu.porkolab.kalliope;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Kiejtés szerinti előfeldolgozás — a {@code kalliope.exe} saját adata
 * ({@code FUN_00462a94}): rövidítések feloldása, magában álló mássalhangzók
 * betűnévvé bontása, és két zöngésségi hasonulás, ami megakadályozza, hogy a
 * szóösszetételi határon álló {@code z+s} kétjegyű betűnek látsszon
 * (igazság → igasság).
 *
 * <p>A központozás-lista szintén a binárisé (0x463114).
 */
public final class TextNormalizer {

    /** Skandálás előtt eltávolított írásjelek — a bináris saját listája. */
    public static final String PUNCTUATION = "?.,:\"[]()!-=+/\\{}<>'0123456789";

    /** Rövidítések kiejtése. */
    private static final Map<String, String> ABBREVIATIONS = Map.of("vc", "vécé", "tv", "tévé", "cd", "cédé");

    /** Magában álló mássalhangzók betűneve. */
    private static final Map<String, String> LETTER_NAMES = letterNames();

    /** Szóvégi zöngésségi hasonulások. */
    private static final Map<String, String> FINAL_REWRITES = Map.of("gazság", "gasság", "cság", "csség");

    private TextNormalizer() {}

    private static Map<String, String> letterNames() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("b", "bé");
        m.put("c", "cé");
        m.put("d", "dé");
        m.put("f", "eff");
        m.put("g", "gé");
        m.put("h", "há");
        m.put("j", "jé");
        m.put("k", "ká");
        m.put("l", "ell");
        m.put("m", "emm");
        m.put("n", "enn");
        m.put("p", "pé");
        m.put("q", "kú");
        m.put("r", "err");
        m.put("t", "té");
        m.put("v", "vé");
        m.put("x", "iksz");
        m.put("z", "zé");
        return Map.copyOf(m);
    }

    /**
     * Egy sor szavakra bontva, kiejtés szerint normalizálva.
     *
     * @param letterSyllables az {@code az_abece_betuinek_kulon_szotag} beállítás:
     *     ha hamis, a magában álló betűk NEM bomlanak betűnévre
     */
    public static List<String> words(String line, boolean letterSyllables) {
        if (line == null) {
            return List.of();
        }
        StringBuilder cleaned = new StringBuilder(line.length());
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (PUNCTUATION.indexOf(c) >= 0 || Character.isSpaceChar(c) || Character.isWhitespace(c)) {
                cleaned.append(' ');
            } else if (c == '​' || c == '﻿') { // zero-width space, BOM
                cleaned.append(' ');
            } else {
                cleaned.append(c);
            }
        }
        List<String> out = new ArrayList<>();
        for (String raw : cleaned.toString().split(" +")) {
            String w = Phonology.lower(raw).replace('w', 'v');
            w = keepLetters(w);
            if (w.isEmpty()) {
                continue;
            }
            String abbrev = ABBREVIATIONS.get(w);
            if (abbrev != null) {
                out.add(abbrev);
                continue;
            }
            if (letterSyllables) {
                String name = LETTER_NAMES.get(w);
                if (name != null) {
                    out.add(name);
                    continue;
                }
            }
            out.add(applyFinalRewrites(w));
        }
        return out;
    }

    private static String applyFinalRewrites(String word) {
        for (Map.Entry<String, String> e : FINAL_REWRITES.entrySet()) {
            if (word.endsWith(e.getKey())) {
                return word.substring(0, word.length() - e.getKey().length()) + e.getValue();
            }
        }
        return word;
    }

    private static String keepLetters(String w) {
        StringBuilder sb = new StringBuilder(w.length());
        for (int i = 0; i < w.length(); i++) {
            char c = w.charAt(i);
            if (Character.isLetter(c)) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /** A sor normalizált alakja egyetlen szövegként (a rímkulcshoz). */
    public static String normalized(String line, boolean letterSyllables) {
        return String.join(" ", words(line, letterSyllables));
    }
}
