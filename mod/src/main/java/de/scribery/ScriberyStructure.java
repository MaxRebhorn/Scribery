package de.scribery;

import java.util.Optional;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

/**
 * Eine Struktur des Mods in der Weltgenerierung. Welche (kind), steht in der JSON-Datei unter
 * data/scribery/worldgen/structure; Biome, Abstand und Haeufigkeit stehen dort und im structure_set (tools/gen_worldgen.py).
 * Gebaut wird mit denselben Generatoren wie bei den Debug-Befehlen ({@link DebugStructures}, {@link DebugWreck}).
 */
public class ScriberyStructure extends Structure {
    public enum Kind implements StringRepresentable {
        WRECK("wreck", 45), CARAVAN("caravan", 36), WANDERER("wanderer", 20), GRAVE("grave", 20),
        CATACOMB("catacomb", 50), CATACOMB_DESERT("catacomb_desert", 50), HOUSE_SAND("house_sand", 20), HOUSE_MUD("house_mud", 20);

        public static final com.mojang.serialization.Codec<Kind> CODEC = StringRepresentable.fromEnum(Kind::values);
        private final String id;
        /** Halbe Kantenlaenge des Bereichs, den die Struktur belegen darf (Bloecke). */
        final int radius;

        Kind(String id, int radius) {
            this.id = id;
            this.radius = radius;
        }

        @Override
        public String getSerializedName() {
            return id;
        }
    }

    public static final MapCodec<ScriberyStructure> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            settingsCodec(i), Kind.CODEC.fieldOf("kind").forGetter(s -> s.kind)).apply(i, ScriberyStructure::new));

    private final Kind kind;

    public ScriberyStructure(StructureSettings settings, Kind kind) {
        super(settings);
        this.kind = kind;
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext ctx) {
        ChunkPos cp = ctx.chunkPos();
        int x = cp.getMiddleBlockX(), z = cp.getMiddleBlockZ();
        int surface = ctx.chunkGenerator().getFirstOccupiedHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, ctx.heightAccessor(), ctx.randomState());
        int floor = ctx.chunkGenerator().getFirstOccupiedHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, ctx.heightAccessor(), ctx.randomState());
        int sea = ctx.chunkGenerator().getSeaLevel();
        if (kind == Kind.WRECK) {
            if (surface - floor < 4 || surface > sea + 1) return Optional.empty();   // Meer, Fluss oder Sumpf: mindestens 4 Bloecke Wasser, Wasserspiegel auf Meereshoehe
        } else if (surface != floor || floor < sea) {
            return Optional.empty();                                               // nur an Land, nicht im Wasser
        }
        Direction facing = Direction.Plane.HORIZONTAL.getRandomDirection(ctx.random());
        if (kind == Kind.CATACOMB || kind == Kind.CATACOMB_DESERT || kind == Kind.HOUSE_SAND || kind == Kind.HOUSE_MUD) {
            // Nur unter der Erde: Das Gelaende ueber den Raeumen darf nirgends tiefer liegen als am Eingang (hoechstens 1 Block),
            // sonst ragt die Decke an einem Hang aus dem Boden. Geprueft wird ueber die ganze Grundflaeche, nicht nur am Eingang.
            boolean catacomb = kind == Kind.CATACOMB || kind == Kind.CATACOMB_DESERT;
            int reach = catacomb ? 46 : 22, side = catacomb ? 16 : 12;
            int tolerance = catacomb ? 1 : 4;   // Haeuser folgen dem Gelaende (bis 4 Bloecke), Katakomben nicht
            Direction right = facing.getClockWise();
            for (int u = 0; u <= reach; u += 6) {
                for (int v = -side; v <= side; v += side / 2) {
                    int sx = x + facing.getStepX() * u + right.getStepX() * v, sz = z + facing.getStepZ() * u + right.getStepZ() * v;
                    int top = ctx.chunkGenerator().getFirstOccupiedHeight(sx, sz, Heightmap.Types.WORLD_SURFACE_WG, ctx.heightAccessor(), ctx.randomState());
                    int bottom = ctx.chunkGenerator().getFirstOccupiedHeight(sx, sz, Heightmap.Types.OCEAN_FLOOR_WG, ctx.heightAccessor(), ctx.randomState());
                    if (top != bottom || top < floor - tolerance) return Optional.empty();   // Wasser darueber oder Gelaende faellt zu stark ab
                }
            }
        }
        BlockPos origin = new BlockPos(x, floor - 1, z);                           // der oberste feste Block
        int r = kind.radius;
        BoundingBox box = new BoundingBox(x - r, Math.max(ctx.heightAccessor().getMinBuildHeight(), floor - 34), z - r,
                x + r, Math.min(ctx.heightAccessor().getMaxBuildHeight() - 1, floor + 26), z + r);
        long seed = ctx.random().nextLong();
        return Optional.of(new GenerationStub(origin,
                Either.left(builder -> builder.addPiece(new ScriberyPiece(kind, origin, facing, seed, box)))));
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.SCRIBERY.get();
    }
}
