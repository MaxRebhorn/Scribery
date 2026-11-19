package de.scribery.client;

import java.util.function.Function;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Zeichnet den aufgelegten Gegenstand (Fragment oder Bogen) flach auf die Oberseite des Blocks. */
public final class LecternRenderer<T extends BlockEntity> implements BlockEntityRenderer<T> {
    private final Function<T, ItemStack> stack;
    private final double height;
    private final double forward;

    /** height: Hoehe der Auflageflaeche in Bloecken, forward: Verschiebung zur Vorderseite hin (passend zum Modell). */
    public LecternRenderer(Function<T, ItemStack> stack, double height, double forward) {
        this.stack = stack;
        this.height = height;
        this.forward = forward;
    }

    @Override
    public void render(T be, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        ItemStack s = stack.apply(be);
        if (s.isEmpty()) return;
        Direction facing = be.getBlockState().getValue(HorizontalDirectionalBlock.FACING);
        pose.pushPose();
        pose.translate(0.5, height, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
        pose.translate(0, 0, forward);   // nach der Drehung zeigt +z zur Vorderseite
        pose.mulPose(Axis.XP.rotationDegrees(-90));
        pose.scale(0.75f, 0.75f, 0.75f);
        Minecraft.getInstance().getItemRenderer().renderStatic(s, ItemDisplayContext.FIXED, light, overlay,
                pose, buffers, be.getLevel(), (int) be.getBlockPos().asLong());
        pose.popPose();
    }
}
