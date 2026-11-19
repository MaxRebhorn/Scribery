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
    /** Tintenbeutel-Buendel: so viele Tintenbeutel fuer einen Smaragd. */
    public static final int INK_PER_EMERALD = 4;
    /** Fundkarte (Meister): Smaragde plus ein Kompass. */
    public static final int MAP = 16;

    // ---- Er kauft
    /** So viele Papyrusfetzen ergeben einen Smaragd. */
    public static final int SCRAPS_PER_EMERALD = 8;
    /** So viele Ostraka ergeben einen Smaragd. */
    public static final int OSTRAKA_PER_EMERALD = 4;
    /** Smaragde fuer eine einzelne Dublette: leicht, mittel, schwer. */
    public static final int DUPLICATE_EASY = 1, DUPLICATE_MEDIUM = 2, DUPLICATE_HARD = 3;
    /** So viele Dubletten (beliebige) kosten einen Fundbeutel. */
    public static final int DUPLICATES_PER_POUCH = 3;

    // ---- Tageslimits (maxUses; Vanilla fuellt Handel zweimal am Tag auf)
    public static final int USES_BOOK = 1;
    public static final int USES_SCRAPS = 12, USES_OSTRAKA = 6, USES_INK = 8;
    public static final int USES_DUPLICATE_SINGLE = 6;
    public static final int USES_POUCH = 2;
    public static final int USES_MAP = 1;

    // ---- Erfahrung je Handel (Dorfbewohner-XP)
    public static final int XP_SMALL = 2, XP_MEDIUM = 5, XP_BIG = 10;
}
