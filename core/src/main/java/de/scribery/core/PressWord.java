package de.scribery.core;

/**
 * Ein Wort in der Presse.
 *
 * @param greek  Wortform wie im Fragment
 * @param latin  Zielform, aus {@code greek} berechnet
 * @param preset true = vorgedruckt, der Spieler setzt es nicht
 */
public record PressWord(String greek, String latin, boolean preset) {
    /** Woerter bis zu dieser Laenge sind ohne Ueberschreibung vorgedruckt. */
    public static final int PRESET_MAX_LENGTH = 3;

    /**
     * @param setOverride Feld {@code set} aus den Daten: true = setzen, false = vorgedruckt, null = Regel
     */
    public static PressWord of(String greek, Boolean setOverride) {
        String latin = Translit.word(greek);
        boolean preset = setOverride != null ? !setOverride : latin.length() <= PRESET_MAX_LENGTH;
        return new PressWord(greek, latin, preset);
    }
}
