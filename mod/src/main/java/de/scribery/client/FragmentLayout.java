package de.scribery.client;

import java.util.List;

import net.minecraft.client.gui.Font;

/**
 * Wie ein Fragment auf Papier gesetzt wird. Pult, Presse, Uebersetzung und Vollansicht benutzen genau diese
 * Anordnung, damit das Fragment ueberall gleich aussieht.
 */
final class FragmentLayout {
    private FragmentLayout() {}

    /** Papier {x, y, w, h} und je Wort {x, y, breite}. */
    record Result(int[] paper, int[][] words) {}

    static final int PAD = 4;
    static final int LINE = 9;
    /** Wenn der Text sonst nicht in die Flaeche passt, werden die Zeilen enger gesetzt. */
    private static final int TIGHT_LINE = 8;

    /** Setzt die Woerter in die Flaeche; das Papier ist nur so gross wie der Text und sitzt mittig. */
    static Result fit(Font font, List<String> words, int[] area) {
        Result r = place(font, words, area, LINE);
        if (r.paper()[3] >= area[3] && lastBottom(r) > area[1] + area[3] - PAD) r = place(font, words, area, TIGHT_LINE);
        return r;
    }

    private static int lastBottom(Result r) {
        int b = 0;
        for (int[] w : r.words()) b = Math.max(b, w[1] + 9);
        return b;
    }

    private static Result place(Font font, List<String> words, int[] area, int lineHeight) {
        int wrap = area[2] - 2 * PAD, space = font.width(" ");
        int[][] at0 = new int[words.size()][];
        int cx = 0, cy = 0, bw = 0, bh = 0;
        for (int i = 0; i < words.size(); i++) {
            int w = font.width(words.get(i));
            if (cx > 0 && cx + w > wrap) {
                cx = 0;
                cy += lineHeight;
            }
            at0[i] = new int[] { cx, cy, w };
            bw = Math.max(bw, cx + w);
            bh = Math.max(bh, cy + 9);
            cx += w + space;
        }
        int pw = Math.min(area[2], Math.max(24, bw + 2 * PAD));
        int ph = Math.max(16, bh + 2 * PAD);
        int px = area[0] + (area[2] - pw) / 2;
        int py = area[1] + Math.max(0, (area[3] - ph) / 2);
        int[][] out = new int[at0.length][];
        for (int i = 0; i < at0.length; i++) {
            out[i] = new int[] { px + PAD + at0[i][0], py + PAD + at0[i][1], at0[i][2] };
        }
        return new Result(new int[] { px, py, pw, ph }, out);
    }
}
