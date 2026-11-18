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

/** Ein Manuskriptfragment. Welches, steht im Datenfeld {@link ModComponents#MANUSCRIPT} (die ID, z. B. "P7"). */
public class ManuscriptFragmentItem extends Item {
    public ManuscriptFragmentItem(Properties props) {
        super(props.stacksTo(1));
    }

    public static ItemStack of(String id) {
        ItemStack s = new ItemStack(ModItems.MANUSCRIPT_FRAGMENT.get());
        s.set(ModComponents.MANUSCRIPT.get(), id);
        return s;
    }

    /** Zustand des Fragments: der Wert von custom_model_data (0 normal, 1 verkohlt, 2 wasser, 3 verblasst, 4 gefroren). */
    public static int variant(ItemStack stack) {
        var cmd = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_MODEL_DATA);
        return cmd == null ? 0 : cmd.value();
    }

    /** Index des Manuskripts in {@link Manuscripts}, oder -1, wenn das Item keins (mehr) kennt. */
    public static int indexOf(ItemStack stack) {
        String id = stack.get(ModComponents.MANUSCRIPT.get());
        return id == null ? -1 : Manuscripts.indexOf(id);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        int index = indexOf(stack);
        if (index < 0) return InteractionResultHolder.pass(stack);
        if (level.isClientSide) de.scribery.client.ClientHooks.openFragmentView(index, variant(stack));
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        int index = indexOf(stack);
        if (index < 0) return;
        Manuscripts.Fragment f = Manuscripts.load().get(index);
        tip.add(Component.literal(f.id() + "  " + f.ref()).withStyle(ChatFormatting.GRAY));
    }
}
