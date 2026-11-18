package de.scribery;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Test-Generatoren fuer die besprochenen Strukturen (siehe docs/DESIGN-STRUCTURES.md), aufgerufen von den Debug-Befehlen in
 * ModCommands. Alle setzen die Struktur an die Stelle des Spielers, ausgerichtet nach seiner Blickrichtung. Sie sind Vorlagen
 * fuer die spaetere Welt-Erzeugung; Form und Inhalt (Amphoren, Eisbloecke mit Fragment, Suspicious-Boeden) sind zum Ausprobieren.
 */
final class DebugStructures {
    private DebugStructures() {}

    // ------------------------------------------------------------------ gemeinsame Teile

    private static BlockState amphora(StructureBuilder b, Block block) {
        BlockState s = block.defaultBlockState();
        if (s.hasProperty(BrokenAmphoraBlock.FACING)) {
            s = s.setValue(BrokenAmphoraBlock.FACING, Direction.Plane.HORIZONTAL.getRandomDirection(b.random));
        }
        return s;
    }

    private static BlockState anyAmphora(StructureBuilder b) {
        Block block = switch (b.random.nextInt(8)) {
            case 0, 1, 2 -> ModBlocks.AMPHORA.get();
            case 3 -> ModBlocks.AMPHORA_LYING.get();
            case 4 -> ModBlocks.AMPHORA_BROKEN_NECK.get();
            case 5 -> ModBlocks.AMPHORA_HALF.get();
            case 6 -> ModBlocks.AMPHORA_TILTED.get();
            default -> ModBlocks.AMPHORA_LYING_BROKEN.get();
        };
        return amphora(b, block);
    }

    private static BlockState dusted(StructureBuilder b, Block block) {
        return block.defaultBlockState().setValue(BlockStateProperties.DUSTED, 1 + b.random.nextInt(3));
    }

