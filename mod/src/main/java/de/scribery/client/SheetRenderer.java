package de.scribery.client;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Zeichnet einen Bogen mit seinem Druckbild. Presse und Plaettchen-Station benutzen dasselbe Bild,
 * damit der Abzug auf dem Pult genau so aussieht, wie er aus der Presse kam.
 */
final class SheetRenderer {
    private SheetRenderer() {}

    static int width(UiLayout ui) {
        return ui.rect("forme")[2] - 4;
    }

    static int height(UiLayout ui) {
        return ui.rect("paper_tray")[3] - 4;
    }

    /** Leerer Bogen, linke obere Ecke bei (x, y). */
    static void paper(GuiGraphics g, UiLayout ui, int x, int y) {
        int w = width(ui), h = height(ui);
        StationScreen.tex(g, "paper_strip", x, y, w, h);
    }

    /**
     * Druckbild auf einem Bogen bei (x, y). Die Zeichen stehen dort, wo die Lettern in der Setzform lagen.
     *
     * @param printed ein Zeichen pro Slot, 0 = nichts
     * @param smudged pro Slot: verwischt; darf null sein
     */
    static void print(GuiGraphics g, UiLayout ui, int x, int y, char[] printed, boolean[] smudged) {
        int[] forme = ui.rect("forme");
        int ox = x + ui.slotsX - (forme[0] + 2);
        int oy = y + ui.slotsY - (forme[1] + 3);
        for (int i = 0; i < printed.length; i++) {
            if (printed[i] == 0) continue;
            SortGlyphs.draw(g, printed[i], ox + (i % ui.lineSlots) * ui.pitch + 2,
                    oy + (i / ui.lineSlots) * ui.rowPitch + 1, ui.color("ink_0"));
        }
        if (smudged == null) return;
        // Falsche Woerter sind als Ganzes verwischt: man sieht nicht, welcher Buchstabe falsch war.
        int i = 0;
        while (i < smudged.length) {
            if (!smudged[i]) { i++; continue; }
            int start = i;
            int rowEnd = (i / ui.lineSlots + 1) * ui.lineSlots;
            while (i < rowEnd && smudged[i]) i++;
            int x0 = ox + (start % ui.lineSlots) * ui.pitch - 1;
            int x1 = ox + ((i - 1) % ui.lineSlots) * ui.pitch + ui.sortW + 1;
            int sy = oy + (start / ui.lineSlots) * ui.rowPitch + 1;
            for (int row = 0; row < SortGlyphs.HEIGHT + 1; row++) {
                if (row % 3 == 2) continue;
                g.fill(x0 + (row * 5) % 3, sy - 1 + row, x1 - (row * 7) % 4, sy + row, ui.color("smear"));
            }
        }
    }
}
