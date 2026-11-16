package de.scribery.core;

import static org.junit.jupiter.api.Assertions.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

class CoreTest {
    private static final List<PressWord> P1 = List.of(
            PressWord.of("Ἐν", false), PressWord.of("ἀρχῇ", null), PressWord.of("ἦν", null),
            PressWord.of("ὁ", false), PressWord.of("λόγος", null));

    /** Setzform mit nur einer Zeile: P1 braucht zwei Boegen. */
    private static PressSession session() {
        return new PressSession(P1, 16, 1, 4, 4, 8, 24, new Random(1));
    }

    /** Setzt ein Wort ab Slot {@code start}. */
    private static void set(PressSession s, int start, String letters) {
        for (int i = 0; i < letters.length(); i++) {
            assertTrue(s.takeFromCase(letters.charAt(i)));
            assertTrue(s.place(start + i, letters.charAt(i)));
        }
    }

    private static void inkAll(PressSession s) {
        assertTrue(s.dipRoller());
        for (int pass = 0; pass < PressSession.INK_PASSES; pass++) {
            for (int i = 0; i < s.slotCount(); i++) s.inkSlot(i);
        }
        s.returnRoller();
    }

    @Test
    void translitMatchesGoldenFile() throws Exception {
        int n = 0;
        try (var in = new BufferedReader(new InputStreamReader(
                getClass().getResourceAsStream("/translit-golden.tsv"), StandardCharsets.UTF_8))) {
            for (String line; (line = in.readLine()) != null; ) {
                if (line.isBlank() || line.startsWith("#")) continue;
                String[] p = line.split("\t");
                assertEquals(p[1], Translit.word(p[0]), p[0]);
                for (char c : p[1].toCharArray()) assertTrue(Translit.ALPHABET.indexOf(c) >= 0, p[1]);
                n++;
            }
        }
        assertTrue(n >= 20);
    }

    @Test
    void translitKeepsWordsApart() {
        assertEquals("EN ARCHĒ ĒN HO LOGOS", Translit.toLatin("Ἐν ἀρχῇ ἦν ὁ λόγος,"));
    }

    @Test
    void linesFillUpToSlotCountAndWordLimit() {
        assertEquals(List.of(List.of(0, 1, 2, 3), List.of(4)), LineBreaker.breakLines(List.of(2, 5, 2, 2, 5), 16, 4));
        assertEquals(1, LineBreaker.breakLines(List.of(8, 7), 16, 4).size());
        assertEquals(2, LineBreaker.breakLines(List.of(8, 8), 16, 4).size());
        assertEquals(List.of(List.of(0, 1), List.of(2)), LineBreaker.breakLines(List.of(1, 1, 1), 16, 2));
        assertThrows(IllegalArgumentException.class, () -> LineBreaker.breakLines(List.of(5), 4, 4));
    }

    @Test
    void presetRule() {
        assertTrue(PressWord.of("ἦν", null).preset());
        assertFalse(PressWord.of("ἦν", true).preset());
        assertFalse(PressWord.of("λόγος", null).preset());
        assertTrue(PressWord.of("λόγος", false).preset());
    }

    @Test
    void firstLineHasPresetWordsAndGaps() {
        PressSession s = session();
        assertEquals(2, s.pageCount());
        assertEquals('E', s.slot(0));
        assertTrue(s.isFixed(0));
        assertFalse(s.isWordSlot(2));
        assertTrue(s.isWordSlot(3));
        assertEquals(0, s.slot(3));
        assertEquals('Ē', s.slot(9));
        assertEquals(List.of("Ἐν ἀρχῇ ἦν ὁ"), s.currentGreekLines());
        assertEquals(PressSession.Hint.SET_TYPE, s.hint());
    }

    @Test
    void cannotPlaceOnFixedSpacerOrOccupiedSlot() {
        PressSession s = session();
        assertFalse(s.place(0, 'A'));
        assertFalse(s.place(2, 'A'));
        assertTrue(s.place(3, 'A'));
        assertFalse(s.place(3, 'B'));
        assertEquals(0, s.pickUp(0));
        assertEquals('A', s.pickUp(3));
    }

