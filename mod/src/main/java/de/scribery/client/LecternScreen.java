package de.scribery.client;

import java.util.List;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Nur das Pult: Kerze, Titelleiste und in der Mitte das aufgelegte Fragment. Liegt nichts auf dem Pult,
 * bleibt die Flaeche leer. Weitere Stationen erreicht man ueber die Pfeile, wenn sie angrenzen.
 */
final class LecternScreen extends StationScreen {

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
    protected void drawStation(GuiGraphics g) {
        if (bench.fragment == null) return;
        // Dieselbe Anordnung wie auf dem Pult in der Presse, nur doppelt vergroessert.
        List<String> words = bench.fragment.words().stream().map(w -> w.greek()).toList();
        g.pose().pushPose();
        g.pose().translate((ui.canvasW - 2 * 66) / 2f, (ui.canvasH - 2 * 62) / 2f, 0);
        g.pose().scale(2f, 2f, 1f);
        drawFragmentPaper(g, words, new int[] { 0, 0, 66, 62 }, i -> false);
        g.pose().popPose();
    }

    @Override
    protected void drawHand(GuiGraphics g) {}

    @Override
    public void clickCanvas(double x, double y, int button) {}

    @Override
    public void releaseCanvas(double x, double y, int button) {}
}
