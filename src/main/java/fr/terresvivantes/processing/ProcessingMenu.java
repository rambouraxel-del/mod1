package fr.terresvivantes.processing;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.SlotItemHandler;

/**
 * Menu générique des machines de transformation. Disposition (coordonnées du coin supérieur gauche des slots) :
 * entrées sur une rangée à y = 17 centrée, conteneur à (26, 35), flèche de progression à (79, 35), sortie à (134, 35),
 * inventaire joueur à partir de y = 84 et barre rapide à y = 142.
 */
public final class ProcessingMenu extends AbstractContainerMenu {

    /** Coordonnées des slots de la machine. */
    public static final int CONTAINER_X = 26;
    public static final int OUTPUT_X = 134;
    public static final int SLOT_Y = 35;
    public static final int INPUT_Y = 17;
    /** Longueur de la flèche de progression, en pixels. */
    public static final int ARROW_WIDTH = 24;

    private final MachineType machine;
    private final Level level;
    private final BlockPos pos;
    private final ContainerData data;

    /** Côté serveur : progression lue directement dans la BlockEntity. */
    public ProcessingMenu(int id, Inventory playerInventory, BlockPos pos, ProcessingBlockEntity machineEntity) {
        this(id, playerInventory, machineEntity, machineEntity.dataAccess());
    }

    /** Côté client : la BlockEntity est dans le monde, la progression arrive par synchronisation. */
    public static ProcessingMenu fromNetwork(int id, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        if (!(playerInventory.player.level().getBlockEntity(pos) instanceof ProcessingBlockEntity machineEntity)) {
            throw new IllegalStateException("machine de transformation absente en " + pos);
        }
        return new ProcessingMenu(id, playerInventory, machineEntity, new SimpleContainerData(2));
    }

    private ProcessingMenu(int id, Inventory playerInventory, ProcessingBlockEntity machineEntity, ContainerData data) {
        super(ProcessingModule.MENU.get(), id);
        this.machine = machineEntity.machine();
        this.level = machineEntity.getLevel();
        this.pos = machineEntity.getBlockPos();
        this.data = data;

        var handler = machineEntity.inventory();
        int inputs = machine.inputSlots();
        int start = 88 - 9 * inputs;
        for (int i = 0; i < inputs; i++) {
            addSlot(new SlotItemHandler(handler, i, start + 18 * i, INPUT_Y));
        }
        if (machine.container()) {
            addSlot(new SlotItemHandler(handler, machine.containerSlot(), CONTAINER_X, SLOT_Y));
        }
        addSlot(new SlotItemHandler(handler, machine.outputSlot(), OUTPUT_X, SLOT_Y));

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
        addDataSlots(data);
    }

    public MachineType machine() {
        return machine;
    }

    /** Progression courante, en ticks. */
    public int progress() {
        return data.get(0);
    }

    /** Durée de la recette courante, en ticks (0 si aucune). */
    public int totalTime() {
        return data.get(1);
    }

    /** Largeur de la barre de progression, entre 0 et {@code pixels}. */
    public int progressScaled(int pixels) {
        int total = totalTime();
        return total <= 0 ? 0 : Math.min(pixels, progress() * pixels / total);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return result;
        }
        ItemStack stack = slot.getItem();
        result = stack.copy();
        int machineSlots = machine.outputSlot() + 1;
        if (index < machineSlots) {
            if (!moveItemStackTo(stack, machineSlots, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 0, machine.outputSlot(), false)) {
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
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(level, pos), player, level.getBlockState(pos).getBlock());
    }
}
