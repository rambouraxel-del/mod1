package fr.terresvivantes.tools.item;

import fr.terresvivantes.tools.ToolsModule;
import fr.terresvivantes.tools.logic.ToolsLogic;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Arrosoir. Version vide : clic droit sur de l'eau le remplit. Version pleine (durabilite = utilisations) :
 * clic droit sur la terre labouree regle l'humidite a 7 sur la zone 3x3. Vide quand il ne reste plus d'utilisations.
 */
public final class WateringCanItem extends Item {
    private final boolean filled;

    public WateringCanItem(Properties properties, boolean filled) {
        super(properties);
        this.filled = filled;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (filled) {
            return InteractionResultHolder.pass(stack);
        }
        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
        if (hit.getType() != HitResult.Type.BLOCK || !level.getFluidState(hit.getBlockPos()).is(FluidTags.WATER)) {
            return InteractionResultHolder.pass(stack);
        }
        if (!level.isClientSide()) {
            ItemStack result = ItemUtils.createFilledResult(stack, player,
                    new ItemStack(ToolsModule.WATERING_CAN_FILLED.get()));
            level.playSound(player, player.blockPosition(), SoundEvents.BUCKET_FILL, SoundSource.PLAYERS, 1.0f, 1.0f);
            return InteractionResultHolder.success(result);
        }
        return InteractionResultHolder.sidedSuccess(stack, true);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!filled) {
            return InteractionResult.PASS;
        }
        Level level = context.getLevel();
        BlockPos center = context.getClickedPos();
        if (!level.getBlockState(center).is(Blocks.FARMLAND)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide() || !(level instanceof ServerLevel server)) {
            return InteractionResult.SUCCESS;
        }

        for (BlockPos pos : BlockPos.betweenClosed(
                center.offset(-ToolsLogic.AREA_RADIUS, 0, -ToolsLogic.AREA_RADIUS),
                center.offset(ToolsLogic.AREA_RADIUS, 0, ToolsLogic.AREA_RADIUS))) {
            BlockState state = server.getBlockState(pos);
            if (!state.is(Blocks.FARMLAND)) {
                continue;
            }
            BlockPos target = pos.immutable();
            server.setBlock(target, state.setValue(FarmBlock.MOISTURE, ToolsLogic.WATERED_MOISTURE), Block.UPDATE_ALL);
            server.sendParticles(ParticleTypes.SPLASH,
                    target.getX() + 0.5, target.getY() + 1.0, target.getZ() + 0.5,
                    8, 0.3, 0.1, 0.3, 0.0);
        }
        server.playSound(null, center, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0f, 1.0f);

        Player player = context.getPlayer();
        if (player != null) {
            ItemStack stack = context.getItemInHand();
            int used = stack.getDamageValue() + 1;
            if (used >= stack.getMaxDamage()) {
                player.setItemInHand(context.getHand(), new ItemStack(ToolsModule.WATERING_CAN.get()));
            } else {
                stack.setDamageValue(used);
            }
        }
        return InteractionResult.SUCCESS;
    }
}
