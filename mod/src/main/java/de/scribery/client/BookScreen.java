package de.scribery.client;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.scribery.core.BookMarkup;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

/**
 * Leseansicht fuer ein Buch im Format eines Handbuchs (zwei breite Seiten in einem Rahmen) mit den Farben des Minecraft-Buchs
 * (Pergamentseiten, Lederrand, rote Heftung): Seitentitel zentriert
 * und unterstrichen, unten in der Mitte die Seitenzahl, unten in den Ecken geschwungene Blaetter-Pfeile. In den oberen Ecken
 * zwei kleine Symbole: links Zurueck (in der Reihenfolge der Spruenge, auch ueber Buecher), rechts das Inhaltsverzeichnis
 * (Anker {@code contents}).
 *
 * <p>Querverweise im Text ({@code [Text](#anker)}, {@code [Text](book:id#anker)}) sind anklickbar: Ein Klick springt zur Seite
 * der Ueberschrift mit diesem Anker, auch in ein anderes Buch.
 */
final class BookScreen extends Screen {
    // Aufbau in GUI-Einheiten
    private static final int TEXT_W = BookText.WIDTH, TEXT_H = BookText.HEIGHT;
    private static final int MARGIN_X = 14, FRAME = 4, SPINE = 8, PAD_TOP = 10, FOOT = 24;
    private static final int PW = TEXT_W + 2 * MARGIN_X;
    private static final int BOOK_W = 2 * PW + SPINE + 2 * FRAME;
    private static final int BOOK_H = 2 * FRAME + PAD_TOP + TEXT_H + FOOT;

    // Farben
    // Farben wie im Minecraft-Buch: Lederrand in Rotbraun, Pergamentseiten, rote Heftung
    private static final int OUTLINE = 0xFF2E1007, METAL = 0xFF783623, METAL_LIGHT = 0xFF9A4A32, METAL_DARK = 0xFF4C1A0B, BRACKET = 0xFF652816;
    private static final int PAGE = 0xFFFAF0DE, PAGE_SHADE = 0xFFEBDDC0, SPINE_COLOR = 0xFFC9B48E, NUMBER = 0xFF8F7446, STITCH = 0xFFC3251D;
    private static final int ICON = 0xFF5A3A22, ICON_OFF = 0xFFBBA98A, ICON_HOVER = 0xFF2F86E0, NOTE = 0xFF8F1D14;
    /** Die Symbole in den oberen Ecken liegen ueber dem Rahmen auf dem dunklen Hintergrund und sind deshalb hell. */
    private static final int CORNER = 0xFFE6D3A8, CORNER_OFF = 0xFF7D6A4E, CORNER_HOVER = 0xFF7FB8FF;

    private record Loaded(List<BookText.Page> pages, Map<String, Integer> anchors) {}

    /** Ein Ort, zu dem "Back" zurueckkehrt. */
    private record Place(String book, int page) {}

    private record Link(BookText.Drawn line, String target) {}

    private String bookId;
    private final String firstMarkdown;
    private final Map<String, Loaded> loaded = new HashMap<>();
    private List<BookText.Page> pages = List.of();
    private Map<String, Integer> anchors = Map.of();
    private final Deque<Place> history = new ArrayDeque<>();
    private int page;
    private String note = "";
    private int noteTicks;

    BookScreen(String bookId, String markdown) {
        super(Component.literal("Book"));
        this.bookId = bookId;
        this.firstMarkdown = markdown;
    }

    // ------------------------------------------------------------------ Geometrie

    private int bookX() {
        return (width - BOOK_W) / 2;
    }

    private int bookY() {
        return Math.max(16, (height - BOOK_H) / 2);
    }

    private int leftPageX() {
        return bookX() + FRAME;
    }

    private int rightPageX() {
        return leftPageX() + PW + SPINE;
    }

    private int textTop() {
        return bookY() + FRAME + PAD_TOP;
    }

    private int footY() {
        return textTop() + TEXT_H + 8;
    }

    private boolean canForward() {
        return page + 2 < pages.size();
    }

    private boolean canBack() {
        return page > 0;
    }

    @Override
    protected void init() {
        if (pages.isEmpty()) {
            Loaded l = paginated(bookId, firstMarkdown);
            pages = l.pages();
            anchors = l.anchors();
        }
    }

    private Loaded paginated(String id, String md) {
        return loaded.computeIfAbsent(id, k -> {
            var p = BookText.paginate(font, md);
            return new Loaded(p, BookText.anchors(p));
        });
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        if (noteTicks > 0) noteTicks--;
    }

    // ------------------------------------------------------------------ Blaettern

