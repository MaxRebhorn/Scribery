package de.scribery.client;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.glfw.GLFW;

import de.scribery.core.TileSession;
import de.scribery.core.TileSession.Hint;
import de.scribery.core.TileSession.Refusal;
import de.scribery.core.TileSession.Tile;
import de.scribery.core.TileSession.Word;
import de.scribery.core.TileSession.WriteResult;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.sounds.SoundEvents;

/**
 * Die Uebersetzungs-Station: links das Pult mit dem Manuskript und darunter der gedruckte lateinische Bogen,
 * in der Mitte das Blatt, das beschrieben wird, rechts drei Kaesten mit Plaettchen und das Federglas.
 *
 * Das Blatt ist zuerst leer. Ein Klick auf ein Wort im Manuskript oder auf dem gedruckten Bogen laesst die
 * Feder dieses Wort an seinen Platz schreiben, und darunter entsteht der Slot. In den Slot zieht man ein
 * Plaettchen; ein Klick auf das Federglas schreibt alle gelegten Woerter.
 * Koordinaten sind GUI-Pixel der Leinwand aus art/tiles-layout.json.
 */
public class TileScreen extends StationScreen {
    private static final int WRITE_TICKS_PER_WORD = 6;
    private static final int FINISH_TICKS = 30;
    private static final int PEN_TICKS = 8;
    private static final int BOXES = 3;
    private static final int LATIN_LINE = 9;

    private TileSession session;
    /** Fester Platz jedes Plaettchens im Kasten: {x, y, breite}. Bleibt, auch wenn Nachbarn fehlen. */
    private int[][] homes = new int[0][];
    private int loadedPage = -1;
    /** Was ueber jedem Slot steht: die Form, die angeklickt wurde (griechisch oder lateinisch). */
    private String[] heads = new String[0];

    private int handTile = -1;

    // Die Feder schreibt gerade ein angeklicktes Wort aufs Blatt.
    private int penSlot = -1, penTicks;

    // Schreib-Animation der Uebersetzung: Die Feder geht die Slots der Reihe nach durch.
    private int writeTick = -1;
    private List<Integer> writeOrder = List.of();
    private String[] writeFaces = new String[0];
    private int[] writeTileIds = new int[0];
    private WriteResult writeResult;
    private int finishDelay;

    public TileScreen(Workbench bench) {
        super(bench);
    }

    @Override
    protected void init() {
        if (session == null) onBenchChanged();
    }

    /** Uebernimmt den Arbeitsstand. Die Blaetter sind dieselben wie die Boegen der Presse. */
    @Override
    protected void onBenchChanged() {
        if (bench.tiles == null) {
            long seed = bench.fragment.id().hashCode();
            List<List<Integer>> sheets = bench.press.sheets();
            bench.tiles = new TileSession(bench.fragment.tileWords(), sheets,
                    wrongCounts(bench.fragment.tileWords(), sheets, seed), bench.startInk(), seed);
        }
        session = bench.tiles;
        loadedPage = -1;
        handTile = -1;
        writeTick = -1;
        penSlot = -1;
        finishDelay = 0;
    }

    /**
     * Wie viele falsche Plaettchen jedes Wort eines Blatts bekommt: so viele wie moeglich,
     * aber nur so viele, dass alle Plaettchen des Blatts in die drei Kaesten passen.
     */
    private int[] wrongCounts(List<Word> words, List<List<Integer>> sheets, long seed) {
        int[] out = new int[sheets.size()];
        for (int p = 0; p < out.length; p++) {
            int n = TileSession.WRONG_PER_WORD;
            while (n > 0 && layoutHomes(TileSession.tilesFor(words, sheets.get(p), seed, n)) == null) n--;
            out[p] = n;
        }
        return out;
    }

    private int tileWidth(Tile t) {
        return Math.min(ui.tileMaxW, font.width(t.face()) + 2 * ui.tilePad);
    }

