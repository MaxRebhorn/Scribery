package de.scribery.client;

import java.util.List;
import java.util.Random;

import org.lwjgl.glfw.GLFW;


import de.scribery.core.PressSession;
import de.scribery.core.PressSession.Hint;
import de.scribery.core.PressSession.Paper;
import de.scribery.core.PressSession.PullResult;
import de.scribery.core.PressSession.Refusal;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;

/**
 * Die Presse: reiner Client-Screen mit Platzhalter-Fragmenten. Gesetzt wird die lateinische Umschrift.
 * Alle Koordinaten sind GUI-Pixel der 384 x 216 grossen Leinwand aus art/layout.json.
 */
public class PressScreen extends StationScreen {
    private static final int CRANK_TICKS = 12;
    private static final int LEVER_TICKS = 14;
    private static final int LEVER_PROOF_AT = 5; // Rest-Ticks, ab denen der Abzug sichtbar ist
    private static final int FINISH_TICKS = 25;
    /** Halbe Laenge der Walze in GUI-Pixeln. */
    private static final int ROLLER_REACH = 21;

    private enum Hand { NONE, SORT, ROLLER, PAPER }

    private PressSession session;

    private Hand hand = Hand.NONE;
    private char handSort;
    private double lastRollerX = Double.NaN;
    private RollerSound rollerSound;

    private int crankAnim, leverAnim, finishDelay;
    private PullResult proof;

    public PressScreen() {
        this(new Workbench(0));
    }

    public PressScreen(Workbench bench) {
        super(bench);
        session = bench.press;
    }

    @Override
    protected String resourceText() {
        return "Ink " + session.ink() + "   Iron " + session.iron();
    }

    @Override
    protected Stations.Kind kind() {
        return Stations.Kind.PRESS;
    }

    @Override
    protected void drawStation(GuiGraphics g) {
        if (bench.fragment != null) drawLeft(g);
        if (hasPress()) drawPress(g);
        if (hasCabinet()) drawRight(g);
    }

    /** Die Presse selbst: Setzform, Papierablage, Kurbel, Hebel. */
    private boolean hasPress() {
        return bench.stations.press();
    }

    /** Der Lettern-Schrank: Buchstabenbox, Tintenfass mit Walze, Papiervorrat. */
    private boolean hasCabinet() {
        return bench.stations.cabinet();
    }

    @Override
    protected void onBenchChanged() {
        session = bench.press;
        hand = Hand.NONE;
        proof = null;
        crankAnim = leverAnim = finishDelay = 0;
    }

    // ------------------------------------------------------------------ Ablauf

    @Override
    protected void stationTick() {
        if (crankAnim > 0) crankAnim--;
        if (leverAnim > 0) {
            leverAnim--;
            if (leverAnim == LEVER_TICKS - 5) sound(SoundEvents.ANVIL_LAND, 0.7f, 0.35f);
            if (leverAnim == LEVER_PROOF_AT && proof != null) {
                sound(proof.allCorrect() ? SoundEvents.EXPERIENCE_ORB_PICKUP : SoundEvents.VILLAGER_NO, 1f, 0.5f);
            }
            if (leverAnim == 0 && proof != null && proof.allCorrect()) finishDelay = FINISH_TICKS;
        } else if (finishDelay > 0 && --finishDelay == 0) {
            session.finishPage();
            proof = null;
            sound(SoundEvents.BOOK_PAGE_TURN, 1f, 0.6f);
            if (session.lastWorn() > 0) {
                flash(session.lastWorn() == 1 ? "A letter wore out" : session.lastWorn() + " letters wore out");
            }
        }
    }

    @Override
    protected boolean busy() {
        return crankAnim > 0 || leverAnim > 0 || finishDelay > 0;
    }

    // ------------------------------------------------------------------ Eingabe

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

    private int slotX(int slot) {
        return ui.slotsX + (slot % ui.lineSlots) * ui.pitch;
    }

    private int slotY(int slot) {
        return ui.slotsY + (slot / ui.lineSlots) * ui.rowPitch;
    }

