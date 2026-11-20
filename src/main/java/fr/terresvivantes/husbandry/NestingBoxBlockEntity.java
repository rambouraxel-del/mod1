package fr.terresvivantes.husbandry;

import fr.terresvivantes.husbandry.logic.NestLogic;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * BlockEntity du nichoir : huit emplacements d'œufs. Accepte les œufs (ponte interceptée, trémie) et laisse extraire
 * (trémie, clic droit). {@link NestingBoxBlock#EGG} suit le contenu.
 */
public final class NestingBoxBlockEntity extends BlockEntity {

    private final NestInventory inventory = new NestInventory();

    public NestingBoxBlockEntity(BlockPos pos, BlockState state) {
        super(HusbandryModule.NEST_BE.get(), pos, state);
    }

    @Override
    public void setLevel(Level level) {
        super.setLevel(level);
        if (level != null && !level.isClientSide) {
            HusbandryRegistry.trackNest(level.dimension(), this);
        }
    }

    @Override
    public void setRemoved() {
        Level level = getLevel();
        if (level != null) {
            HusbandryRegistry.untrackNest(level.dimension(), this);
        }
        super.setRemoved();
    }

    @Override
    public void onChunkUnloaded() {
        Level level = getLevel();
        if (level != null) {
            HusbandryRegistry.untrackNest(level.dimension(), this);
        }
        super.onChunkUnloaded();
    }

    /** Capacité par trémie et automatisation : insertion d'œufs, extraction. */
    public IItemHandler handler() {
        return inventory;
    }

    /** Ajoute un œuf dans le premier emplacement qui l'accepte. Vrai si l'œuf est entré. */
    public boolean addEgg(ItemStack egg) {
        ItemStack rest = egg.copy();
        for (int slot = 0; slot < NestLogic.SLOTS && !rest.isEmpty(); slot++) {
            rest = inventory.insertItem(slot, rest, false);
        }
        return rest.isEmpty();
    }

    /** Vrai s'il n'y a plus de place pour un œuf. */
    public boolean isFull() {
        ItemStack egg = new ItemStack(Items.EGG);
        for (int slot = 0; slot < NestLogic.SLOTS; slot++) {
            if (inventory.insertItem(slot, egg, true).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /** Nombre total d'œufs dans le nichoir. */
    public int eggCount() {
        int count = 0;
        for (int slot = 0; slot < NestLogic.SLOTS; slot++) {
            count += inventory.getStackInSlot(slot).getCount();
        }
        return count;
    }

    /** Donne tous les œufs au joueur. Renvoie le nombre d'œufs récupérés. */
    public int takeEggs(Player player) {
        int taken = 0;
        for (int slot = 0; slot < NestLogic.SLOTS; slot++) {
            ItemStack stack = inventory.extractItem(slot, Integer.MAX_VALUE, false);
            if (!stack.isEmpty()) {
                taken += stack.getCount();
                player.getInventory().placeItemBackInInventory(stack);
            }
        }
        return taken;
    }

    /** Lâche les œufs à la destruction du bloc. */
    public void dropContents(Level level, BlockPos pos) {
        for (int slot = 0; slot < NestLogic.SLOTS; slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                Block.popResource(level, pos, stack);
            }
        }
    }

    private void onInventoryChanged() {
        setChanged();
        Level level = getLevel();
        if (level == null) {
            return;
        }
        BlockState state = getBlockState();
        boolean hasEggs = eggCount() > 0;
        if (state.getValue(NestingBoxBlock.EGG) != hasEggs) {
            level.setBlock(worldPosition, state.setValue(NestingBoxBlock.EGG, hasEggs), Block.UPDATE_ALL);
        }
        if (!level.isClientSide) {
            BlockState current = getBlockState();
            level.sendBlockUpdated(worldPosition, current, current, Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("eggs", inventory.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        inventory.deserializeNBT(registries, tag.getCompound("eggs"));
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /** Huit emplacements, œufs seulement. */
    private final class NestInventory extends ItemStackHandler {

        NestInventory() {
            super(NestLogic.SLOTS);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.is(HusbandryModule.NEST_EGGS);
        }

        @Override
        protected void onContentsChanged(int slot) {
            onInventoryChanged();
        }
    }
}
