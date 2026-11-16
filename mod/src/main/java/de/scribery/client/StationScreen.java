package de.scribery.client;

import java.util.List;

import com.mojang.blaze3d.platform.Window;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.FormattedCharSequence;

/**
 * Gemeinsame Basis aller Stationen: Leinwand von 384 x 216 GUI-Pixeln, Tisch, Titelleiste, Kerze,
 * Hotbar und die Umrechnung der Maus auf Leinwand-Koordinaten.
 *
 * Die Pfeile in der Titelleiste wechseln die Station. Das ist eine Abkuerzung fuer /scribery debug;
 * im Spiel werden die Stationen spaeter eigene Bloecke.
 */
public abstract class StationScreen extends Screen {
    private static final ResourceLocation HOTBAR = ResourceLocation.withDefaultNamespace("hud/hotbar");
    protected static final int FLASH_TICKS = 50;

    protected final UiLayout ui = UiLayout.get();
    protected final List<DebugFragments.Fragment> fragments = DebugFragments.load();
    protected int fragmentIndex;

    protected int ticks;
    protected int flashTicks;
    protected String flash = "";

    // Leinwand -> Bildschirm
    private float scale = 1f, originX, originY;
    protected int mouseCX, mouseCY;

    /** Der Arbeitsstand am Fragment, geteilt mit den Nachbarstationen. */
    protected Workbench bench;

    protected StationScreen(Workbench bench) {
        super(Component.literal("Scribery & Crafts"));
        this.bench = bench;
        this.fragmentIndex = bench.fragmentIndex;
    }

    /** Beginnt mit einem Fragment von vorn. */
    protected void restart(int index) {
        bench = new Workbench(index, bench.stations);
        fragmentIndex = bench.fragmentIndex;
        flashTicks = 0;
        onBenchChanged();
    }

    /** Die Station uebernimmt den neuen Arbeitsstand. */
    protected abstract void onBenchChanged();

    // ------------------------------------------------------------------ von der Station zu liefern

    protected abstract void stationTick();

    /** Eine Animation laeuft, Klicks werden ignoriert. */
    protected abstract boolean busy();

    /** Text rechts in der Titelleiste, z. B. Vorraete. */
    protected abstract String resourceText();

    /** Welche Station das ist. */
    protected abstract Stations.Kind kind();

    protected abstract void drawStation(GuiGraphics g);

    /** Was am Cursor haengt. */
    protected abstract void drawHand(GuiGraphics g);

    /** Maustaste gedrueckt, in Leinwand-Koordinaten. Oeffentlich fuer den Selbsttest. */
    public abstract void clickCanvas(double x, double y, int button);

    /** Maustaste losgelassen, in Leinwand-Koordinaten. Oeffentlich fuer den Selbsttest. */
    public abstract void releaseCanvas(double x, double y, int button);

    /** Maus bewegt, in Leinwand-Koordinaten. Oeffentlich fuer den Selbsttest. */
    public void moveCanvas(double x, double y) {}

    // ------------------------------------------------------------------ Ablauf und Eingabe

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public final void tick() {
        ticks++;
        if (flashTicks > 0) flashTicks--;
        stationTick();
    }

    protected void flash(String text) {
        flash = text;
        flashTicks = FLASH_TICKS;
    }

    protected void sound(SoundEvent event, float pitch, float volume) {
        if (minecraft != null) minecraft.getSoundManager().play(SimpleSoundInstance.forUI(event, pitch, volume));
    }

