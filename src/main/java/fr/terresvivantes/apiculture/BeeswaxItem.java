package fr.terresvivantes.apiculture;

import net.minecraft.world.item.HoneycombItem;
import net.minecraft.world.item.Item;

/**
 * Cire d'abeille : cire le cuivre comme le rayon de miel vanilla (même comportement que
 * {@link HoneycombItem}). Se craft en bougie.
 */
public final class BeeswaxItem extends HoneycombItem {

    public BeeswaxItem(Item.Properties properties) {
        super(properties);
    }
}