    private int slotAt(double x, double y) {
        if (y < ui.slotsY || x < ui.slotsX) return -1;
        int col = (int) ((x - ui.slotsX) / ui.pitch);
        int row = (int) ((y - ui.slotsY) / ui.rowPitch);
        if (col >= ui.lineSlots || row >= ui.rows) return -1;
        if (x - ui.slotsX - col * ui.pitch >= ui.sortW || y - ui.slotsY - row * ui.rowPitch >= ui.sortH) return -1;
        return row * ui.lineSlots + col;
    }

    /** Zelle der Buchstabenbox unter dem Punkt, oder -1. */
    private int caseCellAt(double x, double y) {
        if (x < ui.caseX || y < ui.caseY) return -1;
        int col = (int) ((x - ui.caseX) / ui.cellW);
        int row = (int) ((y - ui.caseY) / ui.cellH);
        if (col >= ui.caseCols) return -1;
        int i = row * ui.caseCols + col;
        return i < ui.letters.length ? i : -1;
    }

    /** Klick in Leinwand-Koordinaten. Oeffentlich fuer den Selbsttest. */
    @Override
    public void clickCanvas(double x, double y, int button) {
        if (busy()) return;

        int cell = hasCabinet() ? caseCellAt(x, y) : -1;
        if (cell >= 0) {
            char c = ui.letters[cell];
            if (button == 1) {
                if (session.cast(c)) sound(SoundEvents.ANVIL_USE, 1.4f, 0.3f);
                else flash("No iron left");
            } else if (hand == Hand.NONE) {
                if (session.takeFromCase(c)) {
                    hand = Hand.SORT;
                    handSort = c;
                    sound(SoundEvents.ITEM_PICKUP, 1.2f, 0.4f);
                } else {
                    flash("No " + c + " left. Right-click to cast");
                }
            }
            return;
        }

        // Eine gesetzte Letter wieder greifen. Abgelegt wird beim Loslassen, siehe releaseCanvas.
        int slot = hasPress() ? slotAt(x, y) : -1;
        if (slot >= 0) {
            if (hand == Hand.NONE) {
                char c = session.pickUp(slot);
                if (c != 0) {
                    hand = Hand.SORT;
                    handSort = c;
                    sound(SoundEvents.ITEM_PICKUP, 1.2f, 0.4f);
                }
            }
            return;
        }

        if (hasCabinet() && in(ui.rect("barrel"), x, y, 0)) {
            if (hand == Hand.NONE) {
                if (session.dipRoller()) {
                    hand = Hand.ROLLER;
                    lastRollerX = Double.NaN;
                    sound(SoundEvents.BOTTLE_FILL, 0.8f, 0.6f);
                } else {
                    flash("The barrel is empty");
                }
            } else if (hand == Hand.ROLLER) {
                session.returnRoller();
                hand = Hand.NONE;
                sound(SoundEvents.STONE_BUTTON_CLICK_ON, 0.8f, 0.4f);
            }
            return;
        }

        if (hasCabinet() && in(ui.rect("paper_stock"), x, y, 0)) {
            if (hand == Hand.NONE) {
                hand = Hand.PAPER;
                sound(SoundEvents.BOOK_PAGE_TURN, 1.2f, 0.6f);
            } else if (hand == Hand.PAPER) {
                hand = Hand.NONE;
            }
            return;
        }

        if (!hasPress()) return;

        if (in(ui.rect("paper_tray"), x, y, 0)) {
            if (hand == Hand.PAPER && session.layPaper()) {
                hand = Hand.NONE;
                proof = null; // ein alter, verwischter Abzug macht Platz
                sound(SoundEvents.BOOK_PAGE_TURN, 0.9f, 0.6f);
            }
            return;
        }

        if (in(ui.rect("crank"), x, y, 1)) {
            Refusal r = session.crank();
            if (r == null) {
                crankAnim = CRANK_TICKS;
                sound(SoundEvents.GRINDSTONE_USE, 1.2f, 0.4f);
            } else {
                flash(refusalText(r));
            }
            return;
        }

        if (in(ui.rect("lever"), x, y, 3)) {
            PullResult r = session.pull();
            if (r.refusal() != null) {
                flash(refusalText(r.refusal()));
            } else {
                proof = r;
                leverAnim = LEVER_TICKS;
                sound(SoundEvents.LEVER_CLICK, 0.7f, 0.8f);
            }
        }
    }