    private static BlockState snow(StructureBuilder b) {
        return Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, 1 + b.random.nextInt(3));
    }

    // ------------------------------------------------------------------ Eingefrorene Karawane (Taiga, Berge)

    /** Zwei Wagen im Eis, Ladung aus Amphoren, Faessern und Eisbloecken mit Fragment, dazu ein kaltes Lager. Etwa 28 lang. */
    static int frozenCaravan(Level level, BlockPos origin, Direction facing) {
        StructureBuilder b = new StructureBuilder(level, origin, facing);
        // Eisfeld unter der Karawane
        for (int u = -3; u <= 30; u++) {
            for (int v = -7; v <= 7; v++) {
                double e = Math.pow((u - 13.5) / 17.0, 2) + Math.pow(v / 7.5, 2);
                if (e > 1) continue;
                b.put(u, v, 0, b.chance(35) ? Blocks.PACKED_ICE : b.chance(30) ? Blocks.ICE : Blocks.SNOW_BLOCK);
            }
        }
        wagon(b, 3, true);
        wagon(b, 15, false);
        // Kaltes Lager: erloschenes Feuer, Baumstamm-Sitze, Decke, eingefrorene Amphore und ein Eisblock mit Fragment
        b.put(25, 0, 1, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false));
        b.put(23, -2, 1, b.log(Blocks.SPRUCE_LOG, b.axisU()));
        b.put(27, 2, 1, b.log(Blocks.SPRUCE_LOG, b.axisU()));
        b.put(25, 3, 1, b.log(Blocks.SPRUCE_LOG, b.axisV()));
        b.put(24, -2, 1, Blocks.GRAY_CARPET);
        b.put(22, 3, 1, amphora(b, ModBlocks.AMPHORA.get()));
        b.put(22, 3, 2, Blocks.ICE);
        b.put(21, 3, 1, Blocks.ICE);
        b.put(23, 3, 1, Blocks.ICE);
        b.put(26, -1, 1, ModBlocks.FROZEN_FRAGMENT_BLOCK.get());
        frost(b, -2, 30, -7, 7);
        return b.placed;
    }

    /** Ein Wagen: Boden, Bordbretter, Raeder und Deichsel. Der kaputte Wagen hat Luecken und ein flach liegendes Rad. */
    private static void wagon(StructureBuilder b, int u0, boolean intact) {
        int missing = intact ? 12 : 45;
        for (int u = u0; u <= u0 + 6; u++) {
            for (int v = -1; v <= 1; v++) if (!b.chance(missing / 2)) b.put(u, v, 1, Blocks.SPRUCE_PLANKS);
            for (int v : new int[] { -2, 2 }) if (!b.chance(missing)) b.put(u, v, 2, Blocks.SPRUCE_PLANKS);
        }
        for (int side : new int[] { -3, 3 }) {
            for (int wu : new int[] { u0 + 1, u0 + 5 }) {
                if (!intact && b.chance(50)) {
                    b.put(wu, side, 1, b.log(Blocks.SPRUCE_LOG, b.axisU()));   // Rad liegt flach
                    continue;
                }
                for (int du = 0; du <= 1; du++) for (int h = 1; h <= 2; h++) b.put(wu + du, side, h, b.log(Blocks.SPRUCE_LOG, b.axisV()));
            }
        }
        for (int u = u0 - 3; u < u0; u++) b.put(u, 0, 1, Blocks.SPRUCE_FENCE);   // Deichsel
        // Ladung
        for (int u = u0 + 1; u <= u0 + 5; u++) {
            for (int v = -1; v <= 1; v++) {
                if (b.chance(35)) continue;
                int roll = b.random.nextInt(10);
                if (roll < 5) b.put(b.at(u, v, 2), anyAmphora(b));
                else if (roll < 7) b.put(u, v, 2, ModBlocks.FROZEN_FRAGMENT_BLOCK.get());
                else if (roll < 9) b.put(u, v, 2, Blocks.BARREL);
                else b.put(u, v, 2, Blocks.WHITE_WOOL);
                if (b.chance(40)) b.put(u, v, 3, Blocks.ICE);
            }
        }
        // verstreutes Gut neben dem Wagen
        for (int i = 0; i < (intact ? 2 : 6); i++) b.put(b.at(u0 - 1 + b.random.nextInt(9), (b.chance(50) ? 1 : -1) * (3 + b.random.nextInt(3)), 1), anyAmphora(b));
    }

    /** Duenne Schneedecke auf freien Stellen ueber dem Boden. */
    private static void frost(StructureBuilder b, int u0, int u1, int v0, int v1) {
        for (int u = u0; u <= u1; u++) {
            for (int v = v0; v <= v1; v++) {
                BlockPos p = b.at(u, v, 1);
                if (b.level.getBlockState(p).isAir() && !b.level.getBlockState(p.below()).isAir() && b.chance(40)) b.put(p, snow(b));
            }
        }
    }

    // ------------------------------------------------------------------ Erfrorener Wanderer (Eisbiom)

    /** Die kleine Fassung: eine Spur im Schnee, ein eingeschneiter Wanderer mit Gepaeck, Eis und einem eingefrorenen Fragment. */
    static int frozenWanderer(Level level, BlockPos origin, Direction facing) {
        StructureBuilder b = new StructureBuilder(level, origin, facing);
        for (int u = -6; u <= 9; u++) {
            for (int v = -4; v <= 5; v++) {
                double e = Math.pow((u - 1.5) / 8.0, 2) + Math.pow((v - 0.5) / 5.0, 2);
                if (e <= 1 && b.chance(60)) b.put(u, v, 0, b.chance(50) ? Blocks.PACKED_ICE : Blocks.SNOW_BLOCK);
            }
        }
        // Eingeschneiter Hügel, darunter liegt der Wanderer
        for (int u = 0; u <= 2; u++) for (int v = -1; v <= 0; v++) b.put(u, v, 1, Blocks.SNOW_BLOCK);
        b.put(1, -1, 2, Blocks.SNOW_BLOCK);
        b.put(1, 0, 2, snow(b));
        b.put(3, -1, 1, ModBlocks.FROZEN_FRAGMENT_BLOCK.get());          // die Schriftrolle, die er trug
        // Wanderstab, Buendel, Decke, kaltes Lagerfeuer
        for (int h = 1; h <= 3; h++) b.put(-1, 1, h, Blocks.SPRUCE_FENCE);
        b.put(-1, 1, 4, snow(b));
        b.put(-1, -1, 1, Blocks.BROWN_WOOL);
        b.put(-2, -1, 1, Blocks.BROWN_WOOL);
        b.put(4, -2, 1, Blocks.GRAY_CARPET);
        b.put(5, -2, 1, Blocks.GRAY_CARPET);
        b.put(-4, 0, 1, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false));
        for (int[] c : new int[][] { { -5, 0 }, { -3, 0 }, { -4, 1 } }) if (b.chance(70)) b.put(c[0], c[1], 1, Blocks.COBBLESTONE);
        // Steinhaufen als Wegmarke, eingefrorene Amphore
        b.put(7, 2, 1, Blocks.COBBLESTONE);
        b.put(7, 2, 2, Blocks.COBBLESTONE);
        b.put(7, 2, 3, Blocks.COBBLESTONE_SLAB);
        b.put(b.at(4, 2, 1), amphora(b, ModBlocks.AMPHORA_TILTED.get()));
        b.put(4, 2, 2, Blocks.ICE);
        b.put(4, 3, 1, Blocks.ICE);
        b.put(5, 2, 1, Blocks.ICE);
        // Spur: Schneeschichten, die von weither auf die Stelle zulaufen
        for (int u = -14; u <= -6; u++) {
            int v = (int) Math.round(Math.sin(u * 0.6) * 1.2);
            BlockPos p = b.at(u, v, 1);
            if (b.level.getBlockState(p).isAir()) b.put(p, Blocks.SNOW.defaultBlockState());
        }
        frost(b, -6, 9, -4, 5);
        return b.placed;
    }

    // ------------------------------------------------------------------ Soldatengrab (Sumpf)

    /** Flaches Grab im Moor: Schlammhuegel, halb versunkene Randbalken, Speer, Fundboeden mit dem Amulett darunter. */
    static int soldierGrave(Level level, BlockPos origin, Direction facing) {
        StructureBuilder b = new StructureBuilder(level, origin, facing);
        for (int u = -3; u <= 8; u++) {
            for (int v = -4; v <= 4; v++) {
                double e = Math.pow((u - 2.5) / 6.0, 2) + Math.pow(v / 4.5, 2);
                if (e > 1) continue;
                b.put(u, v, 0, b.chance(85) ? Blocks.MUD : Blocks.MOSS_BLOCK);
                b.put(u, v, -1, Blocks.MUD);
            }
        }
        // Huegel
        for (int u = 1; u <= 5; u++) for (int v = -1; v <= 1; v++) b.put(u, v, 1, Blocks.MUD);
        for (int u = 2; u <= 4; u++) b.put(u, 0, 2, Blocks.MUD);
        // Beigaben im Boden darunter: Suspicious Mud
        for (int[] c : new int[][] { { 2, -1 }, { 3, 1 }, { 4, 0 }, { 1, 1 } }) b.put(c[0], c[1], 0, dusted(b, ModBlocks.SUSPICIOUS_MUD.get()));
        // Randbalken, halb im Schlamm
        for (int u = 0; u <= 6; u++) {
            if (b.chance(75)) b.put(u, -2, 1, b.log(Blocks.STRIPPED_SPRUCE_LOG, b.axisU()));
            if (b.chance(75)) b.put(u, 2, 1, b.log(Blocks.STRIPPED_SPRUCE_LOG, b.axisU()));
        }
        // Speer am Kopfende und ein Helmrest, Grabstein mit Moos
        for (int h = 1; h <= 3; h++) b.put(0, 0, h, Blocks.IRON_BARS);
        b.put(0, 0, 4, Blocks.CHAIN);
        b.put(-1, 0, 1, Blocks.COBBLESTONE_WALL);
        b.put(-1, 0, 2, Blocks.MOSS_CARPET);
        for (int u = -2; u <= 7; u++) for (int v = -3; v <= 3; v++) if (b.chance(18) && b.level.getBlockState(b.at(u, v, 1)).isAir()) b.put(u, v, 1, Blocks.MOSS_CARPET);
        return b.placed;
    }

    // ------------------------------------------------------------------ Katakomben-Kirche (unter der Erde)

    /**
     * Eine Kirche in der Katakombe, 9 Stufen unter der Erde. Schiff ohne Sitzplaetze (die fruehen Kirchen hatten keine), am Ende
     * die Nische mit dem Altartisch aus glattem Stein, an einer Wand Grabnischen (teils verschlossen, teils offen mit Kerze und
     * zerbrochener Amphore), an der anderen ein Nebenraum mit ausgetrocknetem Taufbecken. Der Eingang ist eine Treppe, an
     * der Oberflaeche liegen Suspicious-Bloecke als Hinweis.
     *
     * @param desert Sandstein-Fassung fuer die Wueste, sonst Stein
     */
    static int catacombChurch(Level level, BlockPos origin, Direction facing, boolean desert) {
        StructureBuilder b = new StructureBuilder(level, origin, facing);
        BlockState wall = (desert ? Blocks.SANDSTONE : Blocks.STONE_BRICKS).defaultBlockState();
        BlockState floor = (desert ? Blocks.SMOOTH_SANDSTONE : Blocks.SMOOTH_STONE).defaultBlockState();
        BlockState seal = (desert ? Blocks.CHISELED_SANDSTONE : Blocks.CHISELED_STONE_BRICKS).defaultBlockState();
        Block stairBlock = desert ? Blocks.SANDSTONE_STAIRS : Blocks.STONE_BRICK_STAIRS;
        Block slab = desert ? Blocks.SMOOTH_SANDSTONE_SLAB : Blocks.SMOOTH_STONE_SLAB;
        Block altar = desert ? Blocks.SMOOTH_SANDSTONE : Blocks.SMOOTH_STONE;

        Set<BlockPos> cells = new HashSet<>();
        for (int k = 0; k <= 9; k++) b.cells(cells, k, k, 0, 1, -k + 1, -k + 3);              // Treppe
        b.cells(cells, 10, 25, -3, 3, -8, -4);                                                 // Schiff
        for (int u = 26; u <= 29; u++) {                                                       // Nische (Apsis)
            for (int v = -4; v <= 4; v++) if (Math.pow(u - 25, 2) + v * v <= 12) b.cells(cells, u, u, v, v, -8, -4);
        }
        b.cells(cells, 15, 16, -4, -4, -8, -6);                                                // Tuer zum Nebenraum
        b.cells(cells, 13, 19, -10, -5, -8, -5);                                               // Nebenraum
        int[] nicheU = { 11, 14, 17, 20, 23 };
        int[][] tiers = { { -8, -7 }, { -6, -5 } };
        for (int u : nicheU) for (int[] t : tiers) b.cells(cells, u, u + 1, 5, 5, t[0], t[1]);  // Grabnischen
        b.carve(cells, wall, floor);

        for (int k = 0; k <= 9; k++) {                                                          // Stufen
            BlockState stair = stairBlock.defaultBlockState().setValue(StairBlock.FACING, facing.getOpposite());
            b.put(k, 0, -k, stair);
            b.put(k, 1, -k, stair);
        }
        // Grabnischen: meist mit gemeisseltem Stein verschlossen, einige offen mit Kerze und zerbrochener Amphore
        for (int u : nicheU) {
            for (int[] t : tiers) {
                if (b.chance(25)) {
                    for (int du = 0; du <= 1; du++) for (int h = t[0]; h <= t[1]; h++) b.put(u + du, 4, h, Blocks.AIR);
                    b.put(u, 5, t[0], Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.LIT, true).setValue(CandleBlock.CANDLES, 1 + b.random.nextInt(3)));
                    b.put(b.at(u + 1, 5, t[0]), amphora(b, b.chance(50) ? ModBlocks.AMPHORA_HALF.get() : ModBlocks.AMPHORA_SHARDS.get()));
                } else {
                    for (int du = 0; du <= 1; du++) for (int h = t[0]; h <= t[1]; h++) b.put(u + du, 4, h, seal);
                }
            }
        }
        // Altartisch aus glattem Stein: zwei Beine, darueber Platten; daneben Amphoren
        b.put(28, -1, -8, altar);
        b.put(28, 1, -8, altar);
        for (int v = -1; v <= 1; v++) b.put(28, v, -7, slab);
        b.put(b.at(26, -3, -8), amphora(b, ModBlocks.AMPHORA.get()));
        b.put(b.at(26, 3, -8), amphora(b, ModBlocks.AMPHORA_BROKEN_NECK.get()));
        // Ausgetrocknetes Taufbecken: Ring aus glattem Stein, innen trockene Erde
        for (int u = 15; u <= 17; u++) for (int v = -9; v <= -7; v++) if (!(u == 16 && v == -8)) b.put(u, v, -8, altar);
        b.put(16, -8, -9, Blocks.COARSE_DIRT);
        // Haengende Lampen im Schiff
        for (int u : new int[] { 12, 17, 22 }) b.put(u, 0, -4, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
        // Hinweise an der Oberflaeche
        b.put(2, -1, 0, dusted(b, ModBlocks.SUSPICIOUS_SAND.get()));
        b.put(-1, 2, 0, dusted(b, ModBlocks.SUSPICIOUS_SAND.get()));
        b.put(4, 3, 0, dusted(b, ModBlocks.SUSPICIOUS_SAND.get()));
        return b.placed;
    }

    // ------------------------------------------------------------------ Verschuettetes Haus (kurz unter der Erde)

    /**
     * Ein eingestuerztes Haus, nur wenige Bloecke unter der Oberflaeche: Mauern, Boden aus Brettern, die Decke teilweise
     * eingebrochen (Schutt), im Inneren Feuerstelle, Tisch und Amphoren. An der Oberflaeche sitzen Suspicious-Bloecke als Hinweis.
     *
     * @param mud Lehmziegel und Suspicious Mud (Sumpf, Fluss) statt Sandstein und Suspicious Sand
     */
    static int buriedHouse(Level level, BlockPos origin, Direction facing, boolean mud) {
        StructureBuilder b = new StructureBuilder(level, origin, facing);
        BlockState wall = (mud ? Blocks.MUD_BRICKS : Blocks.SANDSTONE).defaultBlockState();
        BlockState floor = Blocks.SPRUCE_PLANKS.defaultBlockState();
        Set<BlockPos> cells = new HashSet<>();
        b.cells(cells, 1, 7, -2, 2, -6, -3);
        b.carve(cells, wall, floor);
        // Decke teilweise eingebrochen: Loecher im Dach, Schutt faellt hinein
        for (int u = 1; u <= 7; u++) {
            for (int v = -2; v <= 2; v++) {
                if (b.chance(40)) {
                    b.put(u, v, -2, Blocks.AIR);
                    b.put(u, v, -6, Blocks.GRAVEL);
                    if (b.chance(50)) b.put(u, v, -5, Blocks.GRAVEL);
                    if (b.chance(20)) b.put(u, v, -4, Blocks.DIRT);
                }
            }
        }
        // Mauerluecken
        for (int i = 0; i < 6; i++) b.put(b.random.nextBoolean() ? 0 : 8, b.random.nextInt(5) - 2, -6 + b.random.nextInt(3), Blocks.DIRT);
        // Einrichtung
        b.put(6, 2, -6, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false));
        b.put(3, -2, -6, Blocks.STONE_SLAB);
        b.put(b.at(6, -2, -6), amphora(b, ModBlocks.AMPHORA.get()));
        b.put(b.at(6, -1, -6), amphora(b, ModBlocks.AMPHORA_LYING.get()));
        b.put(b.at(1, 2, -6), amphora(b, ModBlocks.AMPHORA_BROKEN_NECK.get()));
        b.put(1, -2, -6, ModBlocks.SHARD_PILE.get());
        // Hinweise an der Oberflaeche ueber dem Haus
        Block marker = mud ? ModBlocks.SUSPICIOUS_MUD.get() : ModBlocks.SUSPICIOUS_SAND.get();
        for (int i = 0; i < 5; i++) b.put(1 + b.random.nextInt(7), b.random.nextInt(5) - 2, 0, dusted(b, marker));
        return b.placed;
    }
}
