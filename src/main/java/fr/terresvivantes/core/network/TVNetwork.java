package fr.terresvivantes.core.network;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Enregistrement reseau des payloads. Les modules declarent leurs payloads
 * dans onRegisterPayloads via le registrar de version PROTOCOL_VERSION.
 */
public final class TVNetwork {
    public static final String PROTOCOL_VERSION = "1";

    private TVNetwork() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(TVNetwork::onRegisterPayloads);
    }

    private static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
        // Aucun payload pour l'instant.
    }
}
