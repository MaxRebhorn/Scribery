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
    public static final int INPUT_SLOT = 0, RESULT_SLOT = 1, FUEL_SLOT = 2, INV_START = 3, INV_END = 30, HOTBAR_END = 39;

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
    private final Container fuel;
    /** 1, wenn der Tisch brennt (zum Client gespiegelt, fuer Anzeige und Entnahme-Pruefung). */
    private final DataSlot lit = DataSlot.standalone();

    public MintMenu(int id, Inventory inv) {
        this(id, inv, ContainerLevelAccess.NULL);
    }

    public MintMenu(int id, Inventory inv, ContainerLevelAccess access) {
        super(ModMenus.MINT.get(), id);
        this.access = access;
        this.fuel = access.evaluate((lv, p) -> lv.getBlockEntity(p) instanceof MintBlockEntity be ? (Container) be.fuel() : null)
                .orElseGet(() -> new SimpleContainer(1));
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

            /** Letter Types gibt es nur, solange der Ofen brennt oder aus dem Brennstoffplatz angezuendet werden kann. */
            @Override
            public boolean mayPickup(Player player) {
                return lit.get() == 1 || MintBlockEntity.isFuel(fuel.getItem(0));
            }

            @Override
            public void onTake(Player player, ItemStack stack) {
                access.execute((level, pos) -> {
                    if (level.getBlockEntity(pos) instanceof MintBlockEntity be) be.ensureLit();
                });
                input.removeItem(0, 1);
                updateResult();
                access.execute(MintMenu::castSounds);
                super.onTake(player, stack);
            }
        });
        addSlot(new Slot(fuel, 0, 20, 58) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return MintBlockEntity.isFuel(stack);
            }
        });
        for (int r = 0; r < 3; r++) for (int c = 0; c < 9; c++) addSlot(new Slot(inv, c + r * 9 + 9, 8 + c * 18, 84 + r * 18));
        for (int c = 0; c < 9; c++) addSlot(new Slot(inv, c, 8 + c * 18, 142));
        addDataSlot(selected);
        addDataSlot(lit);
    }

    /** Gussgeraeusche: erst Blubbern der Schmelze, dann zwei kurze Hammerschlaege ("Bing-Bing"). */
    private static void castSounds(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos) {
        level.playSound(null, pos, SoundEvents.LAVA_POP, SoundSource.BLOCKS, 0.7f, 0.7f);
        level.playSound(null, pos, SoundEvents.BUCKET_EMPTY_LAVA, SoundSource.BLOCKS, 0.25f, 1.3f);
        if (level.getServer() == null) return;
        var server = level.getServer();
        for (int delay : new int[] {8, 14}) {
            server.tell(new net.minecraft.server.TickTask(server.getTickCount() + delay,
                    () -> level.playSound(null, pos, SoundEvents.ANVIL_PLACE, SoundSource.BLOCKS, 0.35f, 2.0f)));
        }
    }

    public boolean isLit() {
        return lit.get() == 1;
    }

    @Override
    public void broadcastChanges() {
        access.execute((level, pos) -> lit.set(level.getBlockState(pos).getValue(MintBlock.LIT) ? 1 : 0));
        super.broadcastChanges();
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
            } else if (index == FUEL_SLOT) {
                if (!moveItemStackTo(stack, INV_START, HOTBAR_END, false)) return ItemStack.EMPTY;
            } else if (stack.is(Items.IRON_NUGGET)) {
                if (!moveItemStackTo(stack, INPUT_SLOT, INPUT_SLOT + 1, false)) return ItemStack.EMPTY;
            } else if (MintBlockEntity.isFuel(stack)) {
                if (!moveItemStackTo(stack, FUEL_SLOT, FUEL_SLOT + 1, false)) return ItemStack.EMPTY;
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
