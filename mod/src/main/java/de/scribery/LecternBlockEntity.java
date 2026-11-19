package de.scribery;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Haelt das aufgelegte Fragment, wie ein Bilderrahmen. */
public class LecternBlockEntity extends BlockEntity {
    /** Aussehen eines Ostrakons am Pult und in den Bildschirmen (RaggedPaper.SHERD). */
    public static final int OSTRAKON_LOOK = 5;

    private ItemStack fragment = ItemStack.EMPTY;

    public LecternBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LECTERN.get(), pos, state);
    }

    public ItemStack fragment() {
        return fragment;
    }

    public void setFragment(ItemStack stack) {
        fragment = stack;
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    /** Index des Textes, den dieser Gegenstand traegt (Manuskript oder Ostrakon), oder -1, wenn es keinen gibt. */
    public static int indexOfItem(ItemStack stack) {
        if (stack.getItem() instanceof ManuscriptFragmentItem) return ManuscriptFragmentItem.indexOf(stack);
        if (stack.getItem() instanceof OstrakonItem) return Ostraka.fragmentIndex(stack);
        return -1;
    }

    /** Index des Textes auf dem Pult, oder -1 wenn nichts Lesbares darauf liegt. */
    public int debugFragmentIndex() {
        return indexOfItem(fragment);
    }

    /** Zustand des aufgelegten Fragments (0 normal, sonst eine Variante), 0 wenn nichts darauf liegt. */
    public int fragmentVariant() {
        if (fragment.getItem() instanceof OstrakonItem) return OSTRAKON_LOOK;   // Scherbe statt Papyrus
        return fragment.getItem() instanceof ManuscriptFragmentItem ? ManuscriptFragmentItem.variant(fragment) : 0;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!fragment.isEmpty()) tag.put("fragment", fragment.save(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        fragment = tag.contains("fragment") ? ItemStack.parseOptional(registries, tag.getCompound("fragment")) : ItemStack.EMPTY;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        // Ein leeres Paket ignoriert der Client; darum immer mindestens einen Eintrag schicken.
        CompoundTag tag = saveWithoutMetadata(registries);
        if (tag.isEmpty()) tag.putBoolean("sync", true);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
