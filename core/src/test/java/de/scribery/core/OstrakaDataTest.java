package de.scribery.core;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Die Ostraka zum Uebersetzen (tools/data/ostraka.json, erzeugt von tools/build_ostraka.py): jedes Wort laesst sich umschreiben, die Zeilen
 * passen in die Setzform, jedes Wort hat richtige und drei verschiedene falsche Antworten, und die Witze stimmen mit
 * tools/data/ostraka-jokes.json ueberein.
 */
class OstrakaDataTest {
    private static final int LINE_SLOTS = 17, MAX_WORDS_PER_LINE = 3;

    private static JsonObject read(String name) throws IOException {
        return JsonParser.parseString(Files.readString(Path.of("..", "tools", "data", name), StandardCharsets.UTF_8)).getAsJsonObject();
    }

    @Test
    void everyOstrakonIsTranslatableAndMatchesItsJoke() throws IOException {
        List<JsonObject> frags = new ArrayList<>();
        for (JsonElement e : read("ostraka.json").getAsJsonArray("fragments")) frags.add(e.getAsJsonObject());
        Set<String> jokeIds = new HashSet<>();
        for (JsonElement e : read("ostraka-jokes.json").getAsJsonArray("jokes")) jokeIds.add(e.getAsJsonObject().get("id").getAsString());
        Set<String> fragIds = new HashSet<>();
        for (JsonObject f : frags) fragIds.add(f.get("id").getAsString());
        assertEquals(jokeIds, fragIds, "Witze und Ostraka-Fragmente stimmen nicht ueberein");

        for (JsonObject f : frags) {
            String id = f.get("id").getAsString();
            List<Integer> lengths = new ArrayList<>();
            for (JsonElement we : f.getAsJsonArray("words")) {
                JsonObject w = we.getAsJsonObject();
                String greek = w.get("greek").getAsString();
                PressWord pw = PressWord.of(greek, null);
                assertFalse(pw.latin().isBlank(), id + " " + greek);
                for (char c : pw.latin().toCharArray()) assertTrue(Translit.ALPHABET.indexOf(c) >= 0, id + ": " + greek + " -> unbekannter Buchstabe " + c);
                assertTrue(pw.latin().length() <= LINE_SLOTS, id + ": zu lang fuer eine Zeile: " + greek);
                lengths.add(pw.latin().length());
                JsonObject gloss = w.getAsJsonObject("gloss");
                Set<String> ok = new HashSet<>(), wrong = new HashSet<>();
                gloss.getAsJsonArray("ok").forEach(x -> ok.add(x.getAsString().toLowerCase()));
                gloss.getAsJsonArray("wrong").forEach(x -> wrong.add(x.getAsString().toLowerCase()));
                assertFalse(ok.isEmpty(), id + " " + greek + ": keine richtige Antwort");
                assertEquals(3, wrong.size(), id + " " + greek + ": nicht genau drei verschiedene falsche Antworten");
                for (String x : wrong) assertFalse(ok.contains(x), id + " " + greek + ": falsche Antwort ist auch richtig: " + x);
            }
            assertDoesNotThrow(() -> LineBreaker.breakLines(lengths, LINE_SLOTS, MAX_WORDS_PER_LINE), id);
            assertTrue(lengths.size() >= 5 && lengths.size() <= 24, id + ": " + lengths.size() + " Woerter");
        }
        assertEquals(30, frags.size());
    }
}
