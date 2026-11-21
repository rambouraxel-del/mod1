package fr.terresvivantes.duck.client;

import fr.terresvivantes.duck.DuckModule;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Côté client : rendus et modèle du canard et de son œuf lancé. Chargé seulement sur le client. */
public final class DuckClient {

    private DuckClient() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(DuckClient::onRegisterRenderers);
        modBus.addListener(DuckClient::onRegisterLayers);
    }

    private static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(DuckModule.DUCK.get(), DuckRenderer::new);
        event.registerEntityRenderer(DuckModule.THROWN_DUCK_EGG.get(), ThrownItemRenderer::new);
    }

    private static void onRegisterLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(DuckModel.LAYER, DuckModel::createLayer);
    }
}
