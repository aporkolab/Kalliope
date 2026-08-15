package hu.porkolab.kalliope;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A vers verselésének összegzése — egy mondatban és néhány pontban, úgy, ahogy
 * egy verstani jegyzet fogalmazna: „Időmértékes verselés: disztichonok."
 *
 * <p>Az egyes sorok adatai önmagukban nem mondják meg, milyen vers ez. Az
 * ítélethez arányok kellenek: hány sor illeszkedik klasszikus mértékre, van-e
 * uralkodó ütemhangsúlyos forma, és a kettő együtt áll-e.
 *
 * <p>A verselési rendszerek elnevezése a magyar verstan szerint (Arany Jánosnál
 * az ütemhangsúlyos verselés neve „magyar nemzeti versidom”).
 */
public record VerseSummary(System system, String headline, List<String> details) {

    public VerseSummary {
        details = List.copyOf(details);
    }

    /** A vers ritmusrendje. */
    public enum System {
        IDOMERTEKES("időmértékes verselés"),
        UTEMHANGSULYOS("ütemhangsúlyos (magyaros) verselés"),
        SZIMULTAN("szimultán verselés"),
        VEGYES("vegyes ritmusú vers"),
        SZABAD("szabályos ritmusrendet nem mutat");

        private final String hungarian;

        System(String hungarian) {
            this.hungarian = hungarian;
        }

        public String label() {
            return hungarian;
        }
    }

    /** Ekkora arány fölött mondjuk azt, hogy a vers egésze követi a rendet. */
    private static final double DOMINANT = 0.75;

    static VerseSummary of(Analysis analysis) {
        List<Analysis.Stanza> stanzas = analysis.stanzas();
        if (stanzas.isEmpty()) {
            return new VerseSummary(System.SZABAD, "Nincs elemezhető szöveg.", List.of());
        }
        int lines = 0;
        int matched = 0;
        int synizesis = 0;
        Map<String, Integer> meterCounts = new LinkedHashMap<>();
        Map<String, Integer> formCounts = new LinkedHashMap<>();
        Map<String, Integer> accentualCounts = new LinkedHashMap<>();
        Map<String, Integer> rhymeKinds = new LinkedHashMap<>();
        int dual = 0;

        for (Analysis.Stanza stanza : stanzas) {
            for (Analysis.Line line : stanza.lines()) {
                lines++;
                if (line.matched()) {
                    matched++;
                    meterCounts.merge(line.meters().get(0).meter().name(), 1, Integer::sum);
                }
                if (line.synizesis()) {
                    synizesis++;
                }
                if (line.rhymeKind() != RhymeDetector.Kind.VAKSOR) {
                    rhymeKinds.merge(line.rhymeKind().explanation(), 1, Integer::sum);
                }
            }
            for (MeterMatcher.StanzaMatch f : stanza.forms()) {
                formCounts.merge(f.form().name(), 1, Integer::sum);
            }
            if (stanza.accentual().form() != null) {
                accentualCounts.merge(stanza.accentual().form().name(), 1, Integer::sum);
            }
            if (stanza.dualRhythm()) {
                dual++;
            }
        }

        double classicalRatio = lines == 0 ? 0 : (double) matched / lines;
        String topMeter = top(meterCounts);
        String topForm = topForm(formCounts, stanzas.size());
        String topAccentual = top(accentualCounts);
        // A laza metszet is ütemhangsúlyos rend: Zrínyi felező tizenkettesei
        // híresen átvágják a metszetet, attól még magyaros verselésűek.
        boolean accentualDominant =
                topAccentual != null && accentualCounts.get(topAccentual) >= stanzas.size() * DOMINANT;

        System system;
        if (dual >= stanzas.size() * DOMINANT) {
            system = System.SZIMULTAN;
        } else if (classicalRatio >= DOMINANT) {
            system = System.IDOMERTEKES;
        } else if (accentualDominant && classicalRatio < 0.4) {
            system = System.UTEMHANGSULYOS;
        } else if (classicalRatio > 0.4 || topAccentual != null) {
            system = System.VEGYES;
        } else {
            system = System.SZABAD;
        }

        return new VerseSummary(
                system,
                headline(system, topForm, topMeter, topAccentual),
                details(system, stanzas, lines, matched, synizesis, topForm, meterCounts, rhymeKinds));
    }

    private static String headline(System system, String form, String meter, String accentual) {
        String what = form != null ? form : meter;
        return switch (system) {
            case SZIMULTAN ->
                what == null || accentual == null
                        ? "Szimultán verselés: időmértékes és ütemhangsúlyos rend egyszerre."
                        : "Szimultán verselés: " + describe(what) + ", " + accentual + " ütemtagolással.";
            case IDOMERTEKES ->
                what == null ? "Időmértékes verselés." : "Időmértékes verselés: " + describe(what) + ".";
            case UTEMHANGSULYOS ->
                accentual == null
                        ? "Ütemhangsúlyos (magyaros) verselés."
                        : "Ütemhangsúlyos (magyaros) verselés: " + accentual + ".";
            case VEGYES -> "Vegyes ritmusú vers: a sorok egy része követ szabályos rendet, más része nem.";
            case SZABAD -> "A vers nem mutat szabályos ritmusrendet — szabadvers vagy próza.";
        };
    }

