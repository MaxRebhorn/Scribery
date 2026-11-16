package de.scribery.client;

/**
 * Welche Stationen der Spieler gerade erreicht: das Pult, in dessen Naehe die Presse und der
 * Uebersetzungstisch stehen. Die Reihenfolge der Kinds ist die Reihenfolge der Pfeile.
 */
record Stations(boolean lectern, boolean press, boolean cabinet, boolean desk) {
    enum Kind { LECTERN, PRESS, DESK, MINT }

    /** Presse und Lettern-Schrank teilen sich eine Oberflaeche: PRESS ist die Station, CABINET gibt es nur als Teil davon. */
    /** /scribery debug: Presse und Uebersetzung ohne Pult. */
    /** Eine Station allein, ohne Nachbarn. */
    static final Stations NONE = new Stations(false, false, false, false);

    static final Stations DEBUG = new Stations(false, true, true, true);

    boolean has(Kind k) {
        return switch (k) {
            case LECTERN -> lectern;
            case PRESS -> press || cabinet;
            case DESK -> desk;
            case MINT -> false;
        };
    }
}
