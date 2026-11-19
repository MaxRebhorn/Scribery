package de.scribery;

import java.util.List;

import de.scribery.core.ArchiveProgress;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Das Eintragen ins Manuscript Archive und das Senden der Sammlung zum Client. Laeuft nur auf dem Server; der Client
 * bekommt die Sammlung beim Anmelden und nach jeder Aenderung (SyncArchive) und zeigt sie nur an.
 */
@EventBusSubscriber(modid = Scribery.MODID)
public final class ArchiveService {
    /** Ergebnis eines Eintragsversuchs. */
    public enum Result { ADDED, ALREADY_THERE, NOT_ACCEPTED }

    private ArchiveService() {}

    public static ArchiveData data(Player player) {
        return player.getData(ModAttachments.ARCHIVE.get());
    }

    /** Das Fragment im Archiv eintragen: der griechische Urtext erscheint dann im Text. Ein Duplikat bleibt beim Spieler. */
    public static Result registerFragment(ServerPlayer player, ItemStack stack) {
        int index = ManuscriptFragmentItem.indexOf(stack);
        if (index < 0) return Result.NOT_ACCEPTED;
        String id = Manuscripts.load().get(index).id();
        ArchiveProgress now = data(player).get(id);
        if (now.found()) return Result.ALREADY_THERE;
        store(player, id, now.withFound());
        return Result.ADDED;
    }

    /**
     * Einen gedruckten oder uebersetzten Bogen eintragen. Ein uebersetzter Bogen traegt auch seine Umschrift und
     * zaehlt deshalb als gedruckt.
     */
    public static Result registerSheet(ServerPlayer player, SheetData sheet, boolean translated) {
        var all = Manuscripts.load();
        if (sheet.fragment() < 0 || sheet.fragment() >= all.size() || sheet.page() < 0
                || sheet.page() >= ArchiveProgress.MAX_PAGES) return Result.NOT_ACCEPTED;
        String id = all.get(sheet.fragment()).id();
        ArchiveProgress now = data(player).get(id);
        boolean already = translated ? now.isTranslated(sheet.page()) : now.isPrinted(sheet.page());
        if (already) return Result.ALREADY_THERE;
        ArchiveProgress next = now.withPrinted(sheet.page());
        if (translated) next = next.withTranslated(sheet.page());
        store(player, id, next);
        return Result.ADDED;
    }

    /** Fragment-Item als eingetragen markieren (bleibt im Besitz des Spielers). */
    public static void markCatalogued(ItemStack stack) {
        if (!stack.has(ModComponents.DUPLICATE.get())) stack.set(ModComponents.CATALOGUED.get(), net.minecraft.util.Unit.INSTANCE);
    }

    /** Fragment-Item zur Dublette machen: Schwierigkeit merken, "eingetragen" ersetzen. Liefert false, wenn es schon eine war. */
    public static boolean markDuplicate(ItemStack stack) {
        if (stack.has(ModComponents.DUPLICATE.get())) return false;
        String id = stack.get(ModComponents.MANUSCRIPT.get());
        int tier = id == null ? 0 : FindPools.tier(id);
        stack.remove(ModComponents.CATALOGUED.get());
        stack.set(ModComponents.DUPLICATE.get(), net.minecraft.util.Unit.INSTANCE);
        stack.set(ModComponents.DUPLICATE_TIER.get(), Math.max(1, Math.min(3, tier)));
        return true;
    }

    private static void store(ServerPlayer player, String id, ArchiveProgress progress) {
        player.setData(ModAttachments.ARCHIVE.get(), data(player).with(id, progress));
        sync(player);
    }

    /** Die ganze Sammlung zum Client schicken (bis zu 1.540 kurze Eintraege). */
    public static void sync(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new ModNetwork.SyncArchive(data(player).toEntries()));
    }

    public static void message(Player player, String text) {
        player.displayClientMessage(Component.literal(text), true);
    }

    @SubscribeEvent
    static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) sync(p);
    }

    @SubscribeEvent
    static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) sync(p);
    }

    /** Nur fuer Tests und Debug: die Eintraege als Liste. */
    static List<ArchiveData.Entry> entries(Player player) {
        return data(player).toEntries();
    }
}
