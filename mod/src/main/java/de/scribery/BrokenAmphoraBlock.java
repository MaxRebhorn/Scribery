package de.scribery;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Zerbrochene Amphoren in verschiedener Lage, zum Fuellen von Wracks und Ruinen: liegend, mit abgebrochenem Hals,
 * nur der Unterbau, verstreute Scherben, schief angelehnt. Jede dreht sich nach {@code facing} und laesst sich unter
 * Wasser setzen. Die Modelle erzeugt art/gen_amphora_variants.py. Der Inhalt (Scherben, Ostrakon, Manuskript) steht in
 * den Loot-Tabellen, siehe tools/gen_loot_tables.py.
 */
public class BrokenAmphoraBlock extends Block implements SimpleWaterloggedBlock {
    /** Die Lagen; die Form (Kollision) gilt fuer Blickrichtung Norden und wird fuer die anderen gedreht. */
    public enum Kind implements StringRepresentable {
        LYING("lying", Block.box(3, 0, 0, 13, 10, 16)),
        LYING_BROKEN("lying_broken", Block.box(3, 0, 3, 13, 10, 16)),
        BROKEN_NECK("broken_neck", Shapes.or(Block.box(3, 0, 3, 13, 10, 13), Block.box(5, 10, 5, 11, 14, 11))),
        HALF("half", Block.box(3, 0, 3, 13, 9, 13)),
        SHARDS("shards", Block.box(1, 0, 2, 14, 3, 14)),
        TILTED("tilted", Block.box(2, 0, 3, 14, 16, 13));

        public static final Codec<Kind> CODEC = StringRepresentable.fromEnum(Kind::values);
        private final String name;
        private final VoxelShape north;

        Kind(String name, VoxelShape north) {
            this.name = name;
            this.north = north;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    public static final MapCodec<BrokenAmphoraBlock> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Kind.CODEC.fieldOf("kind").forGetter(b -> b.kind), propertiesCodec())
            .apply(i, BrokenAmphoraBlock::new));

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    private final Kind kind;
    private final VoxelShape[] shapes = new VoxelShape[4];   // nach Direction.get2DDataValue: Sued, West, Nord, Ost

    public BrokenAmphoraBlock(Kind kind, Properties props) {
        super(props);
        this.kind = kind;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(WATERLOGGED, false));
        // Form einmal pro Blickrichtung berechnen: um y drehen (Norden = 0, Osten = 90 Grad im Uhrzeigersinn, ...)
        VoxelShape s = kind.north;
        for (int turn = 0; turn < 4; turn++) {
            Direction d = switch (turn) { case 0 -> Direction.NORTH; case 1 -> Direction.EAST; case 2 -> Direction.SOUTH; default -> Direction.WEST; };
            shapes[d.get2DDataValue()] = s;
            s = turnClockwise(s);
        }
    }

    private static VoxelShape turnClockwise(VoxelShape shape) {
        VoxelShape out = Shapes.empty();
        for (AABB b : shape.toAabbs()) {
            out = Shapes.or(out, Shapes.box(1 - b.maxZ, b.minY, b.minX, 1 - b.minZ, b.maxY, b.maxX));
        }
        return out;
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, WATERLOGGED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        FluidState fluid = ctx.getLevel().getFluidState(ctx.getClickedPos());
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite())
                .setValue(WATERLOGGED, fluid.getType() == Fluids.WATER);
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level,
                                     BlockPos pos, BlockPos neighborPos) {
        if (state.getValue(WATERLOGGED)) level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        return super.updateShape(state, direction, neighbor, level, pos, neighborPos);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return shapes[state.getValue(FACING).get2DDataValue()];
    }
}
