package de.scribery.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

import de.scribery.core.TileSession.Tile;
import de.scribery.core.TileSession.Word;

class TileSessionTest {
    private static final List<Word> P1 = List.of(
            new Word("Ἐν", "EN", List.of("in"), List.of("into", "through", "under"), 2),
            new Word("ἀρχῇ", "ARCHĒ", List.of("beginning"), List.of("beginnings", "ruler", "end"), 1),
            new Word("ἦν", "ĒN", List.of("was"), List.of("is", "were", "being"), 0),
            new Word("ὁ", "HO", List.of("the"), List.of("a", "this", "of the"), 2),
            new Word("λόγος", "LOGOS", List.of("word"), List.of("words", "of word", "light"), 1));

    private static TileSession session(int perPage) {
        return new TileSession(P1, TileSession.pagesOf(P1.size(), perPage), 3, 42L);
    }

    /** Schreibt das Wort aufs Blatt (falls noch nicht geschehen) und legt ein freies Plaettchen in seinen Slot. */
    private static Tile put(TileSession s, String face, int slot) {
        s.reveal(slot);
        for (Tile t : s.tiles()) {
            if (t.used() || t.slot() >= 0 || !t.face().equals(face)) continue;
            assertTrue(s.place(t.id(), slot));
            return t;
        }
        return fail("kein freies Plaettchen mit " + face);
    }

    @Test
    void everyFaceAppearsExactlyOnce() {
        TileSession s = session(12);
        assertEquals(20, s.tiles().size(), "vier Plaettchen pro Wort");
        for (Word w : P1) {
            for (String face : List.of(w.ok().get(0), w.wrong().get(0), w.wrong().get(1), w.wrong().get(2))) {
                long n = s.tiles().stream().filter(t -> t.face().equals(face)).count();
                assertEquals(1, n, face);
            }
        }
    }

    @Test
    void tilesAreDeterministicAndKeepTheirBox() {
        List<Tile> a = TileSession.tilesFor(P1, List.of(0, 1, 2, 3, 4), 42L);
        List<Tile> b = TileSession.tilesFor(P1, List.of(0, 1, 2, 3, 4), 42L);
        for (int i = 0; i < a.size(); i++) {
            assertEquals(a.get(i).face(), b.get(i).face());
        }
        Tile was = a.stream().filter(t -> t.face().equals("was")).findFirst().orElseThrow();
        assertEquals(0, was.box());
    }

    @Test
    void correctSheetIsWrittenInOneGo() {
        TileSession s = session(12);
        assertEquals(TileSession.Hint.PICK_WORDS, s.hint());
        assertEquals(TileSession.Refusal.EMPTY_SLOTS, s.write().refusal());
        put(s, "in", 0);
        put(s, "beginning", 1);
        put(s, "was", 2);
        put(s, "the", 3);
        Tile word = put(s, "word", 4);
        assertEquals(TileSession.Hint.WRITE, s.hint());
        var r = s.write();
        assertNull(r.refusal());
        assertTrue(r.allCorrect());
        assertEquals("word", s.written(4));
        assertTrue(word.used());
        assertEquals(2, s.ink());
        assertTrue(s.finishPage());
        assertTrue(s.isComplete());
    }

    @Test
    void slotExistsOnlyAfterTheWordIsPicked() {
        TileSession s = session(12);
        Tile in = s.tiles().stream().filter(t -> t.face().equals("in")).findFirst().orElseThrow();
        assertFalse(s.isRevealed(0));
        assertFalse(s.place(in.id(), 0), "noch kein Slot");
        assertTrue(s.reveal(0));
        assertFalse(s.reveal(0), "steht schon da");
        assertTrue(s.place(in.id(), 0));
        assertEquals(TileSession.Hint.PICK_WORDS, s.hint(), "vier Woerter fehlen noch");
        for (int i = 1; i < 5; i++) assertTrue(s.reveal(i));
        assertEquals(TileSession.Hint.PLACE_TILES, s.hint());
    }

    @Test
    void wrongTileSmudgesAndGoesBack() {
        TileSession s = session(12);
        put(s, "in", 0);
        put(s, "beginning", 1);
        put(s, "was", 2);
        put(s, "the", 3);
        Tile words = put(s, "words", 4);
        var r = s.write();
        assertFalse(r.allCorrect());
        assertArrayEquals(new boolean[] { false, false, false, false, true }, r.wrong());
        assertEquals("words", s.smudged(4));
        assertEquals(-1, words.slot());
        assertFalse(words.used());
        assertTrue(s.isDone(0));
        assertFalse(s.finishPage());

        // Nur der offene Slot braucht ein neues Plaettchen.
        assertEquals(TileSession.Hint.PLACE_TILES, s.hint());
        put(s, "word", 4);
        assertNull(s.smudged(4));
        var again = s.write();
        assertArrayEquals(new boolean[] { false, false, false, false, true }, again.attempted());
        assertTrue(again.allCorrect());
    }

    @Test
    void placingSwapsAndDoneSlotsAreLocked() {
        TileSession s = session(12);
        Tile in = put(s, "in", 0);
        Tile into = put(s, "into", 0);
        assertEquals(-1, in.slot(), "das verdraengte Plaettchen liegt wieder im Kasten");
        assertEquals(0, into.slot());
        assertTrue(s.reveal(1));
        assertTrue(s.place(into.id(), 1), "umlegen");
        assertNull(s.tileInSlot(0));
        s.unplace(into.id());
        assertNull(s.tileInSlot(1));

        put(s, "in", 0);
        put(s, "beginning", 1);
        put(s, "was", 2);
        put(s, "the", 3);
        put(s, "light", 4);
        s.write();
        Tile free = s.tiles().stream().filter(t -> !t.used() && t.slot() < 0).findFirst().orElseThrow();
        assertFalse(s.place(free.id(), 0), "fertige Slots nehmen nichts mehr an");
    }

    @Test
    void synonymsAreAccepted() {
        List<Word> w = List.of(new Word("πρὸς", "PROS", List.of("with", "toward"), List.of("toward", "in", "under"), 2));
        TileSession s = new TileSession(w, TileSession.pagesOf(1, 12), 3, 1L);
        put(s, "toward", 0);
        assertTrue(s.write().allCorrect());
    }

    @Test
    void longFragmentsUseSeveralSheets() {
        TileSession s = session(2);
        assertEquals(3, s.pageCount());
        assertEquals(2, s.slotCount());
        assertEquals(8, s.tiles().size());
        put(s, "in", 0);
        put(s, "beginning", 1);
        assertTrue(s.write().allCorrect());
        assertTrue(s.finishPage());
        assertEquals("ĒN", s.word(0).latin());
    }

    @Test
    void writingNeedsInk() {
        TileSession s = new TileSession(P1, TileSession.pagesOf(P1.size(), 1), 0, 42L);
        put(s, "in", 0);
        assertEquals(TileSession.Refusal.NO_INK, s.write().refusal());
    }
}
