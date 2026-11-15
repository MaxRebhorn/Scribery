package de.scribery.core;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

/**
 * Umschrift Griechisch zu Lateinisch mit den 22 Buchstaben der Buchstabenbox.
 *
 * Regeln: Buchstabe fuer Buchstabe, ohne Kontextregeln. Akzente, Iota subscriptum, Trema
 * und Satzzeichen fallen weg. Spiritus asper wird zu H am Wortanfang (bei Rho: RH).
 * Die Golden-Datei translit-golden.tsv legt das Verhalten fest.
 */
public final class Translit {
    public static final String ALPHABET = "ABCDEĒGHIKLMNOŌPRSTUXZ";

    private static final char ROUGH_BREATHING = '̔';

    private Translit() {}

    /** Schreibt einen Text um. Woerter bleiben durch je ein Leerzeichen getrennt. */
    public static String toLatin(String greek) {
        return String.join(" ", words(greek));
    }

    /** Schreibt einen Text um und liefert die Woerter einzeln. Leere Woerter entfallen. */
    public static List<String> words(String greek) {
        List<String> out = new ArrayList<>();
        for (String w : greek.trim().split("\\s+")) {
            String latin = word(w);
            if (!latin.isEmpty()) out.add(latin);
        }
        return out;
    }

    /** Schreibt ein einzelnes Wort um. */
    public static String word(String greek) {
        String nfd = Normalizer.normalize(greek, Normalizer.Form.NFD);
        boolean rough = nfd.indexOf(ROUGH_BREATHING) >= 0;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < nfd.length(); i++) {
            String m = map(Character.toLowerCase(nfd.charAt(i)));
            if (m != null) sb.append(m);
        }
        if (rough && sb.length() > 0) {
            if (sb.charAt(0) == 'R') sb.insert(1, 'H');
            else sb.insert(0, 'H');
        }
        return sb.toString();
    }

    private static String map(char c) {
        return switch (c) {
            case 'α' -> "A";
            case 'β' -> "B";
            case 'γ' -> "G";
            case 'δ' -> "D";
            case 'ε' -> "E";
            case 'ζ' -> "Z";
            case 'η' -> "Ē";
            case 'θ' -> "TH";
            case 'ι' -> "I";
            case 'κ' -> "K";
            case 'λ' -> "L";
            case 'μ' -> "M";
            case 'ν' -> "N";
            case 'ξ' -> "X";
            case 'ο' -> "O";
            case 'π' -> "P";
            case 'ρ' -> "R";
            case 'σ', 'ς' -> "S";
            case 'τ' -> "T";
            case 'υ' -> "U";
            case 'φ' -> "PH";
            case 'χ' -> "CH";
            case 'ψ' -> "PS";
            case 'ω' -> "Ō";
            default -> null;
        };
    }
}
