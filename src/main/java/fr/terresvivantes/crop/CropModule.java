package fr.terresvivantes.crop;

import fr.terresvivantes.TerresVivantes;
import fr.terresvivantes.core.TVModule;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Module Crop : cultures evoluant par randomTick, sans BlockEntity (stub Phase 0). */
public final class CropModule implements TVModule {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(TerresVivantes.MODID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TerresVivantes.MODID);

    @Override
    public void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
    }
}
