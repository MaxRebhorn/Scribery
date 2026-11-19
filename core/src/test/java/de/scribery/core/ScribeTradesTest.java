package de.scribery.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.Test;

import de.scribery.core.ScribeTrades.Mark;
import de.scribery.core.ScribeTrades.Offer;

class ScribeTradesTest {
    @Test
    void everyLevelHasOffers() {
        for (int level = 1; level <= 5; level++) assertFalse(ScribeTrades.forLevel(level).isEmpty(), "Stufe " + level);
    }

    @Test
    void neverSellsFinishedSheetsOrFragments() {
        for (Offer o : ScribeTrades.ALL) assertFalse(ScribeTrades.NEVER_SOLD.contains(o.get()), o.toString());
    }

    @Test
    void fragmentsAreOnlyTakenAsMarkedDuplicates() {
        for (Offer o : ScribeTrades.ALL) {
            if (o.give().equals(ScribeTrades.FRAGMENT)) assertTrue(o.mark().isDuplicate(), "nimmt ein unmarkiertes Fragment: " + o);
            else assertEquals(Mark.NONE, o.mark(), o.toString());
        }
    }

    @Test
    void numbersAreSane() {
        for (Offer o : ScribeTrades.ALL) {
            assertTrue(o.giveCount() >= 1 && o.getCount() >= 1 && o.maxUses() >= 1 && o.xp() >= 1, o.toString());
            assertTrue(o.level() >= 1 && o.level() <= 5, o.toString());
        }
    }

    @Test
    void allSevenBooksAreSoldExactlyOnce() {
        List<String> books = List.of("greek_alphabet_primer", "greek_grammar_book", "greek_lexicon_a_c", "greek_lexicon_d_f",
                "greek_forms_grammar_lexicon", "thematic_greek_lexicon", "complete_greek_lexicon");
        for (String b : books) {
            assertEquals(1, ScribeTrades.ALL.stream().filter(o -> o.get().equals("scribery:" + b)).count(), b);
        }
    }

    @Test
    void booksFollowTheLearningOrder() {
        assertEquals(1, level("greek_alphabet_primer"));
        assertTrue(level("greek_grammar_book") < level("greek_lexicon_a_c"));
        assertTrue(level("greek_lexicon_d_f") < level("greek_forms_grammar_lexicon"));
        assertEquals(5, level("complete_greek_lexicon"));
    }

    private static int level(String book) {
        return ScribeTrades.ALL.stream().filter(o -> o.get().equals("scribery:" + book)).findFirst().orElseThrow().level();
    }

    @Test
    void oneDailyCappedPouchPerFindType() {
        for (String find : ScribeTrades.FINDS) {
            List<Offer> o = ScribeTrades.ALL.stream().filter(x -> x.get().equals(ScribeTrades.pouch(find))).toList();
            assertEquals(1, o.size(), find);
            assertEquals(2, o.get(0).maxUses(), find);
            assertEquals(3, o.get(0).giveCount(), find);
            assertEquals(Mark.ANY_DUPLICATE, o.get(0).mark(), find);
        }
    }

    /** Verkaufen darf nie lohnender sein als Kaufen: eine Dublette bringt weniger als ein Drittel des billigsten Lexikons. */
    @Test
    void sellingDuplicatesPaysLessThanBooksCost() {
        int cheapestLexicon = Math.min(ScribePrices.LEXICON_A_C, ScribePrices.LEXICON_D_F);
        for (Offer o : ScribeTrades.ALL) {
            if (o.mark().isDuplicate() && o.get().equals(ScribeTrades.EMERALD)) {
                assertTrue(o.getCount() <= 3, o.toString());
                assertTrue(o.getCount() * 3 < cheapestLexicon, o.toString());
            }
        }
        assertTrue(ScribePrices.DUPLICATE_EASY < ScribePrices.DUPLICATE_MEDIUM && ScribePrices.DUPLICATE_MEDIUM < ScribePrices.DUPLICATE_HARD);
    }

    @Test
    void scrapsNeverPayMoreThanOneEmeraldPerEightEvenWithAllLimits() {
        int perDay = 0;
        for (Offer o : ScribeTrades.ALL) if (o.give().equals("scribery:papyrus_scrap")) perDay += o.getCount() * o.maxUses();
        assertTrue(perDay <= 16, "mehr als 16 Smaragde am Tag fuer Fetzen: " + perDay);
    }

    @Test
    void luckProtectionPrefersMissingManuscripts() {
        List<String> pool = new ArrayList<>();
        for (int i = 0; i < 100; i++) pool.add("P" + i);
        Set<String> owned = new HashSet<>(pool.subList(0, 50));
        Random rnd = new Random(1);
        int missing = 0, n = 20000;
        for (int i = 0; i < n; i++) if (!owned.contains(FindLuck.pick(pool, owned::contains, FindLuck.MISSING_CHANCE, rnd))) missing++;
        double share = missing / (double) n;       // erwartet: 0,7 + 0,3 * 0,5 = 0,85
        assertEquals(0.85, share, 0.02);
    }

    @Test
    void luckProtectionStillWorksWhenEverythingIsOwned() {
        List<String> pool = List.of("P1", "P2");
        assertNotNull(FindLuck.pick(pool, id -> true, 1.0, new Random(2)));
        assertNull(FindLuck.pick(List.of(), id -> false, 1.0, new Random(2)));
    }
}
