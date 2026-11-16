package de.scribery;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
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
    public static final DeferredBlock<Block> TRANSLATOR_DESK = station("translator_desk");
    public static final DeferredBlock<Block> MANUSCRIPT_ARCHIVE = station("manuscript_archive");
    public static final DeferredBlock<Block> MINTING_TABLE = station("minting_table", MintBlock::new);
    public static final DeferredBlock<Block> BOOKSHELF = station("bookshelf");

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        BLOCK_ITEMS.register(bus);
    }

    private ModBlocks() {}
}
