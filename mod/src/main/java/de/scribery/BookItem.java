package de.scribery;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Ein Nachschlagewerk. Der Text liegt unter assets/scribery/books/<id>.md und wird auf dem Client gelesen. */
public class BookItem extends Item {
    private final String id;

    public BookItem(Properties props, String id) {
        super(props.stacksTo(1));
        this.id = id;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) de.scribery.client.ClientHooks.openBook(id, player);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
