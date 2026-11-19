package de.scribery;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Das Pult. Rechtsklick mit einem Fragment legt es auf, wie in einen Bilderrahmen; Rechtsklick mit
 * leerer Hand oeffnet die Oberflaeche; Schleichen + leere Hand nimmt das Fragment wieder.
 */
public class ScribeLecternBlock extends FacingBlock implements EntityBlock {
    public static final MapCodec<ScribeLecternBlock> CODEC = simpleCodec(ScribeLecternBlock::new);

    public ScribeLecternBlock(Properties props) {
        super(props);
    }

    @Override
    protected double[][] boxes() {
        return StationShapes.SCRIBE_LECTERN;
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LecternBlockEntity(pos, state);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack held, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(held.getItem() instanceof ManuscriptFragmentItem) || ManuscriptFragmentItem.indexOf(held) < 0) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!(level.getBlockEntity(pos) instanceof LecternBlockEntity be) || !be.fragment().isEmpty()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide) {
            be.setFragment(held.copyWithCount(1));
            if (!player.getAbilities().instabuild) held.shrink(1);
            level.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 1f, 1f);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof LecternBlockEntity be)) return InteractionResult.PASS;
        if (player.isShiftKeyDown()) {
            if (be.fragment().isEmpty()) return InteractionResult.PASS;
            if (!level.isClientSide) {
                ItemStack back = be.fragment();
                be.setFragment(ItemStack.EMPTY);
                if (!player.getInventory().add(back)) player.drop(back, false);
                level.playSound(null, pos, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 1f, 1f);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (level.isClientSide) de.scribery.client.ClientHooks.openLectern(pos);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof LecternBlockEntity be) {
            Containers.dropItemStack(level, pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5, be.fragment());
        }
        super.onRemove(state, level, pos, newState, moving);
    }
}
