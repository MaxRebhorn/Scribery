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
 * Die Witze auf den Ostraka (nacherzaehlt nach dem Philogelos, Quelle tools/data/ostraka-jokes.json, beim Bauen nach
 * assets/scribery/data/ kopiert). Ein Ostrakon traegt die Witz-ID im Datenfeld {@link ModComponents#JOKE}.
 */
public final class Ostraka {
    private static Map<String, String> jokes;

    private Ostraka() {}

    private static synchronized Map<String, String> jokes() {
        if (jokes != null) return jokes;
        var in = Ostraka.class.getResourceAsStream("/assets/scribery/data/ostraka-jokes.json");
        if (in == null) throw new IllegalStateException("ostraka-jokes.json fehlt im Mod");
        Map<String, String> out = new LinkedHashMap<>();
        try (Reader r = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            for (JsonElement e : JsonParser.parseReader(r).getAsJsonObject().getAsJsonArray("jokes")) {
                var o = e.getAsJsonObject();
                out.put(o.get("id").getAsString(), o.get("en").getAsString());
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

    /** Der Text eines Witzes (Zeilen durch \n getrennt) oder null. */
    public static String text(String id) {
        return jokes().get(id);
    }

    /** Ein Ostrakon mit diesem Witz. */
    public static ItemStack of(String id) {
        ItemStack s = new ItemStack(ModItems.OSTRAKON.get());
        s.set(ModComponents.JOKE.get(), id);
        return s;
    }

    public static ItemStack random(RandomSource random) {
        List<String> ids = ids();
        return of(ids.get(random.nextInt(ids.size())));
    }
}
