package de.scribery.core;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

/** Die Witze auf den Ostraka (tools/data/ostraka-jokes.json): eindeutige IDs, Griechisch und Englisch, kurz genug fuer einen Scherben. */
class OstrakaJokesTest {
    @Test
    void jokesAreUniqueGreekWithEnglishSolutions() throws IOException {
        var root = JsonParser.parseString(Files.readString(Path.of("..", "tools", "data", "ostraka-jokes.json"), StandardCharsets.UTF_8)).getAsJsonObject();
        Set<String> ids = new HashSet<>(), numbers = new HashSet<>();
        for (JsonElement e : root.getAsJsonArray("jokes")) {
            var o = e.getAsJsonObject();
            String id = o.get("id").getAsString(), greek = o.get("greek").getAsString(), en = o.get("en").getAsString();
            assertTrue(ids.add(id), "doppelte ID " + id);
            assertTrue(numbers.add(o.get("philogelos").getAsString()), "doppelte Nummer " + id);
            assertTrue(greek.length() >= 30 && greek.length() <= 200, "Griechisch zu kurz oder zu lang: " + id);
            assertTrue(en.length() >= 30 && en.length() <= 220, "Englisch zu kurz oder zu lang: " + id);
            assertTrue(greek.chars().anyMatch(c -> c >= 0x370 && c <= 0x3ff || c >= 0x1f00 && c <= 0x1fff), "kein Griechisch: " + id);
            assertFalse(greek.contains("ʼ"), "Apostroph U+02BC hat die Schrift nicht: " + id);
            assertFalse(greek.contains("…"), "Textluecke im Witz: " + id);
        }
        assertTrue(ids.size() >= 30);
    }
}
