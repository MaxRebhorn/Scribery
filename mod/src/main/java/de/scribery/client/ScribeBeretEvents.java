package de.scribery.client;

import de.scribery.Scribery;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.VillagerRenderer;
import net.minecraft.world.entity.EntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Meldet Modell und Layer des Scribe-Baretts an. Eigene Klasse, damit ClientModEvents unberuehrt bleibt. */
@EventBusSubscriber(modid = Scribery.MODID, value = Dist.CLIENT)
public final class ScribeBeretEvents {
    private ScribeBeretEvents() {}

    @SubscribeEvent
    static void onLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(ScribeBeretLayer.LOCATION, ScribeBeretLayer::createLayer);
    }

    @SubscribeEvent
    static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        EntityRenderer<?> renderer = event.getRenderer(EntityType.VILLAGER);
        if (renderer instanceof VillagerRenderer villagerRenderer) {
            villagerRenderer.addLayer(new ScribeBeretLayer(villagerRenderer, event.getEntityModels()));
        }
    }
}
