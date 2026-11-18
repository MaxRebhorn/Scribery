package de.scribery;

import com.mojang.serialization.MapCodec;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Schlamm mit Fundstuecken. Hat die Bruststufe {@code dusted} (0 bis 3) nur als Blockzustand fuer die Optik.
 * Noch KEINE Pinsel-Logik: testen mit /setblock ~ ~ ~ scribery:suspicious_archive_mud[dusted=2].
 */
public class SuspiciousMudBlock extends Block {
    public static final MapCodec<SuspiciousMudBlock> CODEC = simpleCodec(SuspiciousMudBlock::new);

    public SuspiciousMudBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(BlockStateProperties.DUSTED, 0));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(BlockStateProperties.DUSTED);
    }
}
