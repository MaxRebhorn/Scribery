package de.scribery;

import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** Debug-Befehle fuer den Server (brauchen Rechtestufe 2, also Cheats in einer Einzelspieler-Welt). */
@EventBusSubscriber(modid = Scribery.MODID)
public final class ModCommands {
    private ModCommands() {}

    /**
     * /scribery_wreck setzt ein antikes Handelsschiff als Wrack an die Stelle, an der der Spieler steht, mit dem Kiel in
     * dem Block unter den Fuessen und der Laenge in Blickrichtung (Norden/Sueden oder Osten/Westen). Gedacht zum Testen
     * der Fundbloecke und Loot-Tabellen, unter Wasser wie an Land.
     */
    @SubscribeEvent
    static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("scribery_wreck")
                .requires(src -> src.hasPermission(2))
                .executes(ctx -> {
                    var src = ctx.getSource();
                    var player = src.getPlayerOrException();
                    BlockPos origin = player.blockPosition().below();
                    int blocks = DebugWreck.build(src.getLevel(), origin, player.getDirection());
                    src.sendSuccess(() -> Component.literal("Shipwreck placed at " + origin.toShortString() + " (" + blocks + " blocks, "
                            + "21 long, along " + player.getDirection().getAxis().getName() + ")"), true);
                    return blocks;
                }));
    }
}
