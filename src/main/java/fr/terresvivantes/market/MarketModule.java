package fr.terresvivantes.market;

import fr.terresvivantes.core.TVModule;
import net.neoforged.bus.api.IEventBus;

/**
 * Module market (v0.7) : tableau de contrats et étal. Module vide pour l'instant : la logique viendra
 * dans une livraison ultérieure. Les prix passent par {@code economy.EconomyApi}.
 */
public final class MarketModule implements TVModule {

    @Override
    public void register(IEventBus modBus) {
    }
}
