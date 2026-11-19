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

/**
 * Ein beschriebener Scherben mit einem Witz auf Griechisch (Philogelos). Er ist zum Uebersetzen da: der griechische Text steht im
 * Tooltip, Rechtsklick zeigt ihn auf einer Scherbe, Schleichen + Rechtsklick zeigt die englische Loesung. Ohne Witz ist er leer.
 */
public class OstrakonItem extends Item {
    private static final int WRAP = 44;

    public OstrakonItem(Properties props) {
        super(props.stacksTo(16));
    }

    private static Ostraka.Joke joke(ItemStack stack) {
        String id = stack.get(ModComponents.JOKE.get());
        return id == null ? null : Ostraka.joke(id);
    }

    /** Text in Zeilen von hoechstens {@link #WRAP} Zeichen umbrechen (an Leerzeichen). */
    static List<String> wrap(String text) {
        List<String> lines = new java.util.ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (String w : text.split(" ")) {
            if (cur.length() > 0 && cur.length() + 1 + w.length() > WRAP) {
                lines.add(cur.toString());
                cur.setLength(0);
            }
            if (cur.length() > 0) cur.append(' ');
            cur.append(w);
        }
        if (cur.length() > 0) lines.add(cur.toString());
        return lines;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        Ostraka.Joke joke = joke(stack);
        if (joke == null) {
            tip.add(Component.translatable("tooltip.scribery.ostrakon_blank").withStyle(ChatFormatting.GRAY));
            return;
        }
        for (String line : wrap(joke.greek())) tip.add(Component.literal(line).withStyle(ChatFormatting.GOLD));
        tip.add(Component.translatable("tooltip.scribery.ostrakon_hint").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        Ostraka.Joke joke = joke(stack);
        if (joke == null) return InteractionResultHolder.pass(stack);
        if (level.isClientSide) {
            // Scherbe anschauen: der griechische Text auf Ton, wie auf dem Pult
            if (!player.isShiftKeyDown()) de.scribery.client.ClientHooks.openFragmentView(Ostraka.fragmentIndex(stack), LecternBlockEntity.OSTRAKON_LOOK);
        } else {
            if (player.isShiftKeyDown()) {
                player.sendSystemMessage(Component.translatable("tooltip.scribery.ostrakon_solution").withStyle(ChatFormatting.GRAY));
                for (String line : wrap(joke.en())) player.sendSystemMessage(Component.literal(line).withStyle(ChatFormatting.GREEN));
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
