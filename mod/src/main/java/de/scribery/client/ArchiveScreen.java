package de.scribery.client;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.glfw.GLFW;

import de.scribery.Chapters;
import de.scribery.Manuscripts;
import de.scribery.core.ArchiveModel;
import de.scribery.core.ArchiveModel.Column;
import de.scribery.core.ArchiveProgress;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Das Manuscript Archive: das Evangelium als ein durchgehender Text in drei Spalten, links der griechische Urtext aus den
 * gefundenen Manuskripten, in der Mitte die lateinische Umschrift der gedruckten Boegen, rechts die englische
 * Interlinear-Uebersetzung der uebersetzten Boegen. Was noch fehlt, ist ein grauer Platzhalter in der Laenge des fehlenden
 * Textes, so sieht man, wo etwas fehlt und wie weit man ist. Die Spalten haengen zusammen: ein Scrollen bewegt alle drei,
 * jede Zeile zeigt in allen Spalten dieselben Woerter.
 *
 * <p>Nichts rastet von Hand ein: Wer ein Fragment oder einen Bogen am Archiv-Block abgibt, dessen Text erscheint von selbst
 * an der richtigen Stelle (ArchiveService). Der Screen zeigt nur die Sammlung, die der Server geschickt hat (ClientArchive).
 */
final class ArchiveScreen extends Screen {
    private static final int GUTTER = 36, COL_GAP = 6, WORD_GAP = 6, ROW_H = 12, HEADER_H = 16, MAX_WORDS_PER_ROW = 4;
    private static final int GAP_COLOR = 0xFFB4AC98, DONE_COLOR = 0xFF6E8F4E;

    private final UiLayout ui = UiLayout.get();
    private final List<Manuscripts.Fragment> all = Manuscripts.load();

    /** Eine Zeile der Liste: Kapitelkopf oder bis zu vier Woerter eines Manuskripts. */
    private record Row(boolean header, int mi, int first, int count, int chapter, String label) {}

    // pro Manuskript, einmal berechnet
    private final int[][] pageOf = new int[all.size()][];
    private final int[] pages = new int[all.size()];

    // Layout, bei jeder Groessenaenderung neu
    private int px0, py0, px1, py1, viewX0, viewY0, viewX1, viewY1, colW, mapX0, builtWidth = -1;
    private final int[] colX = new int[3];
    private final List<Row> rows = new ArrayList<>();
    private int[] rowTop = new int[0];
    private int contentH;
    private final int[] chapterRow = new int[23];

    // Fortschritt, bei jedem neuen Stand der Sammlung neu
    private int seenVersion = -1;
    private ArchiveModel.Stats stats = new ArchiveModel.Stats(0, 0, 0, 0, 0);
    private int[] mapColor = new int[0];
    private final double[] chapterDone = new double[23];

    private double scroll;
    private boolean draggingMap;

