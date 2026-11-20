package fr.terresvivantes.processing;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Façade de la transformation (v0.5). Un module déclare sa machine avec {@link #registerMachine}, puis expose son bloc
 * et sa BlockEntity (héritant de {@link ProcessingBlock} et {@link ProcessingBlockEntity}), branche la capacité d'items
 * avec {@link #registerItemHandler} et ajoute ses recettes JSON dans {@code data/terresvivantes/recipe/}.
 */
public final class ProcessingApi {

    private static final Map<String, MachineType> MACHINES = new ConcurrentHashMap<>();

    private ProcessingApi() {
    }

    /**
     * Déclare une machine. Renvoie son type, à passer à la BlockEntity. Un identifiant déjà déclaré lève une exception.
     */
    public static MachineType registerMachine(String machineId, int inputSlots, boolean container) {
        MachineType type = new MachineType(machineId, inputSlots, container);
        if (MACHINES.putIfAbsent(machineId, type) != null) {
            throw new IllegalStateException("machine déjà déclarée : " + machineId);
        }
        return type;
    }

    /** Machine déclarée sous cet identifiant. */
    public static Optional<MachineType> machine(String machineId) {
        return Optional.ofNullable(MACHINES.get(machineId));
    }

    /** Toutes les machines déclarées. */
    public static Collection<MachineType> machines() {
        return MACHINES.values();
    }

    /**
     * Branche la capacité d'items de la BlockEntity : haut = entrées, côtés = conteneur, bas = sortie.
     * À appeler depuis le module qui possède la BlockEntity, sur le bus mod.
     */
    public static <T extends ProcessingBlockEntity> void registerItemHandler(RegisterCapabilitiesEvent event,
                                                                          BlockEntityType<T> type) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, type,
                (blockEntity, side) -> blockEntity.itemHandler(side));
    }
}
