package de.scribery.core;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

/** Prueft die Querverweise der Buecher: Parser und, dass jeder Link in den echten Buechern ein Ziel hat. */
class BookMarkupTest {
    @Test
    void splitsPlainTextAndLinks() {
        var s = BookMarkup.spans("See [Alpha](#alpha) and [the primer](book:greek_alphabet_primer#contents), then go on.");
        assertEquals(List.of(
                new BookMarkup.Span("See ", null), new BookMarkup.Span("Alpha", "#alpha"), new BookMarkup.Span(" and ", null),
                new BookMarkup.Span("the primer", "book:greek_alphabet_primer#contents"), new BookMarkup.Span(", then go on.", null)), s);
    }

    @Test
    void leavesMalformedLinksAsText() {
        assertEquals(List.of(new BookMarkup.Span("a [b] c", null)), BookMarkup.spans("a [b] c"));
        assertEquals(List.of(new BookMarkup.Span("a [b](c d) e", null)), BookMarkup.spans("a [b](c d) e"));   // Leerzeichen im Ziel
        assertEquals(List.of(new BookMarkup.Span("a [b](", null)), BookMarkup.spans("a [b]("));
        assertEquals(List.of(), BookMarkup.spans(""));
    }

    @Test
    void headingsGetExplicitOrGeneratedAnchors() {
        assertEquals(new BookMarkup.Heading("1. Alpha — Α α", "alpha"), BookMarkup.heading("1. Alpha — Α α {#alpha}"));
        assertEquals(new BookMarkup.Heading("Meet the Alphabet", "meet-the-alphabet"), BookMarkup.heading("Meet the Alphabet"));
        assertEquals("what-s-next", BookMarkup.slug("What's next?"));
        assertEquals("σ-σ-ς", BookMarkup.slug("Σ σ ς"));
    }

    @Test
    void resolvesTargets() {
        assertEquals(new BookMarkup.Target(null, "alpha"), BookMarkup.target("#alpha"));
        assertEquals(new BookMarkup.Target("greek_grammar_book", null), BookMarkup.target("book:greek_grammar_book"));
        assertEquals(new BookMarkup.Target("greek_grammar_book", "cases"), BookMarkup.target("book:greek_grammar_book#cases"));
        assertNull(BookMarkup.target("#"));
        assertNull(BookMarkup.target("book:"));
        assertNull(BookMarkup.target("https://example.com"));
        assertNull(BookMarkup.target(null));
    }

    private static final Path BOOKS = Path.of("..", "mod", "src", "main", "resources", "assets", "scribery", "books");

    /** Alle Anker eines Buchs: feste ({#id}) und erzeugte (Slug der Ueberschrift, bei "## Page N — Titel" des Titels). */
    private static Set<String> anchors(String md) {
        Set<String> out = new HashSet<>();
        for (String line : md.replace("\r", "").split("\n")) {
            String l = line.strip();
            if (!l.startsWith("#")) continue;
            String text = l.replaceFirst("^#+\\s*", "");
            if (l.startsWith("## Page")) {
                int dash = text.indexOf('—');
                if (dash >= 0) text = text.substring(dash + 1).strip();
            }
            out.add(BookMarkup.heading(text).anchor());
        }
        return out;
    }

    /** Jeder Link in einem echten Buch fuehrt zu einem Buch und einem Anker, die es gibt; Anker sind innerhalb eines Buchs eindeutig. */
    @Test
    void everyLinkInTheBooksHasATarget() throws IOException {
        Map<String, String> texts = new HashMap<>();
        try (var stream = Files.list(BOOKS)) {
            for (Path p : stream.filter(f -> f.toString().endsWith(".md")).toList()) {
                texts.put(p.getFileName().toString().replace(".md", ""), Files.readString(p, StandardCharsets.UTF_8));
            }
        }
        assertFalse(texts.isEmpty());
        Map<String, Set<String>> anchorsByBook = new HashMap<>();
        for (var e : texts.entrySet()) anchorsByBook.put(e.getKey(), anchors(e.getValue()));
        Pattern link = Pattern.compile("\\[[^\\[\\]]+\\]\\(([^)\\s]+)\\)");
        int links = 0;
        for (var e : texts.entrySet()) {
            Matcher m = link.matcher(e.getValue());
            while (m.find()) {
                links++;
                var t = BookMarkup.target(m.group(1));
                assertNotNull(t, e.getKey() + ": kein gueltiges Ziel " + m.group(1));
                String book = t.book() == null ? e.getKey() : t.book();
                assertTrue(texts.containsKey(book), e.getKey() + ": Buch fehlt: " + book);
                if (t.anchor() != null) {
                    assertTrue(anchorsByBook.get(book).contains(t.anchor()), e.getKey() + ": Anker fehlt: " + m.group(1));
                }
            }
        }
        assertTrue(links > 0, "keine Links in den Buechern");
        // feste Anker doppelt vergeben?
        for (var e : texts.entrySet()) {
            Set<String> seen = new HashSet<>();
            Matcher m = Pattern.compile("\\{#([^}\\s]+)}").matcher(e.getValue());
            while (m.find()) assertTrue(seen.add(m.group(1)), e.getKey() + ": Anker doppelt: " + m.group(1));
        }
    }
}
