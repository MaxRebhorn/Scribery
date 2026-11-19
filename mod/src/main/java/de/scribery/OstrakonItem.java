package de.scribery;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** Ein beschriebener Scherben. Der Witz steht im Tooltip; Rechtsklick schreibt ihn in den Chat. Ohne Witz ist er leer. */
public class OstrakonItem extends Item {
    public OstrakonItem(Properties props) {
        super(props.stacksTo(16));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        String id = stack.get(ModComponents.JOKE.get());
        String text = id == null ? null : Ostraka.text(id);
        if (text == null) {
            tip.add(Component.translatable("tooltip.scribery.ostrakon_blank").withStyle(ChatFormatting.GRAY));
            return;
        }
        for (String line : text.split("\n")) tip.add(Component.literal(line).withStyle(ChatFormatting.ITALIC, ChatFormatting.GOLD));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        String id = stack.get(ModComponents.JOKE.get());
        String text = id == null ? null : Ostraka.text(id);
        if (text == null) return InteractionResultHolder.pass(stack);
        if (!level.isClientSide) {
            for (String line : text.split("\n")) player.sendSystemMessage(Component.literal(line).withStyle(ChatFormatting.GOLD));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
