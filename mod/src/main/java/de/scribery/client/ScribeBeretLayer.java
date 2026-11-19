package de.scribery.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import de.scribery.Scribery;
import net.minecraft.client.model.VillagerModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.npc.Villager;

/** 3D-Barett fuer den Scribe-Villager: flache, leicht schiefe Scheibe mit Feder, folgt dem Kopf. */
public class ScribeBeretLayer extends RenderLayer<Villager, VillagerModel<Villager>> {
    public static final ModelLayerLocation LOCATION =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(Scribery.MODID, "scribe_beret"), "main");
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Scribery.MODID, "textures/entity/scribe_beret.png");
    private static final ResourceLocation SCRIBE =
            ResourceLocation.fromNamespaceAndPath(Scribery.MODID, "scribe");

    private final ModelPart beret;

    public ScribeBeretLayer(RenderLayerParent<Villager, VillagerModel<Villager>> parent, EntityModelSet models) {
        super(parent);
        this.beret = models.bakeLayer(LOCATION).getChild("beret");
    }

    /** Teile relativ zum Kopf-Ursprung des Villagers (Kopfbox reicht von y=-10 bis y=0). Texturgroesse 64x32. */
    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("beret", CubeListBuilder.create()
                        // Scheibe 11x2x11, liegt knapp ueber dem Scheitel
                        .texOffs(0, 0).addBox(-5.5f, -1.5f, -5.5f, 11.0f, 2.0f, 11.0f)
                        // Feder 1x4x4 am rechten Rand (aus Sicht des Villagers)
                        .texOffs(44, 0).addBox(5.0f, -3.5f, -2.0f, 1.0f, 4.0f, 4.0f),
                PartPose.offsetAndRotation(0.0f, -10.5f, 0.0f, 0.0f, 0.0f, 0.15f));  // leicht schief
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffer, int light, Villager villager, float limbSwing,
                       float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (villager.isBaby() || villager.isInvisible()) return;
        if (!SCRIBE.equals(BuiltInRegistries.VILLAGER_PROFESSION.getKey(villager.getVillagerData().getProfession()))) return;
        pose.pushPose();
        getParentModel().getHead().translateAndRotate(pose);   // folgt Kopfdrehung und -neigung
        VertexConsumer vc = buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        beret.render(pose, vc, light, OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }
}
