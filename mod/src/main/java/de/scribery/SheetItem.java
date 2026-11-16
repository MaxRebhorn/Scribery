package de.scribery;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** Ein bedruckter oder uebersetzter Bogen. Der Inhalt steht im Datenkomponenten-Feld {@link ModComponents#SHEET}. */
public class SheetItem extends Item {
    public SheetItem(Properties props) {
        super(props.stacksTo(1));
    }

    public static ItemStack of(Item item, SheetData data) {
        ItemStack s = new ItemStack(item);
        s.set(ModComponents.SHEET.get(), data);
        return s;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        SheetData d = stack.get(ModComponents.SHEET.get());
        if (d == null) return;
        tip.add(Component.literal(d.fragment() < 0 ? "Free print" : "Sheet " + (d.page() + 1)).withStyle(ChatFormatting.GRAY));
        for (String line : d.lines()) tip.add(Component.literal(line).withStyle(ChatFormatting.DARK_GRAY));
        if (!d.english().isEmpty()) tip.add(Component.literal(String.join(" ", d.english())).withStyle(ChatFormatting.GOLD));
    }
}
