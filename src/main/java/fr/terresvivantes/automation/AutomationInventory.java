package fr.terresvivantes.automation;

import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.function.Predicate;

/** Inventaire interne d'une machine automatique : un nombre fixe de cases, avec un filtre d'entrée. */
public final class AutomationInventory implements Container {

    private final NonNullList<ItemStack> items;
    private final Predicate<ItemStack> accepts;
    private final Runnable onChange;

    AutomationInventory(int size, Predicate<ItemStack> accepts, Runnable onChange) {
        this.items = NonNullList.withSize(size, ItemStack.EMPTY);
        this.accepts = accepts;
        this.onChange = onChange;
    }

    /** Contenu brut, pour la sauvegarde. */
    public NonNullList<ItemStack> items() {
        return items;
    }

    /** Vrai s'il reste au moins une case vide. */
    public boolean hasEmptySlot() {
        for (ItemStack stack : items) {
            if (stack.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int getContainerSize() {
        return items.size();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack taken = ContainerHelper.removeItem(items, slot, amount);
        if (!taken.isEmpty()) {
            setChanged();
        }
        return taken;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        if (stack.getCount() > getMaxStackSize()) {
            stack.setCount(getMaxStackSize());
        }
        setChanged();
    }

    @Override
    public void setChanged() {
        onChange.run();
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void clearContent() {
        items.replaceAll(stack -> ItemStack.EMPTY);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return accepts.test(stack);
    }
}
