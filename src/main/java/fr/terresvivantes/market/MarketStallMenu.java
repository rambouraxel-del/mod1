package fr.terresvivantes.market;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Menu de l'étal : 9 cases de vente en haut, la caisse (pièces seulement) dessous, puis l'inventaire du joueur.
 * Côté client, {@link #fromNetwork} crée un conteneur vide; le contenu réel est synchronisé par le serveur.
 */
public final class MarketStallMenu extends AbstractContainerMenu {

    private static final int SALE_END = MarketStallBlockEntity.SALE_SLOTS;
    private static final int TILL = MarketStallBlockEntity.TILL_SLOT;
    private static final int PLAYER_START = TILL + 1;

    private final Container container;

    public MarketStallMenu(int containerId, Inventory inventory, Container container) {
        super(MarketModule.STALL_MENU.get(), containerId);
        this.container = container;
        for (int col = 0; col < SALE_END; col++) {
            addSlot(new Slot(container, col, 8 + col * 18, 18));
        }
        addSlot(new Slot(container, TILL, 80, 40) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return MarketStallBlockEntity.isCoin(stack);
            }
        });
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 58 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 8 + col * 18, 116));
        }
    }

    /** Constructeur client : le menu est créé par le type, sans bloc; le contenu arrive par synchronisation. */
    public static MarketStallMenu fromNetwork(int containerId, Inventory inventory) {
        return new MarketStallMenu(containerId, inventory, new SimpleContainer(MarketStallBlockEntity.SALE_SLOTS + 1));
    }

    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            if (index < PLAYER_START) {
                if (!moveItemStackTo(stack, PLAYER_START, slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (MarketStallBlockEntity.isCoin(stack)) {
                if (!moveItemStackTo(stack, TILL, TILL + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(stack, 0, SALE_END, false)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
            if (stack.getCount() == result.getCount()) {
                return ItemStack.EMPTY;
            }
            slot.onTake(player, stack);
        }
        return result;
    }
}