    /** Slot unter dem Cursor, grosszuegig: irgendwo in der Setzform zaehlt der naechste Slot. */
    private int slotNear(double x, double y) {
        if (!hasPress() || !in(ui.rect("forme"), x, y, 3)) return -1;
        int col = (int) Math.floor((x - ui.slotsX + 0.5) / ui.pitch);
        int row = (int) Math.floor((y - ui.slotsY + 0.5) / ui.rowPitch);
        col = Math.max(0, Math.min(ui.lineSlots - 1, col));
        row = Math.max(0, Math.min(ui.rows - 1, row));
        return row * ui.lineSlots + col;
    }

    /**
     * Maustaste losgelassen: Eine gezogene Letter wird in den Slot unter dem Cursor gedrueckt.
     * Liegt dort schon eine gesetzte, tauscht sie den Platz und die alte geht in die Box.
     * Ueberall sonst faellt die Letter zurueck in die Box. Oeffentlich fuer den Selbsttest.
     */
    @Override
    public void releaseCanvas(double x, double y, int button) {
        if (hand != Hand.SORT) return;
        hand = Hand.NONE;
        int slot = slotNear(x, y);
        if (slot >= 0) {
            if (session.place(slot, handSort)) {
                sound(SoundEvents.STONE_BUTTON_CLICK_ON, 1.3f, 0.6f);
                return;
            }
            char old = session.pickUp(slot);
            if (old != 0 && session.place(slot, handSort)) {
                session.returnToCase(old);
                sound(SoundEvents.STONE_BUTTON_CLICK_ON, 1.3f, 0.6f);
                return;
            }
        }
        session.returnToCase(handSort);
        sound(SoundEvents.ITEM_PICKUP, 0.9f, 0.3f);
    }

    @Override
    public void removed() {
        if (rollerSound != null) rollerSound.end();
        super.removed();
    }

    /** Mausbewegung in Leinwand-Koordinaten. Oeffentlich fuer den Selbsttest. */
    @Override
    public void moveCanvas(double x, double y) {
        if (hand != Hand.ROLLER || !hasPress()) return;
        int[] forme = ui.rect("forme");
        if (y < forme[1] - 8 || y >= forme[1] + forme[3] + 8) {
            lastRollerX = Double.NaN;
            return;
        }
        if (!Double.isNaN(lastRollerX)) {
            // Die Walze ist so lang, dass sie in der Mitte gefuehrt alle Zeilen erreicht.
            for (int i = 0; i < session.slotCount(); i++) {
                if (Math.abs(slotY(i) + ui.sortH / 2.0 - y) > ROLLER_REACH) continue;
                double center = slotX(i) + ui.sortW / 2.0;
                boolean crossed = (lastRollerX < center && x >= center) || (lastRollerX > center && x <= center);
                if (crossed) session.inkSlot(i);
            }
            // Rollgeraeusch, solange sich die Walze ueber der Setzform bewegt
            if (x != lastRollerX && in(ui.rect("forme"), x, y, 8)) {
                if (rollerSound == null || rollerSound.isStopped()) {
                    rollerSound = new RollerSound();
                    if (minecraft != null) minecraft.getSoundManager().play(rollerSound);
                }
                rollerSound.keepRolling();
            }
        }
        lastRollerX = x;
    }

    private static String refusalText(Refusal r) {
        return switch (r) {
            case NOT_INKED -> "Ink the type first";
            case NO_PAPER -> "Lay a sheet of paper first";
            case NOT_FED -> "Turn the crank first";
            case BUSY -> "";
        };
    }

    private static String hintText(Hint h) {
        return switch (h) {
            case SET_TYPE -> "Set the type";
            case DIP_ROLLER -> "Take the ink roller";
            case ROLL_INK -> "Roll over the type";
            case LAY_PAPER -> "Lay a sheet of paper";
            case TURN_CRANK -> "Turn the crank";
            case PULL_LEVER -> "Pull the lever";
            case DONE -> "Done. N: next, R: again";
        };
    }

