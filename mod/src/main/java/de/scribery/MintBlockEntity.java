package de.scribery;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

/** Der Minting Table: lagert Eisennuggets, aus denen Letter Types gegossen werden. */
public class MintBlockEntity extends SyncedBlockEntity {
    /** So viele Letter Types ergibt ein Eisennugget. */
    public static final int YIELD = 2;
    public static final int MAX_NUGGETS = 4096;

    private int nuggets;

    public MintBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MINT.get(), pos, state);
    }

    public int nuggets() { return nuggets; }

    /** Legt Nuggets ein, bis der Tisch voll ist; liefert, wie viele angenommen wurden. */
    public int add(int n) {
        int taken = Math.min(n, MAX_NUGGETS - nuggets);
        if (taken > 0) { nuggets += taken; changed(); }
        return taken;
    }

    public boolean use(int n) {
        if (nuggets < n) return false;
        nuggets -= n;
        changed();
        return true;
    }

    public int takeAll() {
        int n = nuggets;
        nuggets = 0;
        changed();
        return n;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("nuggets", nuggets);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        nuggets = tag.getInt("nuggets");
    }
}
