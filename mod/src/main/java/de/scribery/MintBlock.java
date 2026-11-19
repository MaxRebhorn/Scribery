package de.scribery;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Minting Table. Der Ofen muss brennen, damit man ihn benutzen kann: Brennstoff (alles, was ein Ofen verbrennt, auch
 * Papyrusfetzen kurz) per Rechtsklick nachlegen. Solange er brennt, oeffnet Rechtsklick die Giess-Oberflaeche.
 */
public class MintBlock extends FacingBlock implements EntityBlock {
    public static final MapCodec<MintBlock> CODEC = simpleCodec(MintBlock::new);
    public static final BooleanProperty LIT = BooleanProperty.create("lit");

    public MintBlock(Properties props) {
        super(props);
        registerDefaultState(defaultBlockState().setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> b) {
        super.createBlockStateDefinition(b);
        b.add(LIT);
    }

    /** Flamme im Ofenmaul vorn (Modell: bei x 12, y 4, z 2, im Maul) und Rauch ueber dem Tiegel (x 12, z 5), nur wenn er brennt. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT)) return;
        StationShapes.flame(level, pos, state.getValue(FACING), 12.0, 4.0, 2.0, random);
        if (random.nextFloat() < 0.15F) {
            double[] c = StationShapes.rotate(12.0, 5.0, state.getValue(FACING));
            level.addParticle(ParticleTypes.SMOKE, pos.getX() + c[0], pos.getY() + 1.1, pos.getZ() + c[1], 0, 0.01, 0);
        }
        if (random.nextFloat() < 0.1F) {
            level.playLocalSound(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5, SoundEvents.FURNACE_FIRE_CRACKLE,
                    SoundSource.BLOCKS, 1F, 1F, false);
        }
    }

    @Override
    protected double[][] boxes() {
        return StationShapes.MINTING_TABLE;
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
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != ModBlockEntities.MINT.get()) return null;
        return (lv, p, st, be) -> MintBlockEntity.serverTick(lv, p, st, (MintBlockEntity) be);
    }

    @Override
    protected MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        return new SimpleMenuProvider((id, inv, p) -> new MintMenu(id, inv, ContainerLevelAccess.create(level, pos)),
                Component.translatable("block.scribery.minting_table"));
    }

    /** Brennstoff in der Hand: ein Stueck verbrauchen und die Brenndauer dazuzaehlen. Alles andere: normale Bedienung. */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack held, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        int ticks = held.getBurnTime(RecipeType.SMELTING);
        if (ticks <= 0 || !(level.getBlockEntity(pos) instanceof MintBlockEntity be)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide) {
            ItemStack rest = held.getCraftingRemainingItem();
            if (!player.getAbilities().instabuild) {
                held.shrink(1);
                if (!rest.isEmpty()) {
                    if (held.isEmpty()) player.setItemInHand(hand, rest);
                    else if (!player.getInventory().add(rest)) player.drop(rest, false);
                }
            }
            be.addBurn(ticks);
            if (!state.getValue(LIT)) level.setBlock(pos, state.setValue(LIT, true), 3);
            level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.6F, 1.2F);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!state.getValue(LIT)) {
            if (!level.isClientSide) player.displayClientMessage(Component.translatable("block.scribery.minting_table.cold"), true);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!level.isClientSide) player.openMenu(state.getMenuProvider(level, pos));
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
