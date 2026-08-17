package hu.porkolab.kalliope;

import java.util.ArrayList;
import java.util.List;

/**
 * Kézi szövegvágás, {@link String#split(String)} helyett.
 *
 * <p>A motor három helyen vágott reguláris kifejezéssel: soronként ({@code \R}),
 * szóközfutamonként ({@code " +"}) és lábhatáronként ({@code "\\|+"}). Egyik sem
 * igényel regexet, viszont a {@code String.split} mindháromnál felhúzza a
 * {@code java.util.regex} gépezetet — ami egyrészt minden hívásnál mintát
 * fordít, másrészt olyan JDK-belsőket ér el ({@code Character.UnicodeScript},
 * {@code Class.getCanonicalName}), amiket a böngészőbe fordító TeaVM nem
 * emulál. Enélkül a motor nem tud kliensoldalon futni.
 *
 * <p>A viselkedés <b>pontosan</b> a {@code String.split} viselkedése; ezt a
 * {@code StringsTest} a JDK kimenetéhez hasonlítva ellenőrzi.
 */
final class Strings {

    /** Függőleges tab — a {@code \R} ezt is sorvégnek veszi. */
    private static final char VERTICAL_TAB = 0x0B;

    /** NEL (next line). */
    private static final char NEL = 0x85;

    /** Unicode sorhatár. */
    private static final char LINE_SEPARATOR = 0x2028;

    /** Unicode bekezdéshatár. */
    private static final char PARAGRAPH_SEPARATOR = 0x2029;

    private Strings() {}

    /**
     * Sorokra vágás a {@code \R} (bármely sorvégjel) mentén, a záró üres
     * elemeket megtartva — vagyis {@code split("\\R", -1)}.
     *
     * <p>A {@code \R} a Windows-féle {@code \r\n} párt EGY sorvégnek veszi, a
     * magányos {@code \r}-t és {@code \n}-t, a függőleges tabot, a lapdobást, a
     * NEL-t és a Unicode sor-/bekezdéshatárt pedig szintén.
     */
    static List<String> lines(String text) {
        List<String> out = new ArrayList<>();
        int start = 0;
        int i = 0;
        while (i < text.length()) {
            int width = lineBreakWidth(text, i);
            if (width == 0) {
                i++;
                continue;
            }
            out.add(text.substring(start, i));
            i += width;
            start = i;
        }
        out.add(text.substring(start));
        return out;
    }

    /**
     * A sorvégjel hossza az adott pozíción, vagy 0, ha ott nincs sorvég. A
     * kódpontok konstansban vannak, nem karakterliterálban: a Java a
     * {@code \\uXXXX} escape-eket még a lexer előtt oldja fel, tehát
     * literálban félrevezetők volnának.
     */
    private static int lineBreakWidth(String text, int i) {
        char c = text.charAt(i);
        if (c == '\r') {
            return i + 1 < text.length() && text.charAt(i + 1) == '\n' ? 2 : 1;
        }
        return c == '\n'
                        || c == VERTICAL_TAB
                        || c == '\f'
                        || c == NEL
                        || c == LINE_SEPARATOR
                        || c == PARAGRAPH_SEPARATOR
                ? 1
                : 0;
    }

    /**
     * Vágás a megadott karakter futamai mentén — vagyis {@code split("x+")}.
     *
     * <p>Három részletben tér el a naiv megoldástól, és mind a három a
     * {@code String.split} viselkedése: üres bemenetre egyetlen üres elem jön,
     * a vezető határoló EGY üres elemet hagy a lista elején, a záró üres elemek
     * viszont lemaradnak.
     */
    static List<String> splitRuns(String text, char delimiter) {
        if (text.isEmpty()) {
            return List.of("");
        }
        List<String> out = new ArrayList<>();
        int i = 0;
        while (i < text.length()) {
            int start = i;
            while (i < text.length() && text.charAt(i) != delimiter) {
                i++;
            }
            out.add(text.substring(start, i));
            while (i < text.length() && text.charAt(i) == delimiter) {
                i++;
            }
        }
        while (!out.isEmpty() && out.get(out.size() - 1).isEmpty()) {
            out.remove(out.size() - 1);
        }
        return out;
    }
}
