package de.scribery;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;

/**
 * Keine Monster in den Katakomben: Die Gaenge sind dunkel (es brennt kein Licht), also wuerden sie dort sonst spawnen. Ein
 * Monster spawnt nicht, wenn in der Naehe ein Block der Katakombe liegt (Tag {@code scribery:catacomb_blocks}: Ziegel,
 * Bodenplatten, Mosaike). Betrifft natuerliches Spawnen und das Spawnen bei der Chunk-Erzeugung; Spawner,
 * Spawn-Eier und Befehle bleiben unberuehrt.
 */
@EventBusSubscriber(modid = Scribery.MODID)
public final class CatacombSpawnGuard {
    public static final TagKey<Block> CATACOMB_BLOCKS =
            TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(Scribery.MODID, "catacomb_blocks"));

    private CatacombSpawnGuard() {}

    @SubscribeEvent
    static void onPositionCheck(MobSpawnEvent.PositionCheck event) {
        MobSpawnType type = event.getSpawnType();
        if (type != MobSpawnType.NATURAL && type != MobSpawnType.CHUNK_GENERATION) return;
        if (event.getEntity().getType().getCategory() != MobCategory.MONSTER) return;
        BlockPos pos = BlockPos.containing(event.getX(), event.getY(), event.getZ());
        if (nearCatacomb(event.getLevel(), pos)) event.setResult(MobSpawnEvent.PositionCheck.Result.FAIL);
    }

    /** Liegt in zwei Bloecken Umkreis (seitlich) und vier Bloecken darunter bis einem darueber ein Block der Katakombe? */
    static boolean nearCatacomb(LevelAccessor level, BlockPos pos) {
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-2, -4, -2), pos.offset(2, 1, 2))) {
            if (level.getBlockState(p).is(CATACOMB_BLOCKS)) return true;
        }
        return false;
    }
}
