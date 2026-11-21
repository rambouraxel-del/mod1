package fr.terresvivantes.duck.client;

import com.mojang.blaze3d.vertex.PoseStack;
import fr.terresvivantes.TerresVivantes;
import fr.terresvivantes.duck.entity.Duck;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Rendu du canard : colvert pour le mâle, brune pour la femelle, choisie d'après la donnée synchronisée. */
public final class DuckRenderer extends MobRenderer<Duck, DuckModel> {

    private static final ResourceLocation MALE =
            ResourceLocation.fromNamespaceAndPath(TerresVivantes.MODID, "textures/entity/duck.png");
    private static final ResourceLocation FEMALE =
            ResourceLocation.fromNamespaceAndPath(TerresVivantes.MODID, "textures/entity/duck_female.png");
    private static final float BABY_SCALE = 0.5F;

    public DuckRenderer(EntityRendererProvider.Context context) {
        super(context, new DuckModel(context.bakeLayer(DuckModel.LAYER)), 0.3F);
    }

    @Override
    public ResourceLocation getTextureLocation(Duck duck) {
        return duck.isMale() ? MALE : FEMALE;
    }

    @Override
    protected void scale(Duck duck, PoseStack poseStack, float partialTick) {
        if (duck.isBaby()) {
            poseStack.scale(BABY_SCALE, BABY_SCALE, BABY_SCALE);
        }
    }
}
