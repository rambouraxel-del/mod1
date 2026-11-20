package fr.terresvivantes.implement.item;

import fr.terresvivantes.implement.logic.ImplementsLogic;
import fr.terresvivantes.soil.SoilApi;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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

/**
 * Grelinette : clic droit sur herbe ou terre laboure la zone 3x3 selon les memes regles que la houe
 * (air au-dessus, face non basse). Sur une terre labouree, aere le sol : +matiere organique,
 * au plus une fois par bloc et par jour MC, avec un court delai d'objet.
 */
public final class BroadforkItem extends Item {

    public BroadforkItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        BlockPos center = context.getClickedPos();
        if (player == null) {
            return InteractionResult.PASS;
        }
        BlockState state = level.getBlockState(center);
        if (state.is(Blocks.FARMLAND)) {
            return aerate(context, level, center, player);
        }
        if (!isTillable(level, center, context.getClickedFace())) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide() || !(level instanceof ServerLevel)) {
            return InteractionResult.SUCCESS;
        }

        int tilled = 0;
        for (int[] offset : ImplementsLogic.square(ImplementsLogic.GRID_RADIUS)) {
            BlockPos pos = center.offset(offset[0], 0, offset[1]);
            if (isTillable(level, pos, Direction.UP)) {
                level.setBlock(pos, Blocks.FARMLAND.defaultBlockState(), Block.UPDATE_ALL);
                tilled++;
            }
        }
        level.playSound(null, center, SoundEvents.HOE_TILL, SoundSource.BLOCKS, 1.0f, 1.0f);
        context.getItemInHand().hurtAndBreak(tilled, player, slotFor(context.getHand()));
        return InteractionResult.SUCCESS;
    }

    private InteractionResult aerate(UseOnContext context, Level level, BlockPos pos, Player player) {
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide() || !(level instanceof ServerLevel)) {
            return InteractionResult.SUCCESS;
        }
        if (!SoilApi.aerate((ServerLevel) level, pos, ImplementsLogic.AERATION_ORGANIC)) {
            return InteractionResult.PASS;
        }
        player.getCooldowns().addCooldown(this, ImplementsLogic.AERATION_COOLDOWN_TICKS);
        level.playSound(null, pos, SoundEvents.ROOTED_DIRT_PLACE, SoundSource.BLOCKS, 1.0f, 1.0f);
        context.getItemInHand().hurtAndBreak(1, player, slotFor(context.getHand()));
        return InteractionResult.SUCCESS;
    }

    /** Meme regle que la houe vanilla : herbe, terre ou chemin de terre, air au-dessus, face non basse. */
    static boolean isTillable(Level level, BlockPos pos, Direction face) {
        BlockState state = level.getBlockState(pos);
        boolean soil = state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT) || state.is(Blocks.DIRT_PATH);
        return soil && face != Direction.DOWN && level.getBlockState(pos.above()).isAir();
    }

    private static EquipmentSlot slotFor(InteractionHand hand) {
        return hand == InteractionHand.OFF_HAND ? EquipmentSlot.OFFHAND : EquipmentSlot.MAINHAND;
    }
}
