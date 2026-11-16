package de.scribery.client;

import de.scribery.MintMenu;
import de.scribery.ModItems;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/** Die Oberflaeche des Minting Table im Look des Steinmetz, mit unseren Buchstaben in der Liste. */
public final class MintScreen extends AbstractContainerScreen<MintMenu> {
    private static final ResourceLocation BG = ResourceLocation.withDefaultNamespace("textures/gui/container/stonecutter.png");
    private static final ResourceLocation SCROLLER = ResourceLocation.withDefaultNamespace("container/stonecutter/scroller");
    private static final ResourceLocation SCROLLER_OFF = ResourceLocation.withDefaultNamespace("container/stonecutter/scroller_disabled");
    private static final ResourceLocation RECIPE = ResourceLocation.withDefaultNamespace("container/stonecutter/recipe");
    private static final ResourceLocation RECIPE_SELECTED = ResourceLocation.withDefaultNamespace("container/stonecutter/recipe_selected");
    private static final ResourceLocation RECIPE_HOVER = ResourceLocation.withDefaultNamespace("container/stonecutter/recipe_highlighted");
    private static final int COLS = 4, ROWS = 3, CELL_W = 16, CELL_H = 18, LIST_X = 52, LIST_Y = 14;

    private float scrollOffs;
    private boolean scrolling;
    private int startIndex;

    public MintScreen(MintMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        --titleLabelY;
    }

    private int count() {
        return ModItems.LETTER_ITEMS.size();
    }

    private int offscreenRows() {
        return (count() + COLS - 1) / COLS - ROWS;
    }

    private boolean scrollActive() {
        return menu.hasInput() && count() > COLS * ROWS;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
        if (!menu.hasInput()) return;
        int x = leftPos + LIST_X, y = topPos + LIST_Y;
        for (int i = startIndex; i < Math.min(startIndex + COLS * ROWS, count()); i++) {
            int k = i - startIndex;
            int bx = x + (k % COLS) * CELL_W, by = y + (k / COLS) * CELL_H;
            if (mouseX >= bx && mouseX < bx + CELL_W && mouseY >= by && mouseY < by + CELL_H) {
                g.renderTooltip(font, new ItemStack(ModItems.LETTER_ITEMS.get(i).get()), mouseX, mouseY);
            }
        }
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        g.blit(BG, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        int k = (int) (41.0F * scrollOffs);
        g.blitSprite(scrollActive() ? SCROLLER : SCROLLER_OFF, leftPos + 119, topPos + 15 + k, 12, 15);
        if (!menu.hasInput()) return;
        int x = leftPos + LIST_X, y = topPos + LIST_Y;
        for (int i = startIndex; i < Math.min(startIndex + COLS * ROWS, count()); i++) {
            int n = i - startIndex;
            int bx = x + (n % COLS) * CELL_W, by = y + (n / COLS) * CELL_H;
            boolean hover = mouseX >= bx && mouseX < bx + CELL_W && mouseY >= by && mouseY < by + CELL_H;
            g.blitSprite(i == menu.selected() ? RECIPE_SELECTED : hover ? RECIPE_HOVER : RECIPE, bx, by, CELL_W, CELL_H);
            g.renderItem(new ItemStack(ModItems.LETTER_ITEMS.get(i).get()), bx, by + 1);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        scrolling = false;
        if (menu.hasInput()) {
            int x = leftPos + LIST_X, y = topPos + LIST_Y;
            for (int i = startIndex; i < Math.min(startIndex + COLS * ROWS, count()); i++) {
                int n = i - startIndex;
                double dx = mouseX - (x + (n % COLS) * CELL_W), dy = mouseY - (y + (n / COLS) * CELL_H);
                if (dx >= 0 && dx < CELL_W && dy >= 0 && dy < CELL_H && menu.clickMenuButton(minecraft.player, i)) {
                    minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_STONECUTTER_SELECT_RECIPE, 1.0F));
                    minecraft.gameMode.handleInventoryButtonClick(menu.containerId, i);
                    return true;
                }
            }
            int sx = leftPos + 119, sy = topPos + 9;
            if (mouseX >= sx && mouseX < sx + 12 && mouseY >= sy && mouseY < sy + 54) scrolling = true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if (scrolling && scrollActive()) {
            int top = topPos + 14, bottom = top + 54;
            scrollOffs = Mth.clamp(((float) mouseY - top - 7.5F) / ((bottom - top) - 15.0F), 0.0F, 1.0F);
            startIndex = (int) (scrollOffs * offscreenRows() + 0.5) * COLS;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollActive()) {
            scrollOffs = Mth.clamp(scrollOffs - (float) scrollY / offscreenRows(), 0.0F, 1.0F);
            startIndex = (int) (scrollOffs * offscreenRows() + 0.5) * COLS;
        }
        return true;
    }
}
