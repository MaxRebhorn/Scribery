package de.scribery.client;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import de.scribery.CabinetBlockEntity;
import de.scribery.LecternBlockEntity;
import de.scribery.ModBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/** Einstieg von den Stationsbloecken in die Screens. Nur auf dem Client geladen. */
public final class ClientHooks {
    /** Arbeitsstand je Stationsgruppe, damit er das Schliessen des Fensters ueberlebt. */
    private static final Map<BlockPos, Workbench> BENCHES = new HashMap<>();

    private ClientHooks() {}

    /** Pult angeklickt: zeigt nur das Pult. */
    public static void openLectern(BlockPos pos) {
        open(pos, Stations.Kind.LECTERN);
    }

    /** Presse oder Lettern-Schrank angeklickt: gemeinsame Oberflaeche, es erscheint nur, was angrenzt. */
    public static void openPress(BlockPos pos) {
        open(pos, Stations.Kind.PRESS);
    }

    private static void open(BlockPos clicked, Stations.Kind start) {
        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level == null) return;

        // Alle Stationsbloecke, die waagerecht zusammenhaengen.
        boolean press = false, cabinet = false, desk = false;
        BlockPos lectern = null, anchor = clicked, cabinetPos = null;
        int lecternFragment = -1;
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(clicked);
        seen.add(clicked);
        while (!queue.isEmpty()) {
            BlockPos p = queue.poll();
            Block b = level.getBlockState(p).getBlock();
            if (b == ModBlocks.PRINTING_PRESS.get()) press = true;
            else if (b == ModBlocks.LETTER_CABINET.get()) {
                cabinet = true;
                if (cabinetPos == null || p.asLong() < cabinetPos.asLong()) cabinetPos = p;
            }
            else if (b == ModBlocks.TRANSLATOR_DESK.get()) desk = true;
            else if (b == ModBlocks.SCRIBE_LECTERN.get()) {
                int idx = level.getBlockEntity(p) instanceof LecternBlockEntity be ? be.debugFragmentIndex() : -1;
                // Das angeklickte Pult gewinnt, sonst eines mit Fragment.
                if (lectern == null || p.equals(clicked) || (lecternFragment < 0 && idx >= 0 && !lectern.equals(clicked))) {
                    lectern = p;
                    lecternFragment = idx;
                }
            } else continue;
            if (p.asLong() < anchor.asLong()) anchor = p;
            for (Direction d : Direction.Plane.HORIZONTAL) {
                BlockPos n = p.relative(d);
                if (seen.add(n)) queue.add(n);
            }
        }

        Stations st = new Stations(lectern != null, press, cabinet, desk);
        Workbench old = BENCHES.get(anchor);
        Workbench bench = (old != null && old.key() == lecternFragment && old.stations.equals(st)) ? old
                : lecternFragment >= 0 ? new Workbench(lecternFragment, st) : Workbench.free(st);
        BENCHES.put(anchor, bench);
        bench.cabinetPos = cabinetPos;
        if (cabinetPos != null && level.getBlockEntity(cabinetPos) instanceof CabinetBlockEntity cbe) {
            bench.applyResources(cbe.letters(), cbe.ink(), cbe.paper());
        }

        mc.setScreen(start == Stations.Kind.LECTERN ? new LecternScreen(bench) : new PressScreen(bench));
    }
}
