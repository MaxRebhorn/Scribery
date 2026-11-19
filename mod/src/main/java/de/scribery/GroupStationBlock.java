package de.scribery;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Presse und Lettern-Schrank: Rechtsklick oeffnet die gemeinsame Druck-Oberflaeche. Welche Teile sie
 * zeigt, haengt davon ab, welche Stationen angrenzen.
 */
public class GroupStationBlock extends FacingBlock {
    public static final MapCodec<GroupStationBlock> CODEC = simpleCodec(GroupStationBlock::new);

    public GroupStationBlock(Properties props) {
        super(props);
    }

    @Override
    protected double[][] boxes() {
        return StationShapes.PRINTING_PRESS;
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) de.scribery.client.ClientHooks.openPress(pos);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
