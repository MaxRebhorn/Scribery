package de.scribery;

import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * Ein Teil eines Mosaikbildes aus 3 x 3 Bloecken (Fisch, Chi-Rho). {@code part} ist Zeile * 3 + Spalte; Zeile 0 liegt im
 * Norden, Spalte 0 im Westen. Wer das Bild baut, setzt die Teile nach Weltkoordinaten (siehe
 * {@link #stateFor(Block, int, int)}), damit es immer zusammenpasst. Texturen: art/gen_catacomb_blocks.py.
 */
public class MosaicPictureBlock extends Block {
    public static final IntegerProperty PART = IntegerProperty.create("part", 0, 8);

    public MosaicPictureBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(PART, 4));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PART);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        // Beim Platzieren von Hand zaehlt die Lage zum Raster: so lassen sich Bilder auch von Hand legen
        int col = Math.floorMod(ctx.getClickedPos().getX(), 3);
        int row = Math.floorMod(ctx.getClickedPos().getZ(), 3);
        return defaultBlockState().setValue(PART, row * 3 + col);
    }

    /** Teil des Bildes fuer die Weltposition, wenn das Bild bei (minX, minZ) beginnt (Nordwestecke). */
    public static BlockState stateFor(Block block, int x, int minX, int z, int minZ) {
        return block.defaultBlockState().setValue(PART, (z - minZ) * 3 + (x - minX));
    }

    public static BlockState stateFor(Block block, int col, int row) {
        return block.defaultBlockState().setValue(PART, row * 3 + col);
    }
}
