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
        List<String> words = bench.fragment.words().stream().map(w -> w.greek()).toList();
        Fit fit = fit(words, SHEET, 14, 12);
        RaggedPaper.draw(g, ui, fit.paper()[0], fit.paper()[1], fit.paper()[2], fit.paper()[3], 2);
        for (int i = 0; i < fit.words().length; i++) {
            g.drawString(font, words.get(i), fit.words()[i][0], fit.words()[i][1], col("ink_1"), false);
        }
        String ref = bench.fragment.ref();
        g.drawString(font, ref, fit.paper()[0] + (fit.paper()[2] - font.width(ref)) / 2,
                fit.paper()[1] + fit.paper()[3] - 11, col("parchment_0"), false);
    }

    @Override
    protected void drawHand(GuiGraphics g) {}

    @Override
    public void clickCanvas(double x, double y, int button) {}

    @Override
    public void releaseCanvas(double x, double y, int button) {}
}
