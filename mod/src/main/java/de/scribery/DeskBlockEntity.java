package de.scribery;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/** Der Uebersetzungstisch: haelt den aufgelegten Bogen sowie Tinte, Papier und Holztaefelchen. */
public class DeskBlockEntity extends SyncedBlockEntity {
    private ItemStack sheet = ItemStack.EMPTY;
    private int ink, paper, tablets;

    public DeskBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DESK.get(), pos, state);
    }

    public ItemStack sheet() { return sheet; }
    public int ink() { return ink; }
    public int paper() { return paper; }
    public int tablets() { return tablets; }

    /** Der aufgelegte Bogen, wenn es ein bedruckter ist, der sich uebersetzen laesst. */
    public SheetData sheetData() {
        return sheet.isEmpty() ? null : sheet.get(ModComponents.SHEET.get());
    }

    public void setSheet(ItemStack s) { sheet = s; changed(); }
    public void addInk(int units) { ink += units; changed(); }
    public void addPaper(int n) { paper += n; changed(); }
    public void addTablets(int n) { tablets += n; changed(); }

    /** Uebernimmt Tinte und Taefelchen nach der Arbeit, aber nie mehr, als da waren. */
    public boolean commit(int newInk, int newTablets) {
        if (newInk < 0 || newInk > ink || newTablets < 0 || newTablets > tablets) return false;
        ink = newInk;
        tablets = newTablets;
        changed();
        return true;
    }

    public boolean usePaper() {
        if (paper <= 0) return false;
        paper--;
        changed();
        return true;
    }

    public void clearResources() {
        ink = 0;
        paper = 0;
        tablets = 0;
        changed();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!sheet.isEmpty()) tag.put("sheet", sheet.save(registries));
        tag.putInt("ink", ink);
        tag.putInt("paper", paper);
        tag.putInt("tablets", tablets);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        sheet = tag.contains("sheet") ? ItemStack.parseOptional(registries, tag.getCompound("sheet")) : ItemStack.EMPTY;
        ink = tag.getInt("ink");
        paper = tag.getInt("paper");
        tablets = tag.getInt("tablets");
    }
}
