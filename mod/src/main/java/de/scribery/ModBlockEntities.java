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

    public static void register(IEventBus bus) {
        TYPES.register(bus);
    }

    private ModBlockEntities() {}
}
