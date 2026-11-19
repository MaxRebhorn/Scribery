package de.scribery;

import java.util.List;
import java.util.Random;

import de.scribery.core.FindLuck;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * Fundbeutel des Scribe Villagers (ein Item je Fundart). Rechtsklick oeffnet ihn auf dem Server und gibt ein Fragment aus dem
 * Pool dieser Fundart; rund 70 % der Zuege waehlen ein Manuskript, das im Archiv des Spielers noch fehlt (FindLuck).
 */
public class FindPouchItem extends Item {
    private final String find;

    public FindPouchItem(Properties props, String find) {
        super(props.stacksTo(16));
        this.find = find;
    }

    public String find() {
        return find;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer sp)) return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        ArchiveData archive = ArchiveService.data(sp);
        String id = FindLuck.pick(FindPools.pool(find), m -> archive.get(m).found(), FindLuck.MISSING_CHANCE,
                new Random(level.random.nextLong()));
        if (id == null) return InteractionResultHolder.fail(stack);
        if (!player.getAbilities().instabuild) stack.shrink(1);
        ItemStack fragment = ManuscriptFragmentItem.of(id);
        if (!player.getInventory().add(fragment)) player.drop(fragment, false);
        level.playSound(null, player.blockPosition(), SoundEvents.BUNDLE_DROP_CONTENTS, SoundSource.PLAYERS, 1f, 1f);
        return InteractionResultHolder.sidedSuccess(stack, false);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable("tooltip.scribery.find_pouch").withStyle(net.minecraft.ChatFormatting.GRAY));
    }
}
