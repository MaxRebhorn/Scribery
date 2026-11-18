package de.scribery;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Anzeige der Kapitel in den Creative-Tabs: ein kurzer Geschichten-Titel und ein Icon-Item je Kapitel
 * (tools/data/chapters.json). Den Versbereich berechnet diese Klasse aus den Manuskripten.
 */
public final class Chapters {
    /** @param title kurzer Geschichten-Titel, @param icon Item-ID fuer das Tab-Icon, @param range z. B. "1:1-51" */
    public record Info(int chapter, String title, String icon, String range) {}

    private static final Pattern VERSE = Pattern.compile("(\\d+):(\\d+)");
    private static Map<Integer, String[]> raw;

    private Chapters() {}

    private static Map<Integer, String[]> raw() {
        if (raw != null) return raw;
        Map<Integer, String[]> out = new HashMap<>();
        var in = Chapters.class.getResourceAsStream("/assets/scribery/data/chapters.json");
        if (in != null) {
            try (Reader r = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                for (JsonElement e : JsonParser.parseReader(r).getAsJsonObject().getAsJsonArray("chapters")) {
                    JsonObject o = e.getAsJsonObject();
                    out.put(o.get("chapter").getAsInt(), new String[] { o.get("title").getAsString(), o.get("icon").getAsString() });
                }
            } catch (java.io.IOException ex) {
                throw new IllegalStateException(ex);
            }
        }
        return raw = out;
    }

    /** Vers einer Referenz wie "John 1:51" oder "John 21:25b" als {Kapitel, Vers}, sonst null. */
    private static int[] verse(String ref) {
        Matcher m = VERSE.matcher(ref);
        return m.find() ? new int[] { Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)) } : null;
    }

    /** "1:1-51" fuer ein Kapitel, oder "1:1-2:12", falls die Manuskripte ueber die Kapitelgrenze reichen. */
    static String range(List<Manuscripts.Fragment> all, int chapter) {
        int[] first = null, last = null;
        for (Manuscripts.Fragment f : all) {
            if (f.chapter() != chapter) continue;
            int[] v = verse(f.ref());
            if (v == null) continue;
            if (first == null) first = v;
            last = v;
        }
        if (first == null) return "?";
        return last[0] == first[0] ? first[0] + ":" + first[1] + "–" + last[1]
                : first[0] + ":" + first[1] + "–" + last[0] + ":" + last[1];
    }

    public static Info of(int chapter) {
        String[] r = raw().get(chapter);
        var all = Boolean.getBoolean("scribery.selftest") ? Manuscripts.loadReal() : Manuscripts.load();
        return new Info(chapter, r == null ? "Chapter " + chapter : r[0], r == null ? "" : r[1], range(all, chapter));
    }
}
