package de.scribery;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * Das einzige Stueck einer Struktur des Mods. Es baut bei jedem Chunk die ganze Struktur neu, schreibt aber nur in den
 * Bereich des Chunks (StructureBuilder.CLIP). Weil alle Zufallszahlen aus Ursprung und Seed kommen, ergibt jeder Durchlauf
 * dasselbe Bauwerk, und die Teile in den Nachbarchunks passen aneinander.
 */
public class ScriberyPiece extends StructurePiece {
    private final ScriberyStructure.Kind kind;
    private final BlockPos origin;
    private final Direction facing;
    private final long seed;

    public ScriberyPiece(ScriberyStructure.Kind kind, BlockPos origin, Direction facing, long seed, BoundingBox box) {
        super(ModStructures.PIECE.get(), 0, box);
        this.kind = kind;
        this.origin = origin;
        this.facing = facing;
        this.seed = seed;
        setOrientation(facing);
    }

    public ScriberyPiece(CompoundTag tag) {
        super(ModStructures.PIECE.get(), tag);
        this.kind = ScriberyStructure.Kind.CODEC.parse(net.minecraft.nbt.NbtOps.INSTANCE, tag.get("Kind")).result().orElse(ScriberyStructure.Kind.CATACOMB);
        this.origin = new BlockPos(tag.getInt("OX"), tag.getInt("OY"), tag.getInt("OZ"));
        this.facing = Direction.from3DDataValue(tag.getInt("Facing"));
        this.seed = tag.getLong("Seed");
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.put("Kind", ScriberyStructure.Kind.CODEC.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, kind).result().orElseThrow());
        tag.putInt("OX", origin.getX());
        tag.putInt("OY", origin.getY());
        tag.putInt("OZ", origin.getZ());
        tag.putInt("Facing", facing.get3DDataValue());
        tag.putLong("Seed", seed);
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
                            BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
        StructureBuilder.CLIP.set(box);
        try {
            switch (kind) {
                case WRECK -> {
                    RandomSource r = RandomSource.create(seed);
                    DebugWreck.build(level, origin, facing, 2 + r.nextInt(8), r.nextInt(4) == 0, ground(level, generator));
                }
                case CARAVAN -> DebugStructures.frozenCaravan(level, origin, facing);
                case WANDERER -> DebugStructures.frozenWanderer(level, origin, facing);
                case GRAVE -> DebugStructures.soldierGrave(level, origin, facing);
                case CATACOMB -> DebugStructures.catacombChurch(level, origin, facing, false);
                case CATACOMB_DESERT -> DebugStructures.catacombChurch(level, origin, facing, true);
                case HOUSE_SAND -> DebugStructures.buriedHouse(level, origin, facing, false, ground(level, generator));
                case HOUSE_MUD -> DebugStructures.buriedHouse(level, origin, facing, true, ground(level, generator));
            }
        } finally {
            StructureBuilder.CLIP.remove();
        }
    }

    /** Bodenhoehe (oberster fester Block) an (x, z) aus dem Gelaende-Generator: fuer alle Chunks gleich, ohne Nachbar-Chunks zu lesen. */
    private static final java.util.Map<Long, Integer> HEIGHTS = new java.util.LinkedHashMap<>(256, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(java.util.Map.Entry<Long, Integer> e) {
            return size() > 2048;
        }
    };

    private static java.util.function.IntBinaryOperator ground(WorldGenLevel level, ChunkGenerator generator) {
        var randomState = level.getLevel().getChunkSource().randomState();
        return (x, z) -> {
            long key = ((long) x << 32) ^ (z & 0xffffffffL);
            synchronized (HEIGHTS) {
                Integer c = HEIGHTS.get(key);
                if (c != null) return c;
            }
            int h = generator.getBaseHeight(x, z, net.minecraft.world.level.levelgen.Heightmap.Types.OCEAN_FLOOR_WG, level, randomState) - 1;
            synchronized (HEIGHTS) {
                HEIGHTS.put(key, h);
            }
            return h;
        };
    }
}
