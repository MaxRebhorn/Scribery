package de.scribery;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

/**
 * Die Witze auf den Ostraka (Philogelos: griechischer Text, englische Loesung; Quelle tools/data/ostraka-jokes.json, beim Bauen nach
 * assets/scribery/data/ kopiert). Ein Ostrakon traegt die Witz-ID im Datenfeld {@link ModComponents#JOKE}.
 */
public final class Ostraka {
    /** Ein Witz: griechischer Text (auf dem Scherben) und englische Loesung. */
    public record Joke(String greek, String en) {}

    private static Map<String, Joke> jokes;

    private Ostraka() {}

    private static synchronized Map<String, Joke> jokes() {
        if (jokes != null) return jokes;
        var in = Ostraka.class.getResourceAsStream("/assets/scribery/data/ostraka-jokes.json");
        if (in == null) throw new IllegalStateException("ostraka-jokes.json fehlt im Mod");
        Map<String, Joke> out = new LinkedHashMap<>();
        try (Reader r = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            for (JsonElement e : JsonParser.parseReader(r).getAsJsonObject().getAsJsonArray("jokes")) {
                var o = e.getAsJsonObject();
                out.put(o.get("id").getAsString(), new Joke(o.get("greek").getAsString(), o.get("en").getAsString()));
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return jokes = out;
    }

    /** Alle Witz-IDs in der Reihenfolge der Datei. */
    public static List<String> ids() {
        return new ArrayList<>(jokes().keySet());
    }

    /** Der Witz oder null. */
    public static Joke joke(String id) {
        return jokes().get(id);
    }

    /** Ein Ostrakon mit diesem Witz. */
    public static ItemStack of(String id) {
        ItemStack s = new ItemStack(ModItems.OSTRAKON.get());
        s.set(ModComponents.JOKE.get(), id);
        return s;
    }

    /** Index des Ostrakons am Pult und Uebersetzungstisch (ab {@link Manuscripts#OSTRAKA_BASE}), oder -1 ohne Witz. */
    public static int fragmentIndex(ItemStack stack) {
        String id = stack.get(ModComponents.JOKE.get());
        return id == null ? -1 : Manuscripts.ostrakonIndex(id);
    }

    public static ItemStack random(RandomSource random) {
        List<String> ids = ids();
        return of(ids.get(random.nextInt(ids.size())));
    }
}
