package fr.terresvivantes.insects.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import fr.terresvivantes.TerresVivantes;
import fr.terresvivantes.insects.entity.Butterfly;
import fr.terresvivantes.insects.logic.ButterflyLogic;
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
 * Modèle du papillon : corps fin et deux paires d'ailes plates. Les ailes battent en sinusoïde en vol
 * ({@link ButterflyLogic#flapAngle}); au repos, elles restent presque fermées. Texture 32x32 (une par variante).
 */
public final class ButterflyModel extends EntityModel<Butterfly> {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(TerresVivantes.MODID, "butterfly"), "main");

    /** Ailes au repos : demi-battement des ailes inférieures par rapport aux supérieures. */
    private static final float LOWER_WING_RATIO = 0.8f;

    private final ModelPart root;
    private final ModelPart upperRight;
    private final ModelPart upperLeft;
    private final ModelPart lowerRight;
    private final ModelPart lowerLeft;

    public ButterflyModel(ModelPart root) {
        super(RenderType::entityCutoutNoCull);
        this.root = root;
        this.upperRight = root.getChild("upper_right");
        this.upperLeft = root.getChild("upper_left");
        this.lowerRight = root.getChild("lower_right");
        this.lowerLeft = root.getChild("lower_left");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("body", CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-0.5F, -0.5F, -4.0F, 1.0F, 1.0F, 8.0F),
                PartPose.offset(0.0F, 20.0F, 0.0F));
        root.addOrReplaceChild("upper_right", CubeListBuilder.create()
                        .texOffs(0, 10)
                        .addBox(-4.0F, 0.0F, -2.0F, 4.0F, 0.0F, 4.0F),
                PartPose.offset(-0.5F, 20.0F, -1.0F));
        root.addOrReplaceChild("upper_left", CubeListBuilder.create()
                        .texOffs(0, 10)
                        .addBox(0.0F, 0.0F, -2.0F, 4.0F, 0.0F, 4.0F),
                PartPose.offset(0.5F, 20.0F, -1.0F));
        root.addOrReplaceChild("lower_right", CubeListBuilder.create()
                        .texOffs(0, 18)
                        .addBox(-3.0F, 0.0F, 0.0F, 3.0F, 0.0F, 3.0F),
                PartPose.offset(-0.5F, 20.0F, 1.0F));
        root.addOrReplaceChild("lower_left", CubeListBuilder.create()
                        .texOffs(0, 18)
                        .addBox(0.0F, 0.0F, 0.0F, 3.0F, 0.0F, 3.0F),
                PartPose.offset(0.5F, 20.0F, 1.0F));
        return LayerDefinition.create(mesh, 32, 32);
    }

    @Override
    public void setupAnim(Butterfly entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                          float netHeadYaw, float headPitch) {
        float flap = ButterflyLogic.flapAngle(ageInTicks, entity.isResting());
        // Les deux côtés battent en miroir : un angle positif relève les deux ailes.
        this.upperRight.zRot = flap;
        this.upperLeft.zRot = -flap;
        this.lowerRight.zRot = flap * LOWER_WING_RATIO;
        this.lowerLeft.zRot = -flap * LOWER_WING_RATIO;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay,
                               int color) {
        this.root.render(poseStack, buffer, packedLight, packedOverlay, color);
    }
}
