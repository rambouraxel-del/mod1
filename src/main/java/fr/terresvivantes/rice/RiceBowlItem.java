package fr.terresvivantes.rice;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Aliment servi dans un bol : à la fin de la consommation, le bol est rendu (il prend la place dans la main
 * si c'était le dernier de la pile).
 */
public final class RiceBowlItem extends Item {

    private final Item vessel;

    public RiceBowlItem(Properties properties, Item vessel) {
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
