package fr.terresvivantes.dairy.item;

import fr.terresvivantes.core.quality.QualityModule;
import fr.terresvivantes.dairy.logic.CheeseLogic;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Aliment dont la saturation croît avec la qualité (+20 % par étoile au-dessus de la première). Les autres valeurs
 * (nutrition, effets) restent celles du composant {@code minecraft:food}.
 */
public final class QualityFoodItem extends Item {

    public QualityFoodItem(Properties properties) {
        super(properties);
    }

    @Override
    @Nullable
    public FoodProperties getFoodProperties(ItemStack stack, @Nullable LivingEntity entity) {
        FoodProperties base = stack.get(DataComponents.FOOD);
        if (base == null) {
            return null;
        }
        float factor = CheeseLogic.saturationFactor(QualityModule.qualityOf(stack));
        return new FoodProperties(base.nutrition(), base.saturation() * factor, base.canAlwaysEat(),
                base.eatSeconds(), base.usingConvertsTo(), base.effects());
    }
}
