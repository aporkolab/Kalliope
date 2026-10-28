package hu.porkolab.kalliope;

import java.util.List;

/**
 * Szóelem-határ egy kétjegyűnek látszó betűpár közepén.
 *
 * <p>A kétjegyű betűk felismerése írásképi, ezért a szóelem-határon álló
 * {@code z+s}, {@code s+z}, {@code c+s} és {@code d+z} egy hangnak látszik
 * ({@code köz|ség}, {@code kékes|zöld}, {@code malac|sült}, {@code vad|zab}).
 * Rövid magánhangzó után ez a skandálást is elrontja: a szótag helyzeténél fogva
 * hosszú volna, a tokenizáló viszont egy mássalhangzót lát. A határ két forrásból
 * ismert:
 *
 * <ul>
 *   <li><b>képzőhatár</b>: {@code z} vagy {@code c} végű tő és a -ság/-ség képző
 *       ({@code igaz|ság}, {@code köz|ség}, {@code rác|ság}) — szabályként, tehát
 *       ragozott alakban is;
 *   <li><b>összetétel</b>: {@link #FIRST_ELEMENTS} — kézzel átnézett előtagok.
 * </ul>
 *
 * <p>Mindkettőt a huhyphn elválasztási mintái ellen mértük egy 3,1 millió
 * szóalakos gyakorisági listán (OpenSubtitles 2018): a képzőszabály 2706
 * szóalakon fut, és csak elírásokon ({@code igazség}, {@code kölcség}) vagy a
 * minta saját tévedésein ({@code kukac|ság}, {@code nyápic|ság}) tér el tőle; az
 * előtagoknál ugyanez a helyzet. A minta maga nem került a motorba: idegen neveken
 * téved (Argoszölő, Vaszilij), és a böngészős változatot is megnövelné.
 */
final class Morphology {

    /**
     * Összetételek előtagjai, amelyek utolsó betűje a következő szóelem első
     * betűjével hamis kétjegyűt alkotna. A {@code ^} jelű csak szó elején számít:
     * a {@code perec} benne van a {@code pipere|csomag}-ban, a {@code vad} a
     * {@code nyamvad}-ban, a {@code köz} sok szó belsejében.
     *
     * <p>Ami kimaradt, mert a gyakorisági listán ütközött: {@code leves}
     * ({@code le|veszem}), {@code pénzes} ({@code pénz|eszköz}). Általános
     * {@code z|sz} szabály sem lehet: a {@code száz|szor} mellett ott a
     * {@code varázs|zene} és a {@code rizs|zabáló}.
     */
    static final List<String> FIRST_ELEMENTS = List.of(
            // c|s, z|s
            "malac",
            "lazac",
            "^perec",
            "száraz",
            "^köz",
            // s|z
            "ritmus",
            "alvás",
            "tejes",
            "szemetes",
            "vizes",
            "kékes",
            "világos",
            "haragos",
            "sárgás",
            "vegyes",
            "mellkas",
            "vörös",
            "mágnes",
            "típus",
            "kórus",
            "blues",
            // d|z — a skandálást nem érinti (dz és d+z egyaránt két pozíció), csak a szótagolást
            "^vad");

    private Morphology() {}

    /** Igaz, ha a {@code [start, start + length)} betűsor belsejében szóelem-határ áll. */
    static boolean splits(String text, int start, int length) {
        for (int p = start + 1; p < start + length; p++) {
            if (boundaryBefore(text, p)) {
                return true;
            }
        }
        return false;
    }

    /** Igaz, ha a {@code p}-edik betű előtt szóelem-határ áll. */
    static boolean boundaryBefore(String text, int p) {
        return suffixBoundary(text, p) || afterFirstElement(text, p);
    }

    /** {@code z}/{@code c} végű tő + -ság/-ség: igaz|ság, köz|ség, rác|ság — ragozva is. */
    private static boolean suffixBoundary(String t, int p) {
        if (p < 2 || p + 2 >= t.length() || t.charAt(p) != 's') {
            return false;
        }
        char stemEnd = t.charAt(p - 1);
        char before = t.charAt(p - 2);
        if ((stemEnd != 'z' && stemEnd != 'c') || !Character.isLetter(before)) {
            return false;
        }
        char vowel = t.charAt(p + 1);
        if ((vowel != 'á' && vowel != 'é') || t.charAt(p + 2) != 'g') {
            return false;
        }
        // az ágy összetétel valódi zs-t hagy: masszázs|ágy, rizs|ágyon
        if (p + 3 < t.length() && t.charAt(p + 3) == 'y') {
            return false;
        }
        // a kettőzött ccs és zzs, meg a dzs egy hang: naccsága, neveccséges
        return stemEnd == 'c' ? before != 'c' : before != 'z' && before != 'd';
    }

    private static boolean afterFirstElement(String t, int p) {
        for (String entry : FIRST_ELEMENTS) {
            boolean anchored = entry.charAt(0) == '^';
            String element = anchored ? entry.substring(1) : entry;
            int start = p - element.length();
            if (start >= 0
                    && t.regionMatches(start, element, 0, element.length())
                    && (!anchored || start == 0 || !Character.isLetter(t.charAt(start - 1)))) {
                return true;
            }
        }
        return false;
    }
}
