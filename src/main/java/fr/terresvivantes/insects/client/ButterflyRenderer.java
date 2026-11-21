package fr.terresvivantes.insects.client;

import fr.terresvivantes.TerresVivantes;
import fr.terresvivantes.insects.entity.Butterfly;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Rendu du papillon : une texture par variante (monarque, piéride, morpho bleu, citron). */
public final class ButterflyRenderer extends MobRenderer<Butterfly, ButterflyModel> {

    private static final ResourceLocation[] TEXTURES = {
            texture("monarch"), texture("cabbage_white"), texture("blue_morpho"), texture("brimstone")
    };

    public ButterflyRenderer(EntityRendererProvider.Context context) {
        super(context, new ButterflyModel(context.bakeLayer(ButterflyModel.LAYER)), 0.05f);
    }

    @Override
    public ResourceLocation getTextureLocation(Butterfly entity) {
        return TEXTURES[entity.getVariant()];
    }

    private static ResourceLocation texture(String variant) {
        return ResourceLocation.fromNamespaceAndPath(TerresVivantes.MODID,
                "textures/entity/butterfly_" + variant + ".png");
    }
}
