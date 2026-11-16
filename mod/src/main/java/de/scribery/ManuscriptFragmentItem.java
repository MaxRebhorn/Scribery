package de.scribery;

import net.minecraft.world.item.Item;

/** Platzhalter-Fragment. Fuer den Debug zeigt jedes Fragment einen Text aus Johannes 1. */
public class ManuscriptFragmentItem extends Item {
    private static final int DEBUG_TEXTS = 3; // P1 bis P3 in fragments.example.json
    private final int number;

    public ManuscriptFragmentItem(Properties props, int number) {
        super(props);
        this.number = number;
    }

    @Override
    public net.minecraft.world.InteractionResultHolder<net.minecraft.world.item.ItemStack> use(
            net.minecraft.world.level.Level level, net.minecraft.world.entity.player.Player player,
            net.minecraft.world.InteractionHand hand) {
        net.minecraft.world.item.ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) de.scribery.client.ClientHooks.openFragmentView(debugFragmentIndex());
        return net.minecraft.world.InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    public int debugFragmentIndex() {
        return (number - 1) % DEBUG_TEXTS;
    }
}