    @Test
    void fullRunPrintsBothLines() {
        PressSession s = session();
        set(s, 3, "ARCHĒ");
        assertEquals(PressSession.Hint.DIP_ROLLER, s.hint());
        assertEquals(PressSession.Refusal.NO_PAPER, s.pull().refusal());
        assertTrue(s.layPaper());
        assertEquals(PressSession.Refusal.NOT_FED, s.pull().refusal());
        assertEquals(PressSession.Refusal.NOT_INKED, s.crank());
        inkAll(s);
        assertEquals(7, s.ink());
        assertEquals(PressSession.Hint.TURN_CRANK, s.hint());
        assertNull(s.crank());
        assertFalse(s.place(3, 'A'), "Setzform ist vom Papier bedeckt");
        var r = s.pull();
        assertNull(r.refusal());
        assertTrue(r.allCorrect());
        assertTrue(s.finishPage());
        assertEquals(List.of("EN ARCHĒ ĒN HO"), s.finishedLines());

        set(s, 0, "LOGOS");
        inkAll(s);
        s.layPaper();
        assertNull(s.crank());
        assertTrue(s.pull().allCorrect());
        assertTrue(s.finishPage());
        assertTrue(s.isComplete());
        assertEquals(PressSession.Hint.DONE, s.hint());
    }

    @Test
    void threeRowFormeHoldsWholeFragmentOnOneSheet() {
        PressSession s = new PressSession(P1, 16, 3, 4, 4, 8, 24, new Random(1));
        assertEquals(1, s.pageCount());
        assertEquals(48, s.slotCount());
        assertEquals(List.of("Ἐν ἀρχῇ ἦν ὁ", "λόγος"), s.currentGreekLines());
        assertTrue(s.isWordSlot(16), "LOGOS beginnt in der zweiten Zeile");
        for (int i = 32; i < 48; i++) assertFalse(s.isWordSlot(i), "dritte Zeile traegt keinen Text");
        assertFalse(s.place(2, 'A'), "Leerstelle zwischen Woertern ist gesperrt");
        assertTrue(s.place(14, 'A'), "Zeilenrest ist frei");
        assertEquals('A', s.pickUp(14));
        set(s, 32, "A"); // freie Zeile: setzen geht, gehoert aber nicht in den Text

        set(s, 3, "ARCHĒ");
        set(s, 16, "LOGOZ");
        inkAll(s);
        s.layPaper();
        assertNull(s.crank());
        var r = s.pull();
        assertFalse(r.allCorrect());
        for (int i = 0; i < 48; i++) assertEquals((i >= 16 && i <= 20) || i == 32, r.smudged()[i], "Slot " + i);

        assertEquals('Z', s.pickUp(20));
        s.returnToCase('Z');
        set(s, 20, "S");
        inkAll(s);
        s.layPaper();
        assertNull(s.crank());
        assertFalse(s.pull().allCorrect(), "die ueberzaehlige Letter verwischt weiter");

        assertEquals('A', s.pickUp(32));
        s.returnToCase('A');
        inkAll(s);
        s.layPaper();
        assertNull(s.crank());
        assertTrue(s.pull().allCorrect());
        assertTrue(s.finishPage());
        assertTrue(s.isComplete());
        assertEquals(List.of("EN ARCHĒ ĒN HO", "LOGOS"), s.finishedLines());
        assertEquals(List.of(List.of(0, 1, 2, 3, 4)), s.sheets());
        assertEquals(1, s.printedCount());
        assertEquals('S', s.printedSheet(0)[20]);
    }

    @Test
    void wrongLetterSmudgesOnlyThatWord() {
        PressSession s = session();
        set(s, 3, "ARCHE"); // E statt Ē
        inkAll(s);
        s.layPaper();
        s.crank();
        var r = s.pull();
        assertFalse(r.allCorrect());
        for (int i = 0; i < 16; i++) assertEquals(i >= 3 && i <= 7, r.smudged()[i], "Slot " + i);
        assertFalse(s.finishPage());
        assertEquals(0, s.pageIndex());
        // Nachbessern: Farbe ist weg, Lettern liegen noch.
        assertEquals('E', s.slot(7));
        assertFalse(s.isInked());
        assertEquals('E', s.pickUp(7));
    }

    @Test
    void emptyGapCountsAsWrong() {
        PressSession s = session();
        set(s, 3, "ARCH");
        inkAll(s);
        s.layPaper();
        assertNull(s.crank());
        assertFalse(s.pull().allCorrect());
    }

