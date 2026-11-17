package de.scribery.client;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

/**
 * Liest ein Buch (Markdown-Teilmenge) und setzt es in Seiten. Unterstuetzt: # ## ### Ueberschriften,
 * Absaetze, "- " Listen, **fett**, *kursiv*, Tabellen mit "|" und "---" als Trenner. Jeder Abschnitt
 * "## Page N - Titel" beginnt eine neue Seite; was davor steht, ist die Titelseite. Ein Abschnitt,
 * der nicht auf eine Seite passt, laeuft auf weitere Seiten.
 */
final class BookText {
    static final int WIDTH = 114, HEIGHT = 126, LINE = 9;
    private static final int BLACK = 0xFF000000, HEAD = 0xFF6A1E14, SUB = 0xFF5A3A22, RULE = 0xFF8F7446;

    /** Etwas, das auf einer Seite steht: Text oder eine Linie. */
    record Drawn(int x, int y, FormattedCharSequence text, int color, float scale, int ruleWidth) {}

    record Page(List<Drawn> items) {}

    private BookText() {}

    static boolean exists(String id) {
        return BookText.class.getResource("/assets/scribery/books/" + id + ".md") != null;
    }

    static String read(String id) {
        try (InputStream in = BookText.class.getResourceAsStream("/assets/scribery/books/" + id + ".md")) {
            if (in == null) return null;
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
        }
    }

    // ------------------------------------------------------------------ Markdown -> Block

    private sealed interface Block permits Heading, Para, Bullet, Table {}

    private record Heading(int level, String text, boolean centered) implements Block {}

    private record Para(String text, boolean centered) implements Block {}

    private record Bullet(String text) implements Block {}

    private record Table(List<List<String>> rows) implements Block {}

    /** Zerlegt den Text in Abschnitte; der erste ist die Titelseite. */
    private static List<List<Block>> sections(String md) {
        List<List<Block>> out = new ArrayList<>();
        List<Block> cur = new ArrayList<>();
        boolean cover = true;
        List<String> para = new ArrayList<>();
        List<List<String>> table = new ArrayList<>();
        Runnable flush = () -> {};
        for (String raw : md.replace("\r", "").split("\n")) {
            String line = raw.strip();
            boolean isTable = line.startsWith("|");
            if (!isTable && !table.isEmpty()) {
                cur.add(new Table(new ArrayList<>(table)));
                table.clear();
            }
            if (!para.isEmpty() && (line.isEmpty() || line.startsWith("#") || line.startsWith("- ") || isTable || line.equals("---"))) {
                cur.add(new Para(String.join(" ", para), cover));
                para.clear();
            }
            if (line.isEmpty()) continue;
            if (line.equals("---")) continue;
            if (line.startsWith("## Page")) {
                if (!cur.isEmpty()) out.add(cur);
                cur = new ArrayList<>();
                cover = false;
                String title = line.substring(2).strip();
                int dash = title.indexOf('—');
                cur.add(new Heading(2, dash >= 0 ? title.substring(dash + 1).strip() : title, false));
            } else if (line.startsWith("### ")) {
                cur.add(new Heading(3, line.substring(4).strip(), cover));
            } else if (line.startsWith("## ")) {
                cur.add(new Heading(2, line.substring(3).strip(), cover));
            } else if (line.startsWith("# ")) {
                cur.add(new Heading(1, line.substring(2).strip(), cover));
            } else if (line.startsWith("- ")) {
                cur.add(new Bullet(line.substring(2).strip()));
            } else if (isTable) {
                if (line.replace("|", "").replace("-", "").replace(" ", "").isEmpty()) continue; // Trennzeile
                List<String> cells = new ArrayList<>();
                for (String c : line.substring(1, line.endsWith("|") ? line.length() - 1 : line.length()).split("\\|")) {
                    cells.add(c.strip());
                }
                table.add(cells);
            } else {
                para.add(line);
            }
        }
        if (!para.isEmpty()) cur.add(new Para(String.join(" ", para), cover));
        if (!table.isEmpty()) cur.add(new Table(table));
        if (!cur.isEmpty()) out.add(cur);
        return out;
    }

    // ------------------------------------------------------------------ Inline: **fett** und *kursiv*

