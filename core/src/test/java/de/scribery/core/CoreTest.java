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

    private static PressSession session() {
        return new PressSession(P1, 16, 4, 8, 24, new Random(1));
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
            for (int i = 0; i < s.lineSlots(); i++) s.inkSlot(i);
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
    void linesFillUpToSlotCount() {
        var lines = LineBreaker.breakLines(List.of("EN", "ARCHĒ", "ĒN", "HO", "LOGOS"), 16);
        assertEquals(List.of(List.of(0, 1, 2, 3), List.of(4)), lines);
        assertEquals(1, LineBreaker.breakLines(List.of("ABCDEFGH", "ABCDEFG"), 16).size());
        assertEquals(2, LineBreaker.breakLines(List.of("ABCDEFGH", "ABCDEFGH"), 16).size());
        assertThrows(IllegalArgumentException.class, () -> LineBreaker.breakLines(List.of("ABCDE"), 4));
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
        assertEquals(2, s.lineCount());
        assertEquals('E', s.slot(0));
        assertTrue(s.isFixed(0));
        assertFalse(s.isWordSlot(2));
        assertTrue(s.isWordSlot(3));
        assertEquals(0, s.slot(3));
        assertEquals('Ē', s.slot(9));
        assertEquals("Ἐν ἀρχῇ ἦν ὁ", s.currentGreekLine());
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
        assertTrue(s.finishLine());
        assertEquals(List.of("EN ARCHĒ ĒN HO"), s.finishedLines());

        set(s, 0, "LOGOS");
        inkAll(s);
        s.layPaper();
        assertNull(s.crank());
        assertTrue(s.pull().allCorrect());
        assertTrue(s.finishLine());
        assertTrue(s.isComplete());
        assertEquals(PressSession.Hint.DONE, s.hint());
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
        assertFalse(s.finishLine());
        assertEquals(0, s.lineIndex());
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
        s.finishLine();
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
        PressSession s = new PressSession(P1, 16, 4, 0, 0, new Random(1));
        assertFalse(s.dipRoller());
        assertFalse(s.cast('A'));
    }
}
