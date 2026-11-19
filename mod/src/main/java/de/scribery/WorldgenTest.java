package de.scribery;

import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/**
 * Nur zum Testen: Laeuft der Server mit {@code -Pwgtest=scribery:catacomb,scribery:grave}, setzt er jede dieser Strukturen
 * mit {@code /place structure} (also ueber die echte Weltgenerierung), zaehlt die Bloecke des Mods im Bereich, schreibt das
 * Ergebnis ins Log und beendet sich. Ohne die Eigenschaft passiert nichts.
 */
@EventBusSubscriber(modid = Scribery.MODID)
public final class WorldgenTest {
    private static final Logger LOG = LoggerFactory.getLogger("scribery-worldgentest");

    private WorldgenTest() {}

    @SubscribeEvent
    static void onStarted(ServerStartedEvent event) {
        String spec = System.getProperty("scribery.worldgentest", "");
        if (spec.isBlank()) return;
        MinecraftServer server = event.getServer();
        ServerLevel level = server.overworld();
        CommandSourceStack src = server.createCommandSourceStack().withPermission(4);
        int i = 0;
        for (String id : spec.split(",")) {
            int x = 200 + i++ * 300, z = 200;
            for (int cx = (x >> 4) - 5; cx <= (x >> 4) + 5; cx++) {
                for (int cz = (z >> 4) - 5; cz <= (z >> 4) + 5; cz++) {
                    level.getChunk(cx, cz, net.minecraft.world.level.chunk.status.ChunkStatus.FULL, true);
                }
            }
            int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
            if (id.trim().startsWith("cave:")) {   // Wracks in gefluteten Hoehlen zaehlen (braucht eine normale Welt, siehe run/server.properties)
                int radius = Integer.parseInt(id.trim().substring(5));
                int before = CaveWreckFeature.PLACED.get(), chunks = 0;
                int cx0 = (x >> 4) + i * 40, cz0 = z >> 4;
                for (int cx = cx0 - radius; cx <= cx0 + radius; cx++) for (int cz = cz0 - radius; cz <= cz0 + radius; cz++) {
                    level.getChunk(cx, cz, net.minecraft.world.level.chunk.status.ChunkStatus.FULL, true);
                    chunks++;
                }
                LOG.info("[wgtest] Welt: Generator {}, Hoehe bei Mitte {}, Biom {}", level.getChunkSource().getGenerator().getClass().getSimpleName(),
                        level.getHeight(Heightmap.Types.WORLD_SURFACE, cx0 * 16, cz0 * 16), level.getBiome(new BlockPos(cx0 * 16, 64, cz0 * 16)).unwrapKey().map(k -> k.location().toString()).orElse("?"));
                LOG.info("[wgtest] Hoehlen-Wracks: {} in {} Chunks (Mitte {},{}); Feature-Aufrufe {}, Hoehlenboeden {}, mit Platz {}", CaveWreckFeature.PLACED.get() - before, chunks, cx0 * 16, cz0 * 16, CaveWreckFeature.CALLS.get(), CaveWreckFeature.FLOORS.get(), CaveWreckFeature.ROOMS.get());
                continue;
            }
            if (id.trim().startsWith("villages:")) {   // echte Doerfer setzen und zaehlen, in wie vielen das Schreiberhaus steht
                String type = id.trim().substring(9);
                int withHouse = 0, placed = 0, total = 4;
                for (int n = 0; n < total; n++) {
                    int px = 6000 + n * 700, pz = 6000 + i * 700;
                    for (int cx = (px >> 4) - 9; cx <= (px >> 4) + 9; cx++) for (int cz = (pz >> 4) - 9; cz <= (pz >> 4) + 9; cz++) {
                        level.getChunk(cx, cz, net.minecraft.world.level.chunk.status.ChunkStatus.FULL, true);
                    }
                    int py = level.getHeight(Heightmap.Types.WORLD_SURFACE, px, pz);
                    try {
                        int r = server.getCommands().getDispatcher().execute("place structure minecraft:" + type + " " + px + " " + py + " " + pz, src);
                        if (r > 0) placed++;
                    } catch (com.mojang.brigadier.exceptions.CommandSyntaxException ex) {
                        LOG.info("[wgtest] Dorf nicht gesetzt: {}", ex.getMessage());
                        continue;
                    }
                    int lecterns = 0;
                    for (int dx = -80; dx <= 80; dx++) for (int dz = -80; dz <= 80; dz++) {
                        for (int dy = -3; dy <= 10; dy++) {
                            if (level.getBlockState(new BlockPos(px + dx, py + dy, pz + dz)).is(ModBlocks.SCRIBE_LECTERN.get())) lecterns++;
                        }
                    }
                    if (lecterns > 0) withHouse++;
                    LOG.info("[wgtest] Dorf {} bei {},{}: Pulte={}", type, px, pz, lecterns);
                }
                LOG.info("[wgtest] {}: {} von {} gesetzten Doerfern haben das Schreiberhaus", type, withHouse, placed);
                continue;
            }
            if (id.trim().startsWith("jigsaw:")) {   // Pool-Test: viele einzelne Haeuser aus einem Dorf-Pool setzen und zaehlen, wie oft unser Haus vorkommt
                String pool = id.trim().substring(7);
                int withHouse = 0, total = 40;
                for (int n = 0; n < total; n++) {
                    int px = 3000 + n * 48, pz = 3000 + i * 100;
                    for (int cx = (px >> 4) - 2; cx <= (px >> 4) + 3; cx++) for (int cz = (pz >> 4) - 2; cz <= (pz >> 4) + 3; cz++) {
                        level.getChunk(cx, cz, net.minecraft.world.level.chunk.status.ChunkStatus.FULL, true);
                    }
                    int py = level.getHeight(Heightmap.Types.WORLD_SURFACE, px, pz);
                    try {
                        server.getCommands().getDispatcher().execute("place jigsaw " + pool + " minecraft:building_entrance 1 " + px + " " + py + " " + pz, src);
                    } catch (com.mojang.brigadier.exceptions.CommandSyntaxException ex) {
                        continue;
                    }
                    boolean found = false;
                    for (int dx = -12; dx <= 12 && !found; dx++) for (int dz = -12; dz <= 12 && !found; dz++) for (int dy = -2; dy <= 8 && !found; dy++) {
                        if (level.getBlockState(new BlockPos(px + dx, py + dy, pz + dz)).is(ModBlocks.SCRIBE_LECTERN.get())) found = true;
                    }
                    if (found) withHouse++;
                }
                LOG.info("[wgtest] Pool {}: {} von {} Haeusern waren das Schreiberhaus", pool, withHouse, total);
                continue;
            }
            if (id.trim().startsWith("house:")) {   // Schreiberhaus bauen und als Dorf-Vorlage speichern (siehe DebugVillageHouse)
                try {
                    DebugVillageHouse.buildAndSave(level, new BlockPos(x, y + 40, z), id.trim().substring(6));
                    LOG.info("[wgtest] Schreiberhaus {} gespeichert", id.trim().substring(6));
                } catch (java.io.IOException | RuntimeException ex) {
                    LOG.error("[wgtest] Schreiberhaus fehlgeschlagen", ex);
                }
                continue;
            }
            if (id.trim().startsWith("dump:")) {   // Vorlage in die Welt setzen und Schichten als Text ausgeben (nur zum Untersuchen)
                var tpl = server.getStructureManager().get(net.minecraft.resources.ResourceLocation.parse(id.trim().substring(5)));
                if (tpl.isEmpty()) { LOG.info("[wgtest] Vorlage fehlt"); continue; }
                var t = tpl.get();
                BlockPos at = new BlockPos(x, y + 20, z);
                t.placeInWorld(level, at, at, new net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings(), level.random, 2);
                var size = t.getSize();
                for (int yy = 0; yy < size.getY(); yy++) {
                    StringBuilder sb = new StringBuilder("[wgtest] y=" + yy + System.lineSeparator());
                    for (int zz = 0; zz < size.getZ(); zz++) {
                        for (int xx = 0; xx < size.getX(); xx++) {
                            var bs = level.getBlockState(at.offset(xx, yy, zz));
                            String n = BuiltInRegistries.BLOCK.getKey(bs.getBlock()).getPath();
                            char c = bs.isAir() ? '.' : n.contains("planks") ? 'p' : n.contains("log") ? 'L' : n.contains("stairs") ? 's' : n.contains("slab") ? '_' : n.contains("door") ? 'D' : n.contains("glass") ? 'g' : n.contains("cobble") ? 'c' : n.contains("jigsaw") ? 'J' : n.contains("bed") ? 'b' : n.contains("torch") || n.contains("lantern") ? 'i' : n.contains("fence") ? 'f' : n.contains("grass") || n.contains("dirt") ? 'd' : n.contains("path") ? '=' : '#';
                            sb.append(c);
                        }
                        sb.append(System.lineSeparator());
                    }
                    LOG.info(sb.toString());
                }
                continue;
            }
            if (id.trim().startsWith("template:")) {   // Nur zum Untersuchen: Groesse und Jigsaw-Bloecke einer Vanilla-Vorlage
                var tpl = server.getStructureManager().get(net.minecraft.resources.ResourceLocation.parse(id.trim().substring(9)));
                LOG.info("[wgtest] Vorlage {}: {}", id.trim().substring(9), tpl.map(t -> t.getSize()).orElse(null));
                tpl.ifPresent(t -> {
                    for (var b : t.filterBlocks(BlockPos.ZERO, new net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings(), net.minecraft.world.level.block.Blocks.JIGSAW)) {
                        LOG.info("[wgtest]   Jigsaw bei {}: {} {}", b.pos(), b.state(), b.nbt());
                    }
                });
                continue;
            }
            if (id.trim().equals("villager")) {   // Scribe Villager: Beruf, Angebote je Stufe, Dubletten
                villagerCheck(level, new BlockPos(x, y, z));
                continue;
            }
            if (id.trim().equals("features")) {  // nur Gelaende laden und die Funde der Biome zaehlen
                var biome = level.getBiome(new BlockPos(x, 64, z));
                var steps = biome.value().getGenerationSettings().features();
                StringBuilder mine = new StringBuilder();
                for (int s = 0; s < steps.size(); s++) {
                    for (var holder : steps.get(s)) {
                        var k = holder.unwrapKey();
                        if (k.isPresent() && k.get().location().getNamespace().equals(Scribery.MODID)) mine.append(k.get().location().getPath()).append("@").append(s).append(' ');
                    }
                }
                LOG.info("[wgtest] Biom {}: Funde im Biom = [{}]", biome.unwrapKey().map(k -> k.location().toString()).orElse("?"), mine);
                Map<String, Integer> found = new TreeMap<>();
                for (int cx = (x >> 4) - 4; cx <= (x >> 4) + 4; cx++) {   // klein halten: ein Tick darf nicht ueber 60 s dauern
                    for (int cz = (z >> 4) - 4; cz <= (z >> 4) + 4; cz++) {
                        var chunk = level.getChunk(cx, cz, net.minecraft.world.level.chunk.status.ChunkStatus.FULL, true);
                        for (int bx = 0; bx < 16; bx++) for (int bz = 0; bz < 16; bz++) {
                            for (int by = level.getMinBuildHeight(); by < level.getMaxBuildHeight(); by++) {
                                var key = BuiltInRegistries.BLOCK.getKey(chunk.getBlockState(new BlockPos(cx * 16 + bx, by, cz * 16 + bz)).getBlock());
                                if (key.getNamespace().equals(Scribery.MODID)) found.merge(key.getPath(), 1, Integer::sum);
                            }
                        }
                    }
                }
                LOG.info("[wgtest] Oberflaeche bei {},{}: {} (Hoehe {})", x, z,
                        BuiltInRegistries.BLOCK.getKey(level.getBlockState(new BlockPos(x, y - 1, z)).getBlock()), y);
                LOG.info("[wgtest] Funde in 9x9 Chunks: {}", found);
                continue;
            }
            try {
                int r = server.getCommands().getDispatcher().execute("place structure " + id.trim() + " " + x + " " + y + " " + z, src);
                LOG.info("[wgtest] place -> {}", r);
            } catch (com.mojang.brigadier.exceptions.CommandSyntaxException ex) {
                LOG.info("[wgtest] place fehlgeschlagen: {}", ex.getMessage());
            }
            Map<String, Integer> counts = new TreeMap<>();
            for (int bx = x - 60; bx <= x + 60; bx++) {
                for (int bz = z - 60; bz <= z + 60; bz++) {
                    for (int by = y - 40; by <= y + 30; by++) {
                        var key = BuiltInRegistries.BLOCK.getKey(level.getBlockState(new BlockPos(bx, by, bz)).getBlock());
                        if (key.getNamespace().equals(Scribery.MODID)) counts.merge(key.getPath(), 1, Integer::sum);
                    }
                }
            }
            LOG.info("[wgtest] {} bei {},{},{}: {}", id, x, y, z, counts);
        }
        LOG.info("[wgtest] fertig");
        server.halt(false);
    }

