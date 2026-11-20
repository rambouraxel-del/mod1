package fr.terresvivantes.health.client;

import fr.terresvivantes.health.HealthModule;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

/** Côté client : fournisseurs des particules de spores. Chargé seulement sur le client. */
public final class HealthClient {

    /** Oïdium : blanc-gris. */
    private static final float[] MILDEW_COLOR = {0.90f, 0.92f, 0.85f};
    /** Rouille : orangé. */
    private static final float[] RUST_COLOR = {0.85f, 0.45f, 0.16f};

    private HealthClient() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(HealthClient::onRegisterParticles);
    }

    private static void onRegisterParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(HealthModule.SPORE_MILDEW.get(),
                sprites -> (options, level, x, y, z, dx, dy, dz) ->
                        new SporeParticle(level, x, y, z, dx, dy, dz, sprites, MILDEW_COLOR));
        event.registerSpriteSet(HealthModule.SPORE_RUST.get(),
                sprites -> (options, level, x, y, z, dx, dy, dz) ->
                        new SporeParticle(level, x, y, z, dx, dy, dz, sprites, RUST_COLOR));
    }
}
