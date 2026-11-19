package de.scribery;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Minting Table: Rechtsklick oeffnet die Giess-Oberflaeche im Stil des Steinmetz. */
public class MintBlock extends FacingBlock {
    public static final MapCodec<MintBlock> CODEC = simpleCodec(MintBlock::new);

    public MintBlock(Properties props) {
        super(props);
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
    protected MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        return new SimpleMenuProvider((id, inv, p) -> new MintMenu(id, inv, ContainerLevelAccess.create(level, pos)),
                Component.translatable("block.scribery.minting_table"));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide) player.openMenu(state.getMenuProvider(level, pos));
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
