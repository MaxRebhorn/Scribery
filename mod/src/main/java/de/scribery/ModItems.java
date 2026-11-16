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
        for (String l : LETTERS) simple("letter_type_" + l);
        for (int i = 1; i <= 5; i++) {
            final int n = i;
            Supplier<Item> s = ITEMS.registerItem("manuscript_fragment_" + n,
                    p -> new ManuscriptFragmentItem(p.stacksTo(1), n));
            ALL.add(s);
        }
        for (String b : new String[] {
                "greek_alphabet_primer", "greek_grammar_book", "greek_lexicon_a_c", "greek_lexicon_d_f",
                "thematic_greek_lexicon", "greek_forms_grammar_lexicon", "complete_greek_lexicon"})
            simple(b, 1);
    }

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.scribery"))
                    .icon(() -> ModItems.ALL.get(ALL.size() - 7).get().getDefaultInstance())
                    .displayItems((params, out) -> { ModBlocks.ALL.forEach(b -> out.accept(b.get())); ALL.forEach(s -> out.accept(s.get())); })
                    .build());

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
        TABS.register(bus);
    }

    private ModItems() {}
}
