package de.scribery.client;

import de.scribery.ModBlockEntities;
import de.scribery.Scribery;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = Scribery.MODID, value = Dist.CLIENT)
public final class ClientModEvents {
    private ClientModEvents() {}

    @SubscribeEvent
    static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(de.scribery.ModMenus.MINT.get(), MintScreen::new);
    }

    @SubscribeEvent
    static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.LECTERN.get(), ctx -> new LecternRenderer<>(de.scribery.LecternBlockEntity::fragment, 13 / 16.0 + 0.01, 1 / 16.0));
        event.registerBlockEntityRenderer(ModBlockEntities.DESK.get(), ctx -> new LecternRenderer<>(de.scribery.DeskBlockEntity::sheet, 12 / 16.0 + 0.01, 1 / 16.0));
    }
}
