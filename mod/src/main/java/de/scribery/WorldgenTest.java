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
}
