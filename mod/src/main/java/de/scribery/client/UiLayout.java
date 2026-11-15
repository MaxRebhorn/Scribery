package de.scribery.client;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/** Raster und Palette der Presse, gelesen aus art/layout.json und art/palette.json. */
public final class UiLayout {
    private static UiLayout instance;

    public final int canvasW, canvasH;
    public final int sortW, sortH, pitch, lineSlots, rows, rowPitch;
    public final int slotsX, slotsY, platenTravel;
    public final int caseCols, cellW, cellH, caseX, caseY, sortOffX, sortOffY, countOffY;
    public final char[] letters;

    private final Map<String, int[]> rects = new HashMap<>();
    private final Map<String, Integer> colors = new HashMap<>();

    public static UiLayout get() {
        if (instance == null) instance = new UiLayout();
        return instance;
    }

    private UiLayout() {
        JsonObject l = read("/assets/scribery/ui/layout.json");
        JsonObject canvas = l.getAsJsonObject("canvas");
        canvasW = canvas.get("w").getAsInt();
        canvasH = canvas.get("h").getAsInt();
        JsonObject sort = l.getAsJsonObject("sort");
        sortW = sort.get("w").getAsInt();
        sortH = sort.get("h").getAsInt();
        pitch = sort.get("pitch").getAsInt();
        lineSlots = l.get("line_slots").getAsInt();
        rows = l.get("forme_rows").getAsInt();
        rowPitch = l.get("row_pitch").getAsInt();
        slotsX = l.getAsJsonArray("forme_slots_origin").get(0).getAsInt();
        slotsY = l.getAsJsonArray("forme_slots_origin").get(1).getAsInt();
        platenTravel = l.get("platen_travel").getAsInt();

        JsonObject c = l.getAsJsonObject("case");
        caseCols = c.get("cols").getAsInt();
        cellW = c.get("cell_w").getAsInt();
        cellH = c.get("cell_h").getAsInt();
        caseX = c.getAsJsonArray("origin").get(0).getAsInt();
        caseY = c.getAsJsonArray("origin").get(1).getAsInt();
        sortOffX = c.getAsJsonArray("sort_offset").get(0).getAsInt();
        sortOffY = c.getAsJsonArray("sort_offset").get(1).getAsInt();
        countOffY = c.get("count_offset_y").getAsInt();
        JsonArray ls = c.getAsJsonArray("letters");
        letters = new char[ls.size()];
        for (int i = 0; i < letters.length; i++) letters[i] = ls.get(i).getAsString().charAt(0);

        for (var e : l.getAsJsonObject("rects").entrySet()) {
            JsonArray r = e.getValue().getAsJsonObject().getAsJsonArray("rect");
            rects.put(e.getKey(), new int[] {
                    r.get(0).getAsInt(), r.get(1).getAsInt(), r.get(2).getAsInt(), r.get(3).getAsInt() });
        }

        for (var e : read("/assets/scribery/ui/palette.json").entrySet()) {
            String v = e.getValue().getAsString();
            if (v.startsWith("#")) colors.put(e.getKey(), 0xFF000000 | Integer.parseInt(v.substring(1), 16));
        }
    }

    private static JsonObject read(String path) {
        var in = UiLayout.class.getResourceAsStream(path);
        if (in == null) throw new IllegalStateException("Fehlt im Mod: " + path);
        try (Reader r = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(r).getAsJsonObject();
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Rechteck als {x, y, w, h} in GUI-Pixeln der Leinwand. */
    public int[] rect(String name) {
        int[] r = rects.get(name);
        if (r == null) throw new IllegalArgumentException("Unbekanntes Rechteck: " + name);
        return r;
    }

    /** Palettenfarbe als ARGB. */
    public int color(String name) {
        Integer c = colors.get(name);
        if (c == null) throw new IllegalArgumentException("Unbekannte Farbe: " + name);
        return c;
    }
}
