package de.scribery.client;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.glfw.GLFW;

import de.scribery.Chapters;
import de.scribery.Manuscripts;
import de.scribery.core.ArchiveModel;
import de.scribery.core.ArchiveProgress;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Das Manuscript Archive als Tisch von oben: drei Bahnen, links die Manuskripte als Papyrus (griechischer Urtext), in der Mitte
 * das gedruckte Papier (lateinische Umschrift der Boegen), rechts die handgeschriebene englische Interlinear-Uebersetzung.
 * In jeder Bahn laeuft das Papier durchgehend weiter. Wo etwas fehlt, ist es abgerissen: das Papier endet mit ausgefransten
 * Kanten, der Tisch ist sichtbar, und danach geht es mit der naechsten abgerissenen Kante weiter. Die Laenge der Luecke ist die
 * Laenge des fehlenden Textes. Die Bahnen fuellen sich unabhaengig voneinander. Ein Scrollen bewegt alle drei zusammen,
 * Strg + Mausrad zoomt.
 *
 * <p>Nichts rastet von Hand ein: Wer ein Fragment oder einen Bogen am Archiv-Block abgibt, dessen Text erscheint von selbst
 * an der richtigen Stelle (ArchiveService). Der Screen zeigt nur die Sammlung, die der Server geschickt hat (ClientArchive).
 */
final class ArchiveScreen extends Screen {
    private static final int GUTTER = 30, LANE_GAP = 8, PAD = 5, WORD_GAP = 5, ROW_H = 12, HEADER_H = 16, MAX_WORDS_PER_ROW = 4;
    private static final int TEAR = 6, TEAR_STEP = 3, PLANK_H = 36;
    private static final double ZOOM_MIN = 0.5, ZOOM_MAX = 1.5;
    private static final int GAP_COLOR = 0xFFB4AC98, DONE_COLOR = 0xFF6E8F4E;
    private static final int PAPYRUS = 0xFFE3CB94, PAPYRUS_EDGE = 0xFFB8985A, PAPYRUS_FIBER = 0x44B8985A;
    private static final int PAPER = 0xFFF4EEDC, PAPER_EDGE = 0xFFCFC6AE, SHADOW = 0x66000000;
    private static final int INK_GREEK = 0xFF3A2A1A, INK_PRINT = 0xFF2A2622, INK_HAND = 0xFF2B3F73, RULE = 0x446070A0;

    private final UiLayout ui = UiLayout.get();
    private final List<Manuscripts.Fragment> all = Manuscripts.load();

    /** Eine Zeile: Kapitelschild oder bis zu vier Woerter eines Bogens. */
    private record Row(boolean header, int mi, int first, int count, int chapter, String label, int page, boolean pageStart) {}

    // pro Manuskript, einmal berechnet
    private final int[][] pageOf = new int[all.size()][];
    private final int[] pages = new int[all.size()];

    // Layout (in Tischeinheiten, ausser den px-Feldern), bei Groessen- oder Zoomaenderung neu
    private int px0, py0, px1, py1, viewX0, viewY0, viewX1, viewY1, mapX0, builtWidth = -1;
    private double zoom = 0.8, builtZoom = -1;
    private int tableW, laneW;
    private final int[] laneX = new int[3];
    private final List<Row> rows = new ArrayList<>();
    private int[] rowTop = new int[0];
    private int contentH;
    private final int[] chapterRow = new int[23];

    // Fortschritt, bei jedem neuen Stand der Sammlung neu
    private int seenVersion = -1;
    private ArchiveModel.Stats stats = new ArchiveModel.Stats(0, 0, 0, 0, 0);
    private boolean[][] present = new boolean[3][0];
    /** Pro Bahn die durchgehenden Papierstuecke: {erste Zeile, letzte Zeile, oben abgerissen, unten abgerissen}. */
    @SuppressWarnings("unchecked")
    private final List<int[]>[] runs = new List[] { new ArrayList<int[]>(), new ArrayList<int[]>(), new ArrayList<int[]>() };
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

