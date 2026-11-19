package de.scribery;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Restbrenndauer des Minting Table in Ticks. Zaehlt nur herunter, solange der Tisch brennt; danach geht er aus. */
public class MintBlockEntity extends BlockEntity {
    private int burn;
    /** Brennstoffplatz im Menue. Brennstoff wird erst verbraucht, wenn der Tisch gebraucht wird und gerade kalt ist. */
    private final SimpleContainer fuel = new SimpleContainer(1);

    public MintBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MINT.get(), pos, state);
        fuel.addListener(c -> setChanged());
    }

    public SimpleContainer fuel() {
        return fuel;
    }

    public static boolean isFuel(ItemStack stack) {
        return stack.getBurnTime(RecipeType.SMELTING) > 0;
    }

    /** Brennt der Tisch, oder koennte er aus dem Brennstoffplatz angezuendet werden? */
    public boolean canRun() {
        return burn > 0 || isFuel(fuel.getItem(0));
    }

    /** Brennt der Tisch nicht, ein Stueck Brennstoff aus dem Platz verbrauchen und anzuenden. Liefert, ob er jetzt brennt. */
    public boolean ensureLit() {
        if (burn <= 0 && level != null) {
            ItemStack f = fuel.getItem(0);
            int ticks = f.getBurnTime(RecipeType.SMELTING);
            if (ticks <= 0) return false;
            ItemStack rest = f.getCraftingRemainingItem();
            f.shrink(1);
            fuel.setItem(0, f.isEmpty() ? rest : f);
            addBurn(ticks);
            BlockState st = getBlockState();
            if (!st.getValue(MintBlock.LIT)) level.setBlock(worldPosition, st.setValue(MintBlock.LIT, true), 3);
            level.playSound(null, worldPosition, net.minecraft.sounds.SoundEvents.FIRECHARGE_USE, net.minecraft.sounds.SoundSource.BLOCKS, 0.6F, 1.2F);
        }
        return burn > 0;
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
        tag.put("Fuel", fuel.createTag(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        burn = tag.getInt("Burn");
        fuel.fromTag(tag.getList("Fuel", 10), registries);
    }
}
