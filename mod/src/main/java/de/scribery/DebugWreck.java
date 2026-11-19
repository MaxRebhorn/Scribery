package de.scribery;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
 * Ein antikes Handelsschiff als Wrack, zum Testen der Amphoren und Loot-Tabellen (Debug-Befehl /scribery_wreck). Gebaut
 * nach zwei Vorbildern: einem fast ganz im Sand versunkenen Holzschiff (lange, schmale Rumpfform, Kiel, Spanten, die aus
 * dem Boden ragen, Bug und Heck hochgezogen) und einem Amphorenhaufen auf dem Meeresboden.
 *
 * <p>Der Rumpf ist 21 Bloecke lang und bis 7 breit, liegt auf einer Sandduene und ist teilweise zerfallen. Es gibt nur
 * Amphoren (heile und zerbrochene) und, wo sich Amphoren stapeln wuerden, einen Scherbenhaufen darunter. Unter Wasser
 * werden alle Bloecke mit Wasser gefuellt (waterlogged), damit keine Luftblasen entstehen.
 *
 * <p>Zwei Lagen: aufrecht (Kiel unten, Amphoren im Laderaum) oder auf der Seite liegend (um 90 Grad um den Kiel gekippt,
 * die Ladung ist aus dem offenen Rumpf auf den Boden daneben gerutscht).
 */
public final class DebugWreck {
    private static final int HALF = 10;   // halbe Rumpflaenge

    private final LevelAccessor level;
    private final BlockPos origin;        // Kielhoehe in der Mitte des Rumpfes
    private final boolean alongX;
    private final boolean side;           // auf der Seite liegend
    private final int center;             // Mitte der Duene quer zur Laengsachse (aufrecht 0, auf der Seite 5)
    private final RandomSource random;
    private final int load;               // Ladung 1 (eine Amphore) bis 10 (komplett voll)
    private int placed, amphorae;
    private de.scribery.core.TerrainShift fit;   // null = starr, sonst folgt das Schiff dem Boden

    /** Ergebnis: gesetzte Bloecke insgesamt und davon Amphoren (alle Varianten). */
    public record Result(int blocks, int amphorae) {}

    private DebugWreck(LevelAccessor level, BlockPos origin, Direction facing, int load, boolean onSide) {
        this.level = level;
        this.origin = origin;
        this.alongX = facing.getAxis() == Direction.Axis.X;
        this.side = onSide;
        this.center = onSide ? 5 : 0;
        this.random = RandomSource.create(origin.asLong());
        this.load = Math.max(1, Math.min(10, load));
    }

    /**
     * Baut das Wrack.
     *
     * @param origin Kielhoehe in der Mitte
     * @param load   Ladung von 1 (so wenig wie moeglich: eine Amphore) bis 10 (Laderaum komplett voll, teils gestapelt,
     *               dazu ein grosser Haufen am Heck)
     * @param onSide {@code true}: das Schiff liegt auf der Seite, sonst aufrecht
     */
    public static Result build(LevelAccessor level, BlockPos origin, Direction facing, int load, boolean onSide) {
        return build(level, origin, facing, load, onSide, null);
    }

