package de.scribery.core;

import java.util.ArrayList;
import java.util.List;

/** Teilt die Woerter eines Fragments in Zeilen, die in die Setzform passen. */
public final class LineBreaker {
    private LineBreaker() {}

    /**
     * Fuellt Zeilen in Textreihenfolge, solange Woerter plus je eine Leerstelle in {@code slots} passen
     * und die Zeile hoechstens {@code maxWords} Woerter hat.
     *
     * @param widths Breite jedes Wortes in Slots
     * @return pro Zeile die Indizes der Woerter
     * @throws IllegalArgumentException wenn ein Wort breiter als eine Zeile ist
     */
    public static List<List<Integer>> breakLines(List<Integer> widths, int slots, int maxWords) {
        List<List<Integer>> lines = new ArrayList<>();
        List<Integer> current = new ArrayList<>();
        int used = 0;
        for (int i = 0; i < widths.size(); i++) {
            int len = widths.get(i);
            if (len > slots) {
                throw new IllegalArgumentException("Wort " + i + " passt nicht in eine Zeile");
            }
            int needed = current.isEmpty() ? len : used + 1 + len;
            if (needed > slots || current.size() >= maxWords) {
                lines.add(current);
                current = new ArrayList<>();
                needed = len;
            }
            current.add(i);
            used = needed;
        }
        if (!current.isEmpty()) lines.add(current);
        return lines;
    }
}