    // ------------------------------------------------------------------ Zeichnen

    private void drawLeft(GuiGraphics g) {
        drawLectern(g, bench.fragment.words().stream().map(w -> w.greek()).toList(), i -> false);

        // Fertige Zeilen: kleine Zettel, jeder so breit wie sein Text, ohne Kasten drumherum
        int[] st = ui.rect("proof_stack");
        List<String> done = session.finishedLines();
        int visible = st[3] / 9;
        int first = Math.max(0, done.size() - visible);
        for (int j = first; j < done.size(); j++) {
            int sy = st[1] + (j - first) * 9;
            String text = done.get(j);
            int sw = Math.min(st[2], text.length() * 3 + 8);
            int sx = st[0] + (st[2] - sw) / 2;
            RaggedPaper.draw(g, ui, sx, sy, sw, 8, 1);
            for (int k = 0; k < text.length(); k++) {
                if (text.charAt(k) != ' ') fill(g, sx + 4 + k * 3, sy + 3, 2, 3, col("ink_1"));
            }
        }
    }

    private int platenOffset() {
        if (leverAnim == 0) return 0;
        int t = LEVER_TICKS - leverAnim;
        if (t <= 3) return Math.min(ui.platenTravel, t * 2);
        if (t <= 8) return ui.platenTravel;
        return Math.max(0, ui.platenTravel - (t - 8) * 2);
    }

