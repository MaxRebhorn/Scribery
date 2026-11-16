package de.scribery;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Reine Platzhalter-Items: nur Registrierung, Textur und Name, noch keine Funktion. */
public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Scribery.MODID);
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Scribery.MODID);

    private static final List<Supplier<Item>> ALL = new ArrayList<>();
    /** Die Letter Types in der Reihenfolge der Buchstabenbox (A B C D E Ē G H ...). */
    public static final List<Supplier<Item>> LETTER_ITEMS = new ArrayList<>();

    public static final Supplier<Item> MANUSCRIPT_FRAGMENT = ITEMS.registerItem("manuscript_fragment", ManuscriptFragmentItem::new);

    private static Supplier<Item> simple(String id) {
        Supplier<Item> s = ITEMS.registerSimpleItem(id);
        ALL.add(s);
        return s;
    }

    private static Supplier<Item> simple(String id, int stack) {
        Supplier<Item> s = ITEMS.registerItem(id, p -> new Item(p.stacksTo(stack)));
        ALL.add(s);
        return s;
    }

    // Letter Types: stapelbar, da pro Text mehrere Exemplare je Buchstabe gebraucht werden.
    public static final String[] LETTERS = {
        "a", "b", "c", "d", "e", "e_long", "g", "h", "i", "k", "l",
        "m", "n", "o", "o_long", "p", "r", "s", "t", "u", "x", "z"
    };
    static {
        for (String l : LETTERS) LETTER_ITEMS.add(simple("letter_type_" + l));
        ALL.add(MANUSCRIPT_FRAGMENT);
        for (String b : new String[] {
                "greek_alphabet_primer", "greek_grammar_book", "greek_lexicon_a_c", "greek_lexicon_d_f",
                "thematic_greek_lexicon", "greek_forms_grammar_lexicon", "complete_greek_lexicon"})
            simple(b, 1);
    }

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.scribery"))
                    .icon(() -> LETTER_ITEMS.get(0).get().getDefaultInstance())
                    .displayItems((params, out) -> {
                        ModBlocks.ALL.forEach(b -> out.accept(b.get()));
                        ALL.forEach(s -> { if (s != MANUSCRIPT_FRAGMENT) out.accept(s.get()); });
                        for (Manuscripts.Fragment f : Manuscripts.load()) out.accept(ManuscriptFragmentItem.of(f.id()));
                    })
                    .build());

    /** Index des Buchstabens in der Box, oder -1 wenn das Item kein Letter Type ist. */
    public static int letterIndex(Item item) {
        for (int i = 0; i < LETTER_ITEMS.size(); i++) if (LETTER_ITEMS.get(i).get() == item) return i;
        return -1;
    }

    public static final Supplier<Item> WOODEN_WORD_TABLET = ITEMS.registerSimpleItem("wooden_word_tablet");
    public static final Supplier<Item> PRINTED_SHEET = ITEMS.registerItem("printed_sheet", SheetItem::new);
    public static final Supplier<Item> TRANSLATED_SHEET = ITEMS.registerItem("translated_sheet", SheetItem::new);
    static {
        ALL.add(WOODEN_WORD_TABLET);
        ALL.add(PRINTED_SHEET);
        ALL.add(TRANSLATED_SHEET);
    }

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
        TABS.register(bus);
    }

    private ModItems() {}
}
