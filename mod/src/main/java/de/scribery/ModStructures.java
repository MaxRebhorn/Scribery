package de.scribery;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Registrierung der Strukturen des Mods fuer die Weltgenerierung (siehe ScriberyStructure, tools/gen_worldgen.py). */
public final class ModStructures {
    public static final DeferredRegister<StructureType<?>> TYPES = DeferredRegister.create(Registries.STRUCTURE_TYPE, Scribery.MODID);
    public static final DeferredRegister<StructurePieceType> PIECES = DeferredRegister.create(Registries.STRUCTURE_PIECE, Scribery.MODID);

    public static final DeferredHolder<StructureType<?>, StructureType<ScriberyStructure>> SCRIBERY =
            TYPES.register("scribery", () -> () -> ScriberyStructure.CODEC);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> PIECE =
            PIECES.register("piece", () -> (StructurePieceType.ContextlessType) ScriberyPiece::new);

    public static void register(IEventBus bus) {
        TYPES.register(bus);
        PIECES.register(bus);
    }

    private ModStructures() {}
}
