package fr.terresvivantes.tools;

import fr.terresvivantes.TerresVivantes;
import fr.terresvivantes.core.TVModule;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Module Tools : outils agricoles (stub Phase 0). */
public final class ToolsModule implements TVModule {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(TerresVivantes.MODID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TerresVivantes.MODID);

    @Override
    public void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
    }
}
