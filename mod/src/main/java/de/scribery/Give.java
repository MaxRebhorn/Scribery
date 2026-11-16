package de.scribery;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Gibt dem Spieler Gegenstaende in Stapeln; was nicht ins Inventar passt, faellt auf den Boden. */
final class Give {
    private Give() {}

    static void items(Player player, Item item, int count) {
        while (count > 0) {
            int n = Math.min(count, item.getDefaultMaxStackSize());
            ItemStack stack = new ItemStack(item, n);
            if (!player.getInventory().add(stack)) player.drop(stack, false);
            count -= n;
        }
    }
}
