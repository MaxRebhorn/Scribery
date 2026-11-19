package de.scribery;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.FrontAndTop;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.JigsawBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.entity.JigsawBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

/**
 * Nur zum Bauen der Dorf-Vorlage: baut das Schreiberhaus (Pult, Presse mit Lettern-Schrank, Buecherregale, Bett) im Testserver
 * und speichert es als Jigsaw-Vorlage unter data/scribery/structure/. Aufruf: {@code -Pwgtest=house:plains|taiga|snowy}.
 * Das Haus haengt ueber den Jigsaw "minecraft:building_entrance" an die Dorfstrasse, wie die Vanilla-Haeuser.
 */
final class DebugVillageHouse {
    /** Groesse der Vorlage: Breite (x), Hoehe (y), Tiefe (z). Vorderseite (Strasse) ist z = 0, y = 0 ist der Boden der Strasse. */
    static final Vec3i SIZE = new Vec3i(9, 10, 11);

    private DebugVillageHouse() {}

    /** Baustoffe je Dorftyp. */
    record Style(String name, Block planks, Block log, Block stairs, Block slab, Block door, Block fence, String villagersPool) {
        static Style of(String name) {
            return switch (name) {
                case "taiga" -> new Style(name, Blocks.SPRUCE_PLANKS, Blocks.SPRUCE_LOG, Blocks.SPRUCE_STAIRS, Blocks.SPRUCE_SLAB, Blocks.SPRUCE_DOOR,
                        Blocks.SPRUCE_FENCE, "minecraft:village/taiga/villagers");
                case "snowy" -> new Style(name, Blocks.SPRUCE_PLANKS, Blocks.SPRUCE_LOG, Blocks.SPRUCE_STAIRS, Blocks.SPRUCE_SLAB, Blocks.SPRUCE_DOOR,
                        Blocks.SPRUCE_FENCE, "minecraft:village/snowy/villagers");
                default -> new Style("plains", Blocks.OAK_PLANKS, Blocks.OAK_LOG, Blocks.OAK_STAIRS, Blocks.OAK_SLAB, Blocks.OAK_DOOR,
                        Blocks.OAK_FENCE, "minecraft:village/plains/villagers");
            };
        }
    }

    private static ServerLevel level;
    private static BlockPos o;

    private static void put(int x, int y, int z, BlockState s) {
        level.setBlock(o.offset(x, y, z), s, 2);
    }

    private static void put(int x, int y, int z, Block b) {
        put(x, y, z, b.defaultBlockState());
    }

    private static void fill(int x1, int y1, int z1, int x2, int y2, int z2, Block b) {
        for (int x = x1; x <= x2; x++) for (int y = y1; y <= y2; y++) for (int z = z1; z <= z2; z++) put(x, y, z, b);
    }

