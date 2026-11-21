package fr.terresvivantes.duck.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import fr.terresvivantes.TerresVivantes;
import fr.terresvivantes.duck.entity.Duck;
import fr.terresvivantes.duck.logic.DuckLogic;
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
 * Modèle du canard, texture 64 x 32 (UV déplié de boîte standard). Corps, tête avec bec, deux ailes, deux pattes.
 *
 * <p>Animations : pattes alternées à la marche, dandinement du corps, tête qui hoche en marchant et suit le regard,
 * ailes qui battent en vol, en chute ou dans l'eau ({@link DuckLogic#wingFlap}).
 */
public final class DuckModel extends EntityModel<Duck> {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(TerresVivantes.MODID, "duck"), "main");

    private static final float LIMB_FREQUENCY = 0.6662F;
    private static final float LEG_SWING = 1.4F;
    private static final float DANDINE = 0.08F;
    private static final float NOD = 0.1F;

    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart leftWing;
    private final ModelPart rightWing;
    private final ModelPart leftLeg;
    private final ModelPart rightLeg;

    public DuckModel(ModelPart root) {
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
                        .addBox(-2.0F, -2.0F, -5.0F, 4.0F, 1.0F, 2.0F),
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
    public void setupAnim(Duck duck, float limbSwing, float limbSwingAmount, float ageInTicks,
                          float netHeadYaw, float headPitch) {
        float walk = limbSwing * LIMB_FREQUENCY;
        this.leftLeg.xRot = Mth.cos(walk) * LEG_SWING * limbSwingAmount;
        this.rightLeg.xRot = Mth.cos(walk + Mth.PI) * LEG_SWING * limbSwingAmount;

        this.body.zRot = Mth.cos(walk) * DANDINE * limbSwingAmount;
        this.head.yRot = netHeadYaw * Mth.DEG_TO_RAD;
        this.head.xRot = headPitch * Mth.DEG_TO_RAD + Mth.sin(walk * 2.0F) * NOD * limbSwingAmount;

        float flap = DuckLogic.wingFlap(ageInTicks, duck.isWingFlapping());
        this.leftWing.zRot = flap;
        this.rightWing.zRot = -flap;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay,
                               int color) {
        this.root.render(poseStack, buffer, packedLight, packedOverlay, color);
    }
}
