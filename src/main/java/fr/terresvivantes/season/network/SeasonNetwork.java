package fr.terresvivantes.season.network;

import fr.terresvivantes.core.network.TVNetwork;
import fr.terresvivantes.season.client.ClientSeasonState;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Enregistrement des payloads du module season, sur la version reseau commune du mod. */
public final class SeasonNetwork {

    private SeasonNetwork() {
    }

    public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar(TVNetwork.PROTOCOL_VERSION).playToClient(
                SeasonSyncPayload.TYPE,
                SeasonSyncPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientSeasonState.update(payload)));
    }
}
