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
import de.scribery.core.TileSession;

/** Platzhalter-Fragmente fuer /scribery debug, im echten Datenformat. */
public final class DebugFragments {
    public record Fragment(String id, String ref, List<PressWord> words, List<TileSession.Word> tileWords) {
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

    private static List<String> strings(JsonObject o, String key) {
        List<String> out = new ArrayList<>();
        for (JsonElement e : o.getAsJsonArray(key)) out.add(e.getAsString());
        return out;
    }

    /** Kasten nach Wortart: 0 Verben, 1 Nomen und Adjektive, 2 alles andere. */
    private static int boxOf(String morph) {
        if (morph.startsWith("V")) return 0;
        if (morph.startsWith("N") || morph.startsWith("A-")) return 1;
        return 2;
    }

    public static List<Fragment> load() {
        var in = DebugFragments.class.getResourceAsStream("/assets/scribery/debug/fragments.example.json");
        if (in == null) throw new IllegalStateException("Platzhalter-Fragmente fehlen im Mod");
        List<Fragment> out = new ArrayList<>();
        try (Reader r = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(r).getAsJsonObject();
            for (JsonElement fe : root.getAsJsonArray("fragments")) {
                JsonObject f = fe.getAsJsonObject();
                List<PressWord> words = new ArrayList<>();
                List<TileSession.Word> tileWords = new ArrayList<>();
                for (JsonElement we : f.getAsJsonArray("words")) {
                    JsonObject w = we.getAsJsonObject();
                    Boolean set = w.has("set") ? w.get("set").getAsBoolean() : null;
                    PressWord pw = PressWord.of(w.get("greek").getAsString(), set);
                    words.add(pw);
                    JsonObject gloss = w.getAsJsonObject("gloss");
                    tileWords.add(new TileSession.Word(pw.greek(), pw.latin(), strings(gloss, "ok"),
                            strings(gloss, "wrong"), boxOf(w.get("morph").getAsString())));
                }
                out.add(new Fragment(f.get("id").getAsString(), f.get("ref").getAsString(), words, tileWords));
            }
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
        return out;
    }
}
