package de.scribery;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
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
 * Uebersetzungstisch. Rechtsklick mit einem bedruckten Bogen legt ihn auf; mit Tintenbeutel, Papier oder
 * Holztaefelchen fuellt man die Vorraete; leere Hand oeffnet die Uebersetzung; Schleichen nimmt alles zurueck.
 */
public class DeskBlock extends FacingBlock implements EntityBlock {
    public static final MapCodec<DeskBlock> CODEC = simpleCodec(DeskBlock::new);

    public DeskBlock(Properties props) {
        super(props);
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DeskBlockEntity(pos, state);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack held, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof DeskBlockEntity be)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        boolean sheet = held.is(ModItems.PRINTED_SHEET.get()) && be.sheet().isEmpty();
        boolean ink = held.is(Items.INK_SAC), paper = held.is(Items.PAPER), tablets = held.is(ModItems.WOODEN_WORD_TABLET.get());
        if (!sheet && !ink && !paper && !tablets) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!level.isClientSide) {
            int n = held.getCount();
            if (sheet) {
                be.setSheet(held.copyWithCount(1));
                n = 1;
            } else if (ink) be.addInk(n * CabinetBlockEntity.INK_PER_SAC);
            else if (paper) be.addPaper(n);
            else be.addTablets(n);
            if (!player.getAbilities().instabuild) held.shrink(n);
            level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.6f, 1.2f);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof DeskBlockEntity be)) return InteractionResult.PASS;
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                if (!be.sheet().isEmpty()) {
                    ItemStack back = be.sheet();
                    be.setSheet(ItemStack.EMPTY);
                    if (!player.getInventory().add(back)) player.drop(back, false);
                }
                Give.items(player, Items.INK_SAC, be.ink() / CabinetBlockEntity.INK_PER_SAC);
                Give.items(player, Items.PAPER, be.paper());
                Give.items(player, ModItems.WOODEN_WORD_TABLET.get(), be.tablets());
                be.clearResources();
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (level.isClientSide) {
            SheetData d = be.sheetData();
            if (d == null) player.displayClientMessage(Component.literal("Place a printed sheet on the desk first"), true);
            else if (!d.translatable()) player.displayClientMessage(Component.literal("This sheet cannot be translated"), true);
            else if (be.paper() <= 0) player.displayClientMessage(Component.literal("The desk needs a sheet of paper for the translation"), true);
            else de.scribery.client.ClientHooks.openDesk(pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock()) && !level.isClientSide && level.getBlockEntity(pos) instanceof DeskBlockEntity be) {
            double x = pos.getX() + .5, y = pos.getY() + .5, z = pos.getZ() + .5;
            Containers.dropItemStack(level, x, y, z, be.sheet());
            drop(level, x, y, z, Items.INK_SAC, be.ink() / CabinetBlockEntity.INK_PER_SAC);
            drop(level, x, y, z, Items.PAPER, be.paper());
            drop(level, x, y, z, ModItems.WOODEN_WORD_TABLET.get(), be.tablets());
        }
        super.onRemove(state, level, pos, newState, moving);
    }

    private static void drop(Level level, double x, double y, double z, net.minecraft.world.item.Item item, int count) {
        while (count > 0) {
            int n = Math.min(count, item.getDefaultMaxStackSize());
            Containers.dropItemStack(level, x, y, z, new ItemStack(item, n));
            count -= n;
        }
    }
}
