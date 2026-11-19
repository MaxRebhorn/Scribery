package de.scribery;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import de.scribery.core.ScribePrices;
import de.scribery.core.ScribeTrades;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentPredicate;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerTrades.ItemListing;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;

/**
 * Handel des Scribe Villagers. Die Liste kommt aus core/ScribeTrades (Preise: ScribePrices), hier werden daraus die
 * MerchantOffers gebaut.
 *
 * Vanilla waehlt je Stufe zufaellig nur zwei Eintraege aus der Liste. Damit das Angebot fest ist, steht je Stufe genau ein
 * Eintrag in der Liste ({@link LevelListing}); der haengt alle Angebote der Stufe selbst an die Angebotsliste des Dorfbewohners.
 */
@EventBusSubscriber(modid = Scribery.MODID)
public final class ScribeTradeEvents {
    private ScribeTradeEvents() {}

    public static final TagKey<Structure> MAP_TARGETS =
            TagKey.create(Registries.STRUCTURE, ResourceLocation.fromNamespaceAndPath(Scribery.MODID, "scribe_map_targets"));

    @SubscribeEvent
    static void onTrades(VillagerTradesEvent event) {
        ResourceLocation key = BuiltInRegistries.VILLAGER_PROFESSION.getKey(event.getType());
        if (!ModVillagers.SCRIBE.getId().equals(key)) return;
        for (int level = 1; level <= 5; level++) {
            List<ItemListing> list = new ArrayList<>();
            list.add(new LevelListing(level));
            event.getTrades().put(level, list);
        }
    }

    /** Alle Angebote einer Stufe; das erste wird zurueckgegeben, die uebrigen gleich an die Angebotsliste angehaengt. */
    static final class LevelListing implements ItemListing {
        private final int level;

        LevelListing(int level) {
            this.level = level;
        }

        @Override
        public MerchantOffer getOffer(Entity trader, net.minecraft.util.RandomSource random) {
            List<MerchantOffer> offers = offersFor(level, trader);
            if (offers.isEmpty()) return null;
            if (trader instanceof Villager v) {
                for (int i = 1; i < offers.size(); i++) v.getOffers().add(offers.get(i));
            }
            return offers.get(0);
        }
    }

    /** Die Angebote einer Stufe (ohne die Welt zu aendern). Auf Stufe 5 kommt die Fundkarte dazu, wenn eine Struktur gefunden wird. */
    static List<MerchantOffer> offersFor(int level, Entity trader) {
        List<MerchantOffer> out = new ArrayList<>();
        for (ScribeTrades.Offer o : ScribeTrades.forLevel(level)) out.add(toOffer(o));
        if (level == 5) {
            MerchantOffer map = mapOffer(trader);
            if (map != null) out.add(map);
        }
        return out;
    }

    static MerchantOffer toOffer(ScribeTrades.Offer o) {
        var give = BuiltInRegistries.ITEM.getHolderOrThrow(net.minecraft.resources.ResourceKey.create(Registries.ITEM, ResourceLocation.parse(o.give())));
        ItemCost cost = o.mark().isDuplicate()
                ? new ItemCost(give, o.giveCount(), predicate(o.mark()))
                : new ItemCost(give.value(), o.giveCount());
        ItemStack result = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(o.get())), o.getCount());
        return new MerchantOffer(cost, Optional.empty(), result, 0, o.maxUses(), o.xp(), 0.05F);
    }

    /** Nur Fragmente mit dem Zeichen "duplicate" (und bei Einzelverkauf mit passender Schwierigkeit). */
    static DataComponentPredicate predicate(ScribeTrades.Mark mark) {
        DataComponentPredicate.Builder b = DataComponentPredicate.builder().expect(ModComponents.DUPLICATE.get(), Unit.INSTANCE);
        switch (mark) {
            case DUPLICATE_EASY -> b.expect(ModComponents.DUPLICATE_TIER.get(), 1);
            case DUPLICATE_MEDIUM -> b.expect(ModComponents.DUPLICATE_TIER.get(), 2);
            case DUPLICATE_HARD -> b.expect(ModComponents.DUPLICATE_TIER.get(), 3);
            default -> { }
        }
        return b.build();
    }

    /** Karte zur naechsten unserer Ruinen (Struktur-Tag scribe_map_targets); null, wenn keine in der Naehe liegt. */
    private static MerchantOffer mapOffer(Entity trader) {
        if (!(trader.level() instanceof ServerLevel level)) return null;
        BlockPos target = level.findNearestMapStructure(MAP_TARGETS, trader.blockPosition(), 100, true);
        if (target == null) return null;
        ItemStack map = MapItem.create(level, target.getX(), target.getZ(), (byte) 2, true, true);
        MapItem.renderBiomePreviewMap(level, map);
        MapItemSavedData.addTargetDecoration(map, target, "+", MapDecorationTypes.RED_X);
        map.set(DataComponents.ITEM_NAME, Component.translatable("filled_map.scribery.ruin"));
        return new MerchantOffer(new ItemCost(Items.EMERALD, ScribePrices.MAP), Optional.of(new ItemCost(Items.COMPASS)), map, 0,
                ScribePrices.USES_MAP, ScribePrices.XP_BIG, 0.2F);
    }
}
