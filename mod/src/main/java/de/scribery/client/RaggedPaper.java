package de.scribery.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Ausgefranstes Pergament in beliebiger Groesse, aus Palettenfarben gezeichnet. Die Form haengt nur von
 * Groesse, Rand und Zustand ab und wird einmal berechnet, dann nur noch als Streifen gefuellt.
 *
 * <p>Der Zustand ({@code look}) ist der Wert von custom_model_data des Fragments, also dieselben Varianten wie in der
 * Inventar-Textur: 0 normal, 1 verkohlt, 2 wassergeschaedigt, 3 verblasst, 4 gefroren. So passen Nahansicht und
 * Pult zum Item.
 */
final class RaggedPaper {
    static final int NORMAL = 0, CHARRED = 1, WATER_DAMAGED = 2, FADED = 3, FROZEN = 4, SHERD = 5;

    /** Farben je Zustand: parchment_0 (Rand), parchment_1, parchment_2, parchment_3 (Flaeche), Tinte, Sonderfarbe. */
    private static final int[][] LOOKS = {
        {},
        {0xFF1C1411, 0xFF4B3524, 0xFF74553A, 0xFF8B6746, 0xFF160F0C, 0xFF000000},   // verkohlt
        {0xFF5A4630, 0xFF8E7A52, 0xFFB09A68, 0xFFCDB985, 0xFF3E3228, 0xFF7F917C},   // wasser: Sonder = Wasserrand
        {0xFF6B5638, 0xFFB89E6E, 0xFFDCCB9B, 0xFFEBDDB4, 0xFF8E7D66, 0xFF000000},   // verblasst: blasse Tinte
        {0xFF3C5F8E, 0xFF7FA6CE, 0xFFA8C8E4, 0xFFBFD9EE, 0xFF22395A, 0xFFEAF6FF},   // gefroren: Sonder = Frost
        {0xFF5E5246, 0xFF8C7F6A, 0xFFA89A82, 0xFFBBAE96, 0xFF1F1812, 0xFF7C6F5A},   // Scherbe (Ostrakon): grauer Kalkstein wie auf alten Fotos, Sonder = dunkle Flecken
    };

    private static final int SHERD_PAD = 8;

    private record Run(int x, int y, int len, String color) {}

    private static final Map<Long, List<Run>> CACHE = new HashMap<>();

    private RaggedPaper() {}

    /** Zustand aus dem Wert von custom_model_data; alles Unbekannte gilt als normal. */
    static int look(int customModelData) {
        return customModelData >= 1 && customModelData <= 5 ? customModelData : NORMAL;
    }

    /** Farbe einer Palettenfarbe im jeweiligen Zustand. */
    static int color(UiLayout ui, int look, String name) {
        if (look == NORMAL) return ui.color(name);
        int[] t = LOOKS[look];
        return switch (name) {
            case "parchment_0" -> t[0];
            case "parchment_1" -> t[1];
            case "parchment_2" -> t[2];
            case "parchment_3" -> t[3];
            case "special" -> t[5];
            default -> ui.color(name);
        };
    }

    /** Tintenfarbe fuer den Text auf dem Papier dieses Zustands. */
    static int ink(UiLayout ui, int look) {
        return look == NORMAL ? ui.color("ink_1") : LOOKS[look][4];
    }

    /** @param edge groesste Tiefe der Ausfransung in Pixeln, links/rechts und oben/unten */
    static void draw(GuiGraphics g, UiLayout ui, int x, int y, int w, int h, int edge) {
        draw(g, ui, x, y, w, h, edge, NORMAL);
    }

    static void draw(GuiGraphics g, UiLayout ui, int x0, int y0, int w0, int h0, int edge, int look) {
        // Die Scherbe ist rund und braucht rundum Platz um den Text
        final int pad = look == SHERD ? SHERD_PAD : 0;
        final int x = x0 - pad, y = y0 - pad, w = w0 + 2 * pad, h = h0 + 2 * pad;
        long key = ((long) look << 60) ^ ((long) w << 40) ^ ((long) h << 20) ^ edge;
        List<Run> runs = CACHE.computeIfAbsent(key, k -> build(w, h, edge, k, look));
        for (Run r : runs) g.fill(x + r.x, y + r.y, x + r.x + r.len, y + r.y + 1, color(ui, look, r.color));
    }

