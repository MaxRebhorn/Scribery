package de.scribery.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import de.scribery.LecternBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;

/** Zeichnet das aufgelegte Fragment flach auf die Oberseite. */
public final class LecternRenderer implements BlockEntityRenderer<LecternBlockEntity> {
    @Override
    public void render(LecternBlockEntity be, float partialTick, PoseStack pose, MultiBufferSource buffers,
            int light, int overlay) {
        ItemStack stack = be.fragment();
        if (stack.isEmpty()) return;
        Direction facing = be.getBlockState().getValue(HorizontalDirectionalBlock.FACING);
        pose.pushPose();
        pose.translate(0.5, 1.02, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
        pose.mulPose(Axis.XP.rotationDegrees(-90));
        pose.scale(0.75f, 0.75f, 0.75f);
        Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED, light, overlay,
                pose, buffers, be.getLevel(), (int) be.getBlockPos().asLong());
        pose.popPose();
    }
}
