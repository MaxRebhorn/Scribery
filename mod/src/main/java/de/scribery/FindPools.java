package de.scribery;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.util.RandomSource;

/**
 * Die Fundart-Pools: Welcher Fundblock (Sand, Schlamm, Asche, Eis, Scherbenhaufen) liefert welche Manuskripte.
 * Daten aus tools/data/find-pools.json (erzeugt von tools/export_find_pools.py). Der Fundblock bestimmt auch das
 * Aussehen des Fragments, siehe docs/DESIGN-FINDS.md.
 */
public final class FindPools {
    /** Die erlaubten Werte fuer das Feld "find" der Loot-Funktion. */
    public static final List<String> FINDS = List.of("sand", "mud", "ashes", "ice", "shards");

    /** Wert von custom_model_data fuer die Zustandsvarianten (siehe ModItems.VARIANT_NAMES). */
    public static final int CHARRED = 1, WATER_DAMAGED = 2, FADED = 3, FROZEN = 4;

    private static Map<String, List<String>> pools;

    private FindPools() {}

    private static synchronized Map<String, List<String>> pools() {
        if (pools != null) return pools;
        var in = FindPools.class.getResourceAsStream("/assets/scribery/data/find-pools.json");
        if (in == null) throw new IllegalStateException("find-pools.json fehlt im Mod");
        Map<String, List<String>> out = new LinkedHashMap<>();
        try (Reader r = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            JsonObject all = JsonParser.parseReader(r).getAsJsonObject().getAsJsonObject("pools");
            for (String find : FINDS) {
                List<String> ids = new ArrayList<>();
                for (JsonElement e : all.getAsJsonArray(find)) ids.add(e.getAsString());
                out.put(find, List.copyOf(ids));
            }
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
        return pools = out;
    }

    /** Die Manuskript-IDs einer Fundart, oder eine leere Liste bei unbekanntem Namen. */
    public static List<String> pool(String find) {
        return pools().getOrDefault(find, List.of());
    }

    /** Ein zufaelliges Manuskript dieser Fundart, oder null, wenn der Pool leer ist. */
    public static String pick(String find, RandomSource random) {
        List<String> ids = pool(find);
        return ids.isEmpty() ? null : ids.get(random.nextInt(ids.size()));
    }

    /** Aussehen des Fragments nach Fundart: 0 = normal, sonst eine der Zustandsvarianten. */
    public static int variant(String find, RandomSource random) {
        return switch (find) {
            case "mud" -> WATER_DAMAGED;
            case "ashes" -> CHARRED;
            case "ice" -> FROZEN;
            case "sand" -> random.nextFloat() < 0.35f ? FADED : 0;   // in der Wueste manchmal verblasst
            default -> 0;
        };
    }
}
