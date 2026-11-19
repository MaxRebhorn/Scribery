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
    private static Map<String, String> findOfId;
    private static Map<String, Integer> tiers;

    private FindPools() {}

    private static synchronized Map<String, List<String>> pools() {
        if (pools != null) return pools;
        var in = FindPools.class.getResourceAsStream("/assets/scribery/data/find-pools.json");
        if (in == null) throw new IllegalStateException("find-pools.json fehlt im Mod");
        Map<String, List<String>> out = new LinkedHashMap<>();
        try (Reader r = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(r).getAsJsonObject();
            JsonObject all = root.getAsJsonObject("pools");
            Map<String, String> reverse = new java.util.HashMap<>();
            Map<String, Integer> tier = new java.util.HashMap<>();
            for (var e : root.getAsJsonObject("tiers").entrySet()) tier.put(e.getKey(), e.getValue().getAsInt());
            tiers = Map.copyOf(tier);
            for (String find : FINDS) {
                List<String> ids = new ArrayList<>();
                for (JsonElement e : all.getAsJsonArray(find)) {
                    ids.add(e.getAsString());
                    reverse.put(e.getAsString(), find);
                }
                out.put(find, List.copyOf(ids));
            }
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
        findOfId = Map.copyOf(reverse);
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

    /** Fundart, in der dieses Manuskript liegt ("sand", "mud", ...), oder null bei unbekannter ID. */
    public static String findOf(String id) {
        pools();
        return findOfId.get(id);
    }

    /** Schwierigkeit: 1 leicht, 2 mittel, 3 schwer; 0, wenn das Manuskript nicht bekannt ist. */
    public static int tier(String id) {
        pools();
        return tiers.getOrDefault(id, 0);
    }

    /**
     * Aussehen des Fragments nach Fundart: 0 = normal, sonst eine der Zustandsvarianten. Haengt nur vom Manuskript ab
     * (nicht vom Zufall), damit ein Manuskript in der Liste, im Loot und auf dem Pult gleich aussieht.
     * Scherben und Amphoren: normal. Sand: gut ein Drittel der Manuskripte verblasst (feste Auswahl nach ID).
     */
    public static int variantOf(String id) {
        String find = findOf(id);
        if (find == null) return 0;
        return switch (find) {
            case "mud" -> WATER_DAMAGED;
            case "ashes" -> CHARRED;
            case "ice" -> FROZEN;
            case "sand" -> Math.floorMod(id.hashCode(), 100) < 35 ? FADED : 0;
            default -> 0;
        };
    }
}