    private static void villagerCheck(ServerLevel level, BlockPos pos) {
        // Ostrakon an der Uebersetzung: richtige Glossen gelten, falsche nicht
        {
            int idx = Manuscripts.OSTRAKA_BASE;
            var frag = Manuscripts.at(idx);
            var first = frag.tileWords().subList(0, 3);
            var sheet = new SheetData(idx, 0, java.util.List.of(first.stream().map(w -> w.latin()).collect(java.util.stream.Collectors.joining(" "))), java.util.List.of());
            boolean good = Translation.isCorrect(sheet, first.stream().map(w -> w.ok().get(0)).toList());
            boolean bad = Translation.isCorrect(sheet, first.stream().map(w -> w.wrong().get(0)).toList());
            LOG.info("[wgtest] Ostrakon-Uebersetzung: richtig akzeptiert={}, falsch akzeptiert={}", good, bad);
        }
        var joke = Ostraka.random(level.random);
        LOG.info("[wgtest] Ostraka: {} Witze, Beispiel {} = {}", Ostraka.ids().size(), joke.get(ModComponents.JOKE.get()), Ostraka.joke(joke.get(ModComponents.JOKE.get())).greek());
        for (int lvl = 1; lvl <= 5; lvl++) {
            var v = net.minecraft.world.entity.EntityType.VILLAGER.create(level);
            v.moveTo(pos.getX() + .5, pos.getY(), pos.getZ() + .5);
            v.setVillagerData(v.getVillagerData().setProfession(ModVillagers.SCRIBE.get()).setLevel(lvl));
            var offers = v.getOffers();
            StringBuilder sb = new StringBuilder();
            for (var o : offers) {
                sb.append(o.getItemCostA().count()).append("x").append(BuiltInRegistries.ITEM.getKey(o.getItemCostA().item().value()).getPath())
                  
                  .append(" -> ").append(o.getResult().getCount()).append("x").append(BuiltInRegistries.ITEM.getKey(o.getResult().getItem()).getPath())
                  .append(" (max ").append(o.getMaxUses()).append("); ");
            }
            LOG.info("[wgtest] Scribe Stufe {}: {} Angebote: {}", lvl, offers.size(), sb);
        }
        // Lose Fragmente: das Beutel-Angebot nimmt ein Fragment-Item
        var v = net.minecraft.world.entity.EntityType.VILLAGER.create(level);
        v.setVillagerData(v.getVillagerData().setProfession(ModVillagers.SCRIBE.get()).setLevel(2));
        var pouch = v.getOffers().stream().filter(o -> o.getResult().getItem() instanceof FindPouchItem).findFirst().orElse(null);
        net.minecraft.world.item.ItemStack frag = ManuscriptFragmentItem.of(Manuscripts.load().get(0).id());
        LOG.info("[wgtest] Beutel-Angebot gefunden: {}; Fragment akzeptiert: {}; Papier akzeptiert: {}", pouch != null,
                pouch != null && pouch.getItemCostA().test(frag), pouch != null && pouch.getItemCostA().test(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.PAPER)));
    }
}
