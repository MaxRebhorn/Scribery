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
        // 3-fach: dieselbe Anordnung wie am Pult
        int s = Math.max(1, Math.min(3, Math.min(width / 66, height / 62)));
        g.pose().pushPose();
        g.pose().translate((width - s * 66) / 2f, (height - s * 62) / 2f, 0);
        g.pose().scale(s, s, 1f);
        var fit = fit(fragmentWords, new int[] { 0, 0, 66, 62 }, 10, 5);
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
        int s = 2;
        int pw = 150, rowH = translated ? 22 : 12;
        List<int[]> pos = new ArrayList<>();
        int cx = 0, cy = 0;
        for (int i = 0; i < latin.size(); i++) {
            int w = Math.max(font.width(latin.get(i)), translated && i < sheet.english().size() ? font.width(sheet.english().get(i)) : 0);
            if (cx > 0 && cx + w > pw - 16) { cx = 0; cy += rowH; }
            pos.add(new int[] { cx, cy, w });
            cx += w + 6;
        }
        int ph = Math.max(48, cy + rowH + 16);
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

    /** Wie {@link StationScreen#fit}, ohne Station. */
    private record Fit(int[] paper, int[][] words) {}

    private Fit fit(List<String> words, int[] area, int lineHeight, int pad) {
        int[][] at0 = new int[words.size()][];
        int cx = 0, cy = 0, space = font.width(" "), bw = 0, bh = 0, wrap = area[2] - 2 * pad;
        for (int i = 0; i < words.size(); i++) {
            int w = font.width(words.get(i));
            if (cx > 0 && cx + w > wrap) { cx = 0; cy += lineHeight; }
            at0[i] = new int[] { cx, cy, w };
            bw = Math.max(bw, cx + w);
            bh = Math.max(bh, cy + 9);
            cx += w + space;
        }
        int pw = Math.min(area[2], Math.max(24, bw + 2 * pad)), ph = Math.min(area[3], Math.max(16, bh + 2 * pad));
        int px = area[0] + (area[2] - pw) / 2, py = area[1] + (area[3] - ph) / 2;
        int[][] out = new int[at0.length][];
        for (int i = 0; i < at0.length; i++) out[i] = new int[] { px + pad + at0[i][0], py + pad + at0[i][1], at0[i][2] };
        return new Fit(new int[] { px, py, pw, ph }, out);
    }
}
