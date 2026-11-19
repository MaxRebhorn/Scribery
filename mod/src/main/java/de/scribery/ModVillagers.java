package de.scribery;

import com.google.common.collect.ImmutableSet;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Beruf "scribery:scribe" (Schreiber). Arbeitsblock ist das Scribe Lectern; der POI-Typ kennt alle Blockzustaende des Pults
 * (die Blickrichtung ist ein eigener Zustand). Aussehen: textures/entity/villager/profession/scribe.png, Barett in
 * client/ScribeBeretLayer. Handel: siehe docs/DESIGN-VILLAGER.md.
 */
public final class ModVillagers {
    public static final DeferredRegister<PoiType> POI = DeferredRegister.create(Registries.POINT_OF_INTEREST_TYPE, Scribery.MODID);
    public static final DeferredRegister<VillagerProfession> PROFESSIONS =
            DeferredRegister.create(Registries.VILLAGER_PROFESSION, Scribery.MODID);

    public static final DeferredHolder<PoiType, PoiType> SCRIBE_POI = POI.register("scribe",
            () -> new PoiType(ImmutableSet.copyOf(ModBlocks.SCRIBE_LECTERN.get().getStateDefinition().getPossibleStates()), 1, 1));

    private static final ResourceKey<PoiType> SCRIBE_POI_KEY =
            ResourceKey.create(Registries.POINT_OF_INTEREST_TYPE, net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(Scribery.MODID, "scribe"));

    public static final DeferredHolder<VillagerProfession, VillagerProfession> SCRIBE = PROFESSIONS.register("scribe",
            () -> new VillagerProfession("scribe", holder -> holder.is(SCRIBE_POI_KEY), holder -> holder.is(SCRIBE_POI_KEY),
                    ImmutableSet.of(), ImmutableSet.of(), SoundEvents.VILLAGER_WORK_LIBRARIAN));

    private ModVillagers() {}

    public static void register(IEventBus bus) {
        POI.register(bus);
        PROFESSIONS.register(bus);
    }
}
