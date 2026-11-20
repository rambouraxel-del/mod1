package fr.terresvivantes.apiculture;

import fr.terresvivantes.apiculture.logic.ApiaryLogic;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Quatre emplacements de l'apiaire. Entrée : cadres vides (insertion par trémie). Sortie : cadres de miel
 * uniquement (extraction par trémie). Les cadres vides se remplissent sur place par {@link ApiaryBlockEntity}.
 */
final class ApiaryInventory extends ItemStackHandler {

    private final ApiaryBlockEntity owner;

    ApiaryInventory(ApiaryBlockEntity owner) {
        super(ApiaryLogic.FRAME_SLOTS);
        this.owner = owner;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return stack.is(ApicultureModule.EMPTY_FRAME.get());
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (!getStackInSlot(slot).is(ApicultureModule.HONEY_FRAME.get())) {
            return ItemStack.EMPTY;
        }
        return super.extractItem(slot, amount, simulate);
    }

    @Override
    protected void onContentsChanged(int slot) {
        owner.onInventoryChanged();
    }
}
