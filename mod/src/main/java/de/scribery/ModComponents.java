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

    /** Welches Manuskript ein Fragment-Item zeigt: die ID wie "P7". */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> MANUSCRIPT =
            COMPONENTS.registerComponentType("manuscript",
                    b -> b.persistent(com.mojang.serialization.Codec.STRING)
                            .networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.STRING_UTF8));

    /** Welcher Witz auf einem Ostrakon steht (ID aus ostraka-jokes.json). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> JOKE =
            COMPONENTS.registerComponentType("joke",
                    b -> b.persistent(com.mojang.serialization.Codec.STRING)
                            .networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.STRING_UTF8));

    public static void register(IEventBus bus) {
        COMPONENTS.register(bus);
    }

    private ModComponents() {}
}