    static MutableComponent inline(String s, boolean baseBold, int color) {
        MutableComponent out = Component.empty();
        StringBuilder sb = new StringBuilder();
        boolean bold = baseBold, italic = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            boolean twoStars = c == '*' && i + 1 < s.length() && s.charAt(i + 1) == '*';
            if (c == '*') {
                if (sb.length() > 0) out.append(styled(sb.toString(), bold, italic, color));
                sb.setLength(0);
                if (twoStars) {
                    bold = !bold;
                    i++;
                } else {
                    italic = !italic;
                }
            } else {
                sb.append(c);
            }
        }
        if (sb.length() > 0) out.append(styled(sb.toString(), bold, italic, color));
        return out;
    }

    private static MutableComponent styled(String text, boolean bold, boolean italic, int color) {
        Style st = Style.EMPTY.withBold(bold).withItalic(italic).withColor(color & 0xFFFFFF);
        return Component.literal(text).withStyle(st);
    }

    // ------------------------------------------------------------------ Seiten setzen

    private static final class Builder {
        final Font font;
        final List<Page> pages = new ArrayList<>();
        List<Drawn> cur = new ArrayList<>();
        int y;

        Builder(Font font) {
            this.font = font;
        }

        void newPage() {
            if (!cur.isEmpty()) pages.add(new Page(cur));
            cur = new ArrayList<>();
            y = 0;
        }

        void need(int height) {
            if (y + height > HEIGHT && !cur.isEmpty()) newPage();
        }

        void line(int x, FormattedCharSequence seq, int color, float scale, int rowHeight) {
            need(rowHeight);
            cur.add(new Drawn(x, y, seq, color, scale, 0));
            y += rowHeight;
        }

        void rule(int x, int width) {
            need(3);
            cur.add(new Drawn(x, y + 1, null, RULE, 1f, width));
            y += 3;
        }

        void gap(int h) {
            if (y > 0) y += h;
        }

        void finish() {
            if (!cur.isEmpty()) pages.add(new Page(cur));
        }
    }

    /** Setzt das Buch in Seiten. Muss auf dem Client laufen, weil es die Schriftbreite braucht. */
    static List<Page> paginate(Font font, String md) {
        Builder b = new Builder(font);
        boolean firstSection = true;
        for (List<Block> section : sections(md)) {
            // Nur die Titelseite steht allein; danach fliesst der Text weiter, sonst bleiben halbe Seiten leer.
            if (firstSection) {
                b.newPage();
                b.y = 26; // Titelseite: etwas Luft oben
            } else {
                b.gap(8);
            }
            for (Block block : section) place(b, block, firstSection);
            if (firstSection) b.newPage();
            firstSection = false;
        }
        b.finish();
        return b.pages;
    }

    private static int centerX(Font font, FormattedCharSequence seq, int width) {
        return Math.max(0, (width - font.width(seq)) / 2);
    }

    private static void place(Builder b, Block block, boolean cover) {
        Font font = b.font;
        if (block instanceof Heading h) {
            int color = h.level() == 3 ? SUB : HEAD;
            List<FormattedCharSequence> lines = font.split(inline(h.text(), true, color), WIDTH);
            b.gap(h.level() == 3 ? 5 : 0);
            b.need(lines.size() * LINE + 2 * LINE); // Ueberschrift nicht allein am Seitenende
            for (FormattedCharSequence l : lines) b.line(h.centered() ? centerX(font, l, WIDTH) : 0, l, color, 1f, LINE);
            if (h.level() == 2 && !h.centered()) b.rule(0, WIDTH);
            b.y += 2;
        } else if (block instanceof Para p) {
            List<FormattedCharSequence> lines = font.split(inline(p.text(), false, BLACK), WIDTH);
            for (FormattedCharSequence l : lines) b.line(p.centered() ? centerX(font, l, WIDTH) : 0, l, BLACK, 1f, LINE);
            b.y += 4;
        } else if (block instanceof Bullet bl) {
            List<FormattedCharSequence> lines = font.split(inline(bl.text(), false, BLACK), WIDTH - 8);
            for (int i = 0; i < lines.size(); i++) {
                if (i == 0) b.cur.add(new Drawn(0, Math.min(b.y, HEIGHT - LINE), Component.literal("•").getVisualOrderText(), BLACK, 1f, 0));
                b.line(8, lines.get(i), BLACK, 1f, LINE);
            }
            b.y += 1;
        } else if (block instanceof Table t) {
            placeTable(b, t);
        }
    }

    private static void placeTable(Builder b, Table t) {
        Font font = b.font;
        int cols = 0;
        for (List<String> r : t.rows()) cols = Math.max(cols, r.size());
        int[] w = new int[cols];
        for (int ri = 0; ri < t.rows().size(); ri++) {
            List<String> r = t.rows().get(ri);
            for (int c = 0; c < r.size(); c++) {
                w[c] = Math.max(w[c], font.width(inline(r.get(c), ri == 0, BLACK)));
            }
        }
        int gap = 6, total = gap * (cols - 1);
        for (int x : w) total += x;
        float scale = total <= WIDTH ? 1f : Math.max(0.6f, (float) WIDTH / total);
        int rowH = Math.max(7, (int) Math.ceil(LINE * scale));
        b.gap(2);
        for (int ri = 0; ri < t.rows().size(); ri++) {
            boolean header = ri == 0;
            // Auf einer neuen Seite die Kopfzeile wiederholen
            if (!header && b.y + rowH > HEIGHT && !b.cur.isEmpty()) {
                b.newPage();
                placeRow(b, t.rows().get(0), w, gap, scale, rowH, true);
                b.rule(0, WIDTH);
            }
            placeRow(b, t.rows().get(ri), w, gap, scale, rowH, header);
            if (header) b.rule(0, WIDTH);
        }
        b.y += 3;
    }

    private static void placeRow(Builder b, List<String> cells, int[] w, int gap, float scale, int rowH, boolean header) {
        b.need(rowH);
        int x = 0;
        for (int c = 0; c < cells.size(); c++) {
            FormattedCharSequence seq = inline(cells.get(c), header, header ? HEAD : BLACK).getVisualOrderText();
            b.cur.add(new Drawn(Math.round(x * scale), b.y, seq, header ? HEAD : BLACK, scale, 0));
            x += w[c] + gap;
        }
        b.y += rowH;
    }
}