    /** Blaettert um d Doppelseiten. {@code page} ist immer die Nummer der linken Seite. */
    private void turn(int d) {
        goTo(page + 2 * d);
    }

    // ------------------------------------------------------------------ Bedienelemente (Rechtecke fuer Klick und Hover)

    private int[] rectBackIcon() {
        return new int[] { bookX() + 4, bookY() - 12, 15, 10 };
    }

    private int[] rectContentsIcon() {
        return new int[] { bookX() + BOOK_W - 19, bookY() - 12, 15, 10 };
    }

    private int[] rectPrevArrow() {
        return new int[] { leftPageX() + MARGIN_X - 2, footY() - 1, 16, 11 };
    }

    private int[] rectNextArrow() {
        return new int[] { rightPageX() + PW - MARGIN_X - 14, footY() - 1, 16, 11 };
    }

    private static boolean inside(int[] r, double mx, double my) {
        return mx >= r[0] && mx < r[0] + r[2] && my >= r[1] && my < r[1] + r[3];
    }

    private boolean hasContents() {
        return anchors.containsKey("contents");
    }

    // ------------------------------------------------------------------ Querverweise

    /** Der Link an einer Stelle einer Seite, oder null. {@code x0} ist der linke Textrand dieser Seite. */
    private Link linkLine(int index, int x0, double mx, double my) {
        if (index >= pages.size()) return null;
        for (BookText.Drawn d : pages.get(index).items()) {
            if (d.text() == null || d.hover() == null) continue;
            float scale = d.scale();
            double lx = (mx - (x0 + d.x())) / scale, ly = (my - (textTop() + d.y())) / scale;
            if (ly < 0 || ly >= BookText.LINE || lx < 0) continue;
            Style st = font.getSplitter().componentStyleAtWidth(d.text(), (int) lx);
            if (st != null && st.getClickEvent() != null && st.getClickEvent().getAction() == ClickEvent.Action.CHANGE_PAGE) {
                return new Link(d, st.getClickEvent().getValue());
            }
        }
        return null;
    }

    private Link linkAt(double mx, double my) {
        Link l = linkLine(page, leftPageX() + MARGIN_X, mx, my);
        return l != null ? l : linkLine(page + 1, rightPageX() + MARGIN_X, mx, my);
    }

    /** Folgt einem Link. Ein Ziel, das es nicht gibt, bleibt ohne Wirkung und zeigt eine kurze Meldung. */
    private void follow(String raw) {
        BookMarkup.Target t = BookMarkup.target(raw);
        if (t == null) return;
        String id = t.book() == null ? bookId : t.book();
        if (!id.equals(bookId)) {
            String md = BookText.read(id);
            if (md == null) {
                say("That book is still blank");
                return;
            }
            history.push(new Place(bookId, page));
            Loaded l = paginated(id, md);
            bookId = id;
            pages = l.pages();
            anchors = l.anchors();
        } else {
            history.push(new Place(bookId, page));
        }
        Integer target = t.anchor() == null ? Integer.valueOf(0) : anchors.get(t.anchor());
        if (target == null) {
            say("That page is missing");
            target = 0;
        }
        goTo(target);
    }

    /** Geht zum letzten Ort vor dem letzten Sprung zurueck, auch in ein anderes Buch. */
    private void goBack() {
        Place p = history.poll();
        if (p == null) return;
        if (!p.book().equals(bookId)) {
            Loaded l = loaded.get(p.book());
            if (l == null) return;
            bookId = p.book();
            pages = l.pages();
            anchors = l.anchors();
        }
        goTo(p.page());
    }

    private void goContents() {
        Integer c = anchors.get("contents");
        if (c == null || c / 2 * 2 == page) return;
        history.push(new Place(bookId, page));
        goTo(c);
    }

    private void say(String text) {
        note = text;
        noteTicks = 60;
    }