    /** Breite, die ein Wort in jeder Bahn belegt: die des breitesten der drei Texte, damit alle Zeilen deckungsgleich sind. */
    private int advance(int mi, int w) {
        return Math.max(font.width(greek(mi, w)), Math.max(font.width(latin(mi, w)), font.width(english(mi, w)))) + WORD_GAP;
    }

    private static String shortRef(String ref) {
        return ref.startsWith("John ") ? ref.substring(5) : ref;
    }

    /** Zufall ohne Zustand: gleiche Eingabe, gleiches Ergebnis, damit Kanten und Maserung beim Scrollen ruhig bleiben. */
    private static long hash(long x) {
        x ^= x >>> 33;
        x *= 0xff51afd7ed558ccdL;
        x ^= x >>> 33;
        x *= 0xc4ceb9fe1a85ec53L;
        x ^= x >>> 33;
        return x & Long.MAX_VALUE;
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
        tableW = Math.max(120, (int) ((mapX0 - 4 - viewX0) / zoom));
        laneW = Math.max(40, (tableW - GUTTER - 2 * LANE_GAP - 4) / 3);
        for (int i = 0; i < 3; i++) laneX[i] = GUTTER + i * (laneW + LANE_GAP);
        buildRows();
        builtWidth = width;
        builtZoom = zoom;
        seenVersion = -1;
        clampScroll();
    }

