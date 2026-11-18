package de.scribery.core;

import java.util.List;

/**
 * Regeln des Manuscript Archive: Das Evangelium steht als ein durchgehender Text in drei Spalten (griechischer Urtext,
 * lateinische Umschrift, englische Interlinear-Uebersetzung). Was der Spieler noch nicht eingetragen hat, bleibt eine
 * Luecke. Welche Woerter zu welchem Bogen gehoeren, folgt derselben Zeilenteilung wie in der Presse.
 */
public final class ArchiveModel {
    /** Die drei Spalten von links nach rechts. */
    public enum Column { GREEK, LATIN, ENGLISH }

    /**
     * Zusammenfassung des Fortschritts.
     *
     * @param manuscriptsFound    gefundene Manuskripte
     * @param manuscriptsTotal    alle Manuskripte
     * @param pagesPrinted        eingetragene gedruckte Boegen
     * @param pagesTranslated     eingetragene uebersetzte Boegen
     * @param pagesTotal          alle Boegen
     */
    public record Stats(int manuscriptsFound, int manuscriptsTotal, int pagesPrinted, int pagesTranslated, int pagesTotal) {}

    private ArchiveModel() {}

    /**
     * Zu welchem Bogen jedes Wort eines Manuskripts gehoert (Index = Wort, Wert = Bogen ab 0).
     *
     * @param latinLengths Laenge der lateinischen Umschrift jedes Wortes
     * @param lineSlots    Slots pro Zeile der Setzform
     * @param rows         Zeilen pro Bogen
     * @param maxWords     hoechstens so viele Woerter pro Zeile
     */
    public static int[] pageOfWord(List<Integer> latinLengths, int lineSlots, int rows, int maxWords) {
        int[] out = new int[latinLengths.size()];
        List<List<Integer>> lines = LineBreaker.breakLines(latinLengths, lineSlots, maxWords);
        for (int line = 0; line < lines.size(); line++) {
            for (int word : lines.get(line)) out[word] = line / rows;
        }
        return out;
    }

    /** Anzahl der Boegen eines Manuskripts: der hoechste Bogen plus eins, mindestens 1. */
    public static int pageCount(int[] pageOfWord) {
        int max = 0;
        for (int p : pageOfWord) max = Math.max(max, p);
        return pageOfWord.length == 0 ? 1 : max + 1;
    }

    /** Ist das Wort in dieser Spalte schon eingetragen? Sonst bleibt an seiner Stelle eine Luecke. */
    public static boolean known(ArchiveProgress progress, Column column, int page) {
        return switch (column) {
            case GREEK -> progress.found();
            case LATIN -> progress.isPrinted(page);
            case ENGLISH -> progress.isTranslated(page);
        };
    }

    /** Fortschritt ueber alle Manuskripte; beide Listen laufen gleich (ein Eintrag pro Manuskript). */
    public static Stats stats(List<Integer> pagesPerManuscript, List<ArchiveProgress> progress) {
        if (pagesPerManuscript.size() != progress.size()) throw new IllegalArgumentException("Listen verschieden lang");
        int found = 0, printed = 0, translated = 0, total = 0;
        for (int i = 0; i < progress.size(); i++) {
            ArchiveProgress p = progress.get(i);
            int pages = pagesPerManuscript.get(i);
            total += pages;
            if (p.found()) found++;
            for (int page = 0; page < pages; page++) {
                if (p.isPrinted(page)) printed++;
                if (p.isTranslated(page)) translated++;
            }
        }
        return new Stats(found, progress.size(), printed, translated, total);
    }
}
