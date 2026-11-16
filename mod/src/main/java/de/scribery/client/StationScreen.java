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
        Workbench old = bench;
        bench = new Workbench(index, old.stations);
        bench.inherit(old);
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

        drawBackdrop(g);
        drawTopBar(g);
        drawCandle(g);
        drawStation(g);
        drawHand(g);

        g.pose().popPose();
    }

    private static int hash(int n) {
        n = (n ^ 61) ^ (n >>> 16);
        n *= 9;
        n ^= n >>> 4;
        n *= 0x27d4eb2d;
        n ^= n >>> 15;
        return n & 0x7fffffff;
    }

    /** Grosse, nicht kachelnde Bretterwand mit Rahmen (tools/gen_ui_textures.py). */
    private void drawBackdrop(GuiGraphics g) {
        tex(g, "backdrop", 0, 0, ui.canvasW, ui.canvasH);
    }

    private static final java.util.Map<String, ResourceLocation> TEXTURES = new java.util.HashMap<>();

    /** Zeichnet eine statische GUI-Textur aus assets/scribery/textures/gui in Originalgroesse. */
    protected static void tex(GuiGraphics g, String name, int x, int y, int w, int h) {
        ResourceLocation rl = TEXTURES.computeIfAbsent(name,
                n -> ResourceLocation.fromNamespaceAndPath(de.scribery.Scribery.MODID, "textures/gui/" + n + ".png"));
        g.blit(rl, x, y, 0f, 0f, w, h, w, h);
    }

    protected static void tex(GuiGraphics g, String name, int[] r) {
        tex(g, name, r[0], r[1], r[2], r[3]);
    }

    private static final java.util.Map<String, ResourceLocation> VANILLA = new java.util.HashMap<>();

    /** Eine Minecraft-Blocktextur (16 x 16), direkt aus dem Spiel geladen, nicht kopiert. */
    protected static ResourceLocation block(String name) {
        return VANILLA.computeIfAbsent("block/" + name,
                n -> ResourceLocation.withDefaultNamespace("textures/" + n + ".png"));
    }

    protected static ResourceLocation item(String name) {
        return VANILLA.computeIfAbsent("item/" + name,
                n -> ResourceLocation.withDefaultNamespace("textures/" + n + ".png"));
    }

    /** Fuellt eine Flaeche mit einer 16 x 16 grossen Textur, nebeneinander gelegt. */
    protected static void tile(GuiGraphics g, ResourceLocation rl, int x, int y, int w, int h) {
        for (int yy = 0; yy < h; yy += 16) {
            for (int xx = 0; xx < w; xx += 16) {
                g.blit(rl, x + xx, y + yy, 0f, 0f, Math.min(16, w - xx), Math.min(16, h - yy), 16, 16);
            }
        }
    }

    /** Rahmen der Staerke t aus einer Blocktextur um eine vertiefte Flaeche. */
    protected static void frame(GuiGraphics g, int x, int y, int w, int h, int t, String material, int interior) {
        fill(g, x, y, w, h, 0xFF0E0E12);
        tile(g, block(material), x + 1, y + 1, w - 2, h - 2);
        fill(g, x + 1, y + 1, w - 2, 1, 0x55FFFFFF);
        fill(g, x + 1, y + 1, 1, h - 2, 0x55FFFFFF);
        fill(g, x + 1, y + h - 2, w - 2, 1, 0x66000000);
        fill(g, x + w - 2, y + 1, 1, h - 2, 0x66000000);
        fill(g, x + t, y + t, w - 2 * t, h - 2 * t, interior);
        fill(g, x + t, y + t, w - 2 * t, 1, 0xAA000000);
        fill(g, x + t, y + t, 1, h - 2 * t, 0xAA000000);
        fill(g, x + t, y + h - t - 1, w - 2 * t, 1, 0x33FFFFFF);
        fill(g, x + w - t - 1, y + t, 1, h - 2 * t, 0x33FFFFFF);
    }

    protected static void frame(GuiGraphics g, int[] r, int t, String material, int interior) {
        frame(g, r[0], r[1], r[2], r[3], t, material, interior);
    }

    protected void nail(GuiGraphics g, int x, int y) {
        fill(g, x, y, 3, 3, 0xFF4A4D54);
        fill(g, x, y, 2, 2, 0xFF9A9EA6);
        fill(g, x + 2, y + 2, 1, 1, 0xFF2B2D31);
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

    /** Naechste erreichbare Station in Richtung -1 (links) oder +1 (rechts). Uebersetzt wird nur mit Fragment. */
    private Stations.Kind target(int direction) {
        Stations.Kind[] kinds = Stations.Kind.values();
        for (int i = kind().ordinal() + direction; i >= 0 && i < kinds.length; i += direction) {
            if (kinds[i] == Stations.Kind.DESK && bench.fragment == null) continue;
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
            case MINT -> null;
        };
    }

    /** Die Kerze aus dem Minecraft-Item, doppelt so gross, mit eigener Flamme. */
    private void drawCandle(GuiGraphics g) {
        int x = 8, y = 32;
        g.blit(item("candle"), x, y, 14, 20, 5f, 6f, 7, 10, 16, 16);
        fill(g, x + 6, y - 3, 2, 3, col("ink_0"));
        int frame = (ticks / 4) % 4;
        int fh = new int[] { 5, 5, 4, 5 }[frame];
        int dx = new int[] { 0, 0, 1, -1 }[frame];
        int top = y - 3 - fh;
        fill(g, x + 5 + dx, top, 4, fh, col("flame_2"));
        fill(g, x + 6 + dx, top + 1, 2, fh - 1, col("flame_1"));
        fill(g, x + 6, y - 5, 2, 2, col("flame_0"));
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

    /** Papier und Woerter, an die Textmenge angepasst: {papier x, y, w, h} und je Wort {x, y, breite}. */
    protected record Fit(int[] paper, int[][] words) {}

    /**
     * Setzt die Woerter in die Flaeche und macht das Papier nicht groesser als der Text.
     * Das Papier sitzt mittig in der Flaeche.
     */
    protected Fit fit(List<String> words, int[] area, int lineHeight, int pad) {
        int[][] at0 = flowWords(words, 0, 0, area[2] - 2 * pad, lineHeight);
        int bw = 0, bh = 0;
        for (int[] r : at0) {
            bw = Math.max(bw, r[0] + r[2]);
            bh = Math.max(bh, r[1] + 9);
        }
        int pw = Math.min(area[2], Math.max(24, bw + 2 * pad));
        int ph = Math.min(area[3], Math.max(16, bh + 2 * pad));
        int px = area[0] + (area[2] - pw) / 2, py = area[1] + (area[3] - ph) / 2;
        int[][] out = new int[at0.length][];
        for (int i = 0; i < at0.length; i++) out[i] = new int[] { px + pad + at0[i][0], py + pad + at0[i][1], at0[i][2] };
        return new Fit(new int[] { px, py, pw, ph }, out);
    }

    /** Wo die Woerter des Fragments auf dem Pult stehen. */
    protected int[][] lecternWordRects(List<String> words) {
        return fit(words, ui.rect("fragment"), 10, 5).words();
    }

    /** Ein sauberes kleines Lesepult aus glattem Holz: Lesebrett mit Ablageleiste, ohne Naegel. */
    protected void drawLecternBoard(GuiGraphics g, int[] r) {
        int x = r[0], y = r[1], w = r[2], h = r[3];
        int ledge = 8;
        fill(g, x + 2, y, w - 4, h, col("wood_0"));
        fill(g, x, y + 2, w, h - 4, col("wood_0"));
        fill(g, x + 2, y + 1, w - 4, h - ledge - 2, col("wood_3"));
        fill(g, x + 1, y + 2, w - 2, h - ledge - 3, col("wood_3"));
        fill(g, x + 2, y + 1, w - 4, 1, col("wood_4"));
        fill(g, x + 1, y + 2, 1, h - ledge - 3, col("wood_4"));
        fill(g, x + w - 2, y + 2, 1, h - ledge - 3, col("wood_2"));
        fill(g, x + 1, y + h - ledge - 1, w - 2, 1, col("wood_1"));
        fill(g, x + 1, y + h - ledge, w - 2, ledge - 3, col("wood_2"));
        fill(g, x + 2, y + h - ledge, w - 4, 1, col("wood_3"));
        fill(g, x + 2, y + h - 3, w - 4, 2, col("wood_1"));
    }

    /**
     * Das Pult mit dem gefundenen Fragment, links in jeder Station. Das Papier ist nur so gross wie sein Text.
     *
     * @param dimmed Woerter, die blasser gezeichnet werden (z. B. schon uebertragene)
     */
    protected void drawLectern(GuiGraphics g, List<String> words, java.util.function.IntPredicate dimmed) {
        drawLecternBoard(g, ui.rect("lectern"));
        int[] f = ui.rect("fragment");
        Fit fit = fit(words, f, 10, 5);
        RaggedPaper.draw(g, ui, fit.paper()[0], fit.paper()[1], fit.paper()[2], fit.paper()[3], 2);
        int[][] rects = fit.words();
        for (int i = 0; i < rects.length; i++) {
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
            g.drawString(font, left, r[0] + 1, r[1], col("parchment_3"), true);
        }
        g.drawString(font, text, r[0] + r[2] - font.width(text) - 1, r[1],
                flashing ? col("flame_1") : col("gui_light"), true);
    }
}
