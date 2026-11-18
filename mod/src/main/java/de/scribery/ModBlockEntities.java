package de.scribery;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Scribery.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LecternBlockEntity>> LECTERN =
            TYPES.register("scribe_lectern", () -> BlockEntityType.Builder
                    .of(LecternBlockEntity::new, ModBlocks.SCRIBE_LECTERN.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CabinetBlockEntity>> CABINET =
            TYPES.register("letter_cabinet", () -> BlockEntityType.Builder
                    .of(CabinetBlockEntity::new, ModBlocks.LETTER_CABINET.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DeskBlockEntity>> DESK =
            TYPES.register("translator_desk", () -> BlockEntityType.Builder
                    .of(DeskBlockEntity::new, ModBlocks.TRANSLATOR_DESK.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AmphoraStorageBlockEntity>> AMPHORA_STORAGE =
            TYPES.register("amphora_storage", () -> BlockEntityType.Builder
                    .of(AmphoraStorageBlockEntity::new, ModBlocks.AMPHORA_STORAGE.get()).build(null));

    public static void register(IEventBus bus) {
        TYPES.register(bus);
    }

    private ModBlockEntities() {}
}