    @Test
    void lettersAreConservedExceptForWear() {
        PressSession s = session();
        set(s, 3, "ARCHĒ");
        inkAll(s);
        s.layPaper();
        s.crank();
        s.pull();
        s.finishPage();
        int total = 0;
        for (char c : Translit.ALPHABET.toCharArray()) total += s.stock(c);
        assertEquals(22 * 4 - s.lastWorn(), total);
    }

    @Test
    void castingCostsIron() {
        PressSession s = session();
        assertTrue(s.cast('A'));
        assertEquals(5, s.stock('A'));
        assertEquals(23, s.iron());
    }

    @Test
    void rollerNeedsInk() {
        PressSession s = new PressSession(P1, 16, 1, 4, 4, 0, 0, new Random(1));
        assertFalse(s.dipRoller());
        assertFalse(s.cast('A'));
    }

    @Test
    void freePressPrintsAnythingWithoutAText() {
        PressSession s = PressSession.free(16, 4, 4, 8, 24, new Random(1));
        assertFalse(s.isComplete());
        assertEquals(PressSession.Hint.SET_TYPE, s.hint());
        assertTrue(s.takeFromCase('K'));
        assertTrue(s.place(5, 'K'));
        assertTrue(s.takeFromCase('Z'));
        assertTrue(s.place(40, 'Z')); // beliebiger Slot, auch in einer anderen Zeile
        inkAll(s);
        assertTrue(s.layPaper());
        assertNull(s.crank());
        PressSession.PullResult r = s.pull();
        assertNull(r.refusal());
        assertTrue(r.allCorrect());
        assertEquals('K', r.printed()[5]);
        assertTrue(s.finishPage());
        assertEquals(1, s.printedCount());
        assertTrue(s.slot(5) == 'K' || s.lastWorn() > 0, "die Letter bleibt liegen, wenn sie nicht verschleisst");
        assertFalse(s.isComplete());
    }

    @Test
    void paperIsConsumedAndCloseOutReturnsEverything() {
        PressSession s = PressSession.free(16, 4, 4, 8, 24, new Random(1));
        s.setPaperStock(1);
        assertTrue(s.layPaper());
        assertFalse(s.layPaper()); // belegt
        assertEquals(0, s.paperStock());
        assertTrue(s.takeFromCase('K'));
        assertTrue(s.place(3, 'K'));
        s.closeOut();
        assertEquals(1, s.paperStock()); // unbedrucktes Papier zurueck
        assertEquals('K', s.slot(3), "gesetzte Lettern bleiben liegen");
        assertEquals(3, s.stock('K'));
        s.setStock('K', 0);
        assertEquals(0, s.stock('K'));
        s.setPaperStock(0);
        assertFalse(s.layPaper());
    }

    @Test
    void formeIsSavedAndRestored() {
        PressSession s = PressSession.free(16, 4, 4, 8, 24, new Random(1));
        assertTrue(s.takeFromCase('K'));
        assertTrue(s.place(2, 'K'));
        assertTrue(s.takeFromCase('Z'));
        assertTrue(s.place(20, 'Z'));
        String saved = s.formeString();
        assertEquals(64, saved.length());
        assertEquals('K', saved.charAt(2));
        PressSession t = PressSession.free(16, 4, 4, 8, 24, new Random(1));
        t.replaceForme(saved);
        assertEquals('K', t.slot(2));
        assertEquals('Z', t.slot(20));
        assertEquals(4, t.stock('K'), "wiederhergestellte Lettern kommen nicht aus dem Vorrat");
    }

    @Test
    void freePrintKeepsLettersInTheForme() {
        PressSession s = PressSession.free(16, 4, 4, 8, 24, new Random(7));
        s.setStock('K', 100);
        for (int i = 0; i < 10; i++) { assertTrue(s.takeFromCase('K')); assertTrue(s.place(i, 'K')); }
        for (int i = 0; i < 10; i++) { /* einfaerben */ }
        inkAll(s);
        assertTrue(s.layPaper());
        assertNull(s.crank());
        assertTrue(s.pull().allCorrect());
        assertTrue(s.finishPage());
        long left = s.formeString().chars().filter(c -> c == 'K').count();
        assertEquals(10 - s.lastWorn(), left, "was nicht verschlissen ist, bleibt liegen");
        assertFalse(s.awaitingFinish());
        assertEquals(1, s.printedCount());
    }
}
