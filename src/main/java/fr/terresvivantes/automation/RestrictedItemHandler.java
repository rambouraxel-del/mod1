package fr.terresvivantes.automation;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * Vue d'un inventaire qui autorise seulement l'insertion (trémie vers la planteuse) ou seulement l'extraction
 * (trémie sous la récolteuse). Les autres opérations passent au délégué.
 */
final class RestrictedItemHandler implements IItemHandler {

    private final IItemHandler delegate;
    private final boolean insertAllowed;
    private final boolean extractAllowed;

    RestrictedItemHandler(IItemHandler delegate, boolean insertAllowed, boolean extractAllowed) {
        this.delegate = delegate;
        this.insertAllowed = insertAllowed;
        this.extractAllowed = extractAllowed;
    }

    @Override
    public int getSlots() {
        return delegate.getSlots();
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return delegate.getStackInSlot(slot);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        return insertAllowed ? delegate.insertItem(slot, stack, simulate) : stack;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        return extractAllowed ? delegate.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
    }

    @Override
    public int getSlotLimit(int slot) {
        return delegate.getSlotLimit(slot);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return delegate.isItemValid(slot, stack);
    }
}
