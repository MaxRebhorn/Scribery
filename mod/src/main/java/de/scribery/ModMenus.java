package de.scribery;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, Scribery.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<MintMenu>> MINT =
            MENUS.register("minting_table", () -> new MenuType<>(MintMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static void register(IEventBus bus) {
        MENUS.register(bus);
    }

    private ModMenus() {}
}
