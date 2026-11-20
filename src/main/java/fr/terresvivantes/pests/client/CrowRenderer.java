package fr.terresvivantes.pests.client;

import com.mojang.blaze3d.vertex.PoseStack;
import fr.terresvivantes.TerresVivantes;
import fr.terresvivantes.pests.entity.Crow;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Rendu du corbeau : texture unique, un peu plus petit qu'un canard. */
public final class CrowRenderer extends MobRenderer<Crow, CrowModel> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(TerresVivantes.MODID, "textures/entity/crow.png");
    private static final float SCALE = 0.85F;

    public CrowRenderer(EntityRendererProvider.Context context) {
        super(context, new CrowModel(context.bakeLayer(CrowModel.LAYER)), 0.15F);
    }

    @Override
    public ResourceLocation getTextureLocation(Crow crow) {
        return TEXTURE;
    }

    @Override
    protected void scale(Crow crow, PoseStack poseStack, float partialTick) {
        poseStack.scale(SCALE, SCALE, SCALE);
    }
}
