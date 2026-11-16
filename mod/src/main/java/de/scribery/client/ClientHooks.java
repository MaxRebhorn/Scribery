package de.scribery.client;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.ArrayDeque;

import de.scribery.LecternBlockEntity;
import de.scribery.ModBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/** Einstieg vom Pult-Block in die Screens. Nur auf dem Client geladen. */
public final class ClientHooks {
    /** Arbeitsstand je Pult, damit er das Schliessen des Fensters ueberlebt. */
    private static final Map<BlockPos, Workbench> BENCHES = new HashMap<>();

    private ClientHooks() {}

    public static void openLectern(BlockPos pos) {
        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level == null || !(level.getBlockEntity(pos) instanceof LecternBlockEntity be)) return;

        Stations st = connected(level, pos);
        int index = be.debugFragmentIndex();
        Workbench bench;
        if (index < 0) {
            BENCHES.remove(pos);
            bench = Workbench.empty(st);
        } else {
            Workbench old = BENCHES.get(pos);
            bench = (old != null && old.fragmentIndex == index && old.stations.equals(st)) ? old : new Workbench(index, st);
            BENCHES.put(pos, bench);
        }
        mc.setScreen(new LecternScreen(bench));
    }

    /** Presse und Uebersetzungstisch, die ueber waagerecht angrenzende Stationsbloecke am Pult haengen. */
    private static Stations connected(Level level, BlockPos start) {
        boolean press = false, desk = false;
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start);
        seen.add(start);
        while (!queue.isEmpty()) {
            BlockPos p = queue.poll();
            for (Direction d : Direction.Plane.HORIZONTAL) {
                BlockPos n = p.relative(d);
                if (!seen.add(n)) continue;
                Block b = level.getBlockState(n).getBlock();
                if (b == ModBlocks.PRINTING_PRESS.get()) press = true;
                else if (b == ModBlocks.TRANSLATOR_DESK.get()) desk = true;
                else if (b != ModBlocks.SCRIBE_LECTERN.get()) continue;
                queue.add(n);
            }
        }
        return new Stations(true, press, desk);
    }
}