    /**
     * Wie {@link #build(LevelAccessor, BlockPos, Direction, int, boolean)}, aber das Schiff folgt dem Boden: {@code ground} liefert die
     * Hoehe des obersten festen Blocks an (x, z). Jeder Abschnitt wird bis zu 4 Bloecke nach oben oder unten verschoben, laengs
     * und quer, sanft geglaettet. Ohne {@code ground} liegt das Schiff starr wie bisher.
     */
    public static Result build(LevelAccessor level, BlockPos origin, Direction facing, int load, boolean onSide,
            java.util.function.IntBinaryOperator ground) {
        DebugWreck w = new DebugWreck(level, origin, facing, load, onSide);
        if (ground != null) {
            w.fit = new de.scribery.core.TerrainShift(-16, 16, -12, 12, 4, 4, 1, origin.getY(),
                    (u, v) -> w.alongX ? ground.applyAsInt(origin.getX() + u, origin.getZ() + v) : ground.applyAsInt(origin.getX() + v, origin.getZ() + u));
        }
        w.dune();
        w.hull();
        w.cargo();
        w.surroundings();
        return new Result(w.placed, w.amphorae);
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

    /** Weltposition: u laengs, lat quer, h hoch, vom Kielpunkt aus. */
    private BlockPos world(int u, int lat, int h) {
        BlockPos p = alongX ? origin.offset(u, h, lat) : origin.offset(lat, h, u);
        return fit == null ? p : p.above(fit.shift(u, lat));
    }

    /**
     * Position im Schiff: u laengs, v quer zum Schiff, h ueber dem Kiel. Aufrecht ist das die Weltposition. Auf der Seite
     * liegend ist das Schiff um 90 Grad um den Kiel gekippt: oben (h) zeigt zur Seite, und die Seite mit v = -3 liegt
     * unten auf dem Boden.
     */
    private BlockPos at(int u, int v, int h) {
        // Jeder Querschnitt sitzt mit seiner untersten Kante (v = -halbe Breite) buendig auf dem Boden, damit nichts schwebt;
        // an den schmalen Enden liegt das Schiff dadurch flach auf, in der Mitte steht der Kiel hoeher.
        return side ? world(u, h, v + halfWidth(u)) : world(u, v, h);
    }

    /** Beim Schiff auf der Seite ist die obere Haelfte (v > 0) abgebrochen und als Truemmer zusammengefallen. */
    private boolean kept(int v) {
        return !side || v <= 0;
    }

    /** Position im Gelaende rund um das Wrack, quer zur Duenenmitte. */
    private BlockPos ground(int u, int lat, int h) {
        return world(u, lat + center, h);
    }

    private Direction.Axis keelAxis() {
        return alongX ? Direction.Axis.X : Direction.Axis.Z;
    }

    private Direction.Axis crossAxis() {
        return alongX ? Direction.Axis.Z : Direction.Axis.X;
    }

    private void put(BlockPos pos, BlockState state) {
        placed++;
        if (!StructureBuilder.inClip(pos)) return;  // Weltgenerierung: ausserhalb des Chunks nicht lesen oder schreiben
        // Unter Wasser mit Wasser fuellen, wo der Block es kann (Zaeune, Platten)
        if (state.hasProperty(BlockStateProperties.WATERLOGGED) && level.getFluidState(pos).is(FluidTags.WATER)) {
            state = state.setValue(BlockStateProperties.WATERLOGGED, true);
        }
        level.setBlock(pos, state, Block.UPDATE_CLIENTS);
    }

    /** Stamm in Richtung des Schiffs: beim gekippten Schiff werden senkrechte Staemme waagerecht und umgekehrt. */
    private BlockState log(Block block, Direction.Axis axis) {
        if (side) {
            if (axis == Direction.Axis.Y) axis = crossAxis();
            else if (axis == crossAxis()) axis = Direction.Axis.Y;
        }
        return block.defaultBlockState().setValue(RotatedPillarBlock.AXIS, axis);
    }

    // ------------------------------------------------------------------ Teile

    /** Sandduene unter und um den Rumpf, der Rumpf liegt halb darin (der Rumpf ueberschreibt sie danach). */
    private void dune() {
        int lat = side ? 8 : 6;
        for (int u = -HALF - 3; u <= HALF + 3; u++) {
            for (int v = -lat; v <= lat; v++) {
                double e = Math.pow(u / (HALF + 3.5), 2) + Math.pow(v / (lat + 0.5), 2);
                if (e > 1) continue;
                put(ground(u, v, -1), random.nextInt(7) == 0 ? Blocks.GRAVEL.defaultBlockState() : Blocks.SAND.defaultBlockState());
                if (fit != null) support(u, v);
                boolean insideHull = !side && Math.abs(v) <= halfWidth(u) && Math.abs(u) <= HALF;
                if (!insideHull && e > 0.3 && random.nextInt(3) > 0) put(ground(u, v, 0), Blocks.SAND.defaultBlockState());
                if (!insideHull && e > 0.6 && random.nextInt(4) == 0) put(ground(u, v, 1), Blocks.SAND.defaultBlockState());
            }
        }
    }

    /** Wo der Boden unter der verschobenen Duene tiefer liegt, wird mit Sand nach unten aufgefuellt (hoechstens 5 Bloecke), damit nichts schwebt. */
    private void support(int u, int v) {
        for (int k = 2; k <= 6; k++) {
            BlockPos p = ground(u, v, -k);
            if (!StructureBuilder.inClip(p)) return;
            BlockState s = level.getBlockState(p);
            if (!s.isAir() && !s.getFluidState().is(FluidTags.WATER)) return;
            level.setBlock(p, Blocks.SAND.defaultBlockState(), Block.UPDATE_CLIENTS);
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
                if (v != 0 && kept(v) && random.nextInt(4) > 0) put(at(u, v, 0), floor);
            }
            if (w < 1) continue;
            // Bordwaende: Backbord (v>0) halb zerfallen, Steuerbord (v<0) staerker, oben fehlt mehr
            for (int h = 1; h <= rise; h++) {
                double missPort = 0.12 + 0.10 * h, missStarboard = 0.30 + 0.12 * h;
                if (kept(w) && random.nextDouble() > missPort) put(at(u, w, h), wall);
                if (random.nextDouble() > missStarboard) put(at(u, -w, h), wall);
            }
            // Spanten: alle zwei Bloecke ragt ein Rippenbogen ueber die Bordwand hinaus
            if (Math.floorMod(u, 2) == 0) {
                if (kept(w) && random.nextInt(5) > 0) put(at(u, w, rise + 1), rib);
                if (random.nextInt(3) > 0) put(at(u, -w, rise + 1), rib);
            }
        }
        // Deckbalken quer, an zwei Stellen, teilweise abgebrochen
        for (int u : new int[] { -4, 4 }) {
            int w = halfWidth(u), h = Math.min(2, wallHeight(u));
            for (int v = -w; v <= w; v++) if (kept(v) && random.nextInt(2) == 0) put(at(u, v, h), log(Blocks.STRIPPED_OAK_LOG, crossAxis()));
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
        if (kept(1)) put(at(-3, 1, 1), log(Blocks.STRIPPED_OAK_LOG, crossAxis()));   // Mastfisch
        if (side) debris();
    }

    /**
     * Beim Schiff auf der Seite ist die obere Haelfte zusammengefallen: verstreute Bretter und Spantenreste liegen flach
     * auf der anderen Seite des Kiels, vereinzelt zwei Lagen hoch.
     */
    private void debris() {
        BlockState plank = Blocks.SPRUCE_PLANKS.defaultBlockState();
        BlockState rib = Blocks.SPRUCE_FENCE.defaultBlockState();
        for (int i = 0; i < 26; i++) {
            int u = -HALF + 1 + random.nextInt(2 * HALF - 1), lat = -1 - random.nextInt(4);
            put(world(u, lat, 0), random.nextInt(4) == 0 ? rib : plank);
            if (random.nextInt(5) == 0) put(world(u, lat, 1), random.nextBoolean() ? rib : plank);
        }
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
        amphorae++;
        return s;
    }

    /** Position einer Ladungsstelle in der Lage {@code layer} (0 = unten, 1 = darauf). */
    private BlockPos cargoPos(int[] cell, int layer) {
        // aufrecht: im Laderaum ueber dem Boden; auf der Seite: auf der Oberflaeche neben und auf dem flach liegenden Rumpf.
        // Beides liegt eine Lage ueber der Gelaendekante (H = 0 ist der Rumpf und der oberste Gelaendeblock), sonst sitzen die
        // Amphoren im Boden.
        return side ? world(cell[0], cell[1], 1 + layer) : at(cell[0], cell[1], 1 + layer);
    }

    /** Unter einer Ladungsstelle darf kein Loch sein (fehlendes Brett, Luft, Wasser): dort kommt Sand als Unterlage hin. */
    private void ensureSupport(BlockPos pos) {
        BlockPos below = pos.below();
        if (!StructureBuilder.inClip(below) || level.getBlockState(below).canBeReplaced()) put(below, Blocks.SAND.defaultBlockState());
    }

    /**
     * Ladung nach {@code load}: Stufe 1 hat genau eine Amphore, Stufe 10 fuellt jede freie Stelle. Aufrecht liegt die Ladung im
     * Laderaum, auf der Seite ist sie aus dem offenen Rumpf auf den Boden daneben gerutscht. Ab Stufe 7 liegt teils eine
     * zweite Lage darauf; Amphoren stapeln sich aber nie direkt, darunter ist immer ein Scherbenhaufen.
     */
    private void cargo() {
        List<int[]> cells = new ArrayList<>();
        if (side) {
            // Von der Rumpfkante (1) bis weit daneben (13); nahe am Rumpf liegt viel, nach aussen wird es duenn gestreut
            for (int u = -HALF + 2; u <= HALF - 2; u++) {
                for (int lat = 1; lat <= 13; lat++) cells.add(new int[] { u, lat });
            }
        } else {
            for (int u = -HALF + 2; u <= HALF - 2; u++) {
                int w = halfWidth(u);
                for (int v = -(w - 1); v <= w - 1; v++) {
                    if (u == -3 && v == 0) continue;           // Mast
                    cells.add(new int[] { u, v });
                }
            }
        }
        java.util.Random rnd = new java.util.Random(origin.asLong());
        if (side) {
            // Gewichtete Auswahl ohne Zuruecklegen: Stellen nahe am Rumpf kommen zuerst, ferne selten
            final double[] key = new double[cells.size()];
            for (int i = 0; i < cells.size(); i++) {
                double weight = Math.exp(-(cells.get(i)[1] - 1) / 4.5);
                key[i] = Math.pow(rnd.nextDouble(), 1.0 / weight);
            }
            Integer[] order = new Integer[cells.size()];
            for (int i = 0; i < order.length; i++) order[i] = i;
            java.util.Arrays.sort(order, (a, b) -> Double.compare(key[b], key[a]));
            List<int[]> sorted = new ArrayList<>();
            for (int i : order) sorted.add(cells.get(i));
            cells = sorted;
        } else {
            Collections.shuffle(cells, rnd);
        }
        // Auf der Seite verteilt sich die Ladung ueber ein grosses Feld; die Menge richtet sich nach einem festen Fassungsvermoegen
        int capacity = side ? 60 : cells.size();
        // Ab Stufe 7 werden einige Stellen statt mit Amphoren mit Sand und Scherbenhaufen gefuellt (bei Stufe 10 ein Fuenftel),
        // teils mit einer duennen Sandschicht darueber, damit die volle Ladung nicht monoton aussieht
        double fillerShare = load >= 7 ? 0.2 * (load - 6) / 4.0 : 0;
        int total = load <= 1 ? 1 : (int) Math.round(capacity * (load - 1) / 9.0);
        int filler = (int) Math.round(capacity * fillerShare);
        int count = Math.max(1, total - filler);
        for (int i = count; i < count + filler && i < cells.size(); i++) {
            int[] c = cells.get(i);
            ensureSupport(cargoPos(c, 0));
            put(cargoPos(c, 0), random.nextInt(100) < 55 ? Blocks.SAND.defaultBlockState() : ModBlocks.SHARD_PILE.get().defaultBlockState());
            if (random.nextInt(100) < 35) put(cargoPos(c, 1), Blocks.SAND.defaultBlockState());
        }
        for (int i = 0; i < count && i < cells.size(); i++) {
            int[] c = cells.get(i);
            ensureSupport(cargoPos(c, 0));
            boolean stacked = load >= 7 && random.nextFloat() < (load - 6) / 4f;
            if (stacked) {
                // Amphoren stapeln sich nicht: wo eine auf einer anderen liegen wuerde, ist die untere nur ein Scherbenhaufen
                put(cargoPos(c, 0), ModBlocks.SHARD_PILE.get().defaultBlockState());
                put(cargoPos(c, 1), anyAmphora());
            } else {
                put(cargoPos(c, 0), anyAmphora());
            }
        }
    }

    /** Rund um das Wrack nur Amphoren: ein Haufen am Heck wie auf dem Meeresboden (Scherbenhaufen als Grund), waechst mit der Ladung. */
    private void surroundings() {
        if (load < 3) return;
        double fill = load / 10.0;
        for (int u = HALF + 2; u <= HALF + 4; u++) {
            for (int v = -2; v <= 2; v++) {
                if (random.nextDouble() > fill) continue;
                put(ground(u, v, 0), ModBlocks.SHARD_PILE.get().defaultBlockState());
                if (random.nextDouble() < 0.4 + fill * 0.6) put(ground(u, v, 1), anyAmphora());
            }
        }
    }
}
