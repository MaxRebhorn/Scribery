package de.scribery;

import com.mojang.serialization.MapCodec;

import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

/** Platzhalter-Block: Würfel, der mit der Vorderseite zum Spieler platziert wird. Keine Funktion. */
public class FacingBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<FacingBlock> CODEC = simpleCodec(FacingBlock::new);

    public FacingBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    private Map<Direction, VoxelShape> shapes;

    /** Quader der Form fuer Blickrichtung Norden (siehe {@link StationShapes}); null = voller Wuerfel. */
    protected double[][] boxes() {
        return null;
    }

    /** Wie viele Vierteldrehungen (im Uhrzeigersinn) das Modell im Blockstate zusaetzlich zur Blickrichtung gedreht ist. */
    protected int extraTurns() {
        return 0;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        double[][] b = boxes();
        if (b == null) return super.getShape(state, level, pos, ctx);
        if (shapes == null) shapes = StationShapes.build(b);
        Direction d = state.getValue(FACING);
        for (int i = 0; i < extraTurns(); i++) d = d.getClockWise();   // Modell im Blockstate zusaetzlich gedreht
        return shapes.get(d);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> b) {
        b.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }
}
