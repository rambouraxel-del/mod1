package fr.terresvivantes.processing;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Inventaire interne d'une machine : entrées, conteneur (optionnel) et sortie. La sortie n'accepte aucune insertion
 * directe : seule la transformation la remplit.
 */
public final class ProcessingInventory extends ItemStackHandler {

    private final MachineType machine;
    private final Runnable onChange;

    ProcessingInventory(MachineType machine, Runnable onChange) {
        super(machine.slotCount());
        this.machine = machine;
        this.onChange = onChange;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return slot != machine.outputSlot();
    }

    @Override
    protected void onContentsChanged(int slot) {
        onChange.run();
    }

    /** Vrai si aucun item n'est présent dans les entrées ni dans le conteneur. */
    public boolean isInputEmpty() {
        for (int slot = 0; slot < machine.outputSlot(); slot++) {
            if (!getStackInSlot(slot).isEmpty()) {
                return false;
            }
        }
        return true;
    }
}
