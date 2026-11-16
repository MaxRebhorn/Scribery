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

    /** Index des Platzhalter-Textes, oder -1 wenn nichts auf dem Pult liegt. */
    public int debugFragmentIndex() {
        return fragment.getItem() instanceof ManuscriptFragmentItem ? ManuscriptFragmentItem.indexOf(fragment) : -1;
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
