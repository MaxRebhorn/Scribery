package de.scribery;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Eigene Loot-Funktionen. */
public final class ModLoot {
    public static final DeferredRegister<LootItemFunctionType<?>> FUNCTIONS =
            DeferredRegister.create(Registries.LOOT_FUNCTION_TYPE, Scribery.MODID);

    public static final DeferredHolder<LootItemFunctionType<?>, LootItemFunctionType<RandomManuscriptFunction>> RANDOM_MANUSCRIPT =
            FUNCTIONS.register("random_manuscript", () -> new LootItemFunctionType<>(RandomManuscriptFunction.CODEC));

    public static void register(IEventBus bus) {
        FUNCTIONS.register(bus);
    }

    private ModLoot() {}
}
