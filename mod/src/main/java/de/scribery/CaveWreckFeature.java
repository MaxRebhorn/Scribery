package de.scribery;

import java.util.concurrent.atomic.AtomicInteger;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Ein Schiffswrack in einer ueberfluteten Hoehle. Der Hohlraum laesst sich beim Strukturstart noch nicht erkennen (Hoehlen werden
 * erst danach ausgehoehlt), darum ist das ein Feature: Es sucht in der Mitte des Chunks nach einem Wasserraum mit festem Boden und
 * Decke und schafft dann eine Wasserkammer fuer das Schiff (21 lang, 9 breit, 5 hoch). Es schreibt hoechstens 12 Bloecke weit von
 * der Mitte des Chunks, bleibt also im erlaubten Bereich (3 mal 3 Chunks).
 */
public class CaveWreckFeature extends Feature<NoneFeatureConfiguration> {
    /** Zaehler fuer den Server-Test (wie viele Wracks gesetzt wurden). */
    public static final AtomicInteger PLACED = new AtomicInteger();
    /** Nur fuer den Server-Test: Aufrufe, gefundene Hoehlenboeden, davon mit genug Platz. */
    public static final AtomicInteger CALLS = new AtomicInteger(), FLOORS = new AtomicInteger(), ROOMS = new AtomicInteger();
    private static final int TRIES = 14;

    public CaveWreckFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        RandomSource random = ctx.random();
        CALLS.incrementAndGet();
        BlockPos corner = new BlockPos(ctx.origin().getX() & ~15, 0, ctx.origin().getZ() & ~15);
        for (int i = 0; i < TRIES; i++) {
            int x = corner.getX() + 4 + random.nextInt(8), z = corner.getZ() + 4 + random.nextInt(8);
            int floorY = findCaveFloor(level, x, z);
            if (floorY == Integer.MIN_VALUE) continue;
            FLOORS.incrementAndGet();
            Direction facing = Direction.Plane.HORIZONTAL.getRandomDirection(random);
            carve(level, x, floorY, z, facing);
            ROOMS.incrementAndGet();
            DebugWreck.build(level, new BlockPos(x, floorY, z), facing, 2 + random.nextInt(8), random.nextInt(4) == 0);
            PLACED.incrementAndGet();
            return true;
        }
        return false;
    }

    /** Hoehe des festen Bodens unter einer Wassersaeule mit fester Decke darueber (Hoehle, nicht offenes Meer); sonst Integer.MIN_VALUE. */
    static int findCaveFloor(WorldGenLevel level, int x, int z) {
        int sea = level.getSeaLevel();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos(x, 0, z);
        for (int y = sea - 10; y > level.getMinBuildHeight() + 8; y--) {
            p.setY(y);
            if (!level.getFluidState(p).is(FluidTags.WATER)) continue;
            p.setY(y - 1);
            BlockState below = level.getBlockState(p);
            if (below.getFluidState().is(FluidTags.WATER) || below.isAir() || !below.isSolid()) continue;
            int floor = y - 1;
            // Decke: darueber 7 bis 16 Bloecke Wasser, dann fester Stein; erreicht die Suche den Meeresspiegel, ist es offenes Meer
            int water = 0;
            boolean ceiling = false;
            for (int dy = 0; dy <= 18; dy++) {
                p.setY(y + dy);
                if (p.getY() >= sea) break;
                if (level.getFluidState(p).is(FluidTags.WATER)) { water++; continue; }
                ceiling = level.getBlockState(p).isSolid();
                break;
            }
            if (ceiling && water >= 7) return floor;
            return Integer.MIN_VALUE;
        }
        return Integer.MIN_VALUE;
    }

    /**
     * Macht Platz fuer das Schiff: Fester Stein in der Grundflaeche (21 lang, 9 breit, 5 hoch ueber dem Boden) wird durch Wasser
     * ersetzt, damit das Wrack in einer Kammer liegt und nicht im Fels steckt. Bedrock und Fluessigkeiten bleiben.
     */
    static void carve(WorldGenLevel level, int x, int floorY, int z, Direction facing) {
        Direction right = facing.getClockWise();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int u = -11; u <= 11; u++) {
            for (int v = -5; v <= 5; v++) {
                double e = Math.pow(u / 11.5, 2) + Math.pow(v / 5.5, 2);
                if (e > 1) continue;                                  // Kammer ist oval
                for (int h = 1; h <= 6; h++) {
                    p.set(x + facing.getStepX() * u + right.getStepX() * v, floorY + h, z + facing.getStepZ() * u + right.getStepZ() * v);
                    BlockState bs = level.getBlockState(p);
                    if (bs.is(net.minecraft.world.level.block.Blocks.BEDROCK) || bs.getFluidState().is(FluidTags.WATER)) continue;
                    level.setBlock(p, net.minecraft.world.level.block.Blocks.WATER.defaultBlockState(), 2);
                }
            }
        }
    }
}
