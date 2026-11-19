package de.scribery.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Die Handelsliste des Scribe Villagers als reine Daten (ohne Minecraft), damit sie sich testen laesst. Der Mod macht
 * daraus die MerchantOffers (ScribeTradeEvents). Die Fundkarte gehoert nicht hierher, weil sie die Welt braucht.
 * Er verkauft nie fertige Reinschriften oder Uebersetzungen und nimmt lose Fragmente (was im Archiv liegt, hat der Spieler abgelegt und kann es dort wieder herausnehmen).
 */
public final class ScribeTrades {
    private ScribeTrades() {}

    /** Der Spieler gibt {@code giveCount} mal {@code give} und bekommt {@code getCount} mal {@code get}. Ids wie "minecraft:emerald". */
    public record Offer(int level, String give, int giveCount, String get, int getCount, int maxUses, int xp) {}

    public static final String EMERALD = "minecraft:emerald";
    public static final String FRAGMENT = "scribery:manuscript_fragment";
    public static final List<String> FINDS = List.of("sand", "mud", "ashes", "ice", "shards");
    /** Dinge, die er nie hergibt: Reinschriften, Uebersetzungen und Fragmente. */
    public static final List<String> NEVER_SOLD = List.of("scribery:printed_sheet", "scribery:translated_sheet", FRAGMENT);

    public static String pouch(String find) {
        return "scribery:find_pouch_" + find;
    }

    private static Offer sell(int level, int emeralds, String item, int count, int uses, int xp) {
        return new Offer(level, EMERALD, emeralds, item, count, uses, xp);
    }

    private static Offer buy(int level, String item, int count, int emeralds, int uses, int xp) {
        return new Offer(level, item, count, EMERALD, emeralds, uses, xp);
    }

    public static final List<Offer> ALL = build();

    /** Stufe 1, nach Glueck: entweder kauft er Papier (Index 0) oder er verkauft welches (Index 1). Der Mod wuerfelt je Dorfbewohner. */
    public static final List<Offer> PAPER_CHOICE = List.of(
            buy(1, "minecraft:paper", ScribePrices.PAPER_BOUGHT_PER_EMERALD, 1, ScribePrices.USES_PAPER, ScribePrices.XP_SMALL),
            sell(1, 1, "minecraft:paper", ScribePrices.PAPER_SOLD_PER_EMERALD, ScribePrices.USES_PAPER, ScribePrices.XP_SMALL));

    /** Alle moeglichen Angebote, auch die Glueckswahl (fuer Tests). */
    public static List<Offer> everything() {
        List<Offer> l = new ArrayList<>(ALL);
        l.addAll(PAPER_CHOICE);
        return l;
    }

    private static List<Offer> build() {
        List<Offer> o = new ArrayList<>();
        // Stufe 1, Novize: das Alphabet-Buch steht gleich oben; Papier kommt je nach Glueck dazu (siehe PAPER_CHOICE)
        o.add(sell(1, ScribePrices.PRIMER, "scribery:greek_alphabet_primer", 1, ScribePrices.USES_BOOK, ScribePrices.XP_SMALL));
        o.add(buy(1, "scribery:papyrus_scrap", ScribePrices.SCRAPS_PER_EMERALD, 1, ScribePrices.USES_SCRAPS, ScribePrices.XP_SMALL));
        o.add(buy(1, "scribery:ostrakon", ScribePrices.OSTRAKA_PER_EMERALD, 1, ScribePrices.USES_OSTRAKA, ScribePrices.XP_SMALL));
        // Stufe 2, Geselle
        o.add(sell(2, ScribePrices.GRAMMAR, "scribery:greek_grammar_book", 1, ScribePrices.USES_BOOK, ScribePrices.XP_MEDIUM));
        for (String find : FINDS) {
            o.add(new Offer(2, FRAGMENT, ScribePrices.FRAGMENTS_PER_POUCH, pouch(find), 1,
                    ScribePrices.USES_POUCH, ScribePrices.XP_MEDIUM));
        }
        // Stufe 3, Fachmann
        o.add(sell(3, ScribePrices.LEXICON_A_C, "scribery:greek_lexicon_a_c", 1, ScribePrices.USES_BOOK, ScribePrices.XP_MEDIUM));
        o.add(sell(3, ScribePrices.LEXICON_D_F, "scribery:greek_lexicon_d_f", 1, ScribePrices.USES_BOOK, ScribePrices.XP_MEDIUM));
        o.add(new Offer(3, FRAGMENT, 1, EMERALD, ScribePrices.FRAGMENT_SELL, ScribePrices.USES_FRAGMENT_SELL, ScribePrices.XP_SMALL));
        // Stufe 4, Experte
        o.add(sell(4, ScribePrices.FORMS_GRAMMAR_LEXICON, "scribery:greek_forms_grammar_lexicon", 1, ScribePrices.USES_BOOK, ScribePrices.XP_BIG));
        o.add(sell(4, ScribePrices.THEMATIC_LEXICON, "scribery:thematic_greek_lexicon", 1, ScribePrices.USES_BOOK, ScribePrices.XP_BIG));
        // Stufe 5, Meister (die Fundkarte kommt im Mod dazu)
        o.add(sell(5, ScribePrices.COMPLETE_LEXICON, "scribery:complete_greek_lexicon", 1, ScribePrices.USES_BOOK, ScribePrices.XP_BIG));
        return List.copyOf(o);
    }

    public static List<Offer> forLevel(int level) {
        return ALL.stream().filter(x -> x.level() == level).toList();
    }
}
