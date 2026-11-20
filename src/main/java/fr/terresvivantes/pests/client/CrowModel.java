package fr.terresvivantes.pests.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import fr.terresvivantes.TerresVivantes;
import fr.terresvivantes.pests.entity.Crow;
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
import net.minecraft.util.Mth;

/**
 * Modèle du corbeau, texture 64 x 32 (même dépliage UV que le canard). Corps, tête, bec plus long et plus fin,
 * deux ailes, deux pattes. Posé sur une culture, le corbeau penche le corps et baisse la tête; en vol, les ailes
 * battent.
 */
public final class CrowModel extends EntityModel<Crow> {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(TerresVivantes.MODID, "crow"), "main");

    private static final float LIMB_FREQUENCY = 0.6662F;
    private static final float LEG_SWING = 1.2F;
    private static final float FLAP_SPEED = 1.8F;
    private static final float FLAP_AMPLITUDE = 0.9F;
    private static final float PECK_HEAD_PITCH = 0.9F;
    private static final float PECK_BODY_PITCH = 0.6F;

    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart leftWing;
    private final ModelPart rightWing;
    private final ModelPart leftLeg;
    private final ModelPart rightLeg;

    public CrowModel(ModelPart root) {
        super(RenderType::entityCutoutNoCull);
        this.root = root;
        this.body = root.getChild("body");
        this.head = body.getChild("head");
        this.leftWing = body.getChild("left_wing");
        this.rightWing = body.getChild("right_wing");
        this.leftLeg = root.getChild("left_leg");
        this.rightLeg = root.getChild("right_leg");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-3.0F, -5.0F, -4.0F, 6.0F, 5.0F, 8.0F),
                PartPose.offset(0.0F, 22.0F, 0.0F));

        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create()
                        .texOffs(0, 16)
                        .addBox(-2.0F, -5.0F, -3.0F, 4.0F, 5.0F, 3.0F),
                PartPose.offset(0.0F, -5.0F, -4.0F));
        head.addOrReplaceChild("bill", CubeListBuilder.create()
                        .texOffs(16, 16)
                        .addBox(-1.0F, -2.0F, -6.0F, 2.0F, 1.0F, 3.0F),
                PartPose.ZERO);

        body.addOrReplaceChild("left_wing", CubeListBuilder.create()
                        .texOffs(30, 0)
                        .addBox(0.0F, 0.0F, -3.0F, 1.0F, 4.0F, 6.0F),
                PartPose.offset(3.0F, -5.0F, 1.0F));
        body.addOrReplaceChild("right_wing", CubeListBuilder.create()
                        .texOffs(30, 12)
                        .addBox(-1.0F, 0.0F, -3.0F, 1.0F, 4.0F, 6.0F),
                PartPose.offset(-3.0F, -5.0F, 1.0F));

        root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                        .texOffs(46, 0)
                        .addBox(-1.5F, 0.0F, -0.5F, 3.0F, 2.0F, 1.0F),
                PartPose.offset(1.0F, 22.0F, 0.0F));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                        .texOffs(46, 4)
                        .addBox(-1.5F, 0.0F, -0.5F, 3.0F, 2.0F, 1.0F),
                PartPose.offset(-1.0F, 22.0F, 0.0F));

        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(Crow crow, float limbSwing, float limbSwingAmount, float ageInTicks,
                          float netHeadYaw, float headPitch) {
        boolean pecking = crow.isPecking();
        float walk = limbSwing * LIMB_FREQUENCY;
        this.leftLeg.xRot = Mth.cos(walk) * LEG_SWING * limbSwingAmount;
        this.rightLeg.xRot = Mth.cos(walk + Mth.PI) * LEG_SWING * limbSwingAmount;

        this.body.xRot = pecking ? PECK_BODY_PITCH : 0.0F;
        this.head.yRot = netHeadYaw * Mth.DEG_TO_RAD;
        this.head.xRot = pecking ? PECK_HEAD_PITCH + Mth.sin(ageInTicks * 0.5F) * 0.1F : headPitch * Mth.DEG_TO_RAD;

        float flap = pecking ? 0.0F : Mth.sin(ageInTicks * FLAP_SPEED) * FLAP_AMPLITUDE;
        this.leftWing.zRot = flap;
        this.rightWing.zRot = -flap;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay,
                               int color) {
        this.root.render(poseStack, buffer, packedLight, packedOverlay, color);
    }
}
