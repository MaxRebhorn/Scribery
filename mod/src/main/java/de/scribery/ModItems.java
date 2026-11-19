package de.scribery;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import net.minecraft.core.registries.Registries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
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
        {
            final String id = b;
            ALL.add(ITEMS.registerItem(id, p -> new BookItem(p, id)));
        }
    }

    /** Reihenfolge = Wert von custom_model_data (1 bis 4). Der Untergrund, in dem das Fragment liegt, bestimmt die Variante. */
    static final String[] VARIANT_NAMES = {"charred", "water damaged", "faded", "frozen"};

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.scribery"))
                    .icon(() -> LETTER_ITEMS.get(0).get().getDefaultInstance())
                    .displayItems((params, out) -> {
                        ModBlocks.ALL.forEach(b -> out.accept(b.get()));
                        ALL.forEach(s -> { if (s != MANUSCRIPT_FRAGMENT && s != ModItems.OSTRAKON) out.accept(s.get()); });
                        for (String joke : Ostraka.ids()) out.accept(Ostraka.of(joke));   // je Witz ein beschriebener Scherben
                        // Die Manuskripte selbst stehen in eigenen Tabs, einer pro Kapitel (siehe unten).
                        // Testvarianten (nur Optik, siehe Ueberschreibungen in models/item/manuscript_fragment.json)
                        String first = Manuscripts.load().get(0).id();
                        for (int i = 0; i < VARIANT_NAMES.length; i++) {
                            ItemStack v = ManuscriptFragmentItem.of(first);
                            v.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(i + 1));
                            v.set(DataComponents.CUSTOM_NAME, Component.literal("Manuscript Fragment (" + VARIANT_NAMES[i] + ")")
                                    .withStyle(s -> s.withItalic(false)));
                            out.accept(v);
                        }
                    })
                    .build());

    /**
     * Ein Creative-Tab pro Kapitel, damit die rund 1.500 Manuskripte nicht zwischen den anderen Items untergehen.
     * Die Kapitel kommen aus den Daten, die Tabs stehen in der Reihenfolge des Evangeliums.
     */
    public static final List<DeferredHolder<CreativeModeTab, CreativeModeTab>> CHAPTER_TABS = new ArrayList<>();
    static {
        java.util.SortedSet<Integer> chapters = new java.util.TreeSet<>();
        // Immer die echten Daten: der Selbsttest laedt sonst Platzhalter mit nur einem Kapitel, und Items lassen sich spaeter nicht nachregistrieren
        for (Manuscripts.Fragment f : Boolean.getBoolean("scribery.selftest") ? Manuscripts.loadReal() : Manuscripts.load()) {
            chapters.add(f.chapter());
        }
        for (int chapter : chapters) {
            // Nur als Tab-Icon, nicht in einem Tab und nicht herstellbar (Textur: art/gen_chapter_icons.py)
            ITEMS.registerSimpleItem("chapter_icon_" + chapter);
            Chapters.Info info = Chapters.of(chapter);
            CHAPTER_TABS.add(TABS.register("manuscripts_john_" + chapter, () -> CreativeModeTab.builder()
                    // z. B. "The Word & first disciples · John 1:1-51": Geschichte statt Kapitelnummer, dazu der Versbereich
                    .title(Component.translatable("itemGroup.scribery.manuscripts", info.title(), info.range()))
                    .icon(() -> {
                        // Vanilla-Item, das zur Geschichte passt; ohne Eintrag das erste Manuskript des Kapitels
                        var item = net.minecraft.core.registries.BuiltInRegistries.ITEM
                                .getOptional(net.minecraft.resources.ResourceLocation.parse(
                                        info.icon().isEmpty() ? "minecraft:air" : info.icon()));
                        if (item.isPresent() && item.get() != net.minecraft.world.item.Items.AIR) return item.get().getDefaultInstance();
                        for (Manuscripts.Fragment f : Manuscripts.load()) {
                            if (f.chapter() == chapter) return ManuscriptFragmentItem.of(f.id());
                        }
                        return ItemStack.EMPTY;
                    })
                    .displayItems((params, out) -> {
                        for (Manuscripts.Fragment f : Manuscripts.load()) {
                            if (f.chapter() == chapter) out.accept(ManuscriptFragmentItem.of(f.id()));
                        }
                    })
                    .build()));
        }
    }

    /** Index des Buchstabens in der Box, oder -1 wenn das Item kein Letter Type ist. */
    public static int letterIndex(Item item) {
        for (int i = 0; i < LETTER_ITEMS.size(); i++) if (LETTER_ITEMS.get(i).get() == item) return i;
        return -1;
    }

    public static final Supplier<Item> OSTRAKON = ITEMS.registerItem("ostrakon", OstrakonItem::new);
    public static final Supplier<Item> WOODEN_WORD_TABLET = ITEMS.registerSimpleItem("wooden_word_tablet");
    public static final Supplier<Item> PRINTED_SHEET = ITEMS.registerItem("printed_sheet", SheetItem::new);
    public static final Supplier<Item> TRANSLATED_SHEET = ITEMS.registerItem("translated_sheet", SheetItem::new);
    static {
        ALL.add(WOODEN_WORD_TABLET);
        ALL.add(PRINTED_SHEET);
        ALL.add(TRANSLATED_SHEET);
        ALL.add(OSTRAKON);
        // Papyrusfetzen: nur Brennstoff (data/neoforge/data_maps/item/furnace_fuels.json), sonst zu nichts zu gebrauchen
        ALL.add(ITEMS.registerSimpleItem("papyrus_scrap"));
        // Scherbe: Beifund aus Amphoren und Scherbenhaufen
        ALL.add(ITEMS.registerSimpleItem("amphora_shard"));
        // Bauteile des Minting Table (Rezepte siehe docs/DESIGN-RECIPES, Funktion im Block)
        ALL.add(ITEMS.registerSimpleItem("mini_anvil"));
        ALL.add(ITEMS.registerSimpleItem("casting_mold"));
        // Fundbeutel des Scribe Villagers (je Fundart eines)
        for (String find : FindPools.FINDS) {
            final String f = find;
            ALL.add(ITEMS.registerItem("find_pouch_" + f, p -> new FindPouchItem(p, f)));
        }
    }

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
        TABS.register(bus);
    }

    private ModItems() {}
}
