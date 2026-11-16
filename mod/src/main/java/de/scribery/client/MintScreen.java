package de.scribery.client;

import de.scribery.MintBlockEntity;
import de.scribery.ModItems;
import de.scribery.ModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;

/** Minting Table: ein Klick auf einen Buchstaben giesst aus einem Eisennugget Letter Types. */
final class MintScreen extends StationScreen {
    private static final int COLS = 8, CELL_W = 30, CELL_H = 34;
    private static final int X0 = 72, Y0 = 50;

    private final BlockPos pos;

    MintScreen(BlockPos pos) {
        super(Workbench.free(Stations.NONE));
        this.pos = pos;
    }

    private int nuggets() {
        var level = Minecraft.getInstance().level;
        return level != null && level.getBlockEntity(pos) instanceof MintBlockEntity be ? be.nuggets() : 0;
    }

    @Override
    protected Stations.Kind kind() {
        return Stations.Kind.MINT;
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
        return "Iron Nuggets " + nuggets();
    }

    private int cellAt(double x, double y) {
        if (x < X0 || y < Y0) return -1;
        int c = (int) ((x - X0) / CELL_W), r = (int) ((y - Y0) / CELL_H);
        if (c >= COLS || r >= 3) return -1;
        int i = r * COLS + c;
        return i < ModItems.LETTER_ITEMS.size() ? i : -1;
    }

    @Override
    protected void drawStation(GuiGraphics g) {
        frame(g, X0 - 8, Y0 - 8, COLS * CELL_W + 16, 3 * CELL_H + 16, 4, "iron_block", 0xFF0E0E12);
        int hover = cellAt(mouseCX, mouseCY);
        for (int i = 0; i < ModItems.LETTER_ITEMS.size(); i++) {
            int x = X0 + (i % COLS) * CELL_W, y = Y0 + (i / COLS) * CELL_H;
            fill(g, x + 1, y + 1, CELL_W - 2, CELL_H - 2, hover == i ? 0xFF3A3D44 : 0xFF23232B);
            fill(g, x + 1, y + CELL_H - 2, CELL_W - 2, 1, 0xFF4A4D54);
            var id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(ModItems.LETTER_ITEMS.get(i).get());
            g.blit(ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "textures/item/" + id.getPath() + ".png"),
                    x + (CELL_W - 16) / 2, y + 5, 0f, 0f, 16, 16, 16, 16);
            String n = "x" + MintBlockEntity.YIELD;
            g.drawString(font, n, x + (CELL_W - font.width(n)) / 2, y + CELL_H - 12, col("parchment_1"), false);
        }
        g.blit(item("iron_nugget"), 140, 176, 0f, 0f, 16, 16, 16, 16);
        String info = "Iron nuggets: " + nuggets() + "     1 nugget = " + MintBlockEntity.YIELD + " letters";
        g.drawString(font, info, 162, 180, col("gui_light"), true);
        String hint = nuggets() > 0 ? "Click a letter to cast it" : "Right-click the table with iron nuggets";
        g.drawString(font, hint, (ui.canvasW - font.width(hint)) / 2, 30, flashTicks > 0 ? col("flame_1") : col("parchment_3"), true);
    }

    @Override
    protected void drawHand(GuiGraphics g) {}

    @Override
    public void clickCanvas(double x, double y, int button) {
        int i = cellAt(x, y);
        if (i < 0 || button != 0) return;
        if (nuggets() <= 0) {
            flash("No iron nuggets");
            return;
        }
        PacketDistributor.sendToServer(new ModNetwork.CastLetter(pos, i));
        sound(SoundEvents.ANVIL_USE, 1.5f, 0.4f);
    }

    @Override
    public void releaseCanvas(double x, double y, int button) {}
}