    /** Sauberes, glatt geschnittenes Papier: hell, ohne Fransen, mit zarter Kante und Schatten. */
    static void drawClean(GuiGraphics g, UiLayout ui, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, ui.color("parchment_2"));
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, ui.color("parchment_3"));
        g.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, ui.color("parchment_2"));
        g.fill(x, y + h - 1, x + w, y + h, ui.color("parchment_1"));
        g.fill(x + w - 1, y, x + w, y + h, ui.color("parchment_1"));
    }

    private static int[] profile(Random rng, int n, int max) {
        int[] out = new int[n];
        int d = rng.nextInt(max + 1);
        for (int i = 0; i < n; i++) {
            if (rng.nextDouble() < 0.45) d = Math.max(0, Math.min(max, d + (rng.nextBoolean() ? 1 : -1)));
            out[i] = d;
        }
        return out;
    }

    private static List<Run> build(int w, int h, int edge, long seed, int look) {
        if (look == SHERD) return buildSherd(w, h, seed);
        Random rng = new Random(seed);
        // Verkohltes Papier franst tiefer aus
        int depth = look == CHARRED ? edge + 1 : edge;
        int[] top = profile(rng, w, depth), bot = profile(rng, w, depth);
        int[] left = profile(rng, h, depth + 1), right = profile(rng, h, depth + 1);
        boolean[][] solid = new boolean[h][w];
        for (int yy = 0; yy < h; yy++) {
            for (int xx = 0; xx < w; xx++) {
                solid[yy][xx] = !(yy < top[xx] || yy >= h - bot[xx] || xx < left[yy] || xx >= w - right[yy]);
            }
        }
        int chips = Math.max(1, w * h / (look == CHARRED ? 160 : 500));
        for (int i = 0; i < chips; i++) { // kleine Ausbrueche am oberen und unteren Rand
            int cx = 2 + rng.nextInt(Math.max(1, w - 4));
            int cy = rng.nextBoolean() ? rng.nextInt(2) : h - 1 - rng.nextInt(2);
            int cw = 1 + rng.nextInt(3), ch = 1 + rng.nextInt(2);
            for (int dx = 0; dx < cw; dx++) {
                for (int dy = 0; dy < ch; dy++) {
                    int px = cx + dx, py = cy + (cy < h / 2 ? dy : -dy);
                    if (px >= 0 && px < w && py >= 0 && py < h) solid[py][px] = false;
                }
            }
        }
        String[][] color = new String[h][w];
        for (int yy = 0; yy < h; yy++) {
            for (int xx = 0; xx < w; xx++) {
                if (!solid[yy][xx]) continue;
                boolean e = !(at(solid, xx - 1, yy) && at(solid, xx + 1, yy) && at(solid, xx, yy - 1) && at(solid, xx, yy + 1));
                boolean n = !(at(solid, xx - 2, yy) && at(solid, xx + 2, yy) && at(solid, xx, yy - 2) && at(solid, xx, yy + 2));
                color[yy][xx] = e ? "parchment_0" : n ? "parchment_1" : "parchment_3";
                // verkohlt: der Rand ist breit angebrannt
                if (look == CHARRED && n && !e) color[yy][xx] = rng.nextDouble() < 0.7 ? "parchment_1" : "parchment_0";
            }
        }
        for (int i = 0; i < w * h / 40; i++) { // Fasern
            int fx = rng.nextInt(w), fy = rng.nextInt(h), len = 2 + rng.nextInt(4);
            for (int k = 0; k < len; k++) {
                if (fx + k < w && "parchment_3".equals(color[fy][fx + k])) color[fy][fx + k] = "parchment_2";
            }
        }
        switch (look) {
            case CHARRED -> blobs(color, rng, 1 + w * h / 700, 2, 4, 3, 3, "parchment_1", "parchment_2", null);   // Brandflecken
            case WATER_DAMAGED -> blobs(color, rng, 2 + w * h / 1200, 3, 8, 2, 5, "parchment_2", "parchment_2", "special"); // Flecken mit Wasserrand
            case FROZEN -> {
                for (int i = 0; i < w * h / 45; i++) { // Frost: einzelne helle Punkte
                    int fx = rng.nextInt(w), fy = rng.nextInt(h);
                    if (color[fy][fx] != null && !"parchment_0".equals(color[fy][fx])) color[fy][fx] = "special";
                }
            }
            default -> { }
        }
        List<Run> runs = new ArrayList<>();
        for (int yy = 0; yy < h; yy++) {
            int xx = 0;
            while (xx < w) {
                String c = color[yy][xx];
                if (c == null) { xx++; continue; }
                int start = xx;
                while (xx < w && c.equals(color[yy][xx])) xx++;
                runs.add(new Run(start, yy, xx - start, c));
            }
        }
        return runs;
    }

    /**
     * Scherbe nach dem Vorbild echter Ostraka: ein abgerundetes, unregelmaessiges Stueck mit ein, zwei schraegen
     * Bruchkanten, helle Flaeche mit dunklen Flecken, Koernung und feinen Rissen. Kein ausgefranster Rand wie beim Papier.
     */
    private static List<Run> buildSherd(int w, int h, long seed) {
        Random rng = new Random(seed);
        // Umriss: ein Rechteck mit unregelmaessigen Kanten (hoechstens SHERD_PAD - 2 Pixel nach innen) und abgerundeten oder
        // schraeg abgebrochenen Ecken. So bleibt der Text immer vollstaendig auf der Scherbe.
        int maxDepth = SHERD_PAD - 3;
        int[] top = profile(rng, w, maxDepth), bot = profile(rng, w, maxDepth), left = profile(rng, h, maxDepth), right = profile(rng, h, maxDepth);
        boolean[][] solid = new boolean[h][w];
        for (int yy = 0; yy < h; yy++) {
            for (int xx = 0; xx < w; xx++) {
                solid[yy][xx] = !(yy < top[xx] || yy >= h - bot[xx] || xx < left[yy] || xx >= w - right[yy]);
            }
        }
        for (int corner = 0; corner < 4; corner++) {
            int c = 3 + rng.nextInt(2 * SHERD_PAD - 3);          // Groesse der Ecke, kleiner als der Abstand der Textecke
            boolean round = rng.nextBoolean();
            for (int dy = 0; dy < c; dy++) {
                for (int dx = 0; dx < c; dx++) {
                    boolean cut = round ? Math.pow(c - 1 - dx, 2) + Math.pow(c - 1 - dy, 2) > Math.pow(c - 1, 2) : dx + dy < c - 1;
                    if (!cut) continue;
                    int px = corner % 2 == 0 ? dx : w - 1 - dx, py = corner < 2 ? dy : h - 1 - dy;
                    if (px >= 0 && px < w && py >= 0 && py < h) solid[py][px] = false;
                }
            }
        }
        String[][] color = new String[h][w];
        for (int yy = 0; yy < h; yy++) {
            for (int xx = 0; xx < w; xx++) {
                if (!solid[yy][xx]) continue;
                boolean e = !(at(solid, xx - 1, yy) && at(solid, xx + 1, yy) && at(solid, xx, yy - 1) && at(solid, xx, yy + 1));
                boolean nearEdge = !(at(solid, xx - 2, yy) && at(solid, xx + 2, yy) && at(solid, xx, yy - 2) && at(solid, xx, yy + 2));
                color[yy][xx] = e ? "parchment_0" : nearEdge && (xx + yy) % 2 == 0 ? "parchment_1" : "parchment_3";
            }
        }
        blobs(color, rng, 2 + w * h / 900, 3, 9, 3, 8, "parchment_2", "parchment_3", null);   // helle und dunklere Partien der Oberflaeche
        blobs(color, rng, 1 + w * h / 1500, 1, 3, 1, 3, "special", "parchment_2", null);       // dunkle Flecken
        for (int i = 0; i < w * h / 22; i++) { // Koernung
            int fx = rng.nextInt(w), fy = rng.nextInt(h);
            if (color[fy][fx] != null && !"parchment_0".equals(color[fy][fx])) color[fy][fx] = rng.nextDouble() < 0.55 ? "parchment_2" : "special";
        }
        for (int i = 0; i < 1 + w * h / 1400; i++) { // feine Risse: kurze gerade Linien
            double a = rng.nextDouble() * Math.PI;
            double x = rng.nextInt(w), y = rng.nextInt(h);
            int len = 6 + rng.nextInt(12);
            for (int k = 0; k < len; k++) {
                int px = (int) Math.round(x + Math.cos(a) * k), py = (int) Math.round(y + Math.sin(a) * k);
                if (px >= 0 && px < w && py >= 0 && py < h && color[py][px] != null && !"parchment_0".equals(color[py][px])) color[py][px] = "parchment_1";
            }
        }
        List<Run> runs = new ArrayList<>();
        for (int yy = 0; yy < h; yy++) {
            int xx = 0;
            while (xx < w) {
                String c = color[yy][xx];
                if (c == null) { xx++; continue; }
                int start = xx;
                while (xx < w && c.equals(color[yy][xx])) xx++;
                runs.add(new Run(start, yy, xx - start, c));
            }
        }
        return runs;
    }

    /** Flecken (Ellipsen) in die Flaeche malen: innen {@code inner}, mit {@code mix} gemischt, aussen ein Rand {@code ring}. */
    private static void blobs(String[][] color, Random rng, int count, int rxMin, int rxMax, int ryMin, int ryMax,
                              String inner, String mix, String ring) {
        int h = color.length, w = color[0].length;
        for (int i = 0; i < count; i++) {
            int cx = rng.nextInt(w), cy = rng.nextInt(h);
            double rx = rxMin + rng.nextInt(rxMax - rxMin + 1), ry = ryMin + rng.nextInt(ryMax - ryMin + 1);
            for (int yy = Math.max(0, cy - (int) ry - 1); yy <= Math.min(h - 1, cy + (int) ry + 1); yy++) {
                for (int xx = Math.max(0, cx - (int) rx - 1); xx <= Math.min(w - 1, cx + (int) rx + 1); xx++) {
                    if (color[yy][xx] == null || "parchment_0".equals(color[yy][xx])) continue;
                    double d = Math.pow((xx - cx) / rx, 2) + Math.pow((yy - cy) / ry, 2);
                    if (d > 1.0) continue;
                    if (ring != null && d > 0.62) color[yy][xx] = rng.nextDouble() < 0.6 ? ring : inner;
                    else color[yy][xx] = rng.nextDouble() < 0.55 ? inner : mix;
                }
            }
        }
    }

    private static boolean at(boolean[][] s, int x, int y) {
        return y >= 0 && y < s.length && x >= 0 && x < s[0].length && s[y][x];
    }
}
