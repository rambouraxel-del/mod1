package fr.terresvivantes.orchard.logic;

import fr.terresvivantes.season.logic.Season;

import java.util.List;
import java.util.Set;

/** Les six espèces fruitières du module orchard. Logique pure. */
public final class FruitTreeDefinitions {

    private static final List<FruitTreeDefinition> ALL = List.of(
            new FruitTreeDefinition("apple", "oak_log", "minecraft:apple",
                    0f, 28f, Season.SPRING, Set.of(Season.AUTUMN), 1, 3),
            new FruitTreeDefinition("cherry", "cherry_log", "terresvivantes:cherry",
                    6f, 28f, Season.SPRING, Set.of(Season.SPRING, Season.SUMMER), 1, 3),
            new FruitTreeDefinition("orange", "jungle_log", "terresvivantes:orange",
                    0f, 35f, Season.SPRING, Set.of(Season.WINTER), 1, 3),
            new FruitTreeDefinition("olive", "acacia_log", "terresvivantes:olive",
                    10f, 35f, Season.SPRING, Set.of(Season.AUTUMN), 1, 3),
            new FruitTreeDefinition("walnut", "dark_oak_log", "terresvivantes:walnut",
                    0f, 30f, Season.SPRING, Set.of(Season.AUTUMN), 1, 3),
            new FruitTreeDefinition("peach", "birch_log", "terresvivantes:peach",
                    10f, 32f, Season.SPRING, Set.of(Season.SUMMER), 1, 3));

    private FruitTreeDefinitions() {
    }

    /** Toutes les espèces, dans l'ordre de déclaration. */
    public static List<FruitTreeDefinition> all() {
        return ALL;
    }
}
