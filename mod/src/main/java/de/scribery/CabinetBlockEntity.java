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
    private String forme = "";
    public static final int FORME_SLOTS = 64;

    public CabinetBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CABINET.get(), pos, state);
    }

    public int[] letters() { return letters.clone(); }
    public int ink() { return ink; }
    public int paper() { return paper; }

    public void addLetters(int index, int n) { letters[index] += n; changed(); }
    public void addInk(int units) { ink += units; changed(); }
    public void addPaper(int n) { paper += n; changed(); }

    /** Die in der Setzform liegenden Lettern, ein Zeichen pro Slot (Leerzeichen = frei). */
    public String forme() { return forme; }

    /** Alle Lettern eines Typs: im Vorrat und in der Setzform. */
    public int[] totalLetters() {
        int[] t = letters.clone();
        for (char c : forme.toCharArray()) {
            int i = de.scribery.core.Translit.ALPHABET.indexOf(c);
            if (i >= 0) t[i]++;
        }
        return t;
    }

    private static boolean validForme(String f) {
        if (f.length() > FORME_SLOTS) return false;
        for (char c : f.toCharArray()) if (c != ' ' && de.scribery.core.Translit.ALPHABET.indexOf(c) < 0) return false;
        return true;
    }

    /**
     * Uebernimmt, was in der Presse verbraucht wurde, und die neue Setzform. Alles als Differenz: Der Client
     * gibt an, wie viel er verbraucht hat (Verschleiss, Tinte, Papier), nie, wie viel da ist. Dadurch gehen
     * Einlagen zwischendurch nicht verloren, und er kann nicht mehr beanspruchen, als im Block liegt.
     * Liefert false bei ungueltigen Werten, dann bleibt alles unveraendert.
     */
    public boolean commit(int[] lost, int inkUsed, int paperUsed, String newForme) {
        if (lost.length != LETTERS || inkUsed < 0 || inkUsed > ink || paperUsed < 0 || paperUsed > paper) return false;
        if (!validForme(newForme)) return false;
        int[] total = totalLetters();
        int[] inForme = new int[LETTERS];
        for (char c : newForme.toCharArray()) {
            int i = de.scribery.core.Translit.ALPHABET.indexOf(c);
            if (i >= 0) inForme[i]++;
        }
        int[] newStock = new int[LETTERS];
        for (int i = 0; i < LETTERS; i++) {
            if (lost[i] < 0 || lost[i] > total[i]) return false;
            newStock[i] = total[i] - lost[i] - inForme[i];
            if (newStock[i] < 0) return false;
        }
        System.arraycopy(newStock, 0, letters, 0, LETTERS);
        ink -= inkUsed;
        paper -= paperUsed;
        forme = newForme;
        changed();
        return true;
    }

    public void clear() {
        java.util.Arrays.fill(letters, 0);
        ink = 0;
        paper = 0;
        forme = "";
        changed();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putIntArray("letters", letters);
        tag.putInt("ink", ink);
        tag.putInt("paper", paper);
        tag.putString("forme", forme);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        int[] in = tag.getIntArray("letters");
        java.util.Arrays.fill(letters, 0);
        System.arraycopy(in, 0, letters, 0, Math.min(in.length, LETTERS));
        ink = tag.getInt("ink");
        paper = tag.getInt("paper");
        forme = tag.getString("forme");
    }
}
