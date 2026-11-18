package de.scribery;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Amphore: kein Wuerfel. Die Form folgt dem Modell (Fuss, Bauch, Schulter, Hals mit Lippe, zwei Henkel). Noch ohne Inhalt. */
public class AmphoraBlock extends Block {
    public static final MapCodec<AmphoraBlock> CODEC = simpleCodec(AmphoraBlock::new);
    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(5, 0, 5, 11, 2, 11), Block.box(3, 2, 3, 13, 10, 13), Block.box(4, 10, 4, 12, 12, 12),
            Block.box(6, 12, 6, 10, 15, 10), Block.box(5, 15, 5, 11, 16, 11),
            Block.box(4, 12, 7, 6, 15, 9), Block.box(10, 12, 7, 12, 15, 9));

    public AmphoraBlock(Properties props) {
        super(props);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }
}