    // ------------------------------------------------------------------ Eingabe

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            Link link = linkAt(mx, my);
            if (link != null) {
                follow(link.target());
                return true;
            }
            if (inside(rectBackIcon(), mx, my)) {
                goBack();
                return true;
            }
            if (inside(rectContentsIcon(), mx, my)) {
                goContents();
                return true;
            }
            if (canBack() && inside(rectPrevArrow(), mx, my)) {
                turn(-1);
                return true;
            }
            if (canForward() && inside(rectNextArrow(), mx, my)) {
                turn(1);
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (minecraft != null && minecraft.options.keyInventory.matches(keyCode, scanCode)) {
            onClose();
            return true;
        }
        switch (keyCode) {
            case org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT, org.lwjgl.glfw.GLFW.GLFW_KEY_PAGE_DOWN, org.lwjgl.glfw.GLFW.GLFW_KEY_D -> {
                turn(1);
                return true;
            }
            case org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT, org.lwjgl.glfw.GLFW.GLFW_KEY_PAGE_UP, org.lwjgl.glfw.GLFW.GLFW_KEY_A -> {
                turn(-1);
                return true;
            }
            case org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE -> {
                goBack();
                return true;
            }
            case org.lwjgl.glfw.GLFW.GLFW_KEY_C -> {
                goContents();
                return true;
            }
            default -> { }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        turn(scrollY < 0 ? 1 : -1);
        return true;
    }

    // ------------------------------------------------------------------ Zeichnen

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        drawBook(g);
        Link hover = linkAt(mouseX, mouseY);
        drawPage(g, page, leftPageX() + MARGIN_X, hover);
        drawPage(g, page + 1, rightPageX() + MARGIN_X, hover);
        drawFoot(g, mouseX, mouseY);
        drawCornerIcons(g, mouseX, mouseY);
        if (noteTicks > 0) g.drawCenteredString(font, note, width / 2, bookY() + BOOK_H + 6, NOTE);
    }

    /** Rahmen, Seiten, Falz und Eckbeschlaege. */
    private void drawBook(GuiGraphics g) {
        int bx = bookX(), by = bookY();
        g.fill(bx - 1, by - 1, bx + BOOK_W + 1, by + BOOK_H + 1, OUTLINE);
        g.fill(bx, by, bx + BOOK_W, by + BOOK_H, METAL);
        g.fill(bx, by, bx + BOOK_W, by + 1, METAL_LIGHT);
        g.fill(bx, by, bx + 1, by + BOOK_H, METAL_LIGHT);
        g.fill(bx, by + BOOK_H - 1, bx + BOOK_W, by + BOOK_H, METAL_DARK);
        g.fill(bx + BOOK_W - 1, by, bx + BOOK_W, by + BOOK_H, METAL_DARK);
        int top = by + FRAME, bottom = by + BOOK_H - FRAME;
        g.fill(leftPageX(), top, leftPageX() + PW, bottom, PAGE);
        g.fill(rightPageX(), top, rightPageX() + PW, bottom, PAGE);
        // Rote Heftung am linken Rand jeder Seite (wie im Minecraft-Buch)
        for (int y = top + 8; y < bottom - 12; y += 12) {
            g.fill(leftPageX() + 3, y, leftPageX() + 6, y + 6, STITCH);
            g.fill(rightPageX() + 3, y, rightPageX() + 6, y + 6, STITCH);
        }
        // Falz in der Mitte, daneben ein dunklerer Streifen auf jeder Seite
        g.fill(leftPageX() + PW, top, rightPageX(), bottom, SPINE_COLOR);
        g.fill(leftPageX() + PW - 3, top, leftPageX() + PW, bottom, PAGE_SHADE);
        g.fill(rightPageX(), top, rightPageX() + 3, bottom, PAGE_SHADE);
        g.fill(leftPageX() + PW + SPINE / 2 - 1, top, leftPageX() + PW + SPINE / 2, bottom, METAL_DARK);
        // Eckbeschlaege wie Winkel an den vier Ecken
        for (int[] c : new int[][] { { bx, by, 1, 1 }, { bx + BOOK_W, by, -1, 1 }, { bx, by + BOOK_H, 1, -1 }, { bx + BOOK_W, by + BOOK_H, -1, -1 } }) {
            int x = c[0], y = c[1], dx = c[2], dy = c[3];
            g.fill(Math.min(x, x + dx * 9), Math.min(y, y + dy * 4), Math.max(x, x + dx * 9), Math.max(y, y + dy * 4), BRACKET);
            g.fill(Math.min(x, x + dx * 4), Math.min(y, y + dy * 9), Math.max(x, x + dx * 4), Math.max(y, y + dy * 9), BRACKET);
        }
    }

    /** Seitenzahlen in der Mitte und die Blaetter-Pfeile unten. */
    private void drawFoot(GuiGraphics g, int mx, int my) {
        String left = Integer.toString(page + 1), right = Integer.toString(page + 2);
        if (page < pages.size()) g.drawString(font, left, leftPageX() + PW / 2 - font.width(left) / 2, footY() + 1, NUMBER, false);
        if (page + 1 < pages.size()) g.drawString(font, right, rightPageX() + PW / 2 - font.width(right) / 2, footY() + 1, NUMBER, false);
        if (canBack()) arrow(g, rectPrevArrow(), false, inside(rectPrevArrow(), mx, my));
        if (canForward()) arrow(g, rectNextArrow(), true, inside(rectNextArrow(), mx, my));
    }

