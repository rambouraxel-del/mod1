package fr.terresvivantes.orchard;

import fr.terresvivantes.TerresVivantes;
import fr.terresvivantes.core.TVModule;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Module Orchard (contrat v0.2) : enregistrement vide, contenu à venir. */
public final class OrchardModule implements TVModule {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(TerresVivantes.MODID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TerresVivantes.MODID);

    @Override
    public void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
    }
}
