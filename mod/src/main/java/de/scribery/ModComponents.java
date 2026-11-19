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

    /** Das Fragment ist im Archiv eingetragen (das Item bleibt erhalten, der Fortschritt liegt in den Spielerdaten). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<net.minecraft.util.Unit>> CATALOGUED =
            COMPONENTS.registerComponentType("catalogued",
                    b -> b.persistent(net.minecraft.util.Unit.CODEC).networkSynchronized(net.minecraft.network.codec.StreamCodec.unit(net.minecraft.util.Unit.INSTANCE)));

    /** Das Fragment war schon im Archiv: eine Dublette, die der Scribe Villager nimmt. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<net.minecraft.util.Unit>> DUPLICATE =
            COMPONENTS.registerComponentType("duplicate",
                    b -> b.persistent(net.minecraft.util.Unit.CODEC).networkSynchronized(net.minecraft.network.codec.StreamCodec.unit(net.minecraft.util.Unit.INSTANCE)));

    /** Schwierigkeit der Dublette (1 leicht, 2 mittel, 3 schwer), beim Markieren aus dem Manuskript gelesen. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> DUPLICATE_TIER =
            COMPONENTS.registerComponentType("duplicate_tier",
                    b -> b.persistent(com.mojang.serialization.Codec.INT).networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.VAR_INT));

    public static void register(IEventBus bus) {
        COMPONENTS.register(bus);
    }

    private ModComponents() {}
}
