package de.scribery;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

/** Der Lettern-Schrank: Vorrat an Letter Types, Tinte und Papier. */
public class CabinetBlockEntity extends SyncedBlockEntity {
    public static final int LETTERS = 22;
    /** Eine Einheit Tinte in der Presse; ein Tintenbeutel gibt so viele. */
    public static final int INK_PER_SAC = 8;

    private final int[] letters = new int[LETTERS];
    private int ink;
    private int paper;

    public CabinetBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CABINET.get(), pos, state);
    }

    public int[] letters() { return letters.clone(); }
    public int ink() { return ink; }
    public int paper() { return paper; }

    public void addLetters(int index, int n) { letters[index] += n; changed(); }
    public void addInk(int units) { ink += units; changed(); }
    public void addPaper(int n) { paper += n; changed(); }

    /** Uebernimmt den Stand aus der Presse, aber nie mehr, als im Schrank liegt. Liefert false bei ungueltigen Werten. */
    public boolean commit(int[] newLetters, int newInk, int newPaper) {
        if (newLetters.length != LETTERS || newInk < 0 || newInk > ink || newPaper < 0 || newPaper > paper) return false;
        for (int i = 0; i < LETTERS; i++) if (newLetters[i] < 0 || newLetters[i] > letters[i]) return false;
        System.arraycopy(newLetters, 0, letters, 0, LETTERS);
        ink = newInk;
        paper = newPaper;
        changed();
        return true;
    }

    public void clear() {
        java.util.Arrays.fill(letters, 0);
        ink = 0;
        paper = 0;
        changed();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putIntArray("letters", letters);
        tag.putInt("ink", ink);
        tag.putInt("paper", paper);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        int[] in = tag.getIntArray("letters");
        java.util.Arrays.fill(letters, 0);
        System.arraycopy(in, 0, letters, 0, Math.min(in.length, LETTERS));
        ink = tag.getInt("ink");
        paper = tag.getInt("paper");
    }
}
