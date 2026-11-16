package de.scribery.client;

import java.util.ArrayList;
import java.util.List;

import de.scribery.SheetData;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Vollansicht eines Fragments, bedruckten oder uebersetzten Bogens, wie eine Buchseite. Das Fragment hat
 * dieselbe Anordnung wie auf dem Pult, nur vergroessert.
 */
final class SheetViewScreen extends Screen {
    private final UiLayout ui = UiLayout.get();
    private final List<String> fragmentWords;
    private final SheetData sheet;

    /** Fragment: nur die griechischen Woerter. */
    SheetViewScreen(List<String> fragmentWords) {
        super(Component.literal("Fragment"));
        this.fragmentWords = fragmentWords;
        this.sheet = null;
    }

    /** Bedruckter oder uebersetzter Bogen. */
    SheetViewScreen(SheetData sheet) {
        super(Component.literal(sheet.english().isEmpty() ? "Printed Sheet" : "Translated Sheet"));
        this.fragmentWords = null;
        this.sheet = sheet;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (minecraft != null && minecraft.options.keyInventory.matches(keyCode, scanCode)) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        onClose();
        return true;
    }

    private int color(String name) {
        return ui.color(name);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        if (fragmentWords != null) renderFragment(g);
        else renderSheet(g);
    }

    private void renderFragment(GuiGraphics g) {
        // 2-fach: dieselbe Anordnung wie am Pult
        int[] f = ui.rect("fragment");
        int s = Math.max(1, Math.min(2, Math.min(width / f[2], height / f[3])));
        g.pose().pushPose();
        g.pose().translate((width - s * f[2]) / 2f, (height - s * f[3]) / 2f, 0);
        g.pose().scale(s, s, 1f);
        var fit = FragmentLayout.fit(font, fragmentWords, new int[] { 0, 0, f[2], f[3] });
        RaggedPaper.draw(g, ui, fit.paper()[0], fit.paper()[1], fit.paper()[2], fit.paper()[3], 2);
        for (int i = 0; i < fit.words().length; i++) {
            g.drawString(font, fragmentWords.get(i), fit.words()[i][0], fit.words()[i][1], color("ink_1"), false);
        }
        g.pose().popPose();
    }

    private void renderSheet(GuiGraphics g) {
        boolean translated = !sheet.english().isEmpty();
        List<String> latin = new ArrayList<>();
        for (String line : sheet.lines()) for (String w : line.split(" ")) if (!w.isBlank()) latin.add(w);
        int rowH = translated ? 22 : 12, wrap = 134;
        List<int[]> pos = new ArrayList<>();
        int cx = 0, cy = 0, bw = 0;
        for (int i = 0; i < latin.size(); i++) {
            int w = Math.max(font.width(latin.get(i)), translated && i < sheet.english().size() ? font.width(sheet.english().get(i)) : 0);
            if (cx > 0 && cx + w > wrap) { cx = 0; cy += rowH; }
            pos.add(new int[] { cx, cy, w });
            bw = Math.max(bw, cx + w);
            cx += w + 6;
        }
        // Das Blatt ist nur so gross wie sein Text und wird nur vergroessert, wenn es klein genug bleibt.
        int pw = Math.max(64, bw + 16);
        int ph = Math.max(40, cy + rowH + 14);
        int s = (pw * 2 <= width / 2 && ph * 2 <= height * 3 / 5) ? 2 : 1;
        g.pose().pushPose();
        g.pose().translate((width - s * pw) / 2f, (height - s * ph) / 2f, 0);
        g.pose().scale(s, s, 1f);
        RaggedPaper.drawClean(g, ui, 0, 0, pw, ph);
        if (translated) {
            for (int y = 18; y < ph - 4; y += rowH) g.fill(4, y + rowH - 3, pw - 4, y + rowH - 2, color("parchment_2"));
        }
        for (int i = 0; i < latin.size(); i++) {
            int[] p = pos.get(i);
            g.drawString(font, latin.get(i), 8 + p[0], 8 + p[1], translated ? color("rubric") : color("ink_0"), false);
            if (translated && i < sheet.english().size()) {
                g.drawString(font, sheet.english().get(i), 8 + p[0], 8 + p[1] + 10, color("ink_1"), false);
            }
        }
        g.pose().popPose();
    }
}
