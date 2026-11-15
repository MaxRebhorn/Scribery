package de.scribery.client;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import de.scribery.core.PressWord;

/** Platzhalter-Fragmente fuer /scribery debug, im echten Datenformat. */
public final class DebugFragments {
    public record Fragment(String id, String ref, List<PressWord> words) {
        public String greekText() {
            StringBuilder sb = new StringBuilder();
            for (PressWord w : words) {
                if (sb.length() > 0) sb.append(' ');
                sb.append(w.greek());
            }
            return sb.toString();
        }
    }

    private DebugFragments() {}

    public static List<Fragment> load() {
        var in = DebugFragments.class.getResourceAsStream("/assets/scribery/debug/fragments.example.json");
        if (in == null) throw new IllegalStateException("Platzhalter-Fragmente fehlen im Mod");
        List<Fragment> out = new ArrayList<>();
        try (Reader r = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(r).getAsJsonObject();
            for (JsonElement fe : root.getAsJsonArray("fragments")) {
                JsonObject f = fe.getAsJsonObject();
                List<PressWord> words = new ArrayList<>();
                for (JsonElement we : f.getAsJsonArray("words")) {
                    JsonObject w = we.getAsJsonObject();
                    Boolean set = w.has("set") ? w.get("set").getAsBoolean() : null;
                    words.add(PressWord.of(w.get("greek").getAsString(), set));
                }
                out.add(new Fragment(f.get("id").getAsString(), f.get("ref").getAsString(), words));
            }
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
        return out;
    }
}
