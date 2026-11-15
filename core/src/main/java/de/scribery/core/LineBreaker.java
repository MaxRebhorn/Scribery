package de.scribery.core;

import java.util.ArrayList;
import java.util.List;

/** Teilt die Woerter eines Fragments in Zeilen, die in die Setzform passen. */
public final class LineBreaker {
    private LineBreaker() {}

    /**
     * Fuellt Zeilen in Textreihenfolge, solange Woerter plus je eine Leerstelle in {@code slots} passen.
     *
     * @return pro Zeile die Indizes der Woerter
     * @throws IllegalArgumentException wenn ein Wort laenger als eine Zeile ist
     */
    public static List<List<Integer>> breakLines(List<String> words, int slots) {
        List<List<Integer>> lines = new ArrayList<>();
        List<Integer> current = new ArrayList<>();
        int used = 0;
        for (int i = 0; i < words.size(); i++) {
            int len = words.get(i).length();
            if (len > slots) {
                throw new IllegalArgumentException("Wort passt nicht in eine Zeile: " + words.get(i));
            }
            int needed = current.isEmpty() ? len : used + 1 + len;
            if (needed > slots) {
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
