package fr.terresvivantes.insects.client;

import fr.terresvivantes.TerresVivantes;
import fr.terresvivantes.insects.entity.Ladybug;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Rendu de la coccinelle. */
public final class LadybugRenderer extends MobRenderer<Ladybug, LadybugModel> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(TerresVivantes.MODID, "textures/entity/ladybug.png");

    public LadybugRenderer(EntityRendererProvider.Context context) {
        super(context, new LadybugModel(context.bakeLayer(LadybugModel.LAYER)), 0.03f);
    }

    @Override
    public ResourceLocation getTextureLocation(Ladybug entity) {
        return TEXTURE;
    }
}
