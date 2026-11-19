package de.scribery;

import java.util.ArrayList;
import java.util.List;

import de.scribery.core.TileSession;

/** Prueft auf dem Server, dass eine gemeldete Uebersetzung zu den Woertern des Bogens passt. */
final class Translation {
    private Translation() {}

    /**
     * Jedes Wort des Bogens (aus den gedruckten Zeilen) muss eine erlaubte Glosse haben. Gefunden wird es
     * ueber die lateinische Form im Platzhalter-Fragment.
     */
    static boolean isCorrect(SheetData sheet, List<String> english) {
        if (sheet.fragment() < 0 || !Manuscripts.has(sheet.fragment())) return false;
        List<TileSession.Word> words = Manuscripts.at(sheet.fragment()).tileWords();
        List<String> latin = new ArrayList<>();
        for (String line : sheet.lines()) for (String w : line.split(" ")) if (!w.isBlank()) latin.add(w);
        if (latin.size() != english.size()) return false;
        for (int i = 0; i < latin.size(); i++) {
            String face = english.get(i);
            String target = latin.get(i);
            boolean ok = words.stream().anyMatch(w -> w.latin().equals(target) && w.accepts(face));
            if (!ok) return false;
        }
        return true;
    }
}