    private static BlockState facing(Block b, Direction d) {
        return b.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, d);
    }

    private static BlockState stair(Block b, Direction ascending) {
        return b.defaultBlockState().setValue(StairBlock.FACING, ascending).setValue(StairBlock.HALF, Half.BOTTOM);
    }

    /** Baut das Haus bei {@code origin} (kleinste Ecke) und speichert es als Vorlage. */
    static void buildAndSave(ServerLevel lvl, BlockPos origin, String styleName) throws IOException {
        level = lvl;
        o = origin;
        Style st = Style.of(styleName);
        // 1. Alles ausserhalb des Hauses bleibt unberuehrt (Strukturleere), das Innere wird als Luft gesetzt
        fill(0, 0, 0, SIZE.getX() - 1, SIZE.getY() - 1, SIZE.getZ() - 1, Blocks.STRUCTURE_VOID);
        fill(2, 2, 3, 6, 4, 8, Blocks.AIR);                    // Innenraum
        fill(3, 2, 1, 5, 4, 1, Blocks.AIR);                    // Vorbau

        // 2. Fundament und Boden
        fill(1, 0, 1, 7, 0, 9, Blocks.DIRT);
        fill(2, 1, 3, 6, 1, 8, st.planks);                     // Fussboden
        for (int x = 1; x <= 7; x++) { put(x, 1, 2, Blocks.COBBLESTONE); put(x, 1, 9, Blocks.COBBLESTONE); }
        for (int z = 2; z <= 9; z++) { put(1, 1, z, Blocks.COBBLESTONE); put(7, 1, z, Blocks.COBBLESTONE); }
        // 3. Waende: Planken, an den Ecken Staemme
        for (int y = 2; y <= 4; y++) {
            for (int x = 1; x <= 7; x++) { put(x, y, 2, st.planks); put(x, y, 9, st.planks); }
            for (int z = 2; z <= 9; z++) { put(1, y, z, st.planks); put(7, y, z, st.planks); }
        }
        BlockState pillar = st.log.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
        for (int y = 1; y <= 4; y++) { put(1, y, 2, pillar); put(7, y, 2, pillar); put(1, y, 9, pillar); put(7, y, 9, pillar); }
        // 4. Fenster (Glasscheiben)
        for (int[] w : new int[][] {{1, 4}, {1, 7}, {7, 4}, {7, 7}}) put(w[0], 3, w[1], Blocks.GLASS_PANE);
        put(2, 3, 2, Blocks.GLASS_PANE); put(6, 3, 2, Blocks.GLASS_PANE); put(4, 3, 9, Blocks.GLASS_PANE);
        // 5. Tuer, Stufe und Vorbau mit Laternen
        BlockState lower = st.door.defaultBlockState().setValue(DoorBlock.FACING, Direction.NORTH).setValue(DoorBlock.HINGE, DoorHingeSide.LEFT)
                .setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER);
        put(4, 2, 2, lower);
        put(4, 3, 2, lower.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
        put(4, 1, 1, stair(st.stairs, Direction.SOUTH));
        put(3, 1, 1, Blocks.COBBLESTONE); put(5, 1, 1, Blocks.COBBLESTONE);
        put(3, 2, 1, st.fence); put(5, 2, 1, st.fence);
        put(3, 3, 1, Blocks.LANTERN); put(5, 3, 1, Blocks.LANTERN);
        // 6. Dach: ein Giebeldach laengs, Stufen aussen, Planken innen
        for (int z = 1; z <= 10; z++) {
            put(0, 5, z, stair(st.stairs, Direction.EAST)); put(8, 5, z, stair(st.stairs, Direction.WEST));
            fill(1, 5, z, 7, 5, z, st.planks);
            put(1, 6, z, stair(st.stairs, Direction.EAST)); put(7, 6, z, stair(st.stairs, Direction.WEST));
            fill(2, 6, z, 6, 6, z, st.planks);
            put(2, 7, z, stair(st.stairs, Direction.EAST)); put(6, 7, z, stair(st.stairs, Direction.WEST));
            fill(3, 7, z, 5, 7, z, st.planks);
            put(3, 8, z, stair(st.stairs, Direction.EAST)); put(5, 8, z, stair(st.stairs, Direction.WEST));
            put(4, 8, z, st.slab);
        }
        // 7. Einrichtung (Vorderseite = Norden, die Strasse)
        put(2, 2, 7, facing(ModBlocks.SCRIBE_LECTERN.get(), Direction.EAST));             // Pult an der Westwand, Vorderseite zum Raum
        for (int x = 2; x <= 5; x++) for (int y = 2; y <= 3; y++) put(x, y, 8, facing(ModBlocks.BOOKSHELF.get(), Direction.NORTH));   // Buecherregal an der Rueckwand
        put(6, 2, 5, facing(ModBlocks.PRINTING_PRESS.get(), Direction.WEST));            // Presse an der Ostwand
        put(6, 2, 4, facing(ModBlocks.LETTER_CABINET.get(), Direction.WEST));            // Lettern-Schrank daneben
        BlockState bed = Blocks.RED_BED.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH);
        put(6, 2, 7, bed.setValue(BlockStateProperties.BED_PART, BedPart.FOOT));
        put(6, 2, 8, bed.setValue(BlockStateProperties.BED_PART, BedPart.HEAD));
        fill(3, 2, 4, 4, 2, 5, Blocks.RED_CARPET);                                        // Teppich in der Mitte
        put(4, 4, 5, Blocks.LANTERN.defaultBlockState().setValue(BlockStateProperties.HANGING, true));
        put(4, 4, 8, Blocks.WHITE_CANDLE.defaultBlockState().setValue(BlockStateProperties.LIT, true));
        put(2, 2, 3, Blocks.BARREL);
        put(6, 2, 3, Blocks.POTTED_CORNFLOWER);
        // 8. Jigsaws: Eingang zur Strasse und der Platz fuer einen Dorfbewohner
        jigsaw(4, 1, 0, FrontAndTop.NORTH_UP, "minecraft:building_entrance", "minecraft:building_entrance", "minecraft:empty",
                "minecraft:structure_void", JigsawBlockEntity.JointType.ALIGNED);
        jigsaw(4, 1, 6, FrontAndTop.UP_NORTH, "minecraft:bottom", "minecraft:bottom", st.villagersPool,
                BuiltInName.of(st.planks), JigsawBlockEntity.JointType.ROLLABLE);

        // Glasscheiben und Zaeune erst jetzt an ihre Nachbarn anpassen: sonst werden sie als "Strich" ohne Verbindung gespeichert
        for (int x = 0; x < SIZE.getX(); x++) for (int y = 0; y < SIZE.getY(); y++) for (int z = 0; z < SIZE.getZ(); z++) {
            BlockPos at = o.offset(x, y, z);
            BlockState bs = level.getBlockState(at);
            if (bs.getBlock() instanceof net.minecraft.world.level.block.IronBarsBlock || bs.getBlock() instanceof net.minecraft.world.level.block.FenceBlock) {
                level.setBlock(at, Block.updateFromNeighbourShapes(bs, level, at), 2);
            }
        }

        // 9. Speichern
        StructureTemplate t = new StructureTemplate();
        t.fillFromWorld(level, origin, SIZE, false, Blocks.STRUCTURE_VOID);
        t.setAuthor("scribery");
        CompoundTag tag = t.save(new CompoundTag());
        Path p = Path.of(System.getProperty("user.dir")).resolve("../mod/src/main/resources/data/scribery/structure/village_scribe_house_" + st.name + ".nbt").normalize();
        Files.createDirectories(p.getParent());
        NbtIo.writeCompressed(tag, p);
    }

    /** "minecraft:oak_planks" fuer einen Block. */
    private static final class BuiltInName {
        static String of(Block b) {
            return net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(b).toString();
        }
    }

    private static void jigsaw(int x, int y, int z, FrontAndTop orientation, String name, String target, String pool, String finalState,
            JigsawBlockEntity.JointType joint) {
        put(x, y, z, Blocks.JIGSAW.defaultBlockState().setValue(JigsawBlock.ORIENTATION, orientation));
        if (level.getBlockEntity(o.offset(x, y, z)) instanceof JigsawBlockEntity be) {
            be.setName(ResourceLocation.parse(name));
            be.setTarget(ResourceLocation.parse(target));
            be.setPool(ResourceKey.create(Registries.TEMPLATE_POOL, ResourceLocation.parse(pool)));
            be.setFinalState(finalState);
            be.setJoint(joint);
            be.setChanged();
        }
    }

    private static void unused(StructureTemplatePool p) {}
}
