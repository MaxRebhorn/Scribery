package de.scribery;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Eigene Features der Weltgenerierung (siehe tools/gen_worldgen.py). */
public final class ModFeatures {
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, Scribery.MODID);

    public static final DeferredHolder<Feature<?>, CaveWreckFeature> CAVE_WRECK = FEATURES.register("cave_wreck", CaveWreckFeature::new);

    public static void register(IEventBus bus) {
        FEATURES.register(bus);
    }

    private ModFeatures() {}
}
