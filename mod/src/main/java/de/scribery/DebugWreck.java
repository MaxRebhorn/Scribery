package de.scribery;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Ein antikes Handelsschiff als Wrack, zum Testen der Fundbloecke (Debug-Befehl /scribery_wreck). Gebaut nach
 * zwei Vorbildern: einem fast ganz im Sand versunkenen Holzschiff (lange, schmale Rumpfform, Kiel, Spanten, die aus
 * dem Boden ragen, Bug und Heck hochgezogen) und einem Amphorenhaufen auf dem Meeresboden.
 *
 * <p>Der Rumpf ist 21 Bloecke lang und bis 7 breit, liegt auf einer Sandduene und ist teilweise zerfallen. In der
 * Ladung stehen Amphoren, um das Wrack liegen Scherbenhaufen sowie Suspicious Sand, Mud und Ashes. Unter Wasser
 * werden alle Bloecke mit Wasser gefuellt (waterlogged), damit keine Luftblasen entstehen.
 */
public final class DebugWreck {
    private static final int HALF = 10;   // halbe Rumpflaenge

    private final Level level;
    private final BlockPos origin;        // Kielhoehe in der Mitte des Rumpfes
    private final boolean alongX;
    private final RandomSource random;
    private int placed;

    private DebugWreck(Level level, BlockPos origin, Direction facing) {
        this.level = level;
        this.origin = origin;
        this.alongX = facing.getAxis() == Direction.Axis.X;
        this.random = RandomSource.create(origin.asLong());
    }

    /** Baut das Wrack. @param origin Kielhoehe in der Mitte; @return Anzahl gesetzter Bloecke */
    public static int build(Level level, BlockPos origin, Direction facing) {
        DebugWreck w = new DebugWreck(level, origin, facing);
        w.dune();
        w.hull();
        w.cargo();
        w.surroundings();
        return w.placed;
    }

    // ------------------------------------------------------------------ Geometrie

    /** Halbe Breite des Rumpfes an der Laengsposition u: in der Mitte 3, zu den Enden spitz zulaufend. */
    private static int halfWidth(int u) {
        double t = Math.abs(u) / (HALF + 0.5);
        return (int) Math.round(3.4 * Math.sqrt(Math.max(0, 1 - Math.pow(t, 2.2))));
    }

    /** Hoehe der Bordwand: in der Mitte 1, an Bug und Heck bis 4 hochgezogen. */
    private static int wallHeight(int u) {
        double t = Math.abs(u) / (double) HALF;
        return (int) Math.round(1 + 3 * t * t);
    }

    private BlockPos at(int u, int v, int h) {
        return alongX ? origin.offset(u, h, v) : origin.offset(v, h, u);
    }

    private Direction.Axis keelAxis() {
        return alongX ? Direction.Axis.X : Direction.Axis.Z;
    }

    private Direction.Axis crossAxis() {
        return alongX ? Direction.Axis.Z : Direction.Axis.X;
    }

    private void put(BlockPos pos, BlockState state) {
        // Unter Wasser mit Wasser fuellen, wo der Block es kann (Zaeune, Platten)
        if (state.hasProperty(BlockStateProperties.WATERLOGGED) && level.getFluidState(pos).is(FluidTags.WATER)) {
            state = state.setValue(BlockStateProperties.WATERLOGGED, true);
        }
        level.setBlock(pos, state, Block.UPDATE_CLIENTS);
        placed++;
    }

    private BlockState log(Block block, Direction.Axis axis) {
        return block.defaultBlockState().setValue(RotatedPillarBlock.AXIS, axis);
    }

    private BlockState dusted(Block block) {
        return block.defaultBlockState().setValue(BlockStateProperties.DUSTED, random.nextInt(4));
    }

    // ------------------------------------------------------------------ Teile

    /** Sandduene unter und um den Rumpf, der Rumpf liegt halb darin. */
    private void dune() {
        for (int u = -HALF - 3; u <= HALF + 3; u++) {
            for (int v = -6; v <= 6; v++) {
                double e = Math.pow(u / (HALF + 3.5), 2) + Math.pow(v / 6.5, 2);
                if (e > 1) continue;
                put(at(u, v, -1), random.nextInt(7) == 0 ? Blocks.GRAVEL.defaultBlockState() : Blocks.SAND.defaultBlockState());
                boolean insideHull = Math.abs(v) <= halfWidth(u) && Math.abs(u) <= HALF;
                if (!insideHull && e > 0.3 && random.nextInt(3) > 0) put(at(u, v, 0), Blocks.SAND.defaultBlockState());
                if (!insideHull && e > 0.6 && random.nextInt(4) == 0) put(at(u, v, 1), Blocks.SAND.defaultBlockState());
            }
        }
    }

