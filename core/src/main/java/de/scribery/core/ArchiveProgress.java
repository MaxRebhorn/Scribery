package de.scribery.core;

/**
 * Was ein Spieler zu einem Manuskript im Archiv eingetragen hat: ob das Fragment gefunden ist, und welche Boegen
 * (Seiten des Manuskripts) gedruckt und uebersetzt sind. Die Boegen sind Bits in zwei Zahlen, Bogen 0 ist das niedrigste Bit.
 *
 * @param found      das Fragment (griechischer Urtext) ist im Archiv
 * @param printed    Bit n gesetzt: der gedruckte Bogen n (lateinische Umschrift) ist im Archiv
 * @param translated Bit n gesetzt: der uebersetzte Bogen n (englische Interlinear-Uebersetzung) ist im Archiv
 */
public record ArchiveProgress(boolean found, long printed, long translated) {
    public static final ArchiveProgress NONE = new ArchiveProgress(false, 0L, 0L);
    /** So viele Boegen passen in ein Bitfeld; kein Manuskript hat mehr (siehe Test). */
    public static final int MAX_PAGES = 63;

    public boolean isPrinted(int page) {
        return bit(printed, page);
    }

    public boolean isTranslated(int page) {
        return bit(translated, page);
    }

    public ArchiveProgress withFound() {
        return new ArchiveProgress(true, printed, translated);
    }

    public ArchiveProgress withPrinted(int page) {
        return new ArchiveProgress(found, set(printed, page), translated);
    }

    public ArchiveProgress withTranslated(int page) {
        return new ArchiveProgress(found, printed, set(translated, page));
    }

    public boolean isEmpty() {
        return !found && printed == 0L && translated == 0L;
    }

    private static boolean bit(long mask, int page) {
        return page >= 0 && page < MAX_PAGES && ((mask >>> page) & 1L) != 0L;
    }

    private static long set(long mask, int page) {
        if (page < 0 || page >= MAX_PAGES) throw new IllegalArgumentException("Bogen " + page + " ausserhalb von 0.." + (MAX_PAGES - 1));
        return mask | (1L << page);
    }
}
