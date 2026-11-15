package de.scribery.client;

import java.util.List;
import java.util.Random;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.Window;

import de.scribery.core.PressSession;
import de.scribery.core.PressSession.Hint;
import de.scribery.core.PressSession.Paper;
import de.scribery.core.PressSession.PullResult;
import de.scribery.core.PressSession.Refusal;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;

/**
 * Die Presse (Phase 1): reiner Client-Screen mit Platzhalter-Fragmenten.
 * Alle Koordinaten sind GUI-Pixel der 384 x 216 grossen Leinwand aus art/layout.json.
 */
public class PressScreen extends Screen {
    private static final ResourceLocation HOTBAR = ResourceLocation.withDefaultNamespace("hud/hotbar");

    private static final int START_STOCK = 16;
    private static final int START_INK = 8;
    private static final int START_IRON = 24;

    private static final int CRANK_TICKS = 12;
    private static final int LEVER_TICKS = 14;
    private static final int LEVER_PROOF_AT = 5; // Rest-Ticks, ab denen der Abzug sichtbar ist
    private static final int FINISH_TICKS = 25;
    private static final int FLASH_TICKS = 50;

    private enum Hand { NONE, SORT, ROLLER, PAPER }

    private final UiLayout ui = UiLayout.get();
    private final List<DebugFragments.Fragment> fragments = DebugFragments.load();
    private int fragmentIndex;
    private PressSession session;

    private Hand hand = Hand.NONE;
    private char handSort;
    private double lastRollerX = Double.NaN;

    private int ticks;
    private int crankAnim, leverAnim, finishDelay, flashTicks;
    private String flash = "";
    private PullResult proof;

    // Leinwand -> Bildschirm
    private float scale = 1f, originX, originY;
    private int mouseCX, mouseCY;

    public PressScreen() {
        super(Component.literal("Scribery & Crafts"));
        loadFragment(0);
    }