    /** Kiel, Boden, Bordwaende, Spanten, Steven, Deckbalken und Mastrest. Vieles fehlt, das Schiff ist zerfallen. */
    private void hull() {
        BlockState floor = Blocks.DARK_OAK_PLANKS.defaultBlockState();
        BlockState wall = Blocks.SPRUCE_PLANKS.defaultBlockState();
        BlockState rib = Blocks.SPRUCE_FENCE.defaultBlockState();
        for (int u = -HALF; u <= HALF; u++) {
            int w = halfWidth(u), rise = wallHeight(u);
            // Kiel in der Mitte, fast ganz erhalten
            if (random.nextInt(12) > 0) put(at(u, 0, 0), log(Blocks.STRIPPED_DARK_OAK_LOG, keelAxis()));
            // Boden: ein Viertel fehlt
            for (int v = -w + 1; v <= w - 1; v++) {
                if (v != 0 && random.nextInt(4) > 0) put(at(u, v, 0), floor);
            }
            if (w < 1) continue;
            // Bordwaende: Backbord (v>0) halb zerfallen, Steuerbord (v<0) staerker, oben fehlt mehr
            for (int h = 1; h <= rise; h++) {
                double missPort = 0.12 + 0.10 * h, missStarboard = 0.30 + 0.12 * h;
                if (random.nextDouble() > missPort) put(at(u, w, h), wall);
                if (random.nextDouble() > missStarboard) put(at(u, -w, h), wall);
            }
            // Spanten: alle zwei Bloecke ragt ein Rippenbogen ueber die Bordwand hinaus
            if (Math.floorMod(u, 2) == 0) {
                if (random.nextInt(5) > 0) put(at(u, w, rise + 1), rib);
                if (random.nextInt(3) > 0) put(at(u, -w, rise + 1), rib);
            }
        }
        // Deckbalken quer, an zwei Stellen, teilweise abgebrochen
        for (int u : new int[] { -4, 4 }) {
            int w = halfWidth(u), h = Math.min(2, wallHeight(u));
            for (int v = -w; v <= w; v++) if (random.nextInt(2) == 0) put(at(u, v, h), log(Blocks.STRIPPED_OAK_LOG, crossAxis()));
        }
        // Vordersteven (Bug) hoch, Achtersteven (Heck) abgebrochen
        for (int h = 0; h <= 5; h++) put(at(-HALF, 0, h), log(Blocks.DARK_OAK_LOG, Direction.Axis.Y));
        for (int h = 0; h <= 4; h++) {
            if (h < 4 || random.nextBoolean()) put(at(HALF, 0, h), log(Blocks.DARK_OAK_LOG, Direction.Axis.Y));
        }
        put(at(-HALF, 0, 6), rib);                       // Spitze des Stevens
        // Mastrest mit Splittern
        for (int h = 1; h <= 3; h++) put(at(-3, 0, h), log(Blocks.STRIPPED_OAK_LOG, Direction.Axis.Y));
        put(at(-3, 0, 4), rib);
        put(at(-3, 1, 1), log(Blocks.STRIPPED_OAK_LOG, crossAxis()));   // Mastfisch
    }

    /** Eine bunte Mischung: heile, liegende, halbe, schiefe und zerscherbte Amphoren in zufaelliger Blickrichtung. */
    private BlockState anyAmphora() {
        Block b = switch (random.nextInt(10)) {
            case 0, 1 -> ModBlocks.AMPHORA.get();
            case 2 -> ModBlocks.AMPHORA_LYING.get();
            case 3 -> ModBlocks.AMPHORA_LYING_BROKEN.get();
            case 4 -> ModBlocks.AMPHORA_BROKEN_NECK.get();
            case 5 -> ModBlocks.AMPHORA_HALF.get();
            case 6 -> ModBlocks.AMPHORA_TILTED.get();
            default -> ModBlocks.AMPHORA_SHARDS.get();
        };
        BlockState s = b.defaultBlockState();
        if (s.hasProperty(BrokenAmphoraBlock.FACING)) {
            s = s.setValue(BrokenAmphoraBlock.FACING, Direction.Plane.HORIZONTAL.getRandomDirection(random));
        }
        return s;
    }

    /** Ladung: bunt gemischte Amphoren im Laderaum, dazwischen Scherbenhaufen. */
    private void cargo() {
        int amphorae = 0, tries = 0;
        while (amphorae < 14 && tries++ < 300) {
            int u = -7 + random.nextInt(15), v = random.nextInt(7) - 3;
            if (Math.abs(v) >= halfWidth(u) || Math.abs(v) < 0) continue;
            if (u == -3 && v == 0) continue;               // Mast
            put(at(u, v, 1), anyAmphora());
            amphorae++;
        }
        for (int i = 0; i < 3; i++) {
            int u = -5 + random.nextInt(11), v = random.nextInt(3) - 1;
            put(at(u, v, 1), ModBlocks.SHARD_PILE.get().defaultBlockState());
        }
    }

    /** Rund um das Wrack: Amphorenhaufen am Heck, verstreute Scherbenhaufen und Fundboeden. */
    private void surroundings() {
        // Amphorenhaufen am Heck wie auf dem Meeresboden: Scherbenhaufen als Grund, Amphoren darauf
        for (int u = HALF + 2; u <= HALF + 4; u++) {
            for (int v = -2; v <= 2; v++) {
                if (random.nextInt(3) == 0) continue;
                put(at(u, v, 0), ModBlocks.SHARD_PILE.get().defaultBlockState());
                if (random.nextInt(3) > 0) put(at(u, v, 1), anyAmphora());
            }
        }
        // Verstreute Funde im Sand ringsum (Suspicious Sand ueberwiegt, daneben Mud und Ashes)
        for (int i = 0; i < 16; i++) {
            int u = -HALF - 2 + random.nextInt(2 * HALF + 5), v = (random.nextBoolean() ? 1 : -1) * (halfWidth(u) + 1 + random.nextInt(3));
            Block b = switch (random.nextInt(8)) {
                case 0, 1 -> ModBlocks.SUSPICIOUS_MUD.get();
                case 2 -> ModBlocks.SUSPICIOUS_ASHES.get();
                case 3 -> ModBlocks.SHARD_PILE.get();
                default -> ModBlocks.SUSPICIOUS_SAND.get();
            };
            put(at(u, v, 0), b.defaultBlockState().hasProperty(BlockStateProperties.DUSTED) ? dusted(b) : b.defaultBlockState());
        }
    }
}
