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

/** Die Witze auf den Ostraka (tools/data/ostraka-jokes.json): eindeutige IDs, kurze Texte. */
class OstrakaJokesTest {
    @Test
    void jokesAreUniqueAndShortEnoughForAShard() throws IOException {
        var root = JsonParser.parseString(Files.readString(Path.of("..", "tools", "data", "ostraka-jokes.json"), StandardCharsets.UTF_8)).getAsJsonObject();
        Set<String> ids = new HashSet<>();
        for (JsonElement e : root.getAsJsonArray("jokes")) {
            var o = e.getAsJsonObject();
            String id = o.get("id").getAsString(), text = o.get("en").getAsString();
            assertTrue(ids.add(id), "doppelte ID " + id);
            assertFalse(text.isBlank(), id);
            assertTrue(text.split("\n").length <= 3, "mehr als 3 Zeilen: " + id);
            assertTrue(text.length() <= 160, "zu lang fuer einen Scherben: " + id);
        }
        assertTrue(ids.size() >= 10);
    }
}
