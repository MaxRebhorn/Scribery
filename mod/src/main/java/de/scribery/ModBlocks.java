package de.scribery;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Platzhalter-Blöcke der Stationen: nur Registrierung, Textur und Drop, noch keine Funktion. */
public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Scribery.MODID);
    public static final DeferredRegister.Items BLOCK_ITEMS = DeferredRegister.createItems(Scribery.MODID);
    public static final List<DeferredBlock<Block>> ALL = new ArrayList<>();

    private static DeferredBlock<Block> station(String id) {
        return station(id, FacingBlock::new);
    }

    private static DeferredBlock<Block> station(String id, java.util.function.Function<BlockBehaviour.Properties, Block> factory) {
        DeferredBlock<Block> b = BLOCKS.register(id, () -> factory.apply(BlockBehaviour.Properties.of()
                .mapColor(MapColor.WOOD).strength(2.0F, 3.0F).sound(SoundType.WOOD).ignitedByLava()));
        BLOCK_ITEMS.registerSimpleBlockItem(id, b, new Item.Properties());
        ALL.add(b);
        return b;
    }

    public static final DeferredBlock<Block> SCRIBE_LECTERN = station("scribe_lectern", ScribeLecternBlock::new);
    public static final DeferredBlock<Block> LETTER_CABINET = station("letter_cabinet", CabinetBlock::new);
    public static final DeferredBlock<Block> PRINTING_PRESS = station("printing_press", GroupStationBlock::new);
    public static final DeferredBlock<Block> TRANSLATOR_DESK = station("translator_desk", DeskBlock::new);
    public static final DeferredBlock<Block> MANUSCRIPT_ARCHIVE = station("manuscript_archive");
    public static final DeferredBlock<Block> MINTING_TABLE = station("minting_table", MintBlock::new);
    public static final DeferredBlock<Block> BOOKSHELF = station("bookshelf");

    /** Eisblock mit eingefrorenem Fragment (Testblock fuer das Modell, noch ohne Funktion). */
    public static final DeferredBlock<Block> FROZEN_FRAGMENT_BLOCK = BLOCKS.register("frozen_fragment_block",
            () -> new TransparentBlock(BlockBehaviour.Properties.of().mapColor(MapColor.ICE).strength(0.5F)
                    .sound(SoundType.GLASS).noOcclusion().isValidSpawn((st, l, p, e) -> false)
                    .isRedstoneConductor((st, l, p) -> false).isSuffocating((st, l, p) -> false)
                    .isViewBlocking((st, l, p) -> false)));
    /** Amphore (Container-Platzhalter, noch ohne Inhalt). */
    public static final DeferredBlock<Block> AMPHORA = BLOCKS.register("amphora",
            () -> new AmphoraBlock(BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_WHITE)
                    .strength(0.6F).sound(SoundType.DECORATED_POT).noOcclusion()));
    /** Schlamm mit gebleichtem Holz und Papierstuecken (Optik per Blockzustand dusted 0-3, noch keine Pinsel-Logik). */
    public static final DeferredBlock<Block> SUSPICIOUS_ARCHIVE_MUD = BLOCKS.register("suspicious_archive_mud",
            () -> new SuspiciousMudBlock(BlockBehaviour.Properties.of().mapColor(MapColor.DIRT)
                    .strength(0.25F).sound(SoundType.MUD)));
    static {
        BLOCK_ITEMS.registerSimpleBlockItem("amphora", AMPHORA, new Item.Properties());
        BLOCK_ITEMS.registerSimpleBlockItem("suspicious_archive_mud", SUSPICIOUS_ARCHIVE_MUD, new Item.Properties());
        ALL.add(AMPHORA);
        ALL.add(SUSPICIOUS_ARCHIVE_MUD);
    }
    static {
        BLOCK_ITEMS.registerSimpleBlockItem("frozen_fragment_block", FROZEN_FRAGMENT_BLOCK, new Item.Properties());
        ALL.add(FROZEN_FRAGMENT_BLOCK);
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        BLOCK_ITEMS.register(bus);
    }

    private ModBlocks() {}
}