    private void drawPress(GuiGraphics g) {
        int[] forme = ui.rect("forme");
        int[] platen = ui.rect("platen");
        int[] tray = ui.rect("paper_tray");

        // Pfosten, an denen der Tiegel laeuft
        fill(g, platen[0] + 6, platen[1] + 4, 4, forme[1] - platen[1], col("lead_0"));
        fill(g, platen[0] + platen[2] - 10, platen[1] + 4, 4, forme[1] - platen[1], col("lead_0"));

        // Setzform
        frame(g, forme, 4, "iron_block", 0xFF23232B);
        tile(g, block("iron_block"), forme[0] + 4, forme[1] + 4, forme[2] - 8, forme[3] - 8);
        fill(g, forme[0] + 4, forme[1] + 4, forme[2] - 8, forme[3] - 8, 0xB0101014);
        fill(g, forme[0] + 4, forme[1] + 4, forme[2] - 8, 1, 0xFF0E0E12);
        fill(g, forme[0] + 4, forme[1] + 4, 1, forme[3] - 8, 0xFF0E0E12);
        // Die Setzform zeigt immer alle Slots. Was der Text nicht braucht, bleibt einfach frei.
        for (int i = 0; i < session.slotCount(); i++) {
            int x = slotX(i), y = slotY(i);
            char c = session.slot(i);
            if (c != 0) {
                drawSort(g, x, y, c, session.inkLevel(i), session.isFixed(i));
            } else if (session.isSpacer(i)) {
                fill(g, x, y + 1, ui.sortW, ui.sortH - 2, 0xFF4A4D54);
            } else {
                fill(g, x, y + ui.sortH - 1, ui.sortW, 1, col("lead_3"));
            }
        }

        // Papier: auf der Ablage, auf dem Weg in die Presse oder unter dem Tiegel
        frame(g, tray, 2, "oak_planks", 0xFF2A1A10);
        int restY = tray[1] + 2, fedY = forme[1] + 3;
        boolean pressing = leverAnim > LEVER_PROOF_AT;
        if (session.paper() == Paper.ON_TRAY) {
            drawStrip(g, tray[0] + 2, restY);
        } else if (session.paper() == Paper.FED || pressing) {
            int y = crankAnim > 0 ? fedY + (restY - fedY) * crankAnim / CRANK_TICKS : fedY;
            drawStrip(g, tray[0] + 2, y);
        }

        // Tiegel
        int off = platenOffset();
        int py = platen[1] + off;
        frame(g, platen[0], py, platen[2], platen[3], 3, "iron_block", 0xFF6F737B);
        fill(g, platen[0] + 3, py + platen[3] - 6, platen[2] - 6, 3, 0xFF4A4D54);
        nail(g, platen[0] + 6, py + 5);
        nail(g, platen[0] + platen[2] / 2 - 1, py + 5);
        nail(g, platen[0] + platen[2] - 9, py + 5);

        // Kurbel: Eisengehaeuse mit Schwungrad, an dem der Griff laeuft
        int[] cr = ui.rect("crank");
        frame(g, cr, 2, "iron_block", 0xFF4A4D54);
        int cx = cr[0] + 7, cy = cr[1] + 15;
        for (int dy = -6; dy <= 6; dy++) {
            for (int dx = -6; dx <= 6; dx++) {
                int d = dx * dx + dy * dy;
                if (d > 36) continue;
                fill(g, cx + dx, cy + dy, 1, 1, d > 28 ? 0xFF2B2D31 : d > 8 ? 0xFF9A9EA6 : 0xFF6F737B);
            }
        }
        fill(g, cx - 1, cy - 1, 3, 3, 0xFF2B2D31);
        int pos = crankAnim > 0 ? (crankAnim / 3) % 4 : 0;
        int[][] knob = { { 5, 2 }, { 9, 13 }, { 5, 24 }, { 1, 13 } };
        int kx = cr[0] + knob[pos][0] + 2, ky = cr[1] + knob[pos][1] + 2;
        for (int k = 0; k <= 8; k++) {
            fill(g, cx + (kx - cx) * k / 8, cy + (ky - cy) * k / 8, 2, 2, 0xFF7A5230);
        }
        fill(g, kx - 2, ky - 2, 4, 4, 0xFFB98A56);
        fill(g, kx - 2, ky + 1, 4, 1, 0xFF5A3A22);

        // Hebel: Sockel aus Bruchstein, Holzgriff mit rotem Knauf
        int[] lv = ui.rect("lever");
        boolean pulled = leverAnim > 0 && LEVER_TICKS - leverAnim <= 8;
        int topY = lv[1] + (pulled ? 18 : 0);
        fill(g, lv[0] + 1, topY, 4, lv[1] + lv[3] - 8 - topY, 0xFF0E0E12);
        fill(g, lv[0] + 2, topY, 2, lv[1] + lv[3] - 8 - topY, 0xFFB98A56);
        frame(g, lv[0] - 1, lv[1] + lv[3] - 8, lv[2] + 2, 8, 1, "cobblestone", 0xFF6F737B);
        fill(g, lv[0] - 1, topY - 1, lv[2] + 2, 7, 0xFF0E0E12);
        fill(g, lv[0], topY, lv[2], 5, 0xFF9C2F25);
        fill(g, lv[0], topY, lv[2], 1, 0xFFD2574B);

        // Abzug: kommt auf die Ablage zurueck, von der das Papier kam
        if (proof != null && leverAnim <= LEVER_PROOF_AT && session.paper() == Paper.NONE) drawProof(g, tray);

        // Hinweiszeile
        String sheet = session.isComplete() || session.isFree() ? "" : "Sheet " + (session.pageIndex() + 1) + "/" + session.pageCount();
        drawHintLine(g, ui.rect("line_counter"), sheet, hintText(session.hint()));
    }

    private void drawStrip(GuiGraphics g, int x, int y) {
        SheetRenderer.paper(g, ui, x, y);
    }

    private void drawProof(GuiGraphics g, int[] ps) {
        SheetRenderer.paper(g, ui, ps[0] + 2, ps[1] + 2);
        SheetRenderer.print(g, ui, ps[0] + 2, ps[1] + 2, proof.printed(), proof.smudged());
    }

    private static final java.util.Map<Character, net.minecraft.resources.ResourceLocation> LETTER_TEXTURES =
            new java.util.HashMap<>();

