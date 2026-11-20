package fr.terresvivantes.apiculture;

import fr.terresvivantes.apiculture.logic.HoneyType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Extracteur à miel. Clic droit avec un cadre de miel : une bouteille de miel du type du cadre (prise sur une
 * bouteille en verre de l'inventaire), un cadre vide, et parfois de la cire d'abeille.
 */
public final class HoneyExtractorBlock extends Block {

    public HoneyExtractorBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(ApicultureModule.HONEY_FRAME.get())) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }
        Inventory inventory = player.getInventory();
        int bottle = findItem(inventory, Items.GLASS_BOTTLE);
        if (bottle < 0) {
            player.displayClientMessage(Component.translatable("message.terresvivantes.extractor.no_bottle"), true);
            return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        }

        HoneyType type = stack.getOrDefault(ApicultureModule.HONEY_TYPE.get(), HoneyType.WILDFLOWER);
        inventory.getItem(bottle).shrink(1);
        stack.shrink(1);
        inventory.placeItemBackInInventory(new ItemStack(ApicultureModule.honeyItem(type)));
        inventory.placeItemBackInInventory(new ItemStack(ApicultureModule.EMPTY_FRAME.get()));
        if (level.getRandom().nextBoolean()) {
            inventory.placeItemBackInInventory(new ItemStack(ApicultureModule.BEESWAX.get()));
        }

        if (level instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.FALLING_HONEY, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5,
                    12, 0.3, 0.1, 0.3, 0.0);
        }
        level.playSound(null, pos, SoundEvents.HONEY_BLOCK_SLIDE, SoundSource.BLOCKS, 0.8f, 1.0f);
        return ItemInteractionResult.SUCCESS;
    }

    /** Emplacement de l'inventaire contenant l'objet, ou -1. */
    private static int findItem(Inventory inventory, Item item) {
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            if (inventory.getItem(slot).is(item)) {
                return slot;
            }
        }
        return -1;
    }
}
