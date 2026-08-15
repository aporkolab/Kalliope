package hu.porkolab.kalliope;

import java.util.List;

/**
 * Szakaszmérték: sorfajták rögzített sorrendje, opcionálisan kötött rímképlettel.
 *
 * @param closed igaz, ha a forma valódi, üres sorokkal határolt strófa (az eredeti
 *     adatban a {@code #…#} jelölés). A nem zárt formák — mindenekelőtt a
 *     disztichon — ismétlődhetnek egy szakaszon belül, ezért az illesztő
 *     megengedi a többszörös előfordulást is.
 */
public record StanzaForm(String id, String name, List<Meter> lines, String rhymeScheme, boolean closed) {

    public StanzaForm {
        if (lines == null || lines.isEmpty()) {
            throw new IllegalArgumentException("Üres szakaszmérték: " + name);
        }
        lines = List.copyOf(lines);
    }

    public int lineCount() {
        return lines.size();
    }

    public boolean hasRhymeScheme() {
        return rhymeScheme != null && !rhymeScheme.isBlank();
    }
}
