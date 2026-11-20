package fr.terresvivantes.pests.client;

import fr.terresvivantes.pests.PestsModule;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Côté client : rendu et modèle du corbeau. Chargé seulement sur le client. */
public final class PestsClient {

    private PestsClient() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(PestsClient::onRegisterRenderers);
        modBus.addListener(PestsClient::onRegisterLayers);
    }

    private static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(PestsModule.CROW.get(), CrowRenderer::new);
    }

    private static void onRegisterLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(CrowModel.LAYER, CrowModel::createLayer);
    }
}