    /** Die Letter-Textur aus unseren Items (letter_type_*.png). */
    private static net.minecraft.resources.ResourceLocation letterTexture(char c) {
        return LETTER_TEXTURES.computeIfAbsent(c, ch -> {
            String n = ch == 'Ē' ? "e_long" : ch == 'Ō' ? "o_long" : String.valueOf(Character.toLowerCase(ch));
            return net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(de.scribery.Scribery.MODID,
                    "textures/item/letter_type_" + n + ".png");
        });
    }

    /** Eine Bleiletter. Die dunkle Tinte darueber zeigt, wie oft die Walze schon darueber lief. */
    private void drawSort(GuiGraphics g, int x, int y, char c, int ink, boolean fixed) {
        g.blit(letterTexture(c), x, y, ui.sortW, ui.sortH, 2f, 2f, 10, 12, 16, 16);
        if (fixed) fill(g, x, y, ui.sortW, ui.sortH, 0x55000000);
        if (ink == 1) fill(g, x, y, ui.sortW, ui.sortH, 0x66101014);
        if (ink >= 2) fill(g, x, y, ui.sortW, ui.sortH, 0xA0101014);
    }

    private void drawRight(GuiGraphics g) {
        // Fass: die Farbe der Oeffnung zeigt den Fuellstand
        int[] b = ui.rect("barrel");
        tile(g, block("barrel_side"), b[0], b[1], b[2], b[3]);
        fill(g, b[0], b[1], b[2], 1, 0xFF0E0E12);
        fill(g, b[0], b[1] + b[3] - 1, b[2], 1, 0xFF0E0E12);
        fill(g, b[0], b[1], 1, b[3], 0xFF0E0E12);
        fill(g, b[0] + b[2] - 1, b[1], 1, b[3], 0xFF0E0E12);
        fill(g, b[0] + 3, b[1] + 11, b[2] - 6, b[3] - 22, 0xFF0E0E12);
        fill(g, b[0] + 3, b[1] + b[3] - 12, b[2] - 6, 1, 0xFF9A9EA6);
        int ink = session.ink();
        String level = ink >= 6 ? "barrel_full" : ink >= 3 ? "barrel_two_thirds" : ink >= 1 ? "barrel_one_third" : "barrel_empty";
        fill(g, b[0] + 4, b[1] + 12, b[2] - 8, b[3] - 24, col(level));
        if (hand != Hand.ROLLER) {
            int[] r = ui.rect("roller_rest");
            fill(g, r[0], r[1] + 2, r[2] - 8, r[3] - 4, col("lead_3"));
            fill(g, r[0], r[1] + r[3] - 3, r[2] - 8, 1, col("lead_1"));
            fill(g, r[0] + r[2] - 8, r[1] + 4, 8, 2, col("wood_4"));
        }

        // Papiervorrat: ein schief gestapelter Stoss einzelner Boegen
        int[] p = ui.rect("paper_stock");
        fill(g, p[0] + 2, p[1] + p[3] - 5, p[2] - 2, 4, 0x66000000);
        for (int i = 0; i < 8; i++) {
            int dx = new int[] { 1, 0, 2, 1, 0, 2, 1, 0 }[i];
            int y = p[1] + p[3] - 17 - i * 3;
            int w = p[2] - 4;
            fill(g, p[0] + dx, y, w, 14, col("parchment_0"));
            fill(g, p[0] + dx + 1, y, w - 2, 13, col("parchment_3"));
            fill(g, p[0] + dx + 1, y + 11, w - 2, 2, col("parchment_1"));
            if (i % 3 == 0) fill(g, p[0] + dx + 1, y, 2, 13, col("parchment_2"));
        }
        int ty = p[1] + p[3] - 17 - 7 * 3;
        fill(g, p[0] + 3, ty + 3, p[2] - 10, 1, col("parchment_1"));
        fill(g, p[0] + 3, ty + 6, p[2] - 12, 1, col("parchment_1"));
        fill(g, p[0] + 3, ty + 9, p[2] - 9, 1, col("parchment_1"));

        // Buchstabenbox
        int[] lc = ui.rect("letter_case");
        frame(g, lc, 2, "iron_block", 0xFF0E0E12);
        for (int c = 0; c < ui.caseCols; c++) {
            for (int r = 0; r < 5; r++) {
                int x = ui.caseX + c * ui.cellW, y = ui.caseY + r * ui.cellH;
                fill(g, x, y, 1, ui.cellH, 0xFF23232B);
                fill(g, x, y + ui.cellH - 1, ui.cellW, 1, 0xFF23232B);
            }
        }
        for (int i = 0; i < ui.letters.length; i++) {
            char c = ui.letters[i];
            int cx = ui.caseX + (i % ui.caseCols) * ui.cellW;
            int cy = ui.caseY + (i / ui.caseCols) * ui.cellH;
            int n = session.stock(c);
            if (n > 0) drawSort(g, cx + ui.sortOffX, cy + ui.sortOffY, c, 0, false);
            else fill(g, cx + ui.sortOffX, cy + ui.sortOffY, ui.sortW, ui.sortH, col("ink_0"));
            String count = Integer.toString(n);
            g.drawString(font, count, cx + (ui.cellW - font.width(count)) / 2 + 1, cy + ui.countOffY,
                    n > 0 ? col("gui_light") : col("rubric"), false);
        }
    }

