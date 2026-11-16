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
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Minting Table: Eisennuggets einlegen (Rechtsklick mit Nuggets), dann mit leerer Hand die Oberflaeche oeffnen
 * und den gewuenschten Buchstaben anklicken. Schleichen + leere Hand gibt die Nuggets zurueck.
 */
public class MintBlock extends FacingBlock implements EntityBlock {
    public static final MapCodec<MintBlock> CODEC = simpleCodec(MintBlock::new);

    public MintBlock(Properties props) {
        super(props);
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MintBlockEntity(pos, state);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack held, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        if (!held.is(Items.IRON_NUGGET) || !(level.getBlockEntity(pos) instanceof MintBlockEntity be)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide) {
            int taken = be.add(held.getCount());
            if (taken > 0 && !player.getAbilities().instabuild) held.shrink(taken);
            level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.6f, 1.4f);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide && level.getBlockEntity(pos) instanceof MintBlockEntity be) {
                Give.items(player, Items.IRON_NUGGET, be.takeAll());
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (level.isClientSide) de.scribery.client.ClientHooks.openMint(pos);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock()) && !level.isClientSide && level.getBlockEntity(pos) instanceof MintBlockEntity be) {
            int n = be.nuggets();
            while (n > 0) {
                int k = Math.min(n, 64);
                Containers.dropItemStack(level, pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5, new ItemStack(Items.IRON_NUGGET, k));
                n -= k;
            }
        }
        super.onRemove(state, level, pos, newState, moving);
    }
}
