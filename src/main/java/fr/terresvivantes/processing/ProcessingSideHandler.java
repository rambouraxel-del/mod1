package fr.terresvivantes.processing;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

/** Vue d'une face de la machine : n'autorise l'insertion que dans certains slots et l'extraction que dans d'autres. */
final class ProcessingSideHandler implements IItemHandler {

    private final ProcessingInventory inventory;
    private final int[] insertable;
    private final int[] extractable;

    ProcessingSideHandler(ProcessingInventory inventory, int[] insertable, int[] extractable) {
        this.inventory = inventory;
        this.insertable = insertable;
        this.extractable = extractable;
    }

    private static boolean allowed(int[] slots, int slot) {
        for (int allowed : slots) {
            if (allowed == slot) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int getSlots() {
        return inventory.getSlots();
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return inventory.getStackInSlot(slot);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (!allowed(insertable, slot)) {
            return stack;
        }
        return inventory.insertItem(slot, stack, simulate);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (!allowed(extractable, slot)) {
            return ItemStack.EMPTY;
        }
        return inventory.extractItem(slot, amount, simulate);
    }

    @Override
    public int getSlotLimit(int slot) {
        return inventory.getSlotLimit(slot);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return allowed(insertable, slot) && inventory.isItemValid(slot, stack);
    }
}