    /** Wechselt zur Nachbarstation, wenn es eine gibt. Oeffentlich fuer den Selbsttest. */
    public void switchStation(int direction) {
        StationScreen next = neighbour(direction);
        if (next != null && minecraft != null) minecraft.setScreen(next);
    }

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
        double x = (mouseX - originX) / scale, y = (mouseY - originY) / scale;
        if (!busy() && button == 0) {
            if (in(ui.rect("arrow_left"), x, y, 0)) {
                switchStation(-1);
                return true;
            }
            if (in(ui.rect("arrow_right"), x, y, 0)) {
                switchStation(1);
                return true;
            }
        }
        clickCanvas(x, y, button);
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        updateTransform();
        releaseCanvas((mouseX - originX) / scale, (mouseY - originY) / scale, button);
        return true;
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        updateTransform();
        moveCanvas((mouseX - originX) / scale, (mouseY - originY) / scale);
    }

    // ------------------------------------------------------------------ Zeichnen

    protected static boolean in(int[] r, double x, double y, int pad) {
        return x >= r[0] - pad && x < r[0] + r[2] + pad && y >= r[1] - pad && y < r[1] + r[3] + pad;
    }

    protected int col(String name) {
        return ui.color(name);
    }

    protected static void fill(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + h, color);
    }

    /** Flaeche mit hellem Rand oben/links und dunklem unten/rechts. */
    protected static void panel(GuiGraphics g, int[] r, int face, int light, int dark) {
        panel(g, r[0], r[1], r[2], r[3], face, light, dark);
    }

    protected static void panel(GuiGraphics g, int x, int y, int w, int h, int face, int light, int dark) {
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

        fill(g, 0, 0, ui.canvasW, ui.canvasH, col("wood_0"));
        fill(g, 0, 22, ui.canvasW, 168, col("wood_1"));
        for (int y = 22 + 27; y < 190; y += 28) fill(g, 0, y, ui.canvasW, 1, col("wood_0"));
        drawTopBar(g);
        drawCandle(g);
        drawStation(g);
        int[] hb = ui.rect("hotbar");
        g.blitSprite(HOTBAR, hb[0], hb[1], hb[2], hb[3]);
        drawHand(g);

        g.pose().popPose();
    }

    private void drawTopBar(GuiGraphics g) {
        int face = col("gui_face"), light = col("gui_light"), dark = col("gui_shadow");
        int[] bar = ui.rect("title_bar");
        panel(g, bar, face, light, dark);
        String title = "SCRIBERY & CRAFTS";
        g.drawString(font, title, bar[0] + (bar[2] - font.width(title)) / 2, bar[1] + 4, dark, false);
        String res = resourceText();
        g.drawString(font, res, bar[0] + bar[2] - font.width(res) - 6, bar[1] + 4, dark, false);
        if (bench.fragment != null) {
            String id = fragments.get(fragmentIndex).id() + "  " + fragments.get(fragmentIndex).ref();
            g.drawString(font, id, bar[0] + 6, bar[1] + 4, dark, false);
        }

        for (int dir = -1; dir <= 1; dir += 2) {
            int[] r = ui.rect(dir < 0 ? "arrow_left" : "arrow_right");
            panel(g, r, face, light, dark);
            String s = dir < 0 ? "<" : ">";
            boolean active = hasNeighbour(dir);
            g.drawString(font, s, r[0] + (r[2] - font.width(s)) / 2 + 1, r[1] + 4, active ? dark : col("lead_3"), false);
        }
    }

    /** Ob der Pfeil in diese Richtung etwas tut. */
    protected final boolean hasNeighbour(int direction) {
        return target(direction) != null;
    }

    /** Naechste erreichbare Station in Richtung -1 (links) oder +1 (rechts). Ohne Fragment auf dem Pult gibt es keine. */
    private Stations.Kind target(int direction) {
        if (bench.fragment == null) return null;
        Stations.Kind[] kinds = Stations.Kind.values();
        for (int i = kind().ordinal() + direction; i >= 0 && i < kinds.length; i += direction) {
            if (bench.stations.has(kinds[i])) return kinds[i];
        }
        return null;
    }

    private StationScreen neighbour(int direction) {
        Stations.Kind k = target(direction);
        if (k == null) return null;
        return switch (k) {
            case LECTERN -> new LecternScreen(bench);
            case PRESS -> new PressScreen(bench);
            case DESK -> new TileScreen(bench);
        };
    }

    private void drawCandle(GuiGraphics g) {
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
    }

    /**
     * Reiht Woerter zeilenweise auf, wie fortlaufender Text. Liefert pro Wort {x, y, breite},
     * damit man jedes einzeln zeichnen und anklicken kann.
     */
    protected int[][] flowWords(List<String> words, int x, int y, int width, int lineHeight) {
        int[][] out = new int[words.size()][];
        int cx = 0, cy = 0, space = font.width(" ");
        for (int i = 0; i < words.size(); i++) {
            int w = font.width(words.get(i));
            if (cx > 0 && cx + w > width) {
                cx = 0;
                cy += lineHeight;
            }
            out[i] = new int[] { x + cx, y + cy, w };
            cx += w + space;
        }
        return out;
    }

    /** Wo die Woerter des Fragments auf dem Pult stehen. */
    protected int[][] lecternWordRects(List<String> words) {
        int[] f = ui.rect("fragment");
        return flowWords(words, f[0] + 4, f[1] + 5, f[2] - 8, 10);
    }

    /**
     * Das Pult mit dem gefundenen Fragment, links in jeder Station.
     *
     * @param dimmed Woerter, die blasser gezeichnet werden (z. B. schon uebertragene)
     */
    protected void drawLectern(GuiGraphics g, List<String> words, java.util.function.IntPredicate dimmed) {
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
        int[][] rects = lecternWordRects(words);
        for (int i = 0; i < rects.length; i++) {
            if (rects[i][1] + 9 > f[1] + f[3] - 6) break; // passt nicht mehr aufs Fragment
            g.drawString(font, words.get(i), rects[i][0], rects[i][1],
                    dimmed.test(i) ? col("parchment_0") : col("ink_1"), false);
        }
    }

    /** Hinweiszeile: links der Zaehler, rechts der naechste Schritt oder eine kurze Meldung. */
    protected void drawHintLine(GuiGraphics g, int[] r, String left, String hint) {
        boolean flashing = flashTicks > 0 && !flash.isEmpty();
        String text = flashing ? flash : (busy() ? "" : hint);
        // Der Zaehler weicht, wenn der Text rechts die ganze Zeile braucht.
        if (font.width(left) + font.width(text) + 6 <= r[2]) {
            g.drawString(font, left, r[0] + 1, r[1], col("parchment_1"), false);
        }
        g.drawString(font, text, r[0] + r[2] - font.width(text) - 1, r[1],
                flashing ? col("flame_1") : col("gui_light"), false);
    }
}
