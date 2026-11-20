package fr.terresvivantes.health.item;

import fr.terresvivantes.health.HealthApi;
import fr.terresvivantes.health.HealthModule;
import fr.terresvivantes.health.logic.HealthModel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CropBlock;

/**
 * Pulvérisateur : clic droit sur une culture. Traite la culture et les huit voisines (zone 3x3), chacune est
 * guérie et immunisée. Une utilisation consomme une charge (durabilité, {@link HealthModel#SPRAYER_CHARGES} charges).
 * Vide : tenir du fongicide en main secondaire et cliquer sur une culture recharge le réservoir (un fongicide).
 */
public final class SprayerItem extends Item {

    public SprayerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos center = context.getClickedPos();
        if (!(level.getBlockState(center).getBlock() instanceof CropBlock)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        Player player = context.getPlayer();
        if (player == null || !(level instanceof ServerLevel server)) {
            return InteractionResult.PASS;
        }

        ItemStack sprayer = context.getItemInHand();
        if (sprayer.getDamageValue() >= sprayer.getMaxDamage()) {
            return refill(server, player, sprayer, center);
        }

        for (BlockPos around : BlockPos.betweenClosed(center.offset(-1, 0, -1), center.offset(1, 0, 1))) {
            BlockPos target = around.immutable();
            if (level.getBlockState(target).getBlock() instanceof CropBlock) {
                HealthApi.treat(server, target);
            }
        }
        sprayer.setDamageValue(sprayer.getDamageValue() + 1);
        HealthApi.treatmentEffects(server, center);
        return InteractionResult.SUCCESS;
    }

    /** Recharge le pulvérisateur vide avec un fongicide tenu en main secondaire. */
    private static InteractionResult refill(ServerLevel server, Player player, ItemStack sprayer, BlockPos center) {
        ItemStack offhand = player.getOffhandItem();
        if (!offhand.is(HealthModule.FUNGICIDE.get())) {
            player.sendSystemMessage(Component.translatable("message.terresvivantes.health.sprayer_empty"));
            return InteractionResult.FAIL;
        }
        sprayer.setDamageValue(0);
        if (!player.getAbilities().instabuild) {
            offhand.shrink(1);
        }
        player.sendSystemMessage(Component.translatable("message.terresvivantes.health.sprayer_refilled"));
        server.playSound(null, center, SoundEvents.BOTTLE_EMPTY, SoundSource.PLAYERS, 0.8f, 1.0f);
        return InteractionResult.SUCCESS;
    }
}