    /** Ein geschwungener Pfeil (aus Rechtecken gesetzt): Schaft mit Haken und Spitze. */
    private static void arrow(GuiGraphics g, int[] r, boolean right, boolean hover) {
        int c = hover ? ICON_HOVER : ICON;
        int x = r[0] + 1, y = r[1] + 1;
        if (right) {
            g.fill(x, y + 5, x + 3, y + 7, c);              // Haken unten links
            g.fill(x + 1, y + 3, x + 10, y + 5, c);          // Schaft
            for (int i = 0; i < 4; i++) g.fill(x + 9 + i, y + 1 + i, x + 10 + i, y + 8 - i, c);   // Spitze nach rechts
        } else {
            g.fill(x + 11, y + 5, x + 14, y + 7, c);
            g.fill(x + 4, y + 3, x + 13, y + 5, c);
            for (int i = 0; i < 4; i++) g.fill(x + 4 - i, y + 1 + i, x + 5 - i, y + 8 - i, c);
        }
    }

    /** Oben links Zurueck, oben rechts das Inhaltsverzeichnis, jeweils mit Hinweistext. */
    private void drawCornerIcons(GuiGraphics g, int mx, int my) {
        int[] b = rectBackIcon(), c = rectContentsIcon();
        boolean canBackLink = !history.isEmpty(), hoverB = inside(b, mx, my), hoverC = inside(c, mx, my);
        // Zurueck: Pfeil nach links
        int cb = !canBackLink ? CORNER_OFF : hoverB ? CORNER_HOVER : CORNER;
        g.fill(b[0] + 3, b[1] + 3, b[0] + 13, b[1] + 6, cb);                       // Schaft
        for (int i = 0; i < 4; i++) g.fill(b[0] + 3 - i, b[1] + 1 + i, b[0] + 4 - i, b[1] + 8 - i, cb);   // Spitze nach links
        // Inhaltsverzeichnis: drei Zeilen
        int cc = !hasContents() ? CORNER_OFF : hoverC ? CORNER_HOVER : CORNER;
        for (int i = 0; i < 3; i++) g.fill(c[0] + 2, c[1] + 1 + i * 3, c[0] + 13, c[1] + 2 + i * 3, cc);
        if (hoverB && canBackLink) g.renderTooltip(font, Component.literal("Back"), mx, my);
        if (hoverC && hasContents()) g.renderTooltip(font, Component.literal("Contents"), mx, my);
    }

    private void drawPage(GuiGraphics g, int index, int x0, Link hover) {
        if (index >= pages.size()) return;
        int y0 = textTop();
        for (BookText.Drawn d : pages.get(index).items()) {
            if (d.text() == null) {
                g.fill(x0 + d.x(), y0 + d.y(), x0 + d.x() + d.ruleWidth(), y0 + d.y() + 1, d.color());
                continue;
            }
            // Die Zeile unter der Maus bekommt die Fassung mit hervorgehobenem Link
            var text = hover != null && hover.line() == d && d.hover() != null ? d.hover() : d.text();
            if (d.scale() == 1f) {
                g.drawString(font, text, x0 + d.x(), y0 + d.y(), d.color(), false);
            } else {
                g.pose().pushPose();
                g.pose().translate(x0 + d.x(), y0 + d.y(), 0);
                g.pose().scale(d.scale(), d.scale(), 1f);
                g.drawString(font, text, 0, 0, d.color(), false);
                g.pose().popPose();
            }
        }
    }

    // ------------------------------------------------------------------ nur fuer den Selbsttest

    void goTo(int p) {
        page = Math.max(0, Math.min(Math.max(0, pages.size() - 1), p)) / 2 * 2;
    }

    /** Fuer den Selbsttest: Nummer der ersten Seite, deren Text den Suchbegriff enthaelt, sonst 0. */
    int findPage(String needle) {
        for (int i = 0; i < pages.size(); i++) {
            for (BookText.Drawn d : pages.get(i).items()) {
                if (d.text() == null) continue;
                StringBuilder sb = new StringBuilder();
                d.text().accept((idx, st, cp) -> { sb.appendCodePoint(cp); return true; });
                if (sb.toString().contains(needle)) return i;
            }
        }
        return 0;
    }

    int pageCount() {
        return pages.size();
    }

    /** Fuer den Selbsttest: einen Link so folgen, als haette man ihn angeklickt. */
    void debugFollow(String target) {
        follow(target);
    }

    int debugPage() {
        return page;
    }

    String debugBook() {
        return bookId;
    }

    int debugHistory() {
        return history.size();
    }
}
