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
        int[] f = ui.rect("fragment");
        g.pose().translate((ui.canvasW - 2 * f[2]) / 2f, (ui.canvasH - 2 * f[3]) / 2f, 0);
        g.pose().scale(2f, 2f, 1f);
        drawFragmentPaper(g, words, new int[] { 0, 0, f[2], f[3] }, i -> false);
        g.pose().popPose();
        if (bench.lecternPos != null) drawButton(g, takeRect(), TAKE);
    }

    private static final String TAKE = "Take manuscript";

    private int[] takeRect() {
        String t = TAKE;
        return buttonRect(t, (ui.canvasW - (font.width(t) + 12)) / 2, 196);
    }

    @Override
    protected void drawHand(GuiGraphics g) {}

    @Override
    public void clickCanvas(double x, double y, int button) {
        if (button == 0 && bench.lecternPos != null && bench.fragment != null && in(takeRect(), x, y, 0)) {
            net.neoforged.neoforge.network.PacketDistributor.sendToServer(new de.scribery.ModNetwork.TakeItem(bench.lecternPos));
            onClose();
        }
    }

    @Override
    public void releaseCanvas(double x, double y, int button) {}
}
