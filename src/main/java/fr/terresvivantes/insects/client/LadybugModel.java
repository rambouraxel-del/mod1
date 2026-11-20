package fr.terresvivantes.insects.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import fr.terresvivantes.TerresVivantes;
import fr.terresvivantes.insects.entity.Ladybug;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/**
 * Modèle de la coccinelle : dôme rouge à points noirs (texture 32x32), élytres en deux moitiés qui s'ouvrent en vol.
 */
public final class LadybugModel extends EntityModel<Ladybug> {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(TerresVivantes.MODID, "ladybug"), "main");

    /** Angle d'ouverture des élytres en vol, en radians. */
    private static final float OPEN_ANGLE = 0.8f;

    private final ModelPart root;
    private final ModelPart elytraLeft;
    private final ModelPart elytraRight;

    public LadybugModel(ModelPart root) {
        super(RenderType::entityCutoutNoCull);
        this.root = root;
        this.elytraLeft = root.getChild("elytra_left");
        this.elytraRight = root.getChild("elytra_right");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("body", CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-1.5F, -2.0F, -2.0F, 3.0F, 2.0F, 4.0F),
                PartPose.offset(0.0F, 24.0F, 0.0F));
        root.addOrReplaceChild("head", CubeListBuilder.create()
                        .texOffs(0, 16)
                        .addBox(-1.0F, -1.5F, -3.0F, 2.0F, 1.5F, 1.0F),
                PartPose.offset(0.0F, 24.0F, 0.0F));
        root.addOrReplaceChild("elytra_left", CubeListBuilder.create()
                        .texOffs(0, 8)
                        .addBox(0.0F, -2.2F, 0.0F, 1.5F, 2.2F, 4.0F),
                PartPose.offset(0.0F, 24.0F, -2.0F));
        root.addOrReplaceChild("elytra_right", CubeListBuilder.create()
                        .texOffs(0, 8)
                        .addBox(-1.5F, -2.2F, 0.0F, 1.5F, 2.2F, 4.0F),
                PartPose.offset(0.0F, 24.0F, -2.0F));
        return LayerDefinition.create(mesh, 32, 32);
    }

    @Override
    public void setupAnim(Ladybug entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                          float netHeadYaw, float headPitch) {
        // Élytres fermés au sol, ouverts en l'air. Chaque moitié se relève vers l'extérieur.
        float open = entity.onGround() ? 0.0f : OPEN_ANGLE;
        this.elytraLeft.zRot = -open;
        this.elytraRight.zRot = open;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay,
                               int color) {
        this.root.render(poseStack, buffer, packedLight, packedOverlay, color);
    }
}
