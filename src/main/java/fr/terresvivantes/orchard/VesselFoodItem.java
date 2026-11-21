package fr.terresvivantes.orchard;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Aliment servi dans un contenant (bouteille, bol) : à la fin de la consommation, le contenant est rendu.
 * Si l'aliment était le dernier de la pile, le contenant prend la place dans la main.
 */
public final class VesselFoodItem extends Item {

    private final Item vessel;

    public VesselFoodItem(Properties properties, Item vessel) {
        super(properties);
        this.vessel = vessel;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        ItemStack result = super.finishUsingItem(stack, level, entity);
        ItemStack returned = new ItemStack(vessel);
        if (result.isEmpty()) {
            return returned;
        }
        if (entity instanceof Player player && !player.getAbilities().instabuild) {
            if (!player.getInventory().add(returned)) {
                player.drop(returned, false);
            }
        }
        return result;
    }
}
