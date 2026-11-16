package de.scribery;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Minting Table nach dem Vorbild des Steinmetz: Eisennuggets in den Eingabeslot, in der Liste einen Buchstaben
 * waehlen, aus dem Ausgabeslot die Letter Types nehmen. Pro entnommenem Ergebnis geht ein Nugget auf.
 */
public class MintMenu extends AbstractContainerMenu {
    /** So viele Letter Types ergibt ein Eisennugget. */
    public static final int YIELD = 2;
    public static final int INPUT_SLOT = 0, RESULT_SLOT = 1, INV_START = 2, INV_END = 29, HOTBAR_END = 38;

    private final ContainerLevelAccess access;
    private final DataSlot selected = DataSlot.standalone();
    private final Container input = new SimpleContainer(1) {
        @Override
        public void setChanged() {
            super.setChanged();
            MintMenu.this.slotsChanged(this);
        }
    };
    private final Container result = new SimpleContainer(1);

    public MintMenu(int id, Inventory inv) {
        this(id, inv, ContainerLevelAccess.NULL);
    }

    public MintMenu(int id, Inventory inv, ContainerLevelAccess access) {
        super(ModMenus.MINT.get(), id);
        this.access = access;
        selected.set(-1);
        addSlot(new Slot(input, 0, 20, 33) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(Items.IRON_NUGGET);
            }
        });
        addSlot(new Slot(result, 0, 143, 33) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public void onTake(Player player, ItemStack stack) {
                input.removeItem(0, 1);
                updateResult();
                access.execute((level, pos) -> level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.5f, 1.5f));
                super.onTake(player, stack);
            }
        });
        for (int r = 0; r < 3; r++) for (int c = 0; c < 9; c++) addSlot(new Slot(inv, c + r * 9 + 9, 8 + c * 18, 84 + r * 18));
        for (int c = 0; c < 9; c++) addSlot(new Slot(inv, c, 8 + c * 18, 142));
        addDataSlot(selected);
    }

    public int selected() {
        return selected.get();
    }

    public boolean hasInput() {
        return input.getItem(0).is(Items.IRON_NUGGET);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id < 0 || id >= ModItems.LETTER_ITEMS.size()) return false;
        selected.set(id);
        updateResult();
        return true;
    }

    private void updateResult() {
        int s = selected.get();
        if (hasInput() && s >= 0 && s < ModItems.LETTER_ITEMS.size()) {
            result.setItem(0, new ItemStack(ModItems.LETTER_ITEMS.get(s).get(), YIELD));
        } else {
            result.setItem(0, ItemStack.EMPTY);
        }
        broadcastChanges();
    }

    @Override
    public void slotsChanged(Container c) {
        if (c == input) updateResult();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack copy = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            copy = stack.copy();
            if (index == RESULT_SLOT) {
                if (!moveItemStackTo(stack, INV_START, HOTBAR_END, true)) return ItemStack.EMPTY;
                slot.onQuickCraft(stack, copy);
            } else if (index == INPUT_SLOT) {
                if (!moveItemStackTo(stack, INV_START, HOTBAR_END, false)) return ItemStack.EMPTY;
            } else if (stack.is(Items.IRON_NUGGET)) {
                if (!moveItemStackTo(stack, INPUT_SLOT, INPUT_SLOT + 1, false)) return ItemStack.EMPTY;
            } else if (index >= INV_START && index < INV_END) {
                if (!moveItemStackTo(stack, INV_END, HOTBAR_END, false)) return ItemStack.EMPTY;
            } else if (index >= INV_END && index < HOTBAR_END && !moveItemStackTo(stack, INV_START, INV_END, false)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            slot.setChanged();
            if (stack.getCount() == copy.getCount()) return ItemStack.EMPTY;
            slot.onTake(player, stack);
            broadcastChanges();
        }
        return copy;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        access.execute((level, pos) -> clearContainer(player, input));
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.MINTING_TABLE.get());
    }
}
