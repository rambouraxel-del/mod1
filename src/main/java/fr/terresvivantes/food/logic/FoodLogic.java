package fr.terresvivantes.food.logic;

import java.util.List;

/**
 * Donnees des plats du module Food (nutrition, saturation, vaisselle rendue). Aucun import net.minecraft:
 * testable en JUnit. Source : docs/DESIGN.md section 8.
 */
public final class FoodLogic {

    /** Recipient rendu apres consommation. */
    public enum Vessel { NONE, BOWL, GLASS_BOTTLE }

    /**
     * Un plat.
     *
     * @param id          identifiant de registre (snake_case)
     * @param nutrition   points de faim
     * @param saturation  modificateur de saturation (FoodProperties.saturationModifier)
     * @param vessel      recipient rendu apres consommation
     * @param regeneration vrai si le plat donne un court effet regeneration
     */
    public record Dish(String id, int nutrition, float saturation, Vessel vessel, boolean regeneration) {

        /** Les soupes et ragouts (rendus dans un bol) ne s'empilent pas, comme la soupe de champignons. */
        public boolean stacksToOne() {
            return vessel == Vessel.BOWL;
        }
    }

    /** Les 10 plats de la v0.1. */
    public static final List<Dish> DISHES = List.of(
            new Dish("tomato_soup", 6, 0.6f, Vessel.BOWL, false),
            new Dish("garden_salad", 7, 0.7f, Vessel.BOWL, false),
            new Dish("ratatouille", 10, 0.8f, Vessel.BOWL, false),
            new Dish("vegetable_stew", 8, 0.8f, Vessel.BOWL, false),
            new Dish("corn_bread", 6, 0.6f, Vessel.NONE, false),
            new Dish("garlic_bread", 7, 0.7f, Vessel.NONE, false),
            new Dish("barley_bread", 5, 0.6f, Vessel.NONE, false),
            new Dish("grilled_corn", 5, 0.6f, Vessel.NONE, false),
            new Dish("stuffed_pepper", 8, 0.7f, Vessel.NONE, false),
            new Dish("strawberry_jam", 4, 0.4f, Vessel.GLASS_BOTTLE, true)
    );

    private FoodLogic() {
    }
}
