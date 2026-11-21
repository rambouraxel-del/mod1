package fr.terresvivantes.storage;

import fr.terresvivantes.storage.logic.CrateLogic;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Caisse : un seul type d'item, jusqu'à 8 stacks. Le contenu est synchronisé vers le client pour le rendu.
 */
public final class CrateBlockEntity extends BlockEntity {

    private static final String TAG_ITEM = "Item";
    private static final String TAG_COUNT = "Count";

    @Nullable
    private Item item;
    private int count;

    /** Clics récents (non sauvegardés) : détection du double clic. */
    private long lastClickTick = CrateLogic.NO_CLICK;
    @Nullable
    private UUID lastClicker;

    private final IItemHandler handler = new Handler();

    public CrateBlockEntity(BlockPos pos, BlockState state) {
        super(StorageModule.CRATE_BE.get(), pos, state);
    }

    /** Item stocké, ou null si la caisse est vide. */
    @Nullable
    public Item item() {
        return item;
    }

    public int count() {
        return count;
    }

    /** Taille de stack de l'item stocké (64 si vide). */
    public int maxStack() {
        return item == null ? 64 : new ItemStack(item).getMaxStackSize();
    }

    /** Capacité totale de la caisse pour son item courant (512 pour une stack de 64). */
    public int capacity() {
        return CrateLogic.capacity(maxStack());
    }

    /** Pose le contenu (utilisé à la pose d'une caisse qui contenait des items). */
    public void setContents(@Nullable Item newItem, int newCount) {
        this.item = newCount > 0 ? newItem : null;
        this.count = this.item == null ? 0 : newCount;
        changed();
    }

    /** Capacité d'items exposée aux trémies et aux autres blocs. */
    public IItemHandler itemHandler() {
        return handler;
    }

    /** Insère le stack en main (ou une partie) et réduit {@code source} de ce qui est entré. Renvoie le nombre entré. */
    public int insertFrom(ItemStack source) {
        ItemStack rest = handler.insertItem(0, source.copy(), false);
        int moved = source.getCount() - rest.getCount();
        source.shrink(moved);
        return moved;
    }

    /** Retire jusqu'à {@code amount} items (au plus une stack). */
    public ItemStack extract(int amount) {
        return handler.extractItem(0, amount, false);
    }

    /** Enregistre un clic droit ; renvoie vrai si c'est le second clic d'un double clic. */
    public boolean registerClick(Player player, long now) {
        boolean doubleClick = player.getUUID().equals(lastClicker)
                && CrateLogic.isDoubleClick(lastClickTick, now);
        lastClicker = player.getUUID();
        lastClickTick = doubleClick ? CrateLogic.NO_CLICK : now;
        return doubleClick;
    }

    /** Écrit le contenu dans le composant d'un item de caisse (vide : stack inchangée). */
    public void writeTo(ItemStack stack) {
        if (item != null && count > 0) {
            stack.set(StorageModule.CRATE_CONTENTS.get(), new CrateContents(item, count));
        }
    }

    /** Lit le contenu depuis le composant d'un item de caisse posé. */
    public void readFrom(ItemStack stack) {
        CrateContents contents = stack.get(StorageModule.CRATE_CONTENTS.get());
        if (contents != null) {
            setContents(contents.item(), contents.count());
        }
    }

    private void changed() {
        setChanged();
        if (level != null && !level.isClientSide) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (item != null && count > 0) {
            tag.putString(TAG_ITEM, BuiltInRegistries.ITEM.getKey(item).toString());
            tag.putInt(TAG_COUNT, count);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ResourceLocation id = ResourceLocation.tryParse(tag.getString(TAG_ITEM));
        if (id != null && BuiltInRegistries.ITEM.containsKey(id) && tag.contains(TAG_COUNT)) {
            item = BuiltInRegistries.ITEM.get(id);
            count = Math.max(0, tag.getInt(TAG_COUNT));
        } else {
            item = null;
            count = 0;
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    @Nullable
    public Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /** Vue unique : insertion dans le stock, extraction par stacks (au plus la taille de stack de l'item). */
    private final class Handler implements IItemHandler {

        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            if (slot != 0 || item == null) {
                return ItemStack.EMPTY;
            }
            return new ItemStack(item, Math.min(count, new ItemStack(item).getMaxStackSize()));
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (slot != 0 || stack.isEmpty()) {
                return stack;
            }
            boolean sameItem = item != null && stack.is(item);
            if (!CrateLogic.accepts(item == null, sameItem)) {
                return stack;
            }
            int moved = CrateLogic.insertable(count, CrateLogic.capacity(stack.getMaxStackSize()), stack.getCount());
            if (moved <= 0) {
                return stack;
            }
            if (!simulate) {
                if (item == null) {
                    item = stack.getItem();
                }
                count += moved;
                changed();
            }
            int rest = stack.getCount() - moved;
            return rest <= 0 ? ItemStack.EMPTY : stack.copyWithCount(rest);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot != 0 || item == null) {
                return ItemStack.EMPTY;
            }
            int take = CrateLogic.extractable(count, amount, new ItemStack(item).getMaxStackSize());
            if (take <= 0) {
                return ItemStack.EMPTY;
            }
            ItemStack out = new ItemStack(item, take);
            if (!simulate) {
                count -= take;
                if (count == 0) {
                    item = null;
                }
                changed();
            }
            return out;
        }

        @Override
        public int getSlotLimit(int slot) {
            return slot == 0 ? capacity() : 0;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == 0 && (item == null || stack.is(item));
        }
    }
}
