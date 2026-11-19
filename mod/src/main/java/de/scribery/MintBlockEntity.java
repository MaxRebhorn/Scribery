package de.scribery;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Restbrenndauer des Minting Table in Ticks. Zaehlt nur herunter, solange der Tisch brennt; danach geht er aus. */
public class MintBlockEntity extends BlockEntity {
    private int burn;

    public MintBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MINT.get(), pos, state);
    }

    public int burn() {
        return burn;
    }

    /** Brennstoff nachlegen: addiert die Brenndauer (wie im Ofen). */
    public void addBurn(int ticks) {
        burn += ticks;
        setChanged();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, MintBlockEntity be) {
        if (be.burn > 0) {
            be.burn--;
            if (be.burn == 0) {
                level.setBlock(pos, state.setValue(MintBlock.LIT, false), 3);
            }
            if (be.burn % 20 == 0) be.setChanged();
        } else if (state.getValue(MintBlock.LIT)) {
            level.setBlock(pos, state.setValue(MintBlock.LIT, false), 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Burn", burn);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        burn = tag.getInt("Burn");
    }
}
