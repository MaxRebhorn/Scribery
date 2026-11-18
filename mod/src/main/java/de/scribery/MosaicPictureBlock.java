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
    /** Bis zu 5 x 3 Teile (Zeile * Spalten + Spalte). */
    public static final IntegerProperty PART = IntegerProperty.create("part", 0, 14);

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

    /** Teil des Bildes in Spalte {@code col} (West nach Ost) und Zeile {@code row} (Nord nach Sued) eines Bildes mit {@code cols} Spalten. */
    public static BlockState stateFor(Block block, int col, int row, int cols) {
        return block.defaultBlockState().setValue(PART, row * cols + col);
    }
}