    @Override
    protected void drawHand(GuiGraphics g) {
        switch (hand) {
            case SORT -> drawSort(g, mouseCX - ui.sortW / 2, mouseCY - ui.sortH / 2, handSort, 0, false);
            case ROLLER -> {
                // am Cursor groesser gezeichnet: Walze stehend, Griff nach unten
                fill(g, mouseCX - 4, mouseCY - ROLLER_REACH, 8, 2 * ROLLER_REACH, col("ink_1"));
                fill(g, mouseCX - 4, mouseCY - ROLLER_REACH, 2, 2 * ROLLER_REACH, col("lead_2"));
                fill(g, mouseCX - 1, mouseCY + ROLLER_REACH, 2, 4, col("lead_3"));
                fill(g, mouseCX - 2, mouseCY + ROLLER_REACH + 4, 4, 10, col("wood_4"));
            }
            case PAPER -> {
                fill(g, mouseCX - 20, mouseCY - 4, 40, 8, col("parchment_3"));
                fill(g, mouseCX - 20, mouseCY + 3, 40, 1, col("parchment_0"));
            }
            case NONE -> { }
        }
    }

    // ------------------------------------------------------------------ Hilfen fuer den Selbsttest

    public void debugClick(String rectName) {
        int[] r = ui.rect(rectName);
        clickCanvas(r[0] + r[2] / 2.0, r[1] + r[3] / 2.0, 0);
    }

    public void debugClickSlot(int slot) {
        clickCanvas(slotX(slot) + ui.sortW / 2.0, slotY(slot) + ui.sortH / 2.0, 0);
    }

    public void debugReleaseOnSlot(int slot) {
        releaseCanvas(slotX(slot) + ui.sortW / 2.0, slotY(slot) + ui.sortH / 2.0, 0);
    }

    public void debugReleaseOnCase() {
        int[] r = ui.rect("letter_case");
        releaseCanvas(r[0] + r[2] / 2.0, r[1] + r[3] / 2.0, 0);
    }

    public void debugClickLetter(char letter) {
        for (int i = 0; i < ui.letters.length; i++) {
            if (ui.letters[i] != letter) continue;
            clickCanvas(ui.caseX + (i % ui.caseCols) * ui.cellW + ui.cellW / 2.0,
                    ui.caseY + (i / ui.caseCols) * ui.cellH + ui.sortH / 2.0, 0);
            return;
        }
        throw new IllegalArgumentException("Nicht in der Box: " + letter);
    }

    /** Fuehrt die Walze einmal ueber die ganze Setzform, abwechselnd nach rechts und links. */
    public void debugRollAcross(boolean toRight) {
        int[] f = ui.rect("forme");
        double y = f[1] + f[3] / 2.0;
        moveCanvas(toRight ? f[0] - 4 : f[0] + f[2] + 4, y);
        moveCanvas(toRight ? f[0] + f[2] + 4 : f[0] - 4, y);
    }
}
