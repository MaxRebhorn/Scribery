package de.scribery.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Ausgefranstes Pergament in beliebiger Groesse, aus Palettenfarben gezeichnet. Die Form haengt nur von
 * Groesse und Rand ab und wird einmal berechnet, dann nur noch als Streifen gefuellt.
 */
final class RaggedPaper {
    private record Run(int x, int y, int len, String color) {}

    private static final Map<Long, List<Run>> CACHE = new HashMap<>();

    private RaggedPaper() {}

    /** @param edge groesste Tiefe der Ausfransung in Pixeln, links/rechts und oben/unten */
    static void draw(GuiGraphics g, UiLayout ui, int x, int y, int w, int h, int edge) {
        long key = ((long) w << 40) ^ ((long) h << 20) ^ edge;
        List<Run> runs = CACHE.computeIfAbsent(key, k -> build(w, h, edge, k));
        for (Run r : runs) g.fill(x + r.x, y + r.y, x + r.x + r.len, y + r.y + 1, ui.color(r.color));
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

    private static List<Run> build(int w, int h, int edge, long seed) {
        Random rng = new Random(seed);
        int[] top = profile(rng, w, edge), bot = profile(rng, w, edge);
        int[] left = profile(rng, h, edge + 1), right = profile(rng, h, edge + 1);
        boolean[][] solid = new boolean[h][w];
        for (int yy = 0; yy < h; yy++) {
            for (int xx = 0; xx < w; xx++) {
                solid[yy][xx] = !(yy < top[xx] || yy >= h - bot[xx] || xx < left[yy] || xx >= w - right[yy]);
            }
        }
        int chips = Math.max(1, w * h / 500);
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
            }
        }
        for (int i = 0; i < w * h / 40; i++) { // Fasern
            int fx = rng.nextInt(w), fy = rng.nextInt(h), len = 2 + rng.nextInt(4);
            for (int k = 0; k < len; k++) {
                if (fx + k < w && "parchment_3".equals(color[fy][fx + k])) color[fy][fx + k] = "parchment_2";
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

    private static boolean at(boolean[][] s, int x, int y) {
        return y >= 0 && y < s.length && x >= 0 && x < s[0].length && s[y][x];
    }
}