    /** Reiht die Plaettchen in ihren Kaesten auf. Liefert null, wenn ein Kasten ueberlaeuft. */
    private int[][] layoutHomes(List<Tile> tiles) {
        int[][] out = new int[tiles.size()][];
        int[] cx = new int[BOXES], row = new int[BOXES];
        for (Tile t : tiles) {
            int[] box = ui.rect("t_box_" + t.box());
            int inner = box[2] - 2 * ui.boxInset;
            int w = tileWidth(t);
            if (cx[t.box()] > 0 && cx[t.box()] + w > inner) {
                cx[t.box()] = 0;
                row[t.box()]++;
            }
            if (row[t.box()] >= ui.boxRows) return null;
            out[t.id()] = new int[] { box[0] + ui.boxInset + cx[t.box()],
                    box[1] + ui.boxInset + row[t.box()] * ui.tileRowPitch, w };
            cx[t.box()] += w + ui.tileGap;
        }
        return out;
    }

    /** Nach einem Blattwechsel: Plaetze der Plaettchen neu bestimmen, das Blatt ist wieder leer. */
    private void syncPage() {
        if (loadedPage == session.pageIndex()) return;
        loadedPage = session.pageIndex();
        int[][] h = layoutHomes(session.tiles());
        homes = h != null ? h : new int[0][];
        heads = new String[session.slotCount()];
    }

    // ------------------------------------------------------------------ Station

    @Override
    protected String resourceText() {
        return "Ink " + session.ink();
    }

    @Override
    protected Stations.Kind kind() {
        return Stations.Kind.DESK;
    }

    @Override
    protected boolean busy() {
        return writeTick >= 0 || finishDelay > 0;
    }