    private void loadFragment(int index) {
        fragmentIndex = Math.floorMod(index, fragments.size());
        session = new PressSession(fragments.get(fragmentIndex).words(), ui.lineSlots,
                START_STOCK, START_INK, START_IRON, new Random());
        hand = Hand.NONE;
        proof = null;
        crankAnim = leverAnim = finishDelay = flashTicks = 0;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ------------------------------------------------------------------ Ablauf

    @Override
    public void tick() {
        ticks++;
        if (flashTicks > 0) flashTicks--;
        if (crankAnim > 0) crankAnim--;
        if (leverAnim > 0) {
            leverAnim--;
            if (leverAnim == LEVER_TICKS - 5) sound(SoundEvents.ANVIL_LAND, 0.7f, 0.35f);
            if (leverAnim == LEVER_PROOF_AT && proof != null) {
                sound(proof.allCorrect() ? SoundEvents.EXPERIENCE_ORB_PICKUP : SoundEvents.VILLAGER_NO, 1f, 0.5f);
            }
            if (leverAnim == 0 && proof != null && proof.allCorrect()) finishDelay = FINISH_TICKS;
        } else if (finishDelay > 0 && --finishDelay == 0) {
            session.finishLine();
            proof = null;
            sound(SoundEvents.BOOK_PAGE_TURN, 1f, 0.6f);
            if (session.lastWorn() > 0) {
                flash(session.lastWorn() == 1 ? "A letter wore out" : session.lastWorn() + " letters wore out");
            }
        }
    }

    private boolean busy() {
        return crankAnim > 0 || leverAnim > 0 || finishDelay > 0;
    }

    private void flash(String text) {
        flash = text;
        flashTicks = FLASH_TICKS;
    }

    private void sound(SoundEvent event, float pitch, float volume) {
        if (minecraft != null) minecraft.getSoundManager().play(SimpleSoundInstance.forUI(event, pitch, volume));
    }

    // ------------------------------------------------------------------ Eingabe

    private void updateTransform() {
        Window w = minecraft.getWindow();
        double gs = w.getGuiScale();
        int fit = Math.min(w.getWidth() / ui.canvasW, w.getHeight() / ui.canvasH);
        int k = (int) Math.max(1, Math.min(Math.floor(gs), fit));
        scale = (float) (k / gs);
        originX = (float) (((w.getWidth() - ui.canvasW * k) / 2) / gs);
        originY = (float) (((w.getHeight() - ui.canvasH * k) / 2) / gs);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        updateTransform();
        clickCanvas((mouseX - originX) / scale, (mouseY - originY) / scale, button);
        return true;
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        updateTransform();
        moveCanvas((mouseX - originX) / scale, (mouseY - originY) / scale);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_N && session.isComplete()) {
            loadFragment(fragmentIndex + 1);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_R && session.isComplete()) {
            loadFragment(fragmentIndex);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private static boolean in(int[] r, double x, double y, int pad) {
        return x >= r[0] - pad && x < r[0] + r[2] + pad && y >= r[1] - pad && y < r[1] + r[3] + pad;
    }

    private int slotAt(double x, double y) {
        if (y < ui.slotsY || y >= ui.slotsY + ui.sortH || x < ui.slotsX) return -1;
        int i = (int) ((x - ui.slotsX) / ui.pitch);
        if (i >= ui.lineSlots || x - ui.slotsX - i * ui.pitch >= ui.sortW) return -1;
        return i;
    }

    private int caseCellAt(double x, double y) {
        if (x < ui.caseX || y < ui.caseY) return -1;
        int col = (int) ((x - ui.caseX) / ui.cellW);
        int row = (int) ((y - ui.caseY) / ui.cellH);
        if (col >= ui.caseCols) return -1;
        int i = row * ui.caseCols + col;
        return i < ui.letters.length ? i : -1;
    }

    /** Klick in Leinwand-Koordinaten. Oeffentlich fuer den Selbsttest. */
    public void clickCanvas(double x, double y, int button) {
        if (busy()) return;

        int cell = caseCellAt(x, y);
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
            } else if (hand == Hand.SORT) {
                dropSort();
            }
            return;
        }
        if (hand == Hand.SORT && in(ui.rect("letter_case"), x, y, 0)) {
            dropSort();
            return;
        }

        int slot = slotAt(x, y);
        if (slot >= 0) {
            if (hand == Hand.SORT) {
                if (session.place(slot, handSort)) {
                    hand = Hand.NONE;
                    sound(SoundEvents.STONE_BUTTON_CLICK_ON, 1.3f, 0.6f);
                } else {
                    char old = session.pickUp(slot);
                    if (old != 0) {
                        session.place(slot, handSort);
                        handSort = old;
                        sound(SoundEvents.STONE_BUTTON_CLICK_ON, 1.3f, 0.6f);
                    }
                }
            } else if (hand == Hand.NONE) {
                char c = session.pickUp(slot);
                if (c != 0) {
                    hand = Hand.SORT;
                    handSort = c;
                    sound(SoundEvents.ITEM_PICKUP, 1.2f, 0.4f);
                }
            }
            return;
        }

        if (in(ui.rect("barrel"), x, y, 0)) {
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

        if (in(ui.rect("paper_stock"), x, y, 0)) {
            if (hand == Hand.NONE) {
                hand = Hand.PAPER;
                sound(SoundEvents.BOOK_PAGE_TURN, 1.2f, 0.6f);
            } else if (hand == Hand.PAPER) {
                hand = Hand.NONE;
            }
            return;
        }

        if (in(ui.rect("paper_tray"), x, y, 0)) {
            if (hand == Hand.PAPER && session.layPaper()) {
                hand = Hand.NONE;
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

    private void dropSort() {
        session.returnToCase(handSort);
        hand = Hand.NONE;
        sound(SoundEvents.ITEM_PICKUP, 0.9f, 0.3f);
    }

    /** Mausbewegung in Leinwand-Koordinaten. Oeffentlich fuer den Selbsttest. */
    public void moveCanvas(double x, double y) {
        if (hand != Hand.ROLLER) return;
        int[] forme = ui.rect("forme");
        if (y < forme[1] - 8 || y >= forme[1] + forme[3] + 8) {
            lastRollerX = Double.NaN;
            return;
        }
        if (!Double.isNaN(lastRollerX)) {
            boolean inked = false;
            for (int i = 0; i < ui.lineSlots; i++) {
                double center = ui.slotsX + i * ui.pitch + ui.sortW / 2.0;
                boolean crossed = (lastRollerX < center && x >= center) || (lastRollerX > center && x <= center);
                if (crossed && session.inkSlot(i)) inked = true;
            }
            if (inked) sound(SoundEvents.SLIME_BLOCK_STEP, 1.4f, 0.25f);
        }
        lastRollerX = x;
    }

    private static String refusalText(Refusal r) {
        return switch (r) {
            case NOT_INKED -> "Ink the type first";
            case NO_PAPER -> "Lay a paper strip first";
            case NOT_FED -> "Turn the crank first";
            case BUSY -> "";
        };
    }

    private static String hintText(Hint h) {
        return switch (h) {
            case SET_TYPE -> "Set the type";
            case DIP_ROLLER -> "Take the ink roller";
            case ROLL_INK -> "Roll over the type";
            case LAY_PAPER -> "Lay a paper strip";
            case TURN_CRANK -> "Turn the crank";
            case PULL_LEVER -> "Pull the lever";
            case DONE -> "Done. N: next, R: again";
        };
    }

    // ------------------------------------------------------------------ Zeichnen

    private int col(String name) {
        return ui.color(name);
    }

    private static void fill(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + h, color);
    }

    /** Flaeche mit hellem Rand oben/links und dunklem unten/rechts. */
    private static void panel(GuiGraphics g, int[] r, int face, int light, int dark) {
        panel(g, r[0], r[1], r[2], r[3], face, light, dark);
    }

    private static void panel(GuiGraphics g, int x, int y, int w, int h, int face, int light, int dark) {
        fill(g, x, y, w, h, face);
        fill(g, x, y, w, 1, light);
        fill(g, x, y, 1, h, light);
        fill(g, x, y + h - 1, w, 1, dark);
        fill(g, x + w - 1, y, 1, h, dark);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        updateTransform();
        mouseCX = (int) Math.floor((mouseX - originX) / scale);
        mouseCY = (int) Math.floor((mouseY - originY) / scale);

        g.pose().pushPose();
        g.pose().translate(originX, originY, 0);
        g.pose().scale(scale, scale, 1);

        drawTable(g);
        drawTopBar(g);
        drawLeft(g);
        drawPress(g);
        drawRight(g);
        int[] hb = ui.rect("hotbar");
        g.blitSprite(HOTBAR, hb[0], hb[1], hb[2], hb[3]);
        drawHand(g);

        g.pose().popPose();
    }

    private void drawTable(GuiGraphics g) {
        fill(g, 0, 0, ui.canvasW, ui.canvasH, col("wood_0"));
        fill(g, 0, 22, ui.canvasW, 168, col("wood_1"));
        for (int y = 22 + 27; y < 190; y += 28) fill(g, 0, y, ui.canvasW, 1, col("wood_0"));
    }

    private void drawTopBar(GuiGraphics g) {
        int face = col("gui_face"), light = col("gui_light"), dark = col("gui_shadow");
        int[] bar = ui.rect("title_bar");
        panel(g, bar, face, light, dark);
        String title = "SCRIBERY & CRAFTS";
        g.drawString(font, title, bar[0] + (bar[2] - font.width(title)) / 2, bar[1] + 4, dark, false);
        String res = "Ink " + session.ink() + "   Iron " + session.iron();
        g.drawString(font, res, bar[0] + bar[2] - font.width(res) - 6, bar[1] + 4, dark, false);
        String id = fragments.get(fragmentIndex).id() + "  " + fragments.get(fragmentIndex).ref();
        g.drawString(font, id, bar[0] + 6, bar[1] + 4, dark, false);

        // Stationswechsel kommt in Phase 2, die Pfeile sind noch ohne Funktion.
        for (String name : new String[] { "arrow_left", "arrow_right" }) {
            int[] r = ui.rect(name);
            panel(g, r, face, light, dark);
            String s = name.equals("arrow_left") ? "<" : ">";
            g.drawString(font, s, r[0] + (r[2] - font.width(s)) / 2 + 1, r[1] + 4, col("lead_3"), false);
        }
    }

    private void drawLeft(GuiGraphics g) {
        // Kerze
        int[] c = ui.rect("candle");
        fill(g, c[0], c[1] + c[3] - 4, c[2], 4, col("lead_1"));
        fill(g, c[0] + 3, c[1] + 10, 6, c[3] - 14, col("parchment_3"));
        fill(g, c[0] + 8, c[1] + 10, 1, c[3] - 14, col("parchment_1"));
        int frame = (ticks / 4) % 4;
        int fh = new int[] { 7, 8, 6, 8 }[frame];
        int dx = new int[] { 0, 0, 1, -1 }[frame];
        int top = c[1] + 9 - fh;
        fill(g, c[0] + 4 + dx, top, 4, fh, col("flame_2"));
        fill(g, c[0] + 5 + dx, top + 2, 2, fh - 2, col("flame_1"));
        fill(g, c[0] + 5, c[1] + 6, 2, 2, col("flame_0"));
        fill(g, c[0] + 5, c[1] + 9, 1, 1, col("ink_0"));

        // Pult mit Fragment
        panel(g, ui.rect("lectern"), col("wood_2"), col("wood_3"), col("wood_0"));
        int[] f = ui.rect("fragment");
        fill(g, f[0], f[1], f[2], f[3], col("parchment_1"));
        fill(g, f[0], f[1] + f[3] - 1, f[2], 1, col("parchment_0"));
        // ausgerissene Kanten
        int wood = col("wood_2");
        fill(g, f[0], f[1], 3, 2, wood);
        fill(g, f[0] + f[2] - 5, f[1], 5, 3, wood);
        fill(g, f[0] + f[2] - 2, f[1] + 20, 2, 6, wood);
        fill(g, f[0], f[1] + f[3] - 7, 4, 7, wood);
        fill(g, f[0] + 30, f[1] + f[3] - 3, 9, 3, wood);
        fill(g, f[0] + f[2] - 8, f[1] + f[3] - 5, 8, 5, wood);
        List<FormattedCharSequence> lines =
                font.split(FormattedText.of(fragments.get(fragmentIndex).greekText()), f[2] - 8);
        for (int i = 0; i < lines.size() && i < 5; i++) {
            g.drawString(font, lines.get(i), f[0] + 4, f[1] + 5 + i * 10, col("ink_1"), false);
        }

        // Stapel der fertigen Zeilen
        int[] st = ui.rect("proof_stack");
        fill(g, st[0], st[1], st[2], st[3], col("wood_0"));
        List<String> done = session.finishedLines();
        int visible = st[3] / 8;
        int first = Math.max(0, done.size() - visible);
        for (int j = first; j < done.size(); j++) {
            int sy = st[1] + 1 + (j - first) * 8;
            fill(g, st[0] + 2, sy, st[2] - 4, 6, col("parchment_3"));
            fill(g, st[0] + 2, sy + 6, st[2] - 4, 1, col("parchment_0"));
            String text = done.get(j);
            for (int k = 0; k < text.length(); k++) {
                if (text.charAt(k) != ' ') fill(g, st[0] + 5 + k * 3, sy + 2, 2, 3, col("ink_1"));
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
        panel(g, forme, col("wood_3"), col("wood_4"), col("wood_0"));
        fill(g, forme[0] + 2, forme[1] + 2, forme[2] - 4, forme[3] - 4, col("wood_0"));
        for (int i = 0; i < ui.lineSlots; i++) {
            int x = ui.slotsX + i * ui.pitch, y = ui.slotsY;
            char c = session.slot(i);
            if (c != 0) {
                drawSort(g, x, y, c, session.inkLevel(i), session.isFixed(i));
            } else if (session.isWordSlot(i)) {
                fill(g, x, y + ui.sortH - 1, ui.sortW, 1, col("lead_3"));
            } else {
                fill(g, x, y + 1, ui.sortW, ui.sortH - 2, col("wood_2"));
            }
        }

        // Papier: auf der Ablage, auf dem Weg in die Presse oder unter dem Tiegel
        fill(g, tray[0], tray[1], tray[2], tray[3], col("wood_0"));
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
        panel(g, platen[0], platen[1] + off, platen[2], platen[3], col("lead_1"), col("lead_2"), col("lead_0"));
        fill(g, platen[0] + 4, platen[1] + off + platen[3] - 5, platen[2] - 8, 3, col("lead_2"));

        // Kurbel
        int[] cr = ui.rect("crank");
        panel(g, cr, col("lead_2"), col("lead_3"), col("lead_0"));
        fill(g, cr[0] + 5, cr[1] + 13, 4, 4, col("lead_0"));
        int pos = crankAnim > 0 ? (crankAnim / 3) % 4 : 0;
        int[][] knob = { { 5, 2 }, { 9, 13 }, { 5, 24 }, { 1, 13 } };
        fill(g, cr[0] + knob[pos][0], cr[1] + knob[pos][1], 4, 4, col("wood_4"));

        // Hebel
        int[] lv = ui.rect("lever");
        boolean pulled = leverAnim > 0 && LEVER_TICKS - leverAnim <= 8;
        int topY = lv[1] + (pulled ? 18 : 0);
        fill(g, lv[0], lv[1] + lv[3] - 6, lv[2], 6, col("lead_1"));
        fill(g, lv[0] + 2, topY, 2, lv[1] + lv[3] - 6 - topY, col("wood_4"));
        fill(g, lv[0], topY, lv[2], 5, col("rubric"));

        // Abzug
        int[] ps = ui.rect("proof_slot");
        fill(g, ps[0], ps[1], ps[2], ps[3], col("wood_0"));
        if (proof != null && leverAnim <= LEVER_PROOF_AT) drawProof(g, ps);

        // Hinweiszeile
        int[] lc = ui.rect("line_counter");
        if (!session.isComplete()) {
            String line = "Line " + (session.lineIndex() + 1) + "/" + session.lineCount();
            g.drawString(font, line, lc[0] + 1, lc[1], col("parchment_1"), false);
        }
        boolean flashing = flashTicks > 0 && !flash.isEmpty();
        String hint = flashing ? flash : (busy() ? "" : hintText(session.hint()));
        g.drawString(font, hint, lc[0] + lc[2] - font.width(hint) - 1, lc[1],
                flashing ? col("flame_1") : col("gui_light"), false);

        // aktuelle griechische Zeile
        int[] gl = ui.rect("greek_line");
        fill(g, gl[0], gl[1], gl[2], gl[3], col("parchment_1"));
        fill(g, gl[0], gl[1] + gl[3] - 1, gl[2], 1, col("parchment_0"));
        String greek = session.currentGreekLine();
        g.drawString(font, greek, gl[0] + (gl[2] - font.width(greek)) / 2, gl[1] + 4, col("ink_0"), false);
    }

    private void drawStrip(GuiGraphics g, int x, int y) {
        int w = ui.rect("paper_tray")[2] - 4, h = ui.rect("paper_tray")[3] - 4;
        fill(g, x, y, w, h, col("parchment_3"));
        fill(g, x, y + h - 1, w, 1, col("parchment_0"));
    }

    private void drawProof(GuiGraphics g, int[] ps) {
        drawStrip(g, ps[0] + 2, ps[1] + 2);
        int dx = ps[0] - ui.rect("forme")[0];
        int y = ps[1] + 5;
        for (int i = 0; i < ui.lineSlots; i++) {
            char c = proof.printed()[i];
            if (c != 0) SortGlyphs.draw(g, c, ui.slotsX + dx + i * ui.pitch + 2, y, col("ink_0"));
        }
        // Falsche Woerter sind als Ganzes verwischt: man sieht nicht, welcher Buchstabe falsch war.
        int i = 0;
        while (i < ui.lineSlots) {
            if (!proof.smudged()[i]) { i++; continue; }
            int start = i;
            while (i < ui.lineSlots && proof.smudged()[i]) i++;
            int x0 = ui.slotsX + dx + start * ui.pitch - 1;
            int x1 = ui.slotsX + dx + (i - 1) * ui.pitch + ui.sortW + 1;
            for (int row = 0; row < SortGlyphs.HEIGHT + 1; row++) {
                if (row % 3 == 2) continue;
                g.fill(x0 + (row * 5) % 3, y - 1 + row, x1 - (row * 7) % 4, y + row, col("smear"));
            }
        }
    }

    /** Eine Bleiletter. Die Farbe zeigt, wie oft die Walze schon darueber lief. */
    private void drawSort(GuiGraphics g, int x, int y, char c, int ink, boolean fixed) {
        int face = fixed ? col("lead_2") : col("lead_3");
        int glyph = col("lead_0");
        if (ink == 1) { face = col("lead_1"); glyph = col("ink_0"); }
        if (ink >= 2) { face = col("ink_1"); glyph = col("lead_4"); }
        fill(g, x, y, ui.sortW, ui.sortH, face);
        if (!fixed && ink == 0) {
            fill(g, x, y, ui.sortW, 1, col("lead_4"));
            fill(g, x, y, 1, ui.sortH, col("lead_4"));
        }
        int shade = ink >= 2 ? col("ink_0") : col("lead_1");
        if (ink != 1) {
            fill(g, x, y + ui.sortH - 1, ui.sortW, 1, shade);
            fill(g, x + ui.sortW - 1, y, 1, ui.sortH, shade);
        }
        SortGlyphs.draw(g, c, x + 2, y + 1, glyph);
    }

    private void drawRight(GuiGraphics g) {
        // Fass: die Farbe der Oeffnung zeigt den Fuellstand
        int[] b = ui.rect("barrel");
        panel(g, b, col("wood_2"), col("wood_3"), col("wood_0"));
        fill(g, b[0], b[1] + 8, b[2], 2, col("lead_1"));
        fill(g, b[0], b[1] + b[3] - 10, b[2], 2, col("lead_1"));
        int ink = session.ink();
        String level = ink >= 6 ? "barrel_full" : ink >= 3 ? "barrel_two_thirds" : ink >= 1 ? "barrel_one_third" : "barrel_empty";
        fill(g, b[0] + 4, b[1] + 12, b[2] - 8, b[3] - 24, col(level));
        if (hand != Hand.ROLLER) {
            int[] r = ui.rect("roller_rest");
            fill(g, r[0], r[1] + 2, r[2] - 8, r[3] - 4, col("lead_3"));
            fill(g, r[0], r[1] + r[3] - 3, r[2] - 8, 1, col("lead_1"));
            fill(g, r[0] + r[2] - 8, r[1] + 4, 8, 2, col("wood_4"));
        }

        // Papiervorrat
        int[] p = ui.rect("paper_stock");
        fill(g, p[0], p[1], p[2], p[3], col("parchment_3"));
        for (int y = p[1] + 3; y < p[1] + p[3]; y += 4) fill(g, p[0], y, p[2], 1, col("parchment_0"));

        // Buchstabenbox
        panel(g, ui.rect("letter_case"), col("lead_0"), col("lead_1"), col("ink_0"));
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

    private void drawHand(GuiGraphics g) {
        switch (hand) {
            case SORT -> drawSort(g, mouseCX - ui.sortW / 2, mouseCY - ui.sortH / 2, handSort, 0, false);
            case ROLLER -> {
                // am Cursor groesser gezeichnet: Walze stehend, Griff nach unten
                fill(g, mouseCX - 4, mouseCY - 12, 8, 24, col("ink_1"));
                fill(g, mouseCX - 4, mouseCY - 12, 2, 24, col("lead_2"));
                fill(g, mouseCX - 1, mouseCY + 12, 2, 4, col("lead_3"));
                fill(g, mouseCX - 2, mouseCY + 16, 4, 10, col("wood_4"));
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
        clickCanvas(ui.slotsX + slot * ui.pitch + ui.sortW / 2.0, ui.slotsY + ui.sortH / 2.0, 0);
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
