package fr.terresvivantes.storage.client;

import fr.terresvivantes.storage.StorageModule;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Côté client : rendu de la caisse (item et nombre). Chargé seulement sur le client. */
public final class StorageClient {

    private StorageClient() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(StorageClient::onRegisterRenderers);
    }

    private static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(StorageModule.CRATE_BE.get(), CrateRenderer::new);
    }
}
