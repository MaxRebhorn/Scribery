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

    /** Flamme aus dem Ofenmaul vorn (Modell: bei x 11.5, y 3, vor der Vorderseite) und Rauch ueber dem Tiegel (x 12, z 5). */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, net.minecraft.util.RandomSource random) {
        StationShapes.flame(level, pos, state.getValue(FACING), 11.5, 3.0, -0.5, random);
        if (random.nextFloat() < 0.15F) {
            double[] c = StationShapes.rotate(12.0, 5.0, state.getValue(FACING));
            level.addParticle(net.minecraft.core.particles.ParticleTypes.SMOKE, pos.getX() + c[0], pos.getY() + 1.1, pos.getZ() + c[1], 0, 0.01, 0);
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
