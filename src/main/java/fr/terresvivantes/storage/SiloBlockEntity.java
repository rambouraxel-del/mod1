package fr.terresvivantes.storage;

import fr.terresvivantes.storage.logic.SiloLogic;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * BlockEntity d'un bloc de silo. Seul le bloc du bas d'une colonne (le contrôleur) stocke les stacks; les blocs
 * au-dessus délèguent au contrôleur. Les stacks sont fusionnées par item, plusieurs types sont permis.
 */
public final class SiloBlockEntity extends BlockEntity {

    private static final String TAG_STACKS = "Stacks";

    /** Stacks du contrôleur (vide sur les blocs qui délèguent). */
    private final List<ItemStack> stacks = new ArrayList<>();

    public SiloBlockEntity(BlockPos pos, BlockState state) {
        super(StorageModule.SILO_BE.get(), pos, state);
    }

    /** Vrai si ce bloc est le bas de sa colonne (il stocke). */
    public boolean isController() {
        return level == null || !level.getBlockState(worldPosition.below()).is(StorageModule.SILO.get());
    }

    /** Contrôleur de la colonne : ce bloc, ou le bloc le plus bas (recherche bornée à la hauteur max). */
    public SiloBlockEntity controller() {
        if (level == null) {
            return this;
        }
        BlockPos pos = worldPosition;
        int steps = 0;
        while (steps < SiloLogic.MAX_HEIGHT - 1 && level.getBlockState(pos.below()).is(StorageModule.SILO.get())) {
            pos = pos.below();
            steps++;
        }
        if (pos.equals(worldPosition)) {
            return this;
        }
        return level.getBlockEntity(pos) instanceof SiloBlockEntity below ? below : this;
    }

    /** Nombre de blocs de la colonne à partir de ce bloc vers le haut (ce bloc compris, borné à 8). */
    public int height() {
        if (level == null) {
            return 1;
        }
        int height = 1;
        BlockPos pos = worldPosition;
        while (height < SiloLogic.MAX_HEIGHT && level.getBlockState(pos.above()).is(StorageModule.SILO.get())) {
            pos = pos.above();
            height++;
        }
        return height;
    }

    /** Capacité en stacks de la colonne dont ce bloc est le contrôleur. */
    public int capacity() {
        return SiloLogic.capacity(height());
    }

    /** Stacks stockées (lecture seule), du contrôleur de la colonne. */
    public List<ItemStack> contents() {
        return List.copyOf(controller().stacks);
    }

    /** Nombre total d'items stockés dans la colonne. */
    public int totalItems() {
        int total = 0;
        for (ItemStack stack : controller().stacks) {
            total += stack.getCount();
        }
        return total;
    }

    /** Déplace les stacks d'un ancien contrôleur (bloc placé juste en dessous). */
    public void takeStacksFrom(SiloBlockEntity other) {
        stacks.addAll(other.stacks);
        other.stacks.clear();
        setChanged();
        other.setChanged();
    }

    /** Éjecte le contenu du contrôleur dans le monde (casse ou remplacement du bloc du bas). */
    public void dropContents(Level level, BlockPos pos) {
        if (!isController()) {
            return;
        }
        for (ItemStack stack : stacks) {
            Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
        }
        stacks.clear();
        setChanged();
    }

    /** Vue par face : haut = insertion seule, bas = extraction seule, côtés et null = les deux. */
    @Nullable
    public IItemHandler itemHandler(@Nullable Direction side) {
        if (side == Direction.UP) {
            return new SiloHandler(this, true, false);
        }
        if (side == Direction.DOWN) {
            return new SiloHandler(this, false, true);
        }
        return new SiloHandler(this, true, true);
    }

    /** Insère dans le contrôleur ; renvoie ce qui n'a pas pu entrer. */
    ItemStack insert(ItemStack stack, boolean simulate) {
        int remaining = stack.getCount();
        int max = stack.getMaxStackSize();
        for (ItemStack existing : stacks) {
            if (remaining <= 0) {
                break;
            }
            if (ItemStack.isSameItemSameComponents(existing, stack) && existing.getCount() < max) {
                int moved = Math.min(remaining, max - existing.getCount());
                if (!simulate) {
                    existing.grow(moved);
                }
                remaining -= moved;
            }
        }
        int capacity = capacity();
        int created = stacks.size();
        while (remaining > 0 && created < capacity) {
            int moved = Math.min(remaining, max);
            if (!simulate) {
                stacks.add(stack.copyWithCount(moved));
            }
            remaining -= moved;
            created++;
        }
        if (!simulate && remaining != stack.getCount()) {
            setChanged();
        }
        return remaining <= 0 ? ItemStack.EMPTY : stack.copyWithCount(remaining);
    }

    /** Extrait jusqu'à {@code amount} items de la stack d'index {@code slot} du contrôleur. */
    ItemStack extract(int slot, int amount, boolean simulate) {
        List<ItemStack> list = controller().stacks;
        if (slot < 0 || slot >= list.size() || amount <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack existing = list.get(slot);
        int take = Math.min(amount, existing.getCount());
        ItemStack out = existing.copyWithCount(take);
        if (!simulate) {
            existing.shrink(take);
            if (existing.isEmpty()) {
                list.remove(slot);
            }
            controller().setChanged();
        }
        return out;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!stacks.isEmpty()) {
            ListTag list = new ListTag();
            for (ItemStack stack : stacks) {
                list.add(stack.save(registries));
            }
            tag.put(TAG_STACKS, list);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        stacks.clear();
        ListTag list = tag.getList(TAG_STACKS, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            ItemStack stack = ItemStack.parseOptional(registries, list.getCompound(i));
            if (!stack.isEmpty()) {
                stacks.add(stack);
            }
        }
    }

    /** Vue d'une face sur le contrôleur. */
    private static final class SiloHandler implements IItemHandler {

        private final SiloBlockEntity owner;
        private final boolean insertable;
        private final boolean extractable;

        SiloHandler(SiloBlockEntity owner, boolean insertable, boolean extractable) {
            this.owner = owner;
            this.insertable = insertable;
            this.extractable = extractable;
        }

        @Override
        public int getSlots() {
            return owner.controller().capacity();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            List<ItemStack> list = owner.controller().stacks;
            return slot >= 0 && slot < list.size() ? list.get(slot).copy() : ItemStack.EMPTY;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (!insertable || stack.isEmpty() || !stack.is(StorageModule.SILO_STORABLE)) {
                return stack;
            }
            return owner.controller().insert(stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (!extractable) {
                return ItemStack.EMPTY;
            }
            return owner.controller().extract(slot, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.is(StorageModule.SILO_STORABLE);
        }
    }
}
