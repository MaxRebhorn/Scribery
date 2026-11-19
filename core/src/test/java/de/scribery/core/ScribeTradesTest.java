package de.scribery.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.Test;

import de.scribery.core.ScribeTrades.Offer;

class ScribeTradesTest {
    @Test
    void everyLevelHasOffers() {
        for (int level = 1; level <= 5; level++) assertFalse(ScribeTrades.forLevel(level).isEmpty(), "Stufe " + level);
    }

    @Test
    void neverSellsFinishedSheetsOrFragments() {
        for (Offer o : ScribeTrades.everything()) assertFalse(ScribeTrades.NEVER_SOLD.contains(o.get()), o.toString());
    }

    @Test
    void doesNotSellInkAndOffersPaperEitherWay() {
        for (Offer o : ScribeTrades.everything()) assertNotEquals("minecraft:ink_sac", o.get(), o.toString());
        assertEquals("minecraft:emerald", ScribeTrades.PAPER_CHOICE.get(0).get());   // kauft
        assertEquals("minecraft:paper", ScribeTrades.PAPER_CHOICE.get(1).get());     // verkauft
        // Verkaufen darf nicht billiger sein als das, was er beim Kaufen zahlt
        assertTrue(ScribePrices.PAPER_SOLD_PER_EMERALD < ScribePrices.PAPER_BOUGHT_PER_EMERALD);
    }

    @Test
    void primerIsTheFirstOffer() {
        assertEquals("scribery:greek_alphabet_primer", ScribeTrades.forLevel(1).get(0).get());
    }

    @Test
    void fragmentsAreOnlyTakenAsPayment() {
        for (Offer o : ScribeTrades.everything()) {
            if (o.give().equals(ScribeTrades.FRAGMENT)) assertTrue(o.get().equals(ScribeTrades.EMERALD) || o.get().startsWith("scribery:find_pouch_"), o.toString());
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
    void allWrittenBooksAreSoldExactlyOnce() {
        List<String> books = List.of("greek_alphabet_primer", "greek_grammar_book", "greek_lexicon_a_c", "greek_lexicon_d_f",
                "thematic_greek_lexicon", "complete_greek_lexicon");
        for (String b : books) {
            assertEquals(1, ScribeTrades.ALL.stream().filter(o -> o.get().equals("scribery:" + b)).count(), b);
        }
    }

    @Test
    void booksFollowTheLearningOrder() {
        assertEquals(1, level("greek_alphabet_primer"));
        assertEquals(1, level("greek_grammar_book"));                      // Grammatik gleich am Anfang
        assertEquals(3, level("greek_lexicon_a_c"));
        assertEquals(3, level("greek_lexicon_d_f"));                      // mittlere Stufe: A-C und D-F
        assertTrue(level("thematic_greek_lexicon") > level("greek_lexicon_a_c"));
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
        }
    }

    /** Verkaufen darf nie lohnender sein als Kaufen: ein loses Fragment bringt weniger als ein Drittel des billigsten Lexikons. */
    @Test
    void sellingFragmentsPaysLessThanBooksCost() {
        int cheapestLexicon = Math.min(ScribePrices.LEXICON_A_C, ScribePrices.LEXICON_D_F);
        for (Offer o : ScribeTrades.ALL) {
            if (o.give().equals(ScribeTrades.FRAGMENT) && o.get().equals(ScribeTrades.EMERALD)) {
                assertTrue(o.getCount() * 3 < cheapestLexicon, o.toString());
            }
        }
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
