package de.scribery;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.commands.CommandSourceStack;
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
     * {@code /scribery_wreck [Ladung 1-10]} setzt ein antikes Handelsschiff als Wrack an die Stelle, an der der Spieler steht,
     * mit dem Kiel in dem Block unter den Fuessen und der Laenge in Blickrichtung. Die Ladung steuert die Menge an Amphoren:
     * 1 = so wenig wie moeglich (eine), 10 = Laderaum komplett voll mit ganzen und zerbrochenen Amphoren. Ohne Angabe 5.
     * Gedacht zum Testen der Fundbloecke und Loot-Tabellen, unter Wasser wie an Land.
     */
    @SubscribeEvent
    static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("scribery_wreck")
                .requires(src -> src.hasPermission(2))
                .executes(ctx -> wreck(ctx, 5))
                .then(Commands.argument("load", IntegerArgumentType.integer(1, 10))
                        .executes(ctx -> wreck(ctx, IntegerArgumentType.getInteger(ctx, "load")))));
    }

    private static int wreck(CommandContext<CommandSourceStack> ctx, int load) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var src = ctx.getSource();
        var player = src.getPlayerOrException();
        BlockPos origin = player.blockPosition().below();
        var facing = player.getDirection();
        var result = DebugWreck.build(src.getLevel(), origin, facing, load);
        src.sendSuccess(() -> Component.literal("Shipwreck placed at " + origin.toShortString() + ": load " + load + "/10, "
                + result.amphorae() + " amphorae, " + result.blocks() + " blocks, along " + facing.getAxis().getName()), true);
        return result.blocks();
    }
}
