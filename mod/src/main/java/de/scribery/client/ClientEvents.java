package de.scribery.client;

import de.scribery.Scribery;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

@EventBusSubscriber(modid = Scribery.MODID, value = Dist.CLIENT)
public final class ClientEvents {
    private ClientEvents() {}

    /** /scribery debug oeffnet die Presse ohne Vorbereitung in der Welt. */
    @SubscribeEvent
    static void onRegisterCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("scribery")
                .then(Commands.literal("debug").executes(ctx -> {
                    Minecraft mc = Minecraft.getInstance();
                    // Erst nach dem Schliessen des Chats oeffnen, sonst schliesst der Chat den Screen wieder.
                    mc.tell(() -> mc.setScreen(new PressScreen()));
                    return 1;
                })));
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        SelfTest.tick();
    }
}
