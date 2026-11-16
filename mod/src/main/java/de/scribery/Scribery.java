package de.scribery;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(Scribery.MODID)
public final class Scribery {
    public static final String MODID = "scribery";

    public Scribery(IEventBus modBus) {
        ModBlocks.register(modBus);
        ModBlockEntities.register(modBus);
        ModItems.register(modBus);
    }
}
