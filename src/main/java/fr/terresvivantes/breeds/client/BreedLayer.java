package fr.terresvivantes.breeds.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import fr.terresvivantes.TerresVivantes;
import fr.terresvivantes.breeds.logic.Breed;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Animal;

import java.util.Optional;

/**
 * Superposition de race : redessine le modèle parent avec la texture de la race (taches Holstein, etc.). Le modèle
 * parent a déjà reçu son animation; adulte et bébé partagent la même superposition.
 */
public final class BreedLayer<T extends Animal, M extends EntityModel<T>> extends RenderLayer<T, M> {

    public BreedLayer(RenderLayerParent<T, M> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, T entity,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        Optional<Breed> breed = BreedClientCache.get(entity.getId());
        if (breed.isEmpty()) {
            return;
        }
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(texture(breed.get())));
        getParentModel().renderToBuffer(poseStack, consumer, packedLight,
                LivingEntityRenderer.getOverlayCoords(entity, 0.0F), -1);
    }

    /** Texture de superposition, par espèce et race : {@code textures/entity/breed/<espèce>_<race>.png}. */
    static ResourceLocation texture(Breed breed) {
        return ResourceLocation.fromNamespaceAndPath(TerresVivantes.MODID,
                "textures/entity/breed/" + breed.species().texturePrefix() + "_" + breed.id() + ".png");
    }
}
