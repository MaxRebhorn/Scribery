package de.scribery.core;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/** Prueft das Modell des Manuscript Archive: Fortschritt, Zuordnung der Woerter zu Boegen, Zusammenfassung. */
class ArchiveModelTest {
    private static final int LINE_SLOTS = 17, ROWS = 4, MAX_WORDS = 3;   // wie art/layout.json

    @Test
    void progressTracksFoundPrintedAndTranslatedPages() {
        ArchiveProgress p = ArchiveProgress.NONE;
        assertTrue(p.isEmpty());
        p = p.withFound().withPrinted(0).withPrinted(2).withTranslated(2);
        assertTrue(p.found());
        assertTrue(p.isPrinted(0) && !p.isPrinted(1) && p.isPrinted(2));
        assertTrue(!p.isTranslated(0) && p.isTranslated(2));
        assertFalse(p.isPrinted(-1));
        assertFalse(p.isPrinted(ArchiveProgress.MAX_PAGES));
        assertThrows(IllegalArgumentException.class, () -> ArchiveProgress.NONE.withPrinted(ArchiveProgress.MAX_PAGES));
    }

    @Test
    void columnsOpenIndependentlyAndGapsStayWhereTheTextIsMissing() {
        ArchiveProgress onlyFound = ArchiveProgress.NONE.withFound();
        assertTrue(ArchiveModel.known(onlyFound, ArchiveModel.Column.GREEK, 0));
        assertFalse(ArchiveModel.known(onlyFound, ArchiveModel.Column.LATIN, 0));
        assertFalse(ArchiveModel.known(onlyFound, ArchiveModel.Column.ENGLISH, 0));
        ArchiveProgress printedPageOne = ArchiveProgress.NONE.withPrinted(1);
        assertFalse(ArchiveModel.known(printedPageOne, ArchiveModel.Column.GREEK, 1));
        assertTrue(ArchiveModel.known(printedPageOne, ArchiveModel.Column.LATIN, 1));
        assertFalse(ArchiveModel.known(printedPageOne, ArchiveModel.Column.LATIN, 0));
    }

    @Test
    void wordsAreSplitIntoPagesLikeThePress() {
        // 14 Woerter zu je 4 Buchstaben: pro Zeile drei Woerter (Grenze maxWords), vier Zeilen pro Bogen = 12 Woerter pro Bogen
        List<Integer> lengths = new ArrayList<>();
        for (int i = 0; i < 14; i++) lengths.add(4);
        int[] page = ArchiveModel.pageOfWord(lengths, LINE_SLOTS, ROWS, MAX_WORDS);
        assertEquals(0, page[0]);
        assertEquals(0, page[11]);
        assertEquals(1, page[12]);
        assertEquals(1, page[13]);
        assertEquals(2, ArchiveModel.pageCount(page));
        assertEquals(1, ArchiveModel.pageCount(new int[0]));
    }

    @Test
    void statsCountFoundPrintedAndTranslated() {
        var progress = List.of(ArchiveProgress.NONE.withFound().withPrinted(0).withTranslated(0),
                ArchiveProgress.NONE.withFound().withPrinted(1), ArchiveProgress.NONE);
        var stats = ArchiveModel.stats(List.of(1, 2, 3), progress);
        assertEquals(new ArchiveModel.Stats(2, 3, 2, 1, 6), stats);
    }

    /** Mit den echten Daten: gleiche Bogenzahl wie die Presse (2205), und kein Manuskript sprengt das Bitfeld. */
    @Test
    void realManuscriptsFitTheBitfieldAndMatchThePress() throws IOException {
        int total = 0, maxPages = 0, count = 0;
        for (String file : List.of("manuscripts.john1.json", "manuscripts.john-rest.json")) {
            JsonObject root = JsonParser.parseString(Files.readString(Path.of("..", "tools", "data", file), StandardCharsets.UTF_8)).getAsJsonObject();
            for (JsonElement fe : root.getAsJsonArray("fragments")) {
                List<Integer> lengths = new ArrayList<>();
                for (JsonElement we : fe.getAsJsonObject().getAsJsonArray("words")) {
                    lengths.add(Translit.word(we.getAsJsonObject().get("greek").getAsString()).length());
                }
                int pages = ArchiveModel.pageCount(ArchiveModel.pageOfWord(lengths, LINE_SLOTS, ROWS, MAX_WORDS));
                total += pages;
                maxPages = Math.max(maxPages, pages);
                count++;
            }
        }
        assertEquals(1540, count);
        assertEquals(2205, total, "Boegen insgesamt wie im Selbsttest der Presse");
        assertTrue(maxPages < ArchiveProgress.MAX_PAGES, "ein Manuskript hat " + maxPages + " Boegen");
    }
}
