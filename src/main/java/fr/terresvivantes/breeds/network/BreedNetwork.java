package fr.terresvivantes.breeds.network;

import fr.terresvivantes.breeds.client.BreedClientCache;
import fr.terresvivantes.core.network.TVNetwork;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Enregistrement des payloads du module breeds, sur la version reseau commune du mod. */
public final class BreedNetwork {

    private BreedNetwork() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(BreedNetwork::onRegisterPayloads);
    }

    private static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar(TVNetwork.PROTOCOL_VERSION).playToClient(
                BreedSyncPayload.TYPE,
                BreedSyncPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(
                        () -> BreedClientCache.put(payload.entityId(), payload.breedId())));
    }
}
