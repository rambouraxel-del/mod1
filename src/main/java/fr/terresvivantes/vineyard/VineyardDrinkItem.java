package fr.terresvivantes.vineyard;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Boisson du vignoble : animation de boire. Si un contenant est donné (bouteille), il est rendu à la fin de la
 * consommation, comme pour les jus de la meule. Sans contenant, rien n'est rendu.
 */
public final class VineyardDrinkItem extends Item {

    @Nullable
    private final Item vessel;

    public VineyardDrinkItem(Properties properties, @Nullable Item vessel) {
        super(properties);
        this.vessel = vessel;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        ItemStack result = super.finishUsingItem(stack, level, entity);
        if (vessel == null) {
            return result;
        }
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
