package de.scribery;

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

/**
 * Die Manuskripte des Spiels. Geladen werden die erzeugten Johannes-Daten (tools/build_manuscripts.py).
 * Der Index in der Liste ist die Nummer eines Manuskripts; die ID (P1, P2, ...) ist sein Name.
 * Mit der Systemeigenschaft {@code scribery.selftest} werden stattdessen die Platzhalter geladen, auf die der
 * Selbsttest zugeschnitten ist.
 */
public final class Manuscripts {
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

    private Manuscripts() {}

    private static List<Fragment> cache;

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

    /** Index des Manuskripts mit dieser ID, oder -1. */
    public static int indexOf(String id) {
        List<Fragment> all = load();
        for (int i = 0; i < all.size(); i++) if (all.get(i).id().equals(id)) return i;
        return -1;
    }

    public static List<Fragment> load() {
        if (cache == null) cache = read();
        return cache;
    }

    /** Nur fuer den Selbsttest: ab jetzt die echten Manuskripte statt der Platzhalter verwenden. */
    public static void useReal() {
        cache = loadReal();
    }

    /** Immer die echten Manuskripte, auch im Selbsttest. */
    public static List<Fragment> loadReal() {
        return read("/assets/scribery/data/manuscripts.json");
    }

    private static List<Fragment> read() {
        return read(Boolean.getBoolean("scribery.selftest")
                ? "/assets/scribery/debug/fragments.example.json" : "/assets/scribery/data/manuscripts.json");
    }

    private static List<Fragment> read(String path) {
        var in = Manuscripts.class.getResourceAsStream(path);
        if (in == null) throw new IllegalStateException("Manuskripte fehlen im Mod: " + path);
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
