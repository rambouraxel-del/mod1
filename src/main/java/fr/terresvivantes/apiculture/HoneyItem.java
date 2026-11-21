package fr.terresvivantes.apiculture;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

/** Bouteille de miel : se boit comme la bouteille de miel vanilla et rend une bouteille en verre. */
public final class HoneyItem extends Item {

    public HoneyItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        ItemStack result = super.finishUsingItem(stack, level, entity);
        if (entity instanceof Player player && player.getAbilities().instabuild) {
            return result;
        }
        ItemStack bottle = new ItemStack(Items.GLASS_BOTTLE);
        if (result.isEmpty()) {
            return bottle;
        }
        if (entity instanceof Player player) {
            player.getInventory().placeItemBackInInventory(bottle);
        }
        return result;
    }
}
