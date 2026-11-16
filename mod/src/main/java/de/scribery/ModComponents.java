package de.scribery;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModComponents {
    public static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Scribery.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<SheetData>> SHEET =
            COMPONENTS.registerComponentType("sheet", b -> b.persistent(SheetData.CODEC).networkSynchronized(SheetData.STREAM_CODEC));

    public static void register(IEventBus bus) {
        COMPONENTS.register(bus);
    }

    private ModComponents() {}
}
