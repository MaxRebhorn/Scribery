package de.scribery;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Gemeinsame Hilfe der Debug-Generatoren fuer Strukturen (DebugStructures). Das Koordinatensystem liegt am Block unter den
 * Fuessen des Spielers: {@code u} laeuft in Blickrichtung, {@code v} nach rechts, {@code h} nach oben (0 = oberster Gelaendeblock,
 * negative Werte liegen unter der Erde). Alles wird mit {@code setBlock} gesetzt; unter Wasser werden Bloecke, die es koennen,
 * mit Wasser gefuellt.
 */
final class StructureBuilder {
    /** Bei der Weltgenerierung: nur Bloecke in diesem Bereich (dem Chunk) werden wirklich gesetzt. Pro Thread. */
    static final ThreadLocal<net.minecraft.world.level.levelgen.structure.BoundingBox> CLIP = new ThreadLocal<>();

    static boolean inClip(BlockPos pos) {
        var c = CLIP.get();
        return c == null || c.isInside(pos);
    }

    final LevelAccessor level;
    final BlockPos origin;
    final Direction forward, right;
    final RandomSource random;
    int placed;

    StructureBuilder(LevelAccessor level, BlockPos origin, Direction facing) {
        this.level = level;
        this.origin = origin;
        this.forward = facing;
        this.right = facing.getClockWise();
        this.random = RandomSource.create(origin.asLong());
    }

    BlockPos at(int u, int v, int h) {
        return origin.relative(forward, u).relative(right, v).above(h);
    }

    Direction.Axis axisU() {
        return forward.getAxis();
    }

    Direction.Axis axisV() {
        return right.getAxis();
    }

    void put(int u, int v, int h, BlockState state) {
        put(at(u, v, h), state);
    }

    void put(BlockPos pos, BlockState state) {
        placed++;
        if (!inClip(pos)) return;  // bei der Weltgenerierung: ausserhalb des Chunks wird weder gelesen noch geschrieben
        if (state.hasProperty(BlockStateProperties.WATERLOGGED) && level.getFluidState(pos).is(FluidTags.WATER)) {
            state = state.setValue(BlockStateProperties.WATERLOGGED, true);
        }
        level.setBlock(pos, state, Block.UPDATE_CLIENTS);
    }

    /** Block an der Stelle; ausserhalb des Bereichs der Weltgenerierung gilt Luft (dort darf nicht gelesen werden). */
    BlockState stateAt(BlockPos pos) {
        return inClip(pos) ? level.getBlockState(pos) : Blocks.AIR.defaultBlockState();
    }

    void put(int u, int v, int h, Block block) {
        put(at(u, v, h), block.defaultBlockState());
    }

    /** Quader fuellen (Grenzen inklusive). */
    void box(int u0, int u1, int v0, int v1, int h0, int h1, BlockState state) {
        for (int u = u0; u <= u1; u++) for (int v = v0; v <= v1; v++) for (int h = h0; h <= h1; h++) put(at(u, v, h), state);
    }

    BlockState log(Block block, Direction.Axis axis) {
        return block.defaultBlockState().setValue(RotatedPillarBlock.AXIS, axis);
    }

    /** Mit Wahrscheinlichkeit {@code percent} (0 bis 100) wahr. */
    boolean chance(int percent) {
        return random.nextInt(100) < percent;
    }

    /**
     * Hohlraeume graben und mit einer duennen Huelle auskleiden: Jede Zelle wird zu Luft, jeder Nachbar, der selbst keine
     * Zelle ist, wird zur Wand, nach unten zum Boden. So sieht ein unterirdischer Raum gebaut aus, ohne einen Block Stein zu
     * fuellen.
     */
    void carve(Set<BlockPos> cells, BlockState wall, BlockState floor) {
        carve(cells, wall, p -> floor);
    }

    /** Wie die Variante mit einem Boden, aber der Boden wird je Position gewaehlt (z. B. Mosaike). */
    void carve(Set<BlockPos> cells, BlockState wall, java.util.function.Function<BlockPos, BlockState> floor) {
        carve(cells, p -> wall, floor);
    }

    /** Wand und Boden werden je Position gewaehlt (z. B. gemischte, teils brueechige Ziegel). */
    void carve(Set<BlockPos> cells, java.util.function.Function<BlockPos, BlockState> wall,
               java.util.function.Function<BlockPos, BlockState> floor) {
        Set<BlockPos> shell = new HashSet<>();
        Set<BlockPos> floors = new HashSet<>();
        for (BlockPos p : cells) {
            for (Direction d : Direction.values()) {
                BlockPos n = p.relative(d);
                if (cells.contains(n)) continue;
                if (d == Direction.DOWN) floors.add(n);
                else shell.add(n);
            }
        }
        for (BlockPos p : shell) if (!floors.contains(p)) put(p, wall.apply(p));
        for (BlockPos p : floors) put(p, floor.apply(p));
        for (BlockPos p : cells) {
            if (inClip(p)) level.setBlock(p, level.getFluidState(p).is(FluidTags.WATER) ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            placed++;
        }
    }

    /** Zellen eines Quaders (Grenzen inklusive) zu einer Menge hinzufuegen. */
    void cells(Set<BlockPos> into, int u0, int u1, int v0, int v1, int h0, int h1) {
        for (int u = u0; u <= u1; u++) for (int v = v0; v <= v1; v++) for (int h = h0; h <= h1; h++) into.add(at(u, v, h));
    }
}