    @Override
    protected void stationTick() {
        if (penTicks > 0 && --penTicks == 0) penSlot = -1;
        if (writeTick >= 0) {
            writeTick++;
            if (writeTick % WRITE_TICKS_PER_WORD == 0) {
                int n = writeTick / WRITE_TICKS_PER_WORD;
                if (n <= writeOrder.size()) {
                    boolean wrong = writeResult.wrong()[writeOrder.get(n - 1)];
                    sound(SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, wrong ? 0.7f : 1.3f, 0.6f);
                }
                if (n >= writeOrder.size()) {
                    writeTick = -1;
                    sound(writeResult.allCorrect() ? SoundEvents.EXPERIENCE_ORB_PICKUP : SoundEvents.VILLAGER_NO, 1f, 0.5f);
                    if (writeResult.allCorrect()) finishDelay = FINISH_TICKS;
                }
            }
        } else if (finishDelay > 0 && --finishDelay == 0) {
            session.finishPage();
            sound(SoundEvents.BOOK_PAGE_TURN, 1f, 0.6f);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_N && session.isComplete()) {
            restart(fragmentIndex + 1);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_R && session.isComplete()) {
            restart(fragmentIndex);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    // ------------------------------------------------------------------ Geometrie

    /** Der Bogen dieses Blatts ist an der Presse schon gedruckt. */
    private boolean printed() {
        return !session.isComplete() && bench.press.printedCount() > session.pageIndex();
    }

    private List<String> pageWords(boolean greek) {
        List<String> out = new ArrayList<>();
        for (int i = 0; i < session.slotCount(); i++) {
            out.add(greek ? session.word(i).greek() : session.word(i).latin());
        }
        return out;
    }

    /** Wo die Woerter auf dem gedruckten Bogen stehen. */
    private int[][] latinWordRects() {
        return fit(pageWords(false), ui.rect("t_latin"), LATIN_LINE, 5).words();
    }

    private static int wordAt(int[][] rects, double x, double y) {
        for (int i = 0; i < rects.length; i++) {
            int[] r = rects[i];
            if (x >= r[0] - 1 && x < r[0] + r[2] + 1 && y >= r[1] - 1 && y < r[1] + 9) return i;
        }
        return -1;
    }

    private int cellX(int slot) {
        return ui.cellsX + (slot % ui.cellCols) * ui.sheetCellW;
    }

    private int cellY(int slot) {
        return ui.cellsY + (slot / ui.cellCols) * ui.cellRowH;
    }

    /** Slot, in dessen Zelle (Wort und Slot darunter) der Punkt liegt. */
    private int slotAt(double x, double y) {
        if (x < ui.cellsX || y < ui.cellsY) return -1;
        int col = (int) ((x - ui.cellsX) / ui.sheetCellW);
        int row = (int) ((y - ui.cellsY) / ui.cellRowH);
        if (col >= ui.cellCols || row >= ui.cellRows) return -1;
        int slot = row * ui.cellCols + col;
        return slot < session.slotCount() ? slot : -1;
    }

    /** Wo ein Plaettchen gerade liegt: im Slot zentriert oder an seinem festen Platz im Kasten. */
    private int[] tileRect(Tile t) {
        int[] home = homes[t.id()];
        if (t.slot() >= 0) {
            return new int[] { cellX(t.slot()) + (ui.slotW - home[2]) / 2, cellY(t.slot()) + ui.slotDy, home[2], ui.tileH };
        }
        return new int[] { home[0], home[1], home[2], ui.tileH };
    }

    private Tile tileAt(double x, double y) {
        for (Tile t : session.tiles()) {
            if (t.used() || t.id() == handTile) continue;
            if (in(tileRect(t), x, y, 0)) return t;
        }
        return null;
    }

    // ------------------------------------------------------------------ Eingabe

    @Override
    public void clickCanvas(double x, double y, int button) {
        if (busy() || session.isComplete() || button != 0) return;
        syncPage();

        if (in(ui.rect("t_quill"), x, y, 2)) {
            startWriting();
            return;
        }

        // Ein Wort im Manuskript oder auf dem gedruckten Bogen anklicken
        int word = wordAt(lecternWordRects(pageWords(true)), x, y);
        if (word >= 0) {
            pick(word, session.word(word).greek());
            return;
        }
        if (printed()) {
            word = wordAt(latinWordRects(), x, y);
            if (word >= 0) {
                pick(word, session.word(word).latin());
                return;
            }
        }

        Tile t = tileAt(x, y);
        if (t != null) {
            session.unplace(t.id());
            handTile = t.id();
            sound(SoundEvents.ITEM_PICKUP, 1.2f, 0.4f);
        }
    }

    /** Die Feder schreibt das angeklickte Wort an seinen Platz, darunter entsteht der Slot. */
    private void pick(int slot, String form) {
        if (!printed()) {
            flash("Print this sheet at the press first");
            return;
        }
        boolean fresh = session.reveal(slot);
        if (!fresh && form.equals(heads[slot])) return;
        heads[slot] = form; // ein zweiter Klick auf die andere Schrift schreibt das Wort in dieser neu
        penSlot = slot;
        penTicks = PEN_TICKS;
        sound(SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, 1.3f, 0.7f);
    }

    /** Losgelassen ueber einer Zelle des Blatts: Das Plaettchen liegt dort. Sonst faellt es in den Kasten. */
    @Override
    public void releaseCanvas(double x, double y, int button) {
        if (handTile < 0) return;
        int slot = slotAt(x, y);
        boolean placed = slot >= 0 && session.place(handTile, slot);
        handTile = -1;
        sound(placed ? SoundEvents.STONE_BUTTON_CLICK_ON : SoundEvents.ITEM_PICKUP, placed ? 1.1f : 0.9f, 0.5f);
    }

    private void startWriting() {
        if (!printed()) {
            flash("Print this sheet at the press first");
            return;
        }
        // Merken, was auf dem Blatt lag: Waehrend die Feder schreibt, bleiben die Plaettchen sichtbar.
        int n = session.slotCount();
        String[] faces = new String[n];
        int[] ids = new int[n];
        for (int i = 0; i < n; i++) {
            Tile t = session.tileInSlot(i);
            faces[i] = t == null ? null : t.face();
            ids[i] = t == null ? -1 : t.id();
        }
        WriteResult r = session.write();
        if (r.refusal() != null) {
            flash(refusalText(r.refusal()));
            return;
        }
        writeResult = r;
        writeFaces = faces;
        writeTileIds = ids;
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < n; i++) if (r.attempted()[i]) order.add(i);
        writeOrder = order;
        writeTick = 0;
    }

    /** Dieser Slot wird in der laufenden Animation erst noch geschrieben. */
    private boolean pendingWrite(int slot) {
        if (writeTick < 0) return false;
        int pos = writeOrder.indexOf(slot);
        return pos >= 0 && pos >= writeTick / WRITE_TICKS_PER_WORD;
    }

    private static String refusalText(Refusal r) {
        return switch (r) {
            case EMPTY_SLOTS -> "Every word needs a tile first";
            case NO_INK -> "The inkwell is empty";
            case BUSY -> "";
        };
    }

    private static String hintText(Hint h) {
        return switch (h) {
            case PICK_WORDS -> "Click a word on the manuscript";
            case PLACE_TILES -> "Drag tiles under the words";
            case WRITE -> "Click the inkwell";
            case DONE -> "Done. N: next, R: again";
        };
    }

    // ------------------------------------------------------------------ Zeichnen

    @Override
    protected void drawStation(GuiGraphics g) {
        syncPage();
        drawLectern(g, pageWords(true), session::isRevealed);
        drawPrintedSheet(g);
        drawSheet(g);
        drawBoxes(g);
        drawQuill(g);
        String sheet = session.isComplete() ? "" : "Sheet " + (session.pageIndex() + 1) + "/" + session.pageCount();
        boolean waiting = !session.isComplete() && !printed();
        drawHintLine(g, ui.rect("t_hint"), sheet, waiting ? "Print this sheet at the press first" : hintText(session.hint()));
    }

    private void drawCentered(GuiGraphics g, String text, int x, int y, int width, int color) {
        g.drawString(font, text, x + (width - font.width(text)) / 2, y, color, false);
    }

    /** Der gedruckte lateinische Bogen unter dem Pult. Leer, solange an der Presse nichts gedruckt ist. */
    private void drawPrintedSheet(GuiGraphics g) {
        int[] r = ui.rect("t_latin");
        if (!printed()) return;
        Fit fit = fit(pageWords(false), r, LATIN_LINE, 5);
        RaggedPaper.draw(g, ui, fit.paper()[0], fit.paper()[1], fit.paper()[2], fit.paper()[3], 2);
        List<String> words = pageWords(false);
        int[][] rects = latinWordRects();
        for (int i = 0; i < rects.length; i++) {
            g.drawString(font, words.get(i), rects[i][0], rects[i][1],
                    session.isRevealed(i) ? col("parchment_1") : col("ink_0"), false);
        }
    }

    /** Das Blatt, das beschrieben wird. Ein Wort erscheint erst, wenn es angeklickt wurde. */
    private void drawSheet(GuiGraphics g) {
        int[] sh = ui.rect("t_sheet");
        RaggedPaper.draw(g, ui, sh[0], sh[1], sh[2], sh[3], 2);

        for (int i = 0; i < session.slotCount(); i++) {
            if (!session.isRevealed(i)) continue;
            int x = cellX(i), y = cellY(i), sy = y + ui.slotDy;
            drawCentered(g, heads[i] == null ? session.word(i).latin() : heads[i], x, y + ui.headDy, ui.slotW, col("rubric"));

            if (pendingWrite(i)) {
                // die Feder ist noch nicht hier: das gelegte Plaettchen liegt noch
                int tw = homes[writeTileIds[i]][2];
                drawTile(g, x + (ui.slotW - tw) / 2, sy, tw, writeFaces[i]);
            } else if (session.isDone(i)) {
                drawCentered(g, session.written(i), x, sy + 1, ui.slotW, col("ink_0"));
                fill(g, x, sy + ui.tileH - 1, ui.slotW, 1, col("parchment_1"));
            } else {
                fill(g, x, sy, ui.slotW, ui.tileH, col("parchment_2"));
                fill(g, x, sy + ui.tileH - 1, ui.slotW, 1, col("parchment_0"));
                String smudged = session.smudged(i);
                if (smudged != null) {
                    drawCentered(g, smudged, x, sy + 1, ui.slotW, col("ink_0"));
                    int tw = font.width(smudged), x0 = x + (ui.slotW - tw) / 2 - 2;
                    for (int row = 0; row < ui.tileH - 2; row++) {
                        if (row % 3 == 2) continue;
                        g.fill(x0 + (row * 5) % 3, sy + 1 + row, x0 + tw + 4 - (row * 7) % 4, sy + 2 + row, col("smear"));
                    }
                }
            }
        }
        for (Tile t : session.tiles()) {
            if (t.slot() < 0 || t.id() == handTile) continue;
            int[] r = tileRect(t);
            drawTile(g, r[0], r[1], r[2], t.face());
        }
    }

    private void drawBoxes(GuiGraphics g) {
        for (int b = 0; b < BOXES; b++) {
            int[] r = ui.rect("t_box_" + b);
            frame(g, r, 3, "spruce_planks", 0xFF2A1A10);
            for (int row = 1; row < 4; row++) fill(g, r[0] + 4, r[1] + 3 + row * 10, r[2] - 8, 1, 0xFF3B2618);
        }
        for (Tile t : session.tiles()) {
            if (t.used() || t.slot() >= 0 || t.id() == handTile) continue;
            if (hiddenWhileWriting(t.id())) continue;
            int[] h = homes[t.id()];
            drawTile(g, h[0], h[1], h[2], t.face());
        }
    }

    /** Ein falsches Plaettchen kehrt erst in den Kasten zurueck, wenn die Feder an seinem Slot war. */
    private boolean hiddenWhileWriting(int tileId) {
        if (writeTick < 0) return false;
        for (int i = 0; i < writeTileIds.length; i++) {
            if (writeTileIds[i] == tileId) return pendingWrite(i);
        }
        return false;
    }

    private void drawTile(GuiGraphics g, int x, int y, int w, String text) {
        fill(g, x, y, w, ui.tileH, col("wood_4"));
        fill(g, x, y, w, 1, col("parchment_1"));
        fill(g, x, y, 1, ui.tileH, col("parchment_1"));
        fill(g, x, y + ui.tileH - 1, w, 1, col("wood_1"));
        fill(g, x + w - 1, y, 1, ui.tileH, col("wood_1"));
        g.drawString(font, text, x + (w - font.width(text)) / 2 + 1, y + 1, col("ink_0"), false);
    }

    /** Das Federglas. Die Feder steckt darin, ausser sie schreibt gerade auf dem Blatt. */
    private void drawQuill(GuiGraphics g) {
        int[] q = ui.rect("t_quill");
        fill(g, q[0], q[1] + 10, 12, 10, col("lead_1"));
        fill(g, q[0] + 2, q[1] + 8, 8, 2, col("lead_2"));
        fill(g, q[0] + 3, q[1] + 10, 6, 2, session.ink() > 0 ? col("ink_0") : col("barrel_empty"));
        if (writeTick >= 0) {
            int n = Math.min(writeOrder.size() - 1, writeTick / WRITE_TICKS_PER_WORD);
            int slot = writeOrder.get(n);
            drawFeather(g, cellX(slot) + ui.slotW - 12 + (writeTick % 2), cellY(slot) + ui.slotDy - 8);
        } else if (penSlot >= 0) {
            drawFeather(g, cellX(penSlot) + ui.slotW / 2 + (penTicks % 2) * 2, cellY(penSlot) + ui.headDy - 10);
        } else {
            drawFeather(g, q[0] + 5, q[1] - 4);
        }
    }

    /** Feder, schraeg nach rechts oben, die Spitze links unten. */
    private void drawFeather(GuiGraphics g, int x, int y) {
        for (int i = 0; i < 9; i++) {
            fill(g, x + i * 2, y + 14 - i * 2, 3, 3, i < 2 ? col("ink_0") : col("gui_light"));
            if (i >= 3) fill(g, x + i * 2 + 3, y + 15 - i * 2, 2, 2, col("lead_4"));
        }
    }

    @Override
    protected void drawHand(GuiGraphics g) {
        if (handTile < 0) return;
        Tile t = session.tiles().get(handTile);
        int w = homes[handTile][2];
        drawTile(g, mouseCX - w / 2, mouseCY - ui.tileH / 2, w, t.face());
    }

    // ------------------------------------------------------------------ Hilfen fuer den Selbsttest

    /** Klickt ein Wort an: im Manuskript ({@code greek}) oder auf dem gedruckten Bogen. */
    public void debugPick(int slot, boolean greek) {
        syncPage();
        int[] r = (greek ? lecternWordRects(pageWords(true)) : latinWordRects())[slot];
        clickCanvas(r[0] + r[2] / 2.0, r[1] + 4, 0);
    }

    /** Zieht ein freies Plaettchen mit diesem Wort in den Slot. */
    public void debugPlace(String face, int slot) {
        syncPage();
        for (Tile t : session.tiles()) {
            if (t.used() || t.slot() >= 0 || !t.face().equals(face)) continue;
            int[] r = tileRect(t);
            clickCanvas(r[0] + r[2] / 2.0, r[1] + r[3] / 2.0, 0);
            releaseCanvas(cellX(slot) + ui.slotW / 2.0, cellY(slot) + ui.slotDy + ui.tileH / 2.0, 0);
            return;
        }
        throw new IllegalArgumentException("Kein freies Plaettchen mit: " + face);
    }

    public void debugClickQuill() {
        int[] q = ui.rect("t_quill");
        clickCanvas(q[0] + q[2] / 2.0, q[1] + q[3] / 2.0, 0);
    }
}
