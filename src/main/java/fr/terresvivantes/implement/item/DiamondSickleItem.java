package fr.terresvivantes.implement.item;

import fr.terresvivantes.crop.CropApi;
import fr.terresvivantes.implement.logic.ImplementsLogic;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Faucille en diamant : meme usage que la faucille (clic droit sur une culture mure, recolte et replante),
 * sur une zone 5x5 au lieu de 3x3. Une utilisation de durabilite par culture recoltee.
 * La recolte passe par {@link CropApi#harvestAndReplant}.
 */
public final class DiamondSickleItem extends Item {

    public DiamondSickleItem(Properties properties) {
        super(properties);
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        return isPlant(state) ? 12.0f : super.getDestroySpeed(stack, state);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos center = context.getClickedPos();
        if (!CropApi.isMatureCrop(level.getBlockState(center))) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide() || !(level instanceof ServerLevel server)) {
            return InteractionResult.SUCCESS;
        }
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }

        int harvested = 0;
        for (int[] offset : ImplementsLogic.square(ImplementsLogic.SICKLE_RADIUS)) {
            BlockPos target = center.offset(offset[0], 0, offset[1]);
            if (!CropApi.isMatureCrop(server.getBlockState(target))) {
                continue;
            }
            List<ItemStack> drops = CropApi.harvestAndReplant(server, target, player);
            for (ItemStack drop : drops) {
                Block.popResource(server, target, drop);
            }
            harvested++;
        }

        if (harvested > 0) {
            EquipmentSlot slot = context.getHand() == InteractionHand.OFF_HAND
                    ? EquipmentSlot.OFFHAND : EquipmentSlot.MAINHAND;
            context.getItemInHand().hurtAndBreak(harvested, player, slot);
        }
        return InteractionResult.SUCCESS;
    }

    private static boolean isPlant(BlockState state) {
        return state.is(BlockTags.CROPS)
                || state.is(BlockTags.LEAVES)
                || state.is(BlockTags.FLOWERS)
                || state.is(BlockTags.SAPLINGS)
                || state.is(Blocks.SHORT_GRASS)
                || state.is(Blocks.TALL_GRASS)
                || state.is(Blocks.FERN);
    }
}
