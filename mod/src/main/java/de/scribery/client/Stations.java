package de.scribery.client;

/**
 * Welche Stationen der Spieler gerade erreicht: das Pult, in dessen Naehe die Presse und der
 * Uebersetzungstisch stehen. Die Reihenfolge der Kinds ist die Reihenfolge der Pfeile.
 */
record Stations(boolean lectern, boolean press, boolean desk) {
    enum Kind { LECTERN, PRESS, DESK }

    /** /scribery debug: Presse und Uebersetzung ohne Pult. */
    static final Stations DEBUG = new Stations(false, true, true);

    boolean has(Kind k) {
        return switch (k) {
            case LECTERN -> lectern;
            case PRESS -> press;
            case DESK -> desk;
        };
    }
}
