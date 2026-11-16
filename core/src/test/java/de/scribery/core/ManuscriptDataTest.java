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
 * Prueft die erzeugten Manuskripte (tools/build_manuscripts.py): Jedes Wort muss sich umschreiben lassen,
 * in eine Zeile der Setzform passen, und jedes Wort braucht eine richtige und drei verschiedene falsche Antworten.
 */
class ManuscriptDataTest {
    private static final Path DATA = Path.of("..", "tools", "data", "manuscripts.john1.json");
    private static final int LINE_SLOTS = 16, MAX_WORDS_PER_LINE = 3, ROWS = 4;

    private static List<JsonObject> fragments() throws IOException {
        JsonObject root = JsonParser.parseString(Files.readString(DATA, StandardCharsets.UTF_8)).getAsJsonObject();
        List<JsonObject> out = new ArrayList<>();
        for (JsonElement e : root.getAsJsonArray("fragments")) out.add(e.getAsJsonObject());
        return out;
    }

    @Test
    void everyWordFitsTheFormeAndHasAnswers() throws IOException {
        List<String> problems = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        for (JsonObject f : fragments()) {
            String id = f.get("id").getAsString();
            assertTrue(ids.add(id), "doppelte ID " + id);
            List<Integer> widths = new ArrayList<>();
            for (JsonElement we : f.getAsJsonArray("words")) {
                JsonObject w = we.getAsJsonObject();
                String greek = w.get("greek").getAsString();
                String latin = Translit.word(greek);
                if (latin.isEmpty()) problems.add(id + " " + greek + ": Umschrift leer");
                for (char c : latin.toCharArray()) {
                    if (Translit.ALPHABET.indexOf(c) < 0) problems.add(id + " " + greek + ": Buchstabe " + c + " fehlt in der Box");
                }
                if (latin.length() > LINE_SLOTS) problems.add(id + " " + greek + ": " + latin + " ist laenger als eine Zeile");
                widths.add(latin.length());

                JsonObject g = w.getAsJsonObject("gloss");
                List<String> ok = new ArrayList<>(), wrong = new ArrayList<>();
                g.getAsJsonArray("ok").forEach(x -> ok.add(x.getAsString().toLowerCase()));
                g.getAsJsonArray("wrong").forEach(x -> wrong.add(x.getAsString().toLowerCase()));
                if (ok.isEmpty() || ok.get(0).isBlank()) problems.add(id + " " + greek + ": keine richtige Antwort");
                if (new HashSet<>(wrong).size() != TileSession.WRONG_PER_WORD) {
                    problems.add(id + " " + greek + ": " + wrong + " sind keine drei verschiedenen falschen Antworten");
                }
                if (wrong.stream().anyMatch(ok::contains)) problems.add(id + " " + greek + ": falsche Antwort ist auch richtig");
            }
            if (problems.stream().noneMatch(p -> p.startsWith(id + " ") && p.contains("laenger"))) {
                int lines = LineBreaker.breakLines(widths, LINE_SLOTS, MAX_WORDS_PER_LINE).size();
                assertTrue(lines >= 1);
            }
        }
        assertTrue(problems.isEmpty(), String.join("\n", problems));
    }

    @Test
    void difficultyIsMixedNotSortedByOrder() throws IOException {
        List<JsonObject> fs = fragments();
        Set<Integer> tiers = new HashSet<>();
        int rises = 0;
        for (int i = 0; i < fs.size(); i++) {
            tiers.add(fs.get(i).get("tier").getAsInt());
            if (i > 0 && fs.get(i).get("tier").getAsInt() >= fs.get(i - 1).get("tier").getAsInt()) rises++;
        }
        assertEquals(Set.of(1, 2, 3), tiers, "alle Stufen kommen vor");
        assertTrue(rises < fs.size() - 1, "die Schwierigkeit steigt nicht streng mit der Nummer");
    }
}
