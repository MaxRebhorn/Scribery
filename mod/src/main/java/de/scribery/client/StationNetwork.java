package de.scribery.client;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

import de.scribery.LecternBlockEntity;
import de.scribery.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * Welche Stationen zusammenhaengen. Alle Stationsbloecke (Pult, Presse, Lettern-Schrank, Uebersetzungstisch),
 * die sich waagerecht beruehren, bilden ein Netz. Jede Oberflaeche fragt hier, worauf sie Zugriff hat, und
 * zeigt nur diese Teile. Die Regeln stehen in docs/STATIONS.md.
 *
 * @param anchor           kleinste Position im Netz, Schluessel fuer den gemerkten Arbeitsstand
 * @param lectern          das massgebliche Pult, oder null
 * @param lecternFragment  Index des Fragments auf diesem Pult, -1 wenn nichts darauf liegt
 * @param lecternVariant   Zustand dieses Fragments (0 normal, 1 verkohlt, 2 wasser, 3 verblasst, 4 gefroren)
 * @param cabinet          der Lettern-Schrank, aus dem die Presse ihre Vorraete bezieht, oder null
 */
record StationNetwork(BlockPos anchor, BlockPos lectern, int lecternFragment, int lecternVariant, BlockPos cabinet,
                      boolean press, boolean desk) {

    boolean hasLectern() { return lectern != null; }
    boolean hasCabinet() { return cabinet != null; }

    /** Das Manuskript ist da, wenn ein Pult mit genau diesem Fragment anliegt. */
    boolean hasManuscript(int fragment) { return lectern != null && lecternFragment == fragment; }

    Stations stations() {
        return new Stations(lectern != null, press, cabinet != null, desk);
    }

    /**
     * Sucht das Netz um {@code start}. Wurde ein Pult selbst angeklickt, gilt dieses; sonst das erste mit
     * Fragment, sonst irgendeines.
     */
    static StationNetwork scan(Level level, BlockPos start) {
        boolean press = false, desk = false;
        BlockPos lectern = null, anchor = start, cabinet = null;
        int fragment = -1, variant = 0;
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start);
        seen.add(start);
        while (!queue.isEmpty()) {
            BlockPos p = queue.poll();
            Block b = level.getBlockState(p).getBlock();
            if (b == ModBlocks.PRINTING_PRESS.get()) {
                press = true;
            } else if (b == ModBlocks.LETTER_CABINET.get()) {
                if (cabinet == null || p.asLong() < cabinet.asLong()) cabinet = p;
            } else if (b == ModBlocks.TRANSLATOR_DESK.get()) {
                desk = true;
            } else if (b == ModBlocks.SCRIBE_LECTERN.get()) {
                var lbe = level.getBlockEntity(p) instanceof LecternBlockEntity be ? be : null;
                int idx = lbe != null ? lbe.debugFragmentIndex() : -1;
                if (lectern == null || p.equals(start) || (fragment < 0 && idx >= 0 && !lectern.equals(start))) {
                    lectern = p;
                    fragment = idx;
                    variant = lbe != null ? lbe.fragmentVariant() : 0;
                }
            } else {
                continue;
            }
            if (p.asLong() < anchor.asLong()) anchor = p;
            for (Direction d : Direction.Plane.HORIZONTAL) {
                BlockPos n = p.relative(d);
                if (seen.add(n)) queue.add(n);
            }
        }
        return new StationNetwork(anchor, lectern, fragment, variant, cabinet, press, desk);
    }
}
