package de.scribery.core;

/**
 * Alle Zahlen des Scribe Villagers an einer Stelle. Erste Schaetzung, ungetestet (siehe docs/DESIGN-VILLAGER.md, Risiko 5):
 * erst nach einer Spielsimulation (Funde pro Stunde, Dublettenanteil) festlegen. Preise in Smaragden.
 */
public final class ScribePrices {
    private ScribePrices() {}

    // ---- Er verkauft (Preis in Smaragden)
    public static final int PRIMER = 1;
    public static final int GRAMMAR = 6;
    public static final int LEXICON_A_C = 10, LEXICON_D_F = 10;
    public static final int FORMS_GRAMMAR_LEXICON = 18, THEMATIC_LEXICON = 18;
    public static final int COMPLETE_LEXICON = 30;
    /** Papier: je nach Glueck kauft er es (so viele Blatt fuer einen Smaragd) oder verkauft es (so viele Blatt fuer einen Smaragd). */
    public static final int PAPER_BOUGHT_PER_EMERALD = 24, PAPER_SOLD_PER_EMERALD = 6;
    /** Fundkarte (Meister): Smaragde plus ein Kompass. */
    public static final int MAP = 16;

    // ---- Er kauft
    /** So viele Papyrusfetzen ergeben einen Smaragd. */
    public static final int SCRAPS_PER_EMERALD = 8;
    /** So viele Ostraka ergeben einen Smaragd. */
    public static final int OSTRAKA_PER_EMERALD = 4;
    /** Smaragde fuer ein einzelnes loses Fragment (gleich, egal wie schwer). */
    public static final int FRAGMENT_SELL = 2;
    /** So viele lose Fragmente kosten einen Fundbeutel. */
    public static final int FRAGMENTS_PER_POUCH = 3;

    // ---- Tageslimits (maxUses; Vanilla fuellt Handel zweimal am Tag auf)
    public static final int USES_BOOK = 1;
    public static final int USES_SCRAPS = 12, USES_OSTRAKA = 6, USES_PAPER = 8;
    public static final int USES_FRAGMENT_SELL = 6;
    public static final int USES_POUCH = 2;
    public static final int USES_MAP = 1;

    // ---- Erfahrung je Handel (Dorfbewohner-XP)
    public static final int XP_SMALL = 2, XP_MEDIUM = 5, XP_BIG = 10;
}
