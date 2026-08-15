package hu.porkolab.kalliope;

import java.util.List;

/**
 * Ütemhangsúlyos (magyaros) sorfajta: ütemek rögzített szótagszámmal.
 *
 * <p>Az ütemhangsúlyos verselés alapegysége az <b>ütem</b>, amelyet egy
 * hangsúlyos és néhány hangsúlytalan szótag alkot; a hangsúly a magyarban a szó
 * első szótagjára esik, ezért az ütem eleje rendszerint szóhatár. A
 * <b>sormetszet</b> a fő ütemhatár, a sor közepén.
 *
 * <p>Források: Sulinet Tudásbázis — Az ütemhangsúlyos sorfajok; Wikipédia —
 * Ütemhangsúlyos verselés.
 *
 * @param measures az ütemek szótagszáma sorrendben (pl. felező tizenkettes: 6, 6)
 * @param caesuraAfter hányadik ütem után áll a fő sormetszet; 0, ha nincs kitüntetett
 */
public record AccentualForm(String id, String name, List<Integer> measures, int caesuraAfter, String note) {

    public AccentualForm {
        if (measures == null || measures.isEmpty()) {
            throw new IllegalArgumentException("Üres ütemtagolás: " + name);
        }
        measures = List.copyOf(measures);
    }

    public int syllableCount() {
        int n = 0;
        for (int m : measures) {
            n += m;
        }
        return n;
    }

    /** Az ütemek kezdő szótagindexei (az első mindig 0). */
    public List<Integer> measureStarts() {
        List<Integer> starts = new java.util.ArrayList<>(measures.size());
        int at = 0;
        for (int m : measures) {
            starts.add(at);
            at += m;
        }
        return List.copyOf(starts);
    }

    /** A fő sormetszet szótagindexe, vagy -1, ha nincs. */
    public int caesuraSyllable() {
        if (caesuraAfter <= 0 || caesuraAfter >= measures.size()) {
            return -1;
        }
        int at = 0;
        for (int i = 0; i < caesuraAfter; i++) {
            at += measures.get(i);
        }
        return at;
    }

    /** Olvasható tagolás, pl. {@code 6 || 6} vagy {@code 4 | 4 | 3}. */
    public String division() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < measures.size(); i++) {
            if (i > 0) {
                sb.append(i == caesuraAfter ? " || " : " | ");
            }
            sb.append(measures.get(i));
        }
        return sb.toString();
    }
}
