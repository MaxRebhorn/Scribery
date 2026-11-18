package de.scribery;

import com.mojang.serialization.MapCodec;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Boden mit Fundstuecken (Suspicious Mud, Sand, Ashes). Hat die Bruststufe {@code dusted} (0 bis 3) nur als
 * Blockzustand fuer die Optik. Noch KEINE Pinsel-Logik: testen mit /setblock ~ ~ ~ scribery:suspicious_sand[dusted=2].
 * Der Untergrund bestimmt das Aussehen des Fragments, das darin steckt (siehe docs/DESIGN-FINDS.md).
 */
public class SuspiciousFindBlock extends Block {
    public static final MapCodec<SuspiciousFindBlock> CODEC = simpleCodec(SuspiciousFindBlock::new);

    public SuspiciousFindBlock(Properties props) {
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
