package de.scribery.client;

import java.util.List;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Nur das Pult: Kerze, Titelleiste und in der Mitte das aufgelegte Fragment. Liegt nichts auf dem Pult,
 * bleibt die Flaeche leer. Weitere Stationen erreicht man ueber die Pfeile, wenn sie angrenzen.
 */
final class LecternScreen extends StationScreen {
    private static final int[] SHEET = { 96, 44, 192, 122 };

    LecternScreen(Workbench bench) {
        super(bench);
    }

    @Override
    protected Stations.Kind kind() {
        return Stations.Kind.LECTERN;
    }

    @Override
    protected void onBenchChanged() {}

    @Override
    protected void stationTick() {}

    @Override
    protected boolean busy() {
        return false;
    }

    @Override
    protected String resourceText() {
        return "";
    }

    @Override
    protected void drawStation(GuiGraphics g) {
        if (bench.fragment == null) return;
        int wood = col("wood_2");
        panel(g, SHEET[0] - 6, SHEET[1] - 6, SHEET[2] + 12, SHEET[3] + 12, col("wood_2"), col("wood_3"), col("wood_0"));
        fill(g, SHEET[0], SHEET[1], SHEET[2], SHEET[3], col("parchment_1"));
        fill(g, SHEET[0], SHEET[1] + SHEET[3] - 1, SHEET[2], 1, col("parchment_0"));
        // ausgerissene Kanten
        fill(g, SHEET[0], SHEET[1], 4, 3, wood);
        fill(g, SHEET[0] + SHEET[2] - 6, SHEET[1], 6, 4, wood);
        fill(g, SHEET[0], SHEET[1] + SHEET[3] - 8, 5, 8, wood);
        fill(g, SHEET[0] + SHEET[2] - 10, SHEET[1] + SHEET[3] - 6, 10, 6, wood);

        List<String> words = bench.fragment.words().stream().map(w -> w.greek()).toList();
        int[][] rects = flowWords(words, SHEET[0] + 10, SHEET[1] + 12, SHEET[2] - 20, 14);
        for (int i = 0; i < rects.length; i++) {
            g.drawString(font, words.get(i), rects[i][0], rects[i][1], col("ink_1"), false);
        }
        String ref = bench.fragment.ref();
        g.drawString(font, ref, SHEET[0] + (SHEET[2] - font.width(ref)) / 2, SHEET[1] + SHEET[3] - 14,
                col("parchment_0"), false);
    }

    @Override
    protected void drawHand(GuiGraphics g) {}

    @Override
    public void clickCanvas(double x, double y, int button) {}

    @Override
    public void releaseCanvas(double x, double y, int button) {}
}
