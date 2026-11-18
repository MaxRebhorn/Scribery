package de.scribery.client;

import de.scribery.Manuscripts;
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

    /** Buch in der Hand aufgeschlagen. Ein Buch ohne Textdatei ist noch leer. */
    public static void openBook(String id, net.minecraft.world.entity.player.Player player) {
        String md = BookText.read(id);
        if (md == null) {
            player.displayClientMessage(net.minecraft.network.chat.Component.literal("This book is still blank"), true);
            return;
        }
        Minecraft.getInstance().setScreen(new BookScreen(md));
    }

    /** Archiv angeklickt: das Evangelium als Text in drei Spalten mit dem, was der Spieler eingetragen hat. */
    public static void openArchive() {
        Minecraft.getInstance().setScreen(new ArchiveScreen());
    }

    /** Fragment in der Hand angeschaut: gleiche Anordnung wie am Pult. */
    public static void openFragmentView(int debugIndex, int variant) {
        var all = Manuscripts.load();
        if (debugIndex < 0 || debugIndex >= all.size()) return;
        Minecraft.getInstance().setScreen(new SheetViewScreen(
                all.get(debugIndex).words().stream().map(w -> w.greek()).toList(), variant));
    }

    /** Bedruckten oder uebersetzten Bogen in der Hand angeschaut. */
    public static void openSheetView(de.scribery.SheetData data) {
        Minecraft.getInstance().setScreen(new SheetViewScreen(data));
    }

    /** Pult angeklickt: zeigt nur das Pult. */
    public static void openLectern(BlockPos pos) {
        open(pos, Stations.Kind.LECTERN);
    }

    /** Presse oder Lettern-Schrank angeklickt: gemeinsame Oberflaeche, es erscheint nur, was angrenzt. */
    public static void openPress(BlockPos pos) {
        open(pos, Stations.Kind.PRESS);
    }

    /** Uebersetzungstisch angeklickt: uebersetzt den aufgelegten Bogen mit den Vorraeten des Tisches. */
    public static void openDesk(BlockPos pos) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !(mc.level.getBlockEntity(pos) instanceof de.scribery.DeskBlockEntity be)) return;
        de.scribery.SheetData d = be.sheetData();
        if (d == null || !d.translatable()) return;
        // Das Original (Manuskript) ist nur da, wenn ein Pult mit diesem Fragment am Tisch steht.
        boolean manuscript = StationNetwork.scan(mc.level, pos).hasManuscript(d.fragment());
        mc.setScreen(new TileScreen(Workbench.forSheet(d.fragment(), d.page(), pos, be.ink(), be.tablets(), manuscript)));
    }

    private static void open(BlockPos clicked, Stations.Kind start) {
        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level == null) return;

        StationNetwork net = StationNetwork.scan(level, clicked);
        Stations st = net.stations();
        int lecternFragment = net.lecternFragment();
        Workbench old = BENCHES.get(net.anchor());
        Workbench bench = (old != null && old.key() == lecternFragment && old.stations.equals(st)) ? old
                : lecternFragment >= 0 ? new Workbench(lecternFragment, st) : Workbench.free(st);
        BENCHES.put(net.anchor(), bench);
        bench.variant = lecternFragment >= 0 ? net.lecternVariant() : 0;   // Aussehen des Fragments auf dem Pult
        bench.cabinetPos = net.cabinet();
        bench.lecternPos = net.lectern();
        if (net.cabinet() != null && level.getBlockEntity(net.cabinet()) instanceof CabinetBlockEntity cbe) {
            bench.applyResources(cbe.letters(), cbe.ink(), cbe.paper(), cbe.forme());
        }

        mc.setScreen(start == Stations.Kind.LECTERN ? new LecternScreen(bench) : new PressScreen(bench));
    }
}
