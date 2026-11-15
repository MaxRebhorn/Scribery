package de.scribery.client;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.client.gui.GuiGraphics;

/** Eigene 5 x 7 Pixel grosse Buchstaben fuer Lettern und Abzuege. Ē und Ō tragen einen Strich darueber. */
public final class SortGlyphs {
    /** Hoehe inklusive Platz fuer den Laengenstrich (Strich, Luecke, sieben Zeilen). */
    public static final int HEIGHT = 9;
    public static final int WIDTH = 5;

    private static final Map<Character, String[]> GLYPHS = new HashMap<>();

    static {
        put('A', ".###.", "#...#", "#...#", "#####", "#...#", "#...#", "#...#");
        put('B', "####.", "#...#", "#...#", "####.", "#...#", "#...#", "####.");
        put('C', ".###.", "#...#", "#....", "#....", "#....", "#...#", ".###.");
        put('D', "####.", "#...#", "#...#", "#...#", "#...#", "#...#", "####.");
        put('E', "#####", "#....", "#....", "####.", "#....", "#....", "#####");
        put('G', ".###.", "#...#", "#....", "#.###", "#...#", "#...#", ".###.");
        put('H', "#...#", "#...#", "#...#", "#####", "#...#", "#...#", "#...#");
        put('I', ".###.", "..#..", "..#..", "..#..", "..#..", "..#..", ".###.");
        put('K', "#...#", "#..#.", "#.#..", "##...", "#.#..", "#..#.", "#...#");
        put('L', "#....", "#....", "#....", "#....", "#....", "#....", "#####");
        put('M', "#...#", "##.##", "#.#.#", "#.#.#", "#...#", "#...#", "#...#");
        put('N', "#...#", "##..#", "#.#.#", "#..##", "#...#", "#...#", "#...#");
        put('O', ".###.", "#...#", "#...#", "#...#", "#...#", "#...#", ".###.");
        put('P', "####.", "#...#", "#...#", "####.", "#....", "#....", "#....");
        put('R', "####.", "#...#", "#...#", "####.", "#.#..", "#..#.", "#...#");
        put('S', ".####", "#....", "#....", ".###.", "....#", "....#", "####.");
        put('T', "#####", "..#..", "..#..", "..#..", "..#..", "..#..", "..#..");
        put('U', "#...#", "#...#", "#...#", "#...#", "#...#", "#...#", ".###.");
        put('X', "#...#", "#...#", ".#.#.", "..#..", ".#.#.", "#...#", "#...#");
        put('Z', "#####", "....#", "...#.", "..#..", ".#...", "#....", "#####");
    }

    private SortGlyphs() {}

    private static void put(char c, String... rows) {
        GLYPHS.put(c, rows);
    }

    /** Zeichnet einen Buchstaben. (x, y) ist die linke obere Ecke inklusive Strich-Zeile. */
    public static void draw(GuiGraphics g, char c, int x, int y, int color) {
        char base = c;
        boolean macron = false;
        if (c == 'Ē') { base = 'E'; macron = true; }
        if (c == 'Ō') { base = 'O'; macron = true; }
        String[] rows = GLYPHS.get(base);
        if (rows == null) return;
        if (macron) g.fill(x, y, x + WIDTH, y + 1, color);
        for (int r = 0; r < rows.length; r++) {
            String row = rows[r];
            int yy = y + 2 + r;
            int run = -1;
            for (int col = 0; col <= WIDTH; col++) {
                boolean on = col < WIDTH && row.charAt(col) == '#';
                if (on && run < 0) run = col;
                if (!on && run >= 0) {
                    g.fill(x + run, yy, x + col, yy + 1, color);
                    run = -1;
                }
            }
        }
    }
}
