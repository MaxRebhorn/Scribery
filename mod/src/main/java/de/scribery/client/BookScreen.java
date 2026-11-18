package de.scribery.client;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.scribery.core.BookMarkup;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.PageButton;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;

/**
 * Leseansicht fuer ein Buch, mit der Textur und den Seitenknoepfen des Minecraft-Buchs. Querverweise im Text ({@code [Text](#anker)},
 * {@code [Text](book:id#anker)}) sind anklickbar: Ein Klick springt zur Seite der Ueberschrift mit diesem Anker, auch in ein anderes Buch.
 * Oben links fuehrt "Back" in der Reihenfolge der Spruenge zurueck (auch ueber Buecher hinweg, Rueck-Taste), oben rechts "Contents"
 * zum Anker {@code contents} des Buchs (sonst zur ersten Seite).
 */
final class BookScreen extends Screen {
    private static final ResourceLocation BG = ResourceLocation.withDefaultNamespace("textures/gui/book.png");
    private static final int PAGE_W = 192, TEXT_X = 36, TEXT_Y = 32;
    private static final int BLACK = 0xFF000000, BUTTON = 0xFF5A3A22, BUTTON_HOVER = 0xFF2F86E0, NOTE = 0xFF8F1D14;

    private record Loaded(List<BookText.Page> pages, Map<String, Integer> anchors) {}

    /** Ein Ort, zu dem "Back" zurueckkehrt. */
    private record Place(String book, int page) {}

    private String bookId;
    private final String firstMarkdown;
    private final Map<String, Loaded> loaded = new HashMap<>();
    private List<BookText.Page> pages = List.of();
    private Map<String, Integer> anchors = Map.of();
    private final Deque<Place> history = new ArrayDeque<>();
    private int page;
    private PageButton forward, back;
    private String note = "";
    private int noteTicks;

    BookScreen(String bookId, String markdown) {
        super(Component.literal("Book"));
        this.bookId = bookId;
        this.firstMarkdown = markdown;
    }

    /** Linker Rand der linken und der rechten Seite. */
    private int leftX() {
        return (width - 2 * PAGE_W) / 2;
    }

    private int rightX() {
        return leftX() + PAGE_W;
    }

    @Override
    protected void init() {
        if (pages.isEmpty()) {
            Loaded l = paginated(bookId, firstMarkdown);
            pages = l.pages();
            anchors = l.anchors();
        }
        forward = addRenderableWidget(new PageButton(rightX() + 116, 159, true, b -> turn(1), true));
        back = addRenderableWidget(new PageButton(leftX() + 43, 159, false, b -> turn(-1), true));
        updateButtons();
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

    private void updateButtons() {
        forward.visible = page + 2 < pages.size();
        back.visible = page > 0;
    }

    // ------------------------------------------------------------------ Querverweise

    /** Das Linkziel an einer Stelle einer Seite, oder null. {@code x0} ist der linke Textrand dieser Seite. */
    private Drawn linkLine(int index, int x0, double mx, double my) {
        if (index >= pages.size()) return null;
        for (BookText.Drawn d : pages.get(index).items()) {
            if (d.text() == null || d.hover() == null) continue;
            float scale = d.scale();
            double lx = (mx - (x0 + d.x())) / scale, ly = (my - (TEXT_Y + d.y())) / scale;
            if (ly < 0 || ly >= BookText.LINE || lx < 0) continue;
            Style st = font.getSplitter().componentStyleAtWidth(d.text(), (int) lx);
            if (st != null && st.getClickEvent() != null && st.getClickEvent().getAction() == ClickEvent.Action.CHANGE_PAGE) {
                return new Drawn(d, st.getClickEvent().getValue());
            }
        }
        return null;
    }

    private record Drawn(BookText.Drawn line, String target) {}

    private Drawn linkAt(double mx, double my) {
        Drawn l = linkLine(page, leftX() + TEXT_X, mx, my);
        return l != null ? l : linkLine(page + 1, rightX() + TEXT_X, mx, my);
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

    private boolean overBack(double mx, double my) {
        return !history.isEmpty() && mx >= leftX() + TEXT_X && mx < leftX() + TEXT_X + font.width("< Back") && my >= 14 && my < 24;
    }

    private boolean overContents(double mx, double my) {
        String s = "Contents";
        return anchors.containsKey("contents") && mx >= rightX() + TEXT_X && mx < rightX() + TEXT_X + font.width(s) && my >= 14 && my < 24;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            Drawn link = linkAt(mx, my);
            if (link != null) {
                follow(link.target());
                return true;
            }
            if (overBack(mx, my)) {
                goBack();
                return true;
            }
            if (overContents(mx, my)) {
                goContents();
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
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(g, mouseX, mouseY, partialTick);
        g.blit(BG, leftX(), 2, 0, 0, PAGE_W, PAGE_W);
        g.blit(BG, rightX(), 2, 0, 0, PAGE_W, PAGE_W);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        String indicator = pages.isEmpty() ? "" : (page + 1) + (page + 2 <= pages.size() ? "-" + (page + 2) : "") + " / " + pages.size();
        g.drawString(font, indicator, rightX() + PAGE_W - 44 - font.width(indicator), 18, BLACK, false);
        // Zurueck und Inhaltsverzeichnis
        if (!history.isEmpty()) {
            g.drawString(font, "< Back", leftX() + TEXT_X, 15, overBack(mouseX, mouseY) ? BUTTON_HOVER : BUTTON, false);
        }
        if (anchors.containsKey("contents")) {
            g.drawString(font, "Contents", rightX() + TEXT_X, 15, overContents(mouseX, mouseY) ? BUTTON_HOVER : BUTTON, false);
        }
        Drawn hover = linkAt(mouseX, mouseY);
        drawPage(g, page, leftX() + TEXT_X, hover);
        drawPage(g, page + 1, rightX() + TEXT_X, hover);
        if (noteTicks > 0) g.drawCenteredString(font, note, width / 2, 170, NOTE);
    }

    private void drawPage(GuiGraphics g, int index, int x0, Drawn hover) {
        if (index >= pages.size()) return;
        int y0 = TEXT_Y;
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
        updateButtons();
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