    /** „disztichon" → „disztichonok"; a strófanevet változatlanul hagyjuk. */
    private static String describe(String what) {
        if (what.equals("disztichon")) {
            return "disztichonok";
        }
        if (what.endsWith("strófa") || what.startsWith("aszklepiadeszi")) {
            return what;
        }
        return plural(what);
    }

    private static String plural(String what) {
        return switch (what) {
            case "hexameter" -> "hexameterek";
            case "pentameter" -> "pentameterek";
            case "disztichon" -> "disztichonok";
            default -> what;
        };
    }

    private static List<String> details(
            System system,
            List<Analysis.Stanza> stanzas,
            int lines,
            int matched,
            int synizesis,
            String topForm,
            Map<String, Integer> meterCounts,
            Map<String, Integer> rhymeKinds) {
        List<String> out = new ArrayList<>();
        out.add(structure(stanzas, lines));
        if (topForm != null) {
            out.add("Szakaszmérték: " + topForm + ".");
        }
        if (!meterCounts.isEmpty()) {
            out.add("Sorfajták: " + join(meterCounts) + ".");
        }
        Analysis.Stanza first = stanzas.get(0);
        if (first.accentual().form() != null) {
            AccentualForm form = first.accentual().form();
            out.add("Ütemtagolás: " + form.name() + " (" + form.division() + "), "
                    + (first.accentual().strength() == AccentualMatcher.Strength.TISZTA
                            ? "a metszet a szóhatáron van"
                            : "a metszet gyakran szóba esik")
                    + ".");
        }
        out.add(rhyme(stanzas, rhymeKinds));
        boolean classicalExpected =
                system == System.IDOMERTEKES || system == System.SZIMULTAN || system == System.VEGYES;
        if (classicalExpected && matched < lines) {
            out.add((lines - matched) + " sor nem illeszkedik klasszikus mértékre "
                    + "(költői licencia); a soronkénti magyarázat megmondja, min múlik.");
        }
        if (synizesis > 0) {
            out.add(synizesis + " sor csak összevont kettőshangzóval illeszkedik "
                    + "(Európa, Zeusz, Péleidész típusú nevek).");
        }
        int caesuraLines = 0;
        for (Analysis.Stanza stanza : stanzas) {
            for (Analysis.Line line : stanza.lines()) {
                if (!line.caesurae().isEmpty()) {
                    caesuraLines++;
                }
            }
        }
        if (caesuraLines > 0) {
            out.add("Sormetszet " + caesuraLines + " sorban mutatható ki.");
        }
        return out;
    }

    private static String structure(List<Analysis.Stanza> stanzas, int lines) {
        if (stanzas.size() == 1) {
            return "Egyetlen, " + lines + " soros szakasz.";
        }
        int firstSize = stanzas.get(0).lines().size();
        boolean even = stanzas.stream().allMatch(s -> s.lines().size() == firstSize);
        return even
                ? stanzas.size() + " szakasz, egyenként " + firstSize + " sor (összesen " + lines + ")."
                : stanzas.size() + " szakasz, összesen " + lines + " sor.";
    }

    private static String rhyme(List<Analysis.Stanza> stanzas, Map<String, Integer> rhymeKinds) {
        // A leggyakoribb képlet, nem az elsőé: egy hatszakaszos versben az első
        // szakasz lehet rímtelen, miközben a többi rímel.
        Map<String, Integer> patterns = new LinkedHashMap<>();
        for (Analysis.Stanza stanza : stanzas) {
            patterns.merge(stanza.rhymePattern(), 1, Integer::sum);
        }
        String pattern = top(patterns);
        String name = RhymeDetector.schemeName(pattern);
        if (rhymeKinds.isEmpty()) {
            return "Rímtelen.";
        }
        // Hosszú, szakaszokra nem tagolt szövegben a sorvégek közt akadnak
        // véletlen egybecsengések (főleg ragrímek). A rímelés viszont rövid,
        // ismétlődő képletben szervezi a verset — ha egy harmincnégy soros
        // tömbre nem áll ismert képlet, az nem rímes vers, és ne tegyünk úgy.
        if (name == null && pattern.length() > 8) {
            return "Rímtelen; a sorvégek közti egybecsengések esetlegesek " + "(többségük ragrím vagy asszonánc).";
        }
        StringBuilder sb = new StringBuilder("Rímképlet: ").append(pattern);
        if (name != null) {
            sb.append(" (").append(name).append(')');
        }
        sb.append(". A rímek: ").append(join(rhymeKinds)).append('.');
        return sb.toString();
    }

    private static String join(Map<String, Integer> counts) {
        List<String> parts = new ArrayList<>();
        counts.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .limit(4)
                .forEach(e -> parts.add(e.getKey() + " (" + e.getValue() + ")"));
        return String.join(", ", parts);
    }

    private static String top(Map<String, Integer> counts) {
        return counts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    /** Csak akkor nevezzük meg a szakaszmértéket, ha a szakaszok többségére áll. */
    private static String topForm(Map<String, Integer> counts, int stanzaCount) {
        return counts.entrySet().stream()
                .filter(e -> e.getValue() >= stanzaCount * DOMINANT)
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }
}