    /** Zeilen aufbauen: je Kapitel ein Schild, je Bogen Gruppen von Woertern, die in eine Bahn passen (nie ueber eine Bogengrenze). */
    private void buildRows() {
        rows.clear();
        int textW = laneW - 2 * PAD;
        int lastChapter = -1;
        for (int mi = 0; mi < all.size(); mi++) {
            var f = all.get(mi);
            if (f.chapter() != lastChapter) {
                lastChapter = f.chapter();
                chapterRow[Math.min(lastChapter, 22)] = rows.size();
                rows.add(new Row(true, mi, 0, 0, lastChapter, null, 0, false));
            }
            int n = f.words().size();
            int w = 0;
            boolean firstRow = true;
            while (w < n) {
                int page = pageOf[mi][w];
                int used = 0, count = 0;
                while (w + count < n && count < MAX_WORDS_PER_ROW && pageOf[mi][w + count] == page) {
                    int adv = advance(mi, w + count);
                    if (count > 0 && used + adv > textW) break;
                    used += adv;
                    count++;
                }
                boolean pageStart = !firstRow && (w == 0 || pageOf[mi][w - 1] != page);
                rows.add(new Row(false, mi, w, count, f.chapter(), firstRow ? shortRef(f.ref()) : null, page, pageStart));
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

    private int viewH() {
        return viewY1 - viewY0;
    }

    /** Sichtbare Hoehe in Tischeinheiten. */
    private double viewHt() {
        return viewH() / zoom;
    }

    private double maxScroll() {
        return Math.max(0, contentH - viewHt());
    }

    private void clampScroll() {
        scroll = Math.max(0, Math.min(maxScroll(), scroll));
    }

    // ------------------------------------------------------------------ Fortschritt

    /** Anwesenheit der Zeilen, Papierstuecke, Statistik, Uebersichtsleiste und Kapitelfarben neu berechnen, wenn sich die Sammlung aendert. */
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

        int n = rows.size();
        present = new boolean[3][n];
        for (int r = 0; r < n; r++) {
            Row row = rows.get(r);
            if (row.header) continue;
            ArchiveProgress p = progress.get(row.mi);
            present[0][r] = p.found();
            present[1][r] = p.isPrinted(row.page);
            present[2][r] = p.isTranslated(row.page);
        }
        // Durchgehende Papierstuecke: Schilder (Kapitelkoepfe) unterbrechen das Papier nicht, eine fehlende Zeile schon
        for (int lane = 0; lane < 3; lane++) {
            runs[lane].clear();
            int start = -1, last = -1;
            boolean tearTop = false, seenText = false;
            for (int r = 0; r < n; r++) {
                if (rows.get(r).header) continue;
                if (present[lane][r]) {
                    if (start < 0) {
                        start = r;
                        tearTop = seenText;
                    }
                    last = r;
                } else if (start >= 0) {
                    runs[lane].add(new int[] { start, last, tearTop ? 1 : 0, 1 });
                    start = -1;
                }
                seenText = true;
            }
            if (start >= 0) runs[lane].add(new int[] { start, last, tearTop ? 1 : 0, 0 });
        }

        int mapH = Math.max(1, viewH());
        double[] sum = new double[mapH];
        int[] cnt = new int[mapH];
        double[] chSum = new double[23];
        int[] chN = new int[23];
        for (int r = 0; r < n; r++) {
            Row row = rows.get(r);
            if (row.header) continue;
            double f = ((present[0][r] ? 1 : 0) + (present[1][r] ? 1 : 0) + (present[2][r] ? 1 : 0)) / 3.0;
            int y = Math.min(mapH - 1, (int) ((long) rowTop[r] * mapH / Math.max(1, contentH)));
            sum[y] += f * row.count;
            cnt[y] += row.count;
            int ch = Math.min(row.chapter, 22);
            chSum[ch] += f * row.count;
            chN[ch] += row.count;
        }
        mapColor = new int[mapH];
        for (int y = 0; y < mapH; y++) mapColor[y] = blend(GAP_COLOR, DONE_COLOR, cnt[y] == 0 ? 0 : sum[y] / cnt[y]);
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
        if (builtWidth != width || builtZoom != zoom) layout();
        refreshProgress();
        g.fill(0, 0, width, height, 0xFF241E1A);
        g.fill(px0 - 3, py0 - 3, px1 + 3, py1 + 3, col("wood_1"));
        g.fill(px0 - 2, py0 - 2, px1 + 2, py1 + 2, col("wood_3"));

        g.enableScissor(viewX0, viewY0, mapX0 - 2, viewY1);
        g.pose().pushPose();
        g.pose().translate(viewX0, viewY0, 0);
        g.pose().scale((float) zoom, (float) zoom, 1f);
        g.pose().translate(0, -scroll, 0);
        int first = firstVisibleRow();
        double bottom = scroll + viewHt();
        drawTable(g, bottom);
        for (int lane = 0; lane < 3; lane++) drawPaper(g, lane, bottom);
        drawRows(g, first, bottom);
        g.pose().popPose();
        g.disableScissor();

        drawHeader(g);
        drawMap(g);
        drawChapterButtons(g, mx, my);
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

    /** Der Tisch: Holzplanken in Tischkoordinaten, sie scrollen mit dem Papier. */
    private void drawTable(GuiGraphics g, double bottom) {
        int k0 = (int) Math.floor(scroll / PLANK_H);
        for (int k = k0; k * PLANK_H < bottom + PLANK_H; k++) {
            int y0 = k * PLANK_H;
            g.fill(0, y0, tableW, y0 + PLANK_H, (k & 1) == 0 ? col("wood_2") : col("wood_1"));
            g.fill(0, y0 + PLANK_H - 1, tableW, y0 + PLANK_H, col("wood_0"));
            int joint = 12 + (int) (hash(k * 31L + 7) % Math.max(1, tableW - 24));
            g.fill(joint, y0, joint + 1, y0 + PLANK_H - 1, col("wood_0"));
            for (int i = 0; i < 6; i++) {
                long h = hash(k * 977L + i);
                int gx = (int) (h % Math.max(1, tableW - 10)), gy = y0 + 3 + (int) ((h >> 12) % (PLANK_H - 7)), gl = 14 + (int) ((h >> 24) % 34);
                g.fill(gx, gy, Math.min(tableW, gx + gl), gy + 1, col("wood_3"));
            }
        }
    }

    /** Alle durchgehenden Papierstuecke einer Bahn, die im Bild liegen. */
    private void drawPaper(GuiGraphics g, int lane, double bottom) {
        for (int[] run : runs[lane]) {
            int yTop = rowTop[run[0]], yBot = rowTop[run[1] + 1];
            if (yBot < scroll - TEAR || yTop > bottom + TEAR) continue;
            long seed = hash(lane * 1_000_003L + run[0]);
            drawRun(g, lane, yTop, yBot, run[2] == 1, run[3] == 1, seed, 3, 3, SHADOW, 0, 0);
            drawRun(g, lane, yTop, yBot, run[2] == 1, run[3] == 1, seed, 0, 0, 0, 0, 0);
        }
    }

    /**
     * Ein Papierstueck. Mit {@code shadow != 0} wird nur der Schlagschatten gezeichnet (verschoben um dx, dy). Abgerissene Kanten
     * bestehen aus Spalten unterschiedlicher Hoehe, eine helle Faserlinie liegt knapp innerhalb der Risskante.
     */
    private void drawRun(GuiGraphics g, int lane, int yTop, int yBot, boolean tearTop, boolean tearBot, long seed,
                         int dx, int dy, int shadow, int unusedA, int unusedB) {
        boolean shade = shadow != 0;
        int body = lane == 0 ? PAPYRUS : PAPER, edge = lane == 0 ? PAPYRUS_EDGE : PAPER_EDGE;
        int x0 = laneX[lane] + dx, w = laneW;
        // Der Koerper deckt genau die Textzeilen; die Risskante ragt darueber hinaus in die Luecke (dort steht kein Text)
        int bodyTop = yTop + dy, bodyBot = yBot + dy;
        int fill = shade ? shadow : body;
        if (bodyBot > bodyTop) g.fill(x0, bodyTop, x0 + w, bodyBot, fill);
        int cols = (w + TEAR_STEP - 1) / TEAR_STEP;
        for (int i = 0; i < cols; i++) {
            int x = x0 + i * TEAR_STEP, cw = Math.min(TEAR_STEP, x0 + w - x);
            if (tearTop) {
                int reach = TEAR - tearOffset(seed, i, false);          // wie weit die Kante nach oben ueber die erste Zeile ragt
                g.fill(x, yTop - reach + dy, x + cw, yTop + dy, fill);
                if (!shade) {
                    g.fill(x, yTop - reach + dy, x + cw, yTop - reach + dy + 1, edge);
                    g.fill(x, yTop - reach + dy + 1, x + cw, yTop - reach + dy + 2, lane == 0 ? 0xFFF0DEB0 : 0xFFFFFFFF);
                }
            }
            if (tearBot) {
                int reach = TEAR - tearOffset(seed ^ 0x5bd1e995L, i, true);   // ... und nach unten ueber die letzte Zeile
                g.fill(x, yBot + dy, x + cw, yBot + reach + dy, fill);
                if (!shade) {
                    g.fill(x, yBot + reach + dy - 1, x + cw, yBot + reach + dy, edge);
                    g.fill(x, yBot + reach + dy - 2, x + cw, yBot + reach + dy - 1, lane == 0 ? 0xFFF0DEB0 : 0xFFFFFFFF);
                }
            }
        }
        if (shade) return;
        // Seitenkanten und gerade Enden
        g.fill(x0, bodyTop, x0 + 1, bodyBot, edge);
        g.fill(x0 + w - 1, bodyTop, x0 + w, bodyBot, edge);
        if (!tearTop) g.fill(x0, yTop, x0 + w, yTop + 1, edge);
        if (!tearBot) g.fill(x0, yBot - 1, x0 + w, yBot, edge);
        // Papyrus: Fasern
        if (lane == 0) {
            int from = (int) Math.max(bodyTop, scroll), to = (int) Math.min(bodyBot, scroll + viewHt());
            for (int y = from - (from % 5) + 5; y < to; y += 5) {
                int len = 30 + (int) (hash(seed + y) % 60);
                int fx = x0 + 2 + (int) (hash(seed * 3 + y) % Math.max(1, w - len - 4));
                g.fill(fx, y, fx + len, y + 1, PAPYRUS_FIBER);
            }
        }
    }

    /** Tiefe der Risskante an Spalte i: 0 bis TEAR-1, mit einzelnen tiefen Zacken. */
    private static int tearOffset(long seed, int i, boolean bottom) {
        long h = hash(seed + i * 7919L);
        int base = (int) (h % 3) + (int) ((h >> 9) % 2);
        if (((h >> 17) % 6) == 0) base += 2;
        return Math.min(TEAR - 1, Math.max(0, base + 1));
    }

    /** Text der sichtbaren Zeilen und die Kapitelschilder. */
    private void drawRows(GuiGraphics g, int first, double bottom) {
        for (int r = first; r < rows.size(); r++) {
            if (rowTop[r] > bottom) break;
            Row row = rows.get(r);
            int y = rowTop[r];
            if (row.header) {
                drawChapterPlaque(g, row, y);
                continue;
            }
            if (row.label != null) g.drawString(font, row.label, 2, y + 2, col("parchment_3"), true);
            if (row.pageStart) {
                // Bogengrenze: duenne Falznaht quer ueber Druck und Handschrift
                for (int lane = 1; lane <= 2; lane++) if (present[lane][r]) g.fill(laneX[lane] + 3, y, laneX[lane] + laneW - 3, y + 1, 0x33000000);
            }
            int[] offset = new int[3];
            for (int k = 0; k < row.count; k++) {
                int w = row.first + k;
                int adv = advance(row.mi, w);
                if (present[0][r]) g.drawString(font, greek(row.mi, w), laneX[0] + PAD + offset[0], y + 2, INK_GREEK, false);
                if (present[1][r]) g.drawString(font, latin(row.mi, w), laneX[1] + PAD + offset[1], y + 2, INK_PRINT, false);
                if (present[2][r]) {
                    // Handschrift: jedes Wort sitzt ein wenig anders auf der Linie
                    long h = hash(row.mi * 131L + w);
                    int jx = (int) (h % 3) - 1, jy = (int) ((h >> 8) % 3) == 0 ? 1 : 0;
                    g.drawString(font, english(row.mi, w), laneX[2] + PAD + offset[2] + jx, y + 2 + jy, INK_HAND, false);
                }
                for (int c = 0; c < 3; c++) offset[c] += adv;
            }
            if (present[2][r]) g.fill(laneX[2] + 3, y + ROW_H - 2, laneX[2] + laneW - 3, y + ROW_H - 1, RULE);
        }
    }

    private void drawChapterPlaque(GuiGraphics g, Row row, int y) {
        var info = Chapters.of(row.chapter);
        int x0 = laneX[0] - 4, x1 = laneX[2] + laneW + 4;
        g.fill(x0 - 1, y, x1 + 1, y + HEADER_H, col("wood_0"));
        g.fill(x0, y + 1, x1, y + HEADER_H - 1, col("wood_3"));
        g.fill(x0, y + 1, x1, y + 2, col("wood_4"));
        g.drawString(font, info.title() + " · John " + info.range(), x0 + 6, y + 4, col("parchment_3"), true);
    }

    private void drawHeader(GuiGraphics g) {
        g.fill(px0, py0, px1, viewY0, col("wood_2"));
        g.fill(px0, viewY0 - 1, px1, viewY0, col("wood_0"));
        g.drawString(font, "Manuscript Archive · John", px0 + 6, py0 + 5, col("parchment_3"), true);
        int x = px0 + 6, y = py0 + 18;
        x = drawStat(g, x, y, "Found", stats.manuscriptsFound(), stats.manuscriptsTotal());
        x = drawStat(g, x + 10, y, "Printed", stats.pagesPrinted(), stats.pagesTotal());
        drawStat(g, x + 10, y, "Translated", stats.pagesTranslated(), stats.pagesTotal());
        // Schilder ueber den Bahnen
        String[] heads = { "Manuscripts (Greek)", "Print (Transliteration)", "Handwriting (English)" };
        int hy = viewY0 - 13;
        for (int c = 0; c < 3; c++) {
            int tx = viewX0 + (int) (laneX[c] * zoom), tw = (int) (laneW * zoom);
            g.fill(tx, hy, tx + tw, hy + 11, col("wood_3"));
            g.fill(tx, hy, tx + tw, hy + 1, col("wood_4"));
            String h = font.width(heads[c]) > tw - 4 ? font.plainSubstrByWidth(heads[c], tw - 4) : heads[c];
            g.drawString(font, h, tx + (tw - font.width(h)) / 2, hy + 2, col("parchment_3"), true);
        }
    }

    /** "Found 12/1540" mit einem kleinen Balken darunter; liefert die naechste freie x-Position. */
    private int drawStat(GuiGraphics g, int x, int y, String label, int have, int total) {
        String text = label + " " + have + "/" + total;
        int w = font.width(text);
        g.drawString(font, text, x, y - 4, col("parchment_3"), true);
        g.fill(x, y + 6, x + w, y + 8, 0xFF3A2A1A);
        if (total > 0) g.fill(x, y + 6, x + Math.max(have > 0 ? 1 : 0, (int) ((long) w * have / total)), y + 8, DONE_COLOR);
        return x + w;
    }

    /** Uebersichtsleiste: der ganze Text auf einen Streifen gestaucht, gruen wo eingetragen, grau wo es fehlt. */
    private void drawMap(GuiGraphics g) {
        for (int y = 0; y < mapColor.length; y++) g.fill(mapX0, viewY0 + y, viewX1, viewY0 + y + 1, mapColor[y]);
        int mapH = Math.max(1, viewH());
        int top = (int) (scroll * mapH / Math.max(1, contentH));
        int bottom = (int) ((scroll + viewHt()) * mapH / Math.max(1, contentH));
        int ink = col("parchment_3");
        g.fill(mapX0 - 1, viewY0 + top, mapX0, viewY0 + Math.max(top + 2, bottom), ink);
        g.fill(viewX1, viewY0 + top, viewX1 + 1, viewY0 + Math.max(top + 2, bottom), ink);
        g.fill(mapX0 - 1, viewY0 + top, viewX1 + 1, viewY0 + top + 1, ink);
        g.fill(mapX0 - 1, viewY0 + Math.max(top + 1, bottom), viewX1 + 1, viewY0 + Math.max(top + 2, bottom + 1), ink);
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
        g.fill(px0, viewY1 + 2, px1, py1, col("wood_2"));
        g.fill(px0, viewY1 + 2, px1, viewY1 + 3, col("wood_0"));
        int hover = -1;
        for (int c = 1; c <= 21; c++) {
            int x = buttonX(c);
            boolean over = mx >= x && mx < x + w && my >= y && my < y + 12;
            if (over) hover = c;
            g.fill(x, y, x + w, y + 12, over ? col("parchment_3") : col("wood_0"));
            g.fill(x + 1, y + 1, x + w - 1, y + 11, blend(GAP_COLOR, DONE_COLOR, chapterDone[c]));
            String s = Integer.toString(c);
            g.drawString(font, s, x + (w - font.width(s)) / 2, y + 2, INK_GREEK, false);
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
        if (Screen.hasControlDown()) {
            zoomBy(dy > 0 ? 0.1 : -0.1);
            return true;
        }
        scroll -= dy * ROW_H * 3;
        clampScroll();
        return true;
    }

    /** Zoom aendern, ohne die Stelle im Text zu verlieren. */
    private void zoomBy(double delta) {
        double frac = contentH == 0 ? 0 : scroll / contentH;
        zoom = Math.max(ZOOM_MIN, Math.min(ZOOM_MAX, Math.round((zoom + delta) * 10) / 10.0));
        layout();
        scroll = frac * contentH;
        clampScroll();
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
        scroll = t * contentH - viewHt() / 2.0;
        clampScroll();
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        switch (key) {
            case GLFW.GLFW_KEY_PAGE_DOWN -> scroll += viewHt() - ROW_H;
            case GLFW.GLFW_KEY_PAGE_UP -> scroll -= viewHt() - ROW_H;
            case GLFW.GLFW_KEY_DOWN -> scroll += ROW_H;
            case GLFW.GLFW_KEY_UP -> scroll -= ROW_H;
            case GLFW.GLFW_KEY_HOME -> scroll = 0;
            case GLFW.GLFW_KEY_END -> scroll = contentH;
            case GLFW.GLFW_KEY_EQUAL, GLFW.GLFW_KEY_KP_ADD -> {
                zoomBy(0.1);
                return true;
            }
            case GLFW.GLFW_KEY_MINUS, GLFW.GLFW_KEY_KP_SUBTRACT -> {
                zoomBy(-0.1);
                return true;
            }
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

    void debugScrollBy(double units) {
        scroll += units;
        clampScroll();
    }
}
