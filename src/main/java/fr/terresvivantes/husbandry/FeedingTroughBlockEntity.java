package fr.terresvivantes.husbandry;

import fr.terresvivantes.husbandry.logic.FeedType;
import fr.terresvivantes.husbandry.logic.TroughLogic;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.Locale;

/**
 * Auge : stock (0 à {@link TroughLogic#CAPACITY} unités) et type d'aliment. Un seul type à la fois; le type
 * disparaît quand le stock tombe à zéro. Le dessus (propriété {@code fill}) suit le stock.
 *
 * <p>Insertion par trémie : {@link #input()}, insertion seule (pas d'extraction).
 */
public final class FeedingTroughBlockEntity extends BlockEntity {

    private FeedType type;
    private int stock;
    private final IItemHandler input = new TroughInput();

    public FeedingTroughBlockEntity(BlockPos pos, BlockState state) {
        super(HusbandryModule.TROUGH_BE.get(), pos, state);
    }

    @Override
    public void setLevel(Level level) {
        super.setLevel(level);
        if (level != null && !level.isClientSide) {
            HusbandryRegistry.trackTrough(level.dimension(), this);
        }
    }

    @Override
    public void setRemoved() {
        Level level = getLevel();
        if (level != null) {
            HusbandryRegistry.untrackTrough(level.dimension(), this);
        }
        super.setRemoved();
    }

    @Override
    public void onChunkUnloaded() {
        Level level = getLevel();
        if (level != null) {
            HusbandryRegistry.untrackTrough(level.dimension(), this);
        }
        super.onChunkUnloaded();
    }

    /** Type d'aliment présent, null si l'auge est vide. */
    public FeedType type() {
        return type;
    }

    /** Stock en unités. */
    public int stock() {
        return stock;
    }

    public boolean isEmpty() {
        return stock == 0;
    }

    /** Capacité d'insertion par trémie. */
    public IItemHandler input() {
        return input;
    }

    /**
     * Ajoute jusqu'à {@code requested} objets de ce type. Renvoie le nombre d'objets acceptés (0 si refus : autre type
     * présent, ou auge pleine).
     */
    public int insert(FeedType incoming, int requested) {
        int items = TroughLogic.itemsAccepted(type, stock, incoming, requested);
        if (items == 0) {
            return 0;
        }
        if (stock == 0) {
            type = incoming;
        }
        stock += items * incoming.unitsPerItem();
        changed();
        return items;
    }

    /** Consomme une unité (un animal mange). Vrai si l'auge contenait de quoi. */
    public boolean consumeUnit() {
        if (stock <= 0) {
            return false;
        }
        stock--;
        if (stock == 0) {
            type = null;
        }
        changed();
        return true;
    }

    /** Ligne de texte d'état pour l'action bar. */
    public Component describe() {
        String typeKey = type == null ? "empty" : type.name().toLowerCase(Locale.ROOT);
        return Component.translatable("message.terresvivantes.trough.status",
                stock, TroughLogic.CAPACITY,
                Component.translatable("message.terresvivantes.trough.type." + typeKey));
    }

    /** Met à jour la propriété {@code fill}, le comparateur et le client. */
    private void changed() {
        setChanged();
        Level level = getLevel();
        if (level == null) {
            return;
        }
        BlockState state = getBlockState();
        TroughFill fill = TroughFill.of(type, stock);
        if (state.getValue(FeedingTroughBlock.FILL) != fill) {
            level.setBlock(worldPosition, state.setValue(FeedingTroughBlock.FILL, fill), Block.UPDATE_ALL);
        }
        BlockState current = getBlockState();
        level.updateNeighbourForOutputSignal(worldPosition, current.getBlock());
        if (!level.isClientSide) {
            level.sendBlockUpdated(worldPosition, current, current, Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("stock", stock);
        if (type != null) {
            tag.putString("type", type.name());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        stock = Mth.clamp(tag.getInt("stock"), 0, TroughLogic.CAPACITY);
        type = null;
        if (stock > 0 && tag.contains("type")) {
            for (FeedType candidate : FeedType.values()) {
                if (candidate.name().equals(tag.getString("type"))) {
                    type = candidate;
                }
            }
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /** Insertion par trémie : n'accepte que les aliments, jamais d'extraction. */
    private final class TroughInput implements IItemHandler {

        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return ItemStack.EMPTY;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            FeedType feed = HusbandryModule.feedOf(stack);
            if (feed == null || stack.isEmpty()) {
                return stack;
            }
            int items;
            if (simulate) {
                items = TroughLogic.itemsAccepted(type, stock, feed, stack.getCount());
            } else {
                items = insert(feed, stack.getCount());
            }
            return items == 0 ? stack : stack.copyWithCount(stack.getCount() - items);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return HusbandryModule.feedOf(stack) != null;
        }
    }
}
