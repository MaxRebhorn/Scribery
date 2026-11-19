package de.scribery;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import com.mojang.datafixers.util.Pair;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.util.ObfuscationReflectionHelper;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Haengt das Schreiberhaus (Pult, Presse, Buecherregale) an die Haeuser der Dorftypen Ebene, Taiga und Schnee an.
 * Wueste und Savanne (Akazien) bekommen es nicht. Die Vorlagen liegen unter data/scribery/structure/village_scribe_house_*.nbt
 * (gebaut von DebugVillageHouse); eingehaengt wird beim Serverstart in den bestehenden Pool, damit andere Mods ihre Eintraege behalten.
 */
@EventBusSubscriber(modid = Scribery.MODID)
public final class VillageInjection {
    private static final Logger LOG = LoggerFactory.getLogger("scribery-village");
    /** Gewicht im Haeuser-Pool (die Vanilla-Haeuser wiegen zusammen etwa 40 bis 60): so kommt etwa ein Haus je Dorf vor. */
    static final int WEIGHT = 4;

    private VillageInjection() {}

    @SubscribeEvent
    static void onServerAboutToStart(ServerAboutToStartEvent event) {
        Registry<StructureTemplatePool> pools = event.getServer().registryAccess().registryOrThrow(Registries.TEMPLATE_POOL);
        inject(pools, "minecraft:village/plains/houses", "scribery:village_scribe_house_plains");
        inject(pools, "minecraft:village/taiga/houses", "scribery:village_scribe_house_taiga");
        inject(pools, "minecraft:village/snowy/houses", "scribery:village_scribe_house_snowy");
    }

    @SuppressWarnings("unchecked")
    private static void inject(Registry<StructureTemplatePool> pools, String pool, String template) {
        StructureTemplatePool target = pools.get(ResourceLocation.parse(pool));
        if (target == null) {
            LOG.warn("Dorf-Pool {} nicht gefunden, {} wird nicht eingehaengt", pool, template);
            return;
        }
        try {
            StructurePoolElement element = StructurePoolElement.legacy(template).apply(StructureTemplatePool.Projection.RIGID);
            Field rawF = ObfuscationReflectionHelper.findField(StructureTemplatePool.class, "rawTemplates");
            Field tplF = ObfuscationReflectionHelper.findField(StructureTemplatePool.class, "templates");
            List<Pair<StructurePoolElement, Integer>> raw = new ArrayList<>((List<Pair<StructurePoolElement, Integer>>) rawF.get(target));
            raw.add(Pair.of(element, WEIGHT));
            rawF.set(target, raw);
            ObjectArrayList<StructurePoolElement> templates = (ObjectArrayList<StructurePoolElement>) tplF.get(target);
            for (int i = 0; i < WEIGHT; i++) templates.add(element);
            LOG.info("{} in {} eingehaengt (Gewicht {})", template, pool, WEIGHT);
        } catch (ReflectiveOperationException | RuntimeException e) {
            LOG.error("Konnte {} nicht in {} einhaengen", template, pool, e);
        }
    }
}
