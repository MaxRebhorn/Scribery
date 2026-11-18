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
     * {@code /scribery_wreck [Ladung 1-10] [upright|side]} setzt ein antikes Handelsschiff als Wrack an die Stelle, an der der
     * Spieler steht, mit dem Kiel in dem Block unter den Fuessen und der Laenge in Blickrichtung. Die Ladung steuert die Menge
     * an Amphoren: 1 = so wenig wie moeglich (eine), 10 = Laderaum komplett voll mit ganzen und zerbrochenen Amphoren. Ohne
     * Angabe 5 und aufrecht. {@code side} legt das Schiff auf die Seite, die Ladung liegt dann neben dem Rumpf; kurz geht auch
     * {@code /scribery_wreck side [Ladung]}. Gedacht zum Testen der Amphoren und Loot-Tabellen, unter Wasser wie an Land.
     */
    @SubscribeEvent
    static void onRegisterCommands(RegisterCommandsEvent event) {
        // /scribery_wreck [load] [upright|side]   und kurz   /scribery_wreck side [load]
        event.getDispatcher().register(Commands.literal("scribery_wreck")
                .requires(src -> src.hasPermission(2))
                .executes(ctx -> wreck(ctx, 5, false))
                .then(Commands.argument("load", IntegerArgumentType.integer(1, 10))
                        .executes(ctx -> wreck(ctx, IntegerArgumentType.getInteger(ctx, "load"), false))
                        .then(Commands.literal("upright")
                                .executes(ctx -> wreck(ctx, IntegerArgumentType.getInteger(ctx, "load"), false)))
                        .then(Commands.literal("side")
                                .executes(ctx -> wreck(ctx, IntegerArgumentType.getInteger(ctx, "load"), true))))
                .then(Commands.literal("side")
                        .executes(ctx -> wreck(ctx, 5, true))
                        .then(Commands.argument("load", IntegerArgumentType.integer(1, 10))
                                .executes(ctx -> wreck(ctx, IntegerArgumentType.getInteger(ctx, "load"), true)))));

        // Weitere Struktur-Generatoren zum Testen (docs/DESIGN-STRUCTURES.md). Alle setzen an die Stelle des Spielers,
        // ausgerichtet nach der Blickrichtung; die Treppe der Katakombe und das Haus laufen in Blickrichtung unter die Erde.
        var d = event.getDispatcher();
        d.register(Commands.literal("scribery_caravan").requires(src -> src.hasPermission(2))
                .executes(ctx -> structure(ctx, "Frozen caravan", DebugStructures::frozenCaravan)));
        d.register(Commands.literal("scribery_wanderer").requires(src -> src.hasPermission(2))
                .executes(ctx -> structure(ctx, "Frozen wanderer", DebugStructures::frozenWanderer)));
        d.register(Commands.literal("scribery_grave").requires(src -> src.hasPermission(2))
                .executes(ctx -> structure(ctx, "Soldier's grave", DebugStructures::soldierGrave)));
        // /scribery_catacomb [normal|desert]
        d.register(Commands.literal("scribery_catacomb").requires(src -> src.hasPermission(2))
                .executes(ctx -> structure(ctx, "Catacomb church (stone)", (l, o, f) -> DebugStructures.catacombChurch(l, o, f, false)))
                .then(Commands.literal("normal")
                        .executes(ctx -> structure(ctx, "Catacomb church (stone)", (l, o, f) -> DebugStructures.catacombChurch(l, o, f, false))))
                .then(Commands.literal("desert")
                        .executes(ctx -> structure(ctx, "Catacomb church (desert)", (l, o, f) -> DebugStructures.catacombChurch(l, o, f, true)))));
        // /scribery_house [sand|mud]
        d.register(Commands.literal("scribery_house").requires(src -> src.hasPermission(2))
                .executes(ctx -> structure(ctx, "Buried house (sand)", (l, o, f) -> DebugStructures.buriedHouse(l, o, f, false)))
                .then(Commands.literal("sand")
                        .executes(ctx -> structure(ctx, "Buried house (sand)", (l, o, f) -> DebugStructures.buriedHouse(l, o, f, false))))
                .then(Commands.literal("mud")
                        .executes(ctx -> structure(ctx, "Buried house (mud)", (l, o, f) -> DebugStructures.buriedHouse(l, o, f, true)))));
    }

    /** Ein Struktur-Generator: setzt die Struktur und liefert die Zahl gesetzter Bloecke. */
    @FunctionalInterface
    private interface Generator {
        int build(net.minecraft.world.level.Level level, BlockPos origin, net.minecraft.core.Direction facing);
    }

    private static int structure(CommandContext<CommandSourceStack> ctx, String name, Generator gen) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var src = ctx.getSource();
        var player = src.getPlayerOrException();
        BlockPos origin = player.blockPosition().below();
        var facing = player.getDirection();
        int blocks = gen.build(src.getLevel(), origin, facing);
        src.sendSuccess(() -> Component.literal(name + " placed at " + origin.toShortString() + " (" + blocks + " blocks, facing "
                + facing.getName() + ")"), true);
        return blocks;
    }

    private static int wreck(CommandContext<CommandSourceStack> ctx, int load, boolean onSide) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var src = ctx.getSource();
        var player = src.getPlayerOrException();
        BlockPos origin = player.blockPosition().below();
        var facing = player.getDirection();
        var result = DebugWreck.build(src.getLevel(), origin, facing, load, onSide);
        src.sendSuccess(() -> Component.literal("Shipwreck (" + (onSide ? "on its side" : "upright") + ") placed at " + origin.toShortString()
                + ": load " + load + "/10, " + result.amphorae() + " amphorae, " + result.blocks() + " blocks, along "
                + facing.getAxis().getName()), true);
        return result.blocks();
    }
}
