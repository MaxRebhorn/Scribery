package de.scribery.client;

import java.util.List;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.PageButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** Leseansicht fuer ein Buch, mit der Textur und den Seitenknoepfen des Minecraft-Buchs. */
final class BookScreen extends Screen {
    private static final ResourceLocation BG = ResourceLocation.withDefaultNamespace("textures/gui/book.png");
    private static final int PAGE_W = 192, TEXT_X = 36, TEXT_Y = 32;

    private final String markdown;
    private List<BookText.Page> pages = List.of();
    private int page;
    private PageButton forward, back;

    BookScreen(String markdown) {
        super(Component.literal("Book"));
        this.markdown = markdown;
    }

    /** Linker Rand der linken und der rechten Seite. */
    private int leftX() {
        return (width - 2 * PAGE_W) / 2;
    }

    private int rightX() {
        return leftX() + PAGE_W;
    }

    @Override
    protected void init() {
        if (pages.isEmpty()) pages = BookText.paginate(font, markdown);
        forward = addRenderableWidget(new PageButton(rightX() + 116, 159, true, b -> turn(1), true));
        back = addRenderableWidget(new PageButton(leftX() + 43, 159, false, b -> turn(-1), true));
        updateButtons();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** Blaettert um d Doppelseiten. {@code page} ist immer die Nummer der linken Seite. */
    private void turn(int d) {
        goTo(page + 2 * d);
    }

    private void updateButtons() {
        forward.visible = page + 2 < pages.size();
        back.visible = page > 0;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (minecraft != null && minecraft.options.keyInventory.matches(keyCode, scanCode)) {
            onClose();
            return true;
        }
        switch (keyCode) {
            case org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT, org.lwjgl.glfw.GLFW.GLFW_KEY_PAGE_DOWN, org.lwjgl.glfw.GLFW.GLFW_KEY_D -> {
                turn(1);
                return true;
            }
            case org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT, org.lwjgl.glfw.GLFW.GLFW_KEY_PAGE_UP, org.lwjgl.glfw.GLFW.GLFW_KEY_A -> {
                turn(-1);
                return true;
            }
            default -> { }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        turn(scrollY < 0 ? 1 : -1);
        return true;
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(g, mouseX, mouseY, partialTick);
        g.blit(BG, leftX(), 2, 0, 0, PAGE_W, PAGE_W);
        g.blit(BG, rightX(), 2, 0, 0, PAGE_W, PAGE_W);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        String indicator = pages.isEmpty() ? "" : (page + 1) + (page + 2 <= pages.size() ? "-" + (page + 2) : "") + " / " + pages.size();
        g.drawString(font, indicator, rightX() + PAGE_W - 44 - font.width(indicator), 18, 0xFF000000, false);
        drawPage(g, page, leftX() + TEXT_X);
        drawPage(g, page + 1, rightX() + TEXT_X);
    }

    private void drawPage(GuiGraphics g, int index, int x0) {
        if (index >= pages.size()) return;
        int y0 = TEXT_Y;
        for (BookText.Drawn d : pages.get(index).items()) {
            if (d.text() == null) {
                g.fill(x0 + d.x(), y0 + d.y(), x0 + d.x() + d.ruleWidth(), y0 + d.y() + 1, d.color());
            } else if (d.scale() == 1f) {
                g.drawString(font, d.text(), x0 + d.x(), y0 + d.y(), d.color(), false);
            } else {
                g.pose().pushPose();
                g.pose().translate(x0 + d.x(), y0 + d.y(), 0);
                g.pose().scale(d.scale(), d.scale(), 1f);
                g.drawString(font, d.text(), 0, 0, d.color(), false);
                g.pose().popPose();
            }
        }
    }

    /** Fuer den Selbsttest. */
    void goTo(int p) {
        page = Math.max(0, Math.min(Math.max(0, pages.size() - 1), p)) / 2 * 2;
        updateButtons();
    }

    /** Fuer den Selbsttest: Nummer der ersten Seite, deren Text den Suchbegriff enthaelt, sonst 0. */
    int findPage(String needle) {
        for (int i = 0; i < pages.size(); i++) {
            for (BookText.Drawn d : pages.get(i).items()) {
                if (d.text() == null) continue;
                StringBuilder sb = new StringBuilder();
                d.text().accept((idx, st, cp) -> { sb.appendCodePoint(cp); return true; });
                if (sb.toString().contains(needle)) return i;
            }
        }
        return 0;
    }

    int pageCount() {
        return pages.size();
    }
}
