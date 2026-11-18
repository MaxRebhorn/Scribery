package de.scribery.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Die Querverweise in den Buchtexten (docs/BOOK-FORMAT.md), ohne Minecraft: Links der Form {@code [Text](Ziel)} im Text und
 * feste Anker an Ueberschriften ({@code ### 1. Alpha {#alpha}}).
 *
 * <p>Ziele: {@code #anker} (Seite im selben Buch), {@code book:id} (erste Seite eines anderen Buchs) und
 * {@code book:id#anker}. Ohne festen Anker ist der Anker einer Ueberschrift ihr {@link #slug(String) Slug}.
 */
public final class BookMarkup {
    /** Ein Stueck Text: gewoehnlich (target == null) oder ein Link. */
    public record Span(String text, String target) {}

    /** Eine Ueberschrift ohne Ankerzusatz und ihr Anker. */
    public record Heading(String text, String anchor) {}

    /** Ein aufgeloestes Linkziel; book == null heisst das aktuelle Buch, anchor == null die erste Seite. */
    public record Target(String book, String anchor) {}

    private BookMarkup() {}

    /**
     * Teilt einen Text in gewoehnliche Stuecke und Links. Ein Link ist {@code [Text](Ziel)}, der Text ohne {@code [} und
     * {@code ]}, das Ziel ohne Leerzeichen und ohne {@code )}. Alles, was nicht so aussieht, bleibt gewoehnlicher Text.
     */
    public static List<Span> spans(String s) {
        List<Span> out = new ArrayList<>();
        StringBuilder plain = new StringBuilder();
        int i = 0;
        while (i < s.length()) {
            if (s.charAt(i) == '[') {
                int close = s.indexOf("](", i + 1);
                int end = close < 0 ? -1 : s.indexOf(')', close + 2);
                if (close > i + 1 && end > close + 2) {
                    String label = s.substring(i + 1, close), target = s.substring(close + 2, end);
                    if (label.indexOf('[') < 0 && label.indexOf(']') < 0 && target.indexOf(' ') < 0) {
                        if (plain.length() > 0) out.add(new Span(plain.toString(), null));
                        plain.setLength(0);
                        out.add(new Span(label, target));
                        i = end + 1;
                        continue;
                    }
                }
            }
            plain.append(s.charAt(i));
            i++;
        }
        if (plain.length() > 0) out.add(new Span(plain.toString(), null));
        return out;
    }

    /** Ueberschrift mit optionalem festem Anker am Ende ({@code {#id}}); sonst der Slug des Textes. */
    public static Heading heading(String raw) {
        String s = raw.strip();
        int open = s.lastIndexOf("{#");
        if (open >= 0 && s.endsWith("}")) {
            String id = s.substring(open + 2, s.length() - 1).strip();
            if (!id.isEmpty() && id.indexOf(' ') < 0) return new Heading(s.substring(0, open).strip(), id);
        }
        return new Heading(s, slug(s));
    }

    /** Kleinbuchstaben, Buchstaben und Ziffern bleiben (auch griechische), alles andere wird ein Bindestrich, Wiederholungen entfallen. */
    public static String slug(String text) {
        StringBuilder sb = new StringBuilder();
        boolean dash = false;
        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
            i += Character.charCount(cp);
            if (Character.isLetterOrDigit(cp)) {
                if (dash && sb.length() > 0) sb.append('-');
                dash = false;
                sb.appendCodePoint(Character.toLowerCase(cp));
            } else {
                dash = true;
            }
        }
        return sb.toString().toLowerCase(Locale.ROOT);
    }

    /** Loest ein Linkziel auf; {@code null}, wenn es keins ist. */
    public static Target target(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String s = raw.strip();
        if (s.startsWith("#")) return s.length() > 1 ? new Target(null, s.substring(1)) : null;
        if (s.startsWith("book:")) {
            String rest = s.substring(5);
            int hash = rest.indexOf('#');
            String book = hash < 0 ? rest : rest.substring(0, hash);
            String anchor = hash < 0 || hash == rest.length() - 1 ? null : rest.substring(hash + 1);
            return book.isEmpty() ? null : new Target(book, anchor);
        }
        return null;
    }
}
