package de.scribery;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
 * Lettern-Schrank. Rechtsklick mit Letter Types, Tintenbeuteln oder Papier legt sie ein; leere Hand oeffnet
 * die Druck-Oberflaeche; Schleichen + leere Hand nimmt alles wieder heraus.
 */
public class CabinetBlock extends FacingBlock implements EntityBlock {
    public static final MapCodec<CabinetBlock> CODEC = simpleCodec(CabinetBlock::new);

    public CabinetBlock(Properties props) {
        super(props);
    }

    /** Das Modell ist im Blockstate um 90 Grad nach rechts gedreht: Buchstabenfeld vorn, Tintenfass hinten rechts. */
    @Override
    protected int extraTurns() {
        return 1;
    }

    @Override
    protected double[][] boxes() {
        return StationShapes.LETTER_CABINET;
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CabinetBlockEntity(pos, state);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack held, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof CabinetBlockEntity be)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        int letter = ModItems.letterIndex(held.getItem());
        boolean ink = held.is(Items.INK_SAC), paper = held.is(Items.PAPER);
        if (letter < 0 && !ink && !paper) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!level.isClientSide) {
            int n = held.getCount();
            if (letter >= 0) be.addLetters(letter, n);
            else if (ink) be.addInk(n * CabinetBlockEntity.INK_PER_SAC);
            else be.addPaper(n);
            if (!player.getAbilities().instabuild) held.shrink(n);
            level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.6f, 1.2f);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide && level.getBlockEntity(pos) instanceof CabinetBlockEntity be) {
                giveContents(player, be);
                be.clear();
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (level.isClientSide) de.scribery.client.ClientHooks.openPress(pos);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static void giveContents(Player player, CabinetBlockEntity be) {
        int[] letters = be.totalLetters();
        for (int i = 0; i < letters.length; i++) {
            if (letters[i] > 0) Give.items(player, ModItems.LETTER_ITEMS.get(i).get(), letters[i]);
        }
        if (be.ink() >= CabinetBlockEntity.INK_PER_SAC) Give.items(player, Items.INK_SAC, be.ink() / CabinetBlockEntity.INK_PER_SAC);
        if (be.paper() > 0) Give.items(player, Items.PAPER, be.paper());
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock()) && !level.isClientSide && level.getBlockEntity(pos) instanceof CabinetBlockEntity be) {
            giveContentsAt(level, pos, be);
        }
        super.onRemove(state, level, pos, newState, moving);
    }

    private static void giveContentsAt(Level level, BlockPos pos, CabinetBlockEntity be) {
        int[] letters = be.totalLetters();
        for (int i = 0; i < letters.length; i++) {
            if (letters[i] > 0) drop(level, pos, ModItems.LETTER_ITEMS.get(i).get(), letters[i]);
        }
        if (be.ink() >= CabinetBlockEntity.INK_PER_SAC) drop(level, pos, Items.INK_SAC, be.ink() / CabinetBlockEntity.INK_PER_SAC);
        if (be.paper() > 0) drop(level, pos, Items.PAPER, be.paper());
    }

    private static void drop(Level level, BlockPos pos, net.minecraft.world.item.Item item, int count) {
        while (count > 0) {
            int n = Math.min(count, item.getDefaultMaxStackSize());
            net.minecraft.world.Containers.dropItemStack(level, pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5, new ItemStack(item, n));
            count -= n;
        }
    }
}
