package fr.terresvivantes.vet.item;

import fr.terresvivantes.livestock.LivestockApi;
import fr.terresvivantes.livestock.logic.Ailment;
import fr.terresvivantes.vet.VetApi;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Trousse vétérinaire (16 utilisations) : clic droit sur un animal malade pour le soigner. Sans effet sur un animal
 * en bonne santé.
 */
public final class VeterinaryKitItem extends Item {

    public VeterinaryKitItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target,
                                                  InteractionHand hand) {
        if (!(target instanceof Animal animal) || !LivestockApi.isLivestock(animal)
                || LivestockApi.getState(animal).ailment() == Ailment.NONE) {
            return InteractionResult.PASS;
        }
        if (player.level() instanceof ServerLevel level) {
            VetApi.treat(animal);
            level.playSound(null, animal.getX(), animal.getY(), animal.getZ(), SoundEvents.VILLAGER_YES,
                    SoundSource.NEUTRAL, 1.0f, 1.0f);
            level.sendParticles(ParticleTypes.HEART, animal.getX(), animal.getY() + animal.getBbHeight(),
                    animal.getZ(), 7, 0.4, 0.4, 0.4, 0.02);
            if (!player.getAbilities().instabuild) {
                stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
            }
        }
        return InteractionResult.sidedSuccess(player.level().isClientSide);
    }
}