    ArchiveScreen() {
        super(Component.literal("Manuscript Archive"));
        for (int mi = 0; mi < all.size(); mi++) {
            var words = all.get(mi).words();
            pageOf[mi] = ArchiveModel.pageOfWord(words.stream().map(w -> w.latin().length()).toList(),
                    ui.lineSlots, ui.rows, ui.maxWordsPerLine);
            pages[mi] = ArchiveModel.pageCount(pageOf[mi]);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private int col(String name) {
        return ui.color(name);
    }

    // ------------------------------------------------------------------ Texte der Woerter

    private String greek(int mi, int w) {
        return all.get(mi).words().get(w).greek();
    }

    private String latin(int mi, int w) {
        return all.get(mi).words().get(w).latin();
    }

    private String english(int mi, int w) {
        var ok = all.get(mi).tileWords().get(w).ok();
        return ok.isEmpty() ? "?" : ok.get(0);
    }

    // ------------------------------------------------------------------ Aufbau

    @Override
    protected void init() {
        layout();
    }

    private void layout() {
        px0 = 6;
        py0 = 6;
        px1 = width - 6;
        py1 = height - 6;
        viewX0 = px0 + 6;
        viewX1 = px1 - 6;
        viewY0 = py0 + 44;
        viewY1 = py1 - 20;
        mapX0 = viewX1 - 6;
        int textW = (mapX0 - 4) - (viewX0 + GUTTER);
        colW = Math.max(40, (textW - 2 * COL_GAP) / 3);
        for (int c = 0; c < 3; c++) colX[c] = viewX0 + GUTTER + c * (colW + COL_GAP);
        buildRows();
        builtWidth = width;
        seenVersion = -1;
        scroll = Math.min(scroll, maxScroll());
    }

    /** Zeilen aufbauen: je Kapitel ein Kopf, je Manuskript Gruppen von Woertern, die in eine Spalte passen. */
    private void buildRows() {
        rows.clear();
        int lastChapter = -1;
        for (int mi = 0; mi < all.size(); mi++) {
            var f = all.get(mi);
            if (f.chapter() != lastChapter) {
                lastChapter = f.chapter();
                chapterRow[Math.min(lastChapter, 22)] = rows.size();
                rows.add(new Row(true, mi, 0, 0, lastChapter, null));
            }
            int n = f.words().size();
            int w = 0;
            boolean firstRow = true;
            while (w < n) {
                int used = 0, count = 0;
                while (w + count < n && count < MAX_WORDS_PER_ROW) {
                    int adv = advance(mi, w + count);
                    if (count > 0 && used + adv > colW) break;
                    used += adv;
                    count++;
                }
                rows.add(new Row(false, mi, w, count, f.chapter(), firstRow ? shortRef(f.ref()) : null));
                firstRow = false;
                w += count;
            }
        }
        rowTop = new int[rows.size() + 1];
        int y = 0;
        for (int i = 0; i < rows.size(); i++) {
            rowTop[i] = y;
            y += rows.get(i).header ? HEADER_H : ROW_H;
        }
        rowTop[rows.size()] = y;
        contentH = y;
    }

    /** Breite, die ein Wort in jeder Spalte belegt: die des breitesten der drei Texte, damit alle Zeilen deckungsgleich sind. */
    private int advance(int mi, int w) {
        return Math.max(font.width(greek(mi, w)), Math.max(font.width(latin(mi, w)), font.width(english(mi, w)))) + WORD_GAP;
    }

    private static String shortRef(String ref) {
        return ref.startsWith("John ") ? ref.substring(5) : ref;
    }

    private int viewH() {
        return viewY1 - viewY0;
    }

    private double maxScroll() {
        return Math.max(0, contentH - viewH());
    }

    private void clampScroll() {
        scroll = Math.max(0, Math.min(maxScroll(), scroll));
    }

    // ------------------------------------------------------------------ Fortschritt

    private boolean known(ArchiveProgress p, Column c, int mi, int w) {
        return ArchiveModel.known(p, c, pageOf[mi][w]);
    }

    /** Statistik, Uebersichtsleiste und Kapitelfarben neu berechnen, wenn sich die Sammlung geaendert hat. */
    private void refreshProgress() {
        if (seenVersion == ClientArchive.version() && mapColor.length == Math.max(1, viewH())) return;
        seenVersion = ClientArchive.version();
        List<Integer> pageList = new ArrayList<>();
        List<ArchiveProgress> progress = new ArrayList<>();
        for (int mi = 0; mi < all.size(); mi++) {
            pageList.add(pages[mi]);
            progress.add(ClientArchive.get(all.get(mi).id()));
        }
        stats = ArchiveModel.stats(pageList, progress);

        int mapH = Math.max(1, viewH());
        double[] sum = new double[mapH];
        int[] n = new int[mapH];
        double[] chSum = new double[23];
        int[] chN = new int[23];
        for (int r = 0; r < rows.size(); r++) {
            Row row = rows.get(r);
            if (row.header) continue;
            ArchiveProgress p = progress.get(row.mi);
            int have = 0;
            for (int k = 0; k < row.count; k++) {
                for (Column c : Column.values()) if (known(p, c, row.mi, row.first + k)) have++;
            }
            double f = have / (3.0 * row.count);
            int y = (int) ((long) rowTop[r] * mapH / Math.max(1, contentH));
            y = Math.min(mapH - 1, y);
            sum[y] += f * row.count;
            n[y] += row.count;
            int ch = Math.min(row.chapter, 22);
            chSum[ch] += f * row.count;
            chN[ch] += row.count;
        }
        mapColor = new int[mapH];
        for (int y = 0; y < mapH; y++) mapColor[y] = blend(GAP_COLOR, DONE_COLOR, n[y] == 0 ? 0 : sum[y] / n[y]);
        for (int c = 1; c <= 22; c++) chapterDone[c] = chN[c] == 0 ? 0 : chSum[c] / chN[c];
    }

    private static int blend(int a, int b, double t) {
        t = Math.max(0, Math.min(1, t));
        int r = (int) (((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
        int g = (int) (((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
        int bl = (int) ((a & 255) * (1 - t) + (b & 255) * t);
        return 0xFF000000 | (r << 16) | (g << 8) | bl;
    }

    // ------------------------------------------------------------------ Zeichnen

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        if (builtWidth != width) layout();
        refreshProgress();
        g.fill(0, 0, width, height, 0xFF241E1A);
        // Rahmen und Blatt
        g.fill(px0 - 3, py0 - 3, px1 + 3, py1 + 3, col("wood_1"));
        g.fill(px0 - 2, py0 - 2, px1 + 2, py1 + 2, col("wood_3"));
        g.fill(px0, py0, px1, py1, col("parchment_3"));
        drawHeader(g);
        drawColumns(g);
        drawMap(g);
        drawChapterButtons(g, mx, my);
    }

    private void drawHeader(GuiGraphics g) {
        g.fill(px0, py0, px1, viewY0, col("parchment_2"));
        g.drawString(font, "Manuscript Archive · John", px0 + 6, py0 + 5, col("ink_0"), false);
        int x = px0 + 6, y = py0 + 18;
        x = drawStat(g, x, y, "Found", stats.manuscriptsFound(), stats.manuscriptsTotal());
        x = drawStat(g, x + 10, y, "Printed", stats.pagesPrinted(), stats.pagesTotal());
        drawStat(g, x + 10, y, "Translated", stats.pagesTranslated(), stats.pagesTotal());
        // Spaltenkoepfe
        int hy = viewY0 - 12;
        g.fill(px0, hy - 1, px1, hy + 11, col("parchment_1"));
        g.drawString(font, "Ref", viewX0 + 2, hy + 1, col("parchment_3"), false);
        String[] heads = { "Greek", "Transliteration", "Interlinear English" };
        for (int c = 0; c < 3; c++) g.drawString(font, heads[c], colX[c], hy + 1, col("parchment_3"), false);
    }

    /** "Found 12/1540" mit einem kleinen Balken darunter; liefert die naechste freie x-Position. */
    private int drawStat(GuiGraphics g, int x, int y, String label, int have, int total) {
        String text = label + " " + have + "/" + total;
        int w = font.width(text);
        g.drawString(font, text, x, y - 4, col("ink_1"), false);
        g.fill(x, y + 6, x + w, y + 8, GAP_COLOR);
        if (total > 0) g.fill(x, y + 6, x + Math.max(have > 0 ? 1 : 0, (int) ((long) w * have / total)), y + 8, DONE_COLOR);
        return x + w;
    }

    private void drawColumns(GuiGraphics g) {
        g.enableScissor(viewX0, viewY0, mapX0 - 2, viewY1);
        // Spaltentrenner
        for (int c = 1; c < 3; c++) g.fill(colX[c] - COL_GAP / 2 - 1, viewY0, colX[c] - COL_GAP / 2, viewY1, col("parchment_2"));
        g.fill(viewX0 + GUTTER - COL_GAP / 2 - 1, viewY0, viewX0 + GUTTER - COL_GAP / 2, viewY1, col("parchment_2"));
        int first = firstVisibleRow();
        for (int r = first; r < rows.size(); r++) {
            int y = viewY0 + rowTop[r] - (int) scroll;
            if (y > viewY1) break;
            Row row = rows.get(r);
            if (row.header) drawChapterHeader(g, row, y);
            else drawTextRow(g, row, y);
        }
        g.disableScissor();
    }

    private int firstVisibleRow() {
        int lo = 0, hi = rows.size() - 1;
        while (lo < hi) {
            int mid = (lo + hi) / 2;
            if (rowTop[mid + 1] <= (int) scroll) lo = mid + 1;
            else hi = mid;
        }
        return lo;
    }

    private void drawChapterHeader(GuiGraphics g, Row row, int y) {
        var info = Chapters.of(row.chapter);
        g.fill(viewX0, y + 1, mapX0 - 2, y + HEADER_H - 1, col("parchment_2"));
        g.fill(viewX0, y + HEADER_H - 2, mapX0 - 2, y + HEADER_H - 1, col("parchment_1"));
        g.drawString(font, info.title() + " · John " + info.range(), viewX0 + 4, y + 4, col("ink_0"), false);
    }

    private void drawTextRow(GuiGraphics g, Row row, int y) {
        ArchiveProgress p = ClientArchive.get(all.get(row.mi).id());
        if (row.label != null) g.drawString(font, row.label, viewX0 + 2, y + 2, col("parchment_0"), false);
        int[] offset = new int[3];
        for (int k = 0; k < row.count; k++) {
            int w = row.first + k;
            String[] text = { greek(row.mi, w), latin(row.mi, w), english(row.mi, w) };
            int adv = advance(row.mi, w);
            for (int c = 0; c < 3; c++) {
                boolean have = known(p, Column.values()[c], row.mi, w);
                int x = colX[c] + offset[c];
                if (have) {
                    g.drawString(font, text[c], x, y + 2, c == 0 ? col("ink_1") : c == 1 ? col("ink_0") : col("rubric"), false);
                } else {
                    // Platzhalter in der Laenge des fehlenden Textes
                    g.fill(x, y + 3, x + font.width(text[c]), y + 8, GAP_COLOR);
                }
                offset[c] += adv;
            }
        }
    }

    /** Uebersichtsleiste: der ganze Text auf einen Streifen gestaucht, gruen wo eingetragen, grau wo es fehlt. */
    private void drawMap(GuiGraphics g) {
        for (int y = 0; y < mapColor.length; y++) g.fill(mapX0, viewY0 + y, viewX1, viewY0 + y + 1, mapColor[y]);
        // sichtbarer Ausschnitt
        int mapH = Math.max(1, viewH());
        int top = (int) (scroll * mapH / Math.max(1, contentH));
        int bottom = (int) ((scroll + viewH()) * mapH / Math.max(1, contentH));
        g.fill(mapX0 - 1, viewY0 + top, mapX0, viewY0 + Math.max(top + 2, bottom), col("ink_0"));
        g.fill(viewX1, viewY0 + top, viewX1 + 1, viewY0 + Math.max(top + 2, bottom), col("ink_0"));
        g.fill(mapX0 - 1, viewY0 + top, viewX1 + 1, viewY0 + top + 1, col("ink_0"));
        g.fill(mapX0 - 1, viewY0 + Math.max(top + 1, bottom), viewX1 + 1, viewY0 + Math.max(top + 2, bottom + 1), col("ink_0"));
    }

    // ------------------------------------------------------------------ Kapitelknoepfe

    private int buttonW() {
        return Math.max(8, Math.min(16, (px1 - px0 - 12) / 21 - 2));
    }

    private int buttonX(int chapter) {
        return px0 + 6 + (chapter - 1) * (buttonW() + 2);
    }

    private void drawChapterButtons(GuiGraphics g, int mx, int my) {
        int y = viewY1 + 4, w = buttonW();
        g.fill(px0, viewY1 + 2, px1, py1, col("parchment_2"));
        int hover = -1;
        for (int c = 1; c <= 21; c++) {
            int x = buttonX(c);
            boolean over = mx >= x && mx < x + w && my >= y && my < y + 12;
            if (over) hover = c;
            g.fill(x, y, x + w, y + 12, over ? col("ink_0") : col("parchment_1"));
            g.fill(x + 1, y + 1, x + w - 1, y + 11, blend(GAP_COLOR, DONE_COLOR, chapterDone[c]));
            String s = Integer.toString(c);
            g.drawString(font, s, x + (w - font.width(s)) / 2, y + 2, col("ink_1"), false);
        }
        if (hover > 0) {
            var info = Chapters.of(hover);
            g.renderTooltip(font, Component.literal(info.title() + " · John " + info.range() + " · "
                    + Math.round(chapterDone[hover] * 100) + "%"), mx, my);
        }
    }

    // ------------------------------------------------------------------ Eingabe

    @Override
    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        scroll -= dy * ROW_H * 3;
        clampScroll();
        return true;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) return super.mouseClicked(mx, my, button);
        int y = viewY1 + 4, w = buttonW();
        for (int c = 1; c <= 21; c++) {
            int x = buttonX(c);
            if (mx >= x && mx < x + w && my >= y && my < y + 12) {
                jumpToChapter(c);
                return true;
            }
        }
        if (mx >= mapX0 - 1 && mx <= viewX1 + 1 && my >= viewY0 && my <= viewY1) {
            draggingMap = true;
            scrollToMap(my);
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (draggingMap) {
            scrollToMap(my);
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        draggingMap = false;
        return super.mouseReleased(mx, my, button);
    }

    private void scrollToMap(double my) {
        double t = (my - viewY0) / Math.max(1, viewH());
        scroll = t * contentH - viewH() / 2.0;
        clampScroll();
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        switch (key) {
            case GLFW.GLFW_KEY_PAGE_DOWN -> scroll += viewH() - ROW_H;
            case GLFW.GLFW_KEY_PAGE_UP -> scroll -= viewH() - ROW_H;
            case GLFW.GLFW_KEY_DOWN -> scroll += ROW_H;
            case GLFW.GLFW_KEY_UP -> scroll -= ROW_H;
            case GLFW.GLFW_KEY_HOME -> scroll = 0;
            case GLFW.GLFW_KEY_END -> scroll = contentH;
            default -> {
                return super.keyPressed(key, scan, mods);
            }
        }
        clampScroll();
        return true;
    }

    private void jumpToChapter(int chapter) {
        scroll = rowTop[Math.min(rows.size() - 1, chapterRow[Math.min(chapter, 22)])];
        clampScroll();
    }

    // ------------------------------------------------------------------ nur fuer den Selbsttest

    void debugJumpToChapter(int chapter) {
        if (builtWidth != width) layout();
        jumpToChapter(chapter);
    }

    void debugScrollBy(double px) {
        scroll += px;
        clampScroll();
    }
}
