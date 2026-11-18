package de.scribery;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Lager-Amphore: rote Amphore mit Korkstopfen und neun Plaetzen im Inneren (ein Rechtsklick oeffnet sie). Gleiche Form wie
 * die Amphore, siehe {@link AmphoraBlock}. Rezept wie eine Truhe aus Scherben, siehe art/gen_lamp_amphora.py.
 */
public class AmphoraStorageBlock extends BaseEntityBlock {
    public static final MapCodec<AmphoraStorageBlock> CODEC = simpleCodec(AmphoraStorageBlock::new);
    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(5, 0, 5, 11, 2, 11), Block.box(3, 2, 3, 13, 10, 13), Block.box(4, 10, 4, 12, 12, 12),
            Block.box(6, 12, 6, 10, 15, 10), Block.box(6, 15, 6, 10, 16, 10),
            Block.box(4, 12, 7, 6, 15, 9), Block.box(10, 12, 7, 12, 15, 9));

    public AmphoraStorageBlock(Properties props) {
        super(props);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AmphoraStorageBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (level.getBlockEntity(pos) instanceof AmphoraStorageBlockEntity amphora) player.openMenu(amphora);
        return InteractionResult.CONSUME;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        Containers.dropContentsOnDestroy(state, newState, level, pos);
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
