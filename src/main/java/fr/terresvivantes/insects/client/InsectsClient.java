package fr.terresvivantes.insects.client;

import fr.terresvivantes.insects.InsectsModule;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.common.NeoForge;

/** Côté client : rendus, modèles, particules et lucioles du module insects. Chargé seulement sur le client. */
public final class InsectsClient {

    private InsectsClient() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(InsectsClient::onRegisterRenderers);
        modBus.addListener(InsectsClient::onRegisterLayers);
        modBus.addListener(InsectsClient::onRegisterParticles);
        NeoForge.EVENT_BUS.addListener(FireflySpawner::onClientTick);
    }

    private static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(InsectsModule.BUTTERFLY.get(), ButterflyRenderer::new);
        event.registerEntityRenderer(InsectsModule.LADYBUG.get(), LadybugRenderer::new);
    }

    private static void onRegisterLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(ButterflyModel.LAYER, ButterflyModel::createLayer);
        event.registerLayerDefinition(LadybugModel.LAYER, LadybugModel::createLayer);
    }

    private static void onRegisterParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(InsectsModule.APHID.get(),
                sprites -> (options, level, x, y, z, dx, dy, dz) ->
                        new AphidParticle(level, x, y, z, dx, dy, dz, sprites));
        event.registerSpriteSet(InsectsModule.FIREFLY.get(),
                sprites -> (options, level, x, y, z, dx, dy, dz) ->
                        new FireflyParticle(level, x, y, z, sprites));
    }
}
