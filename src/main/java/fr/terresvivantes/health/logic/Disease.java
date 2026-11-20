package fr.terresvivantes.health.logic;

/**
 * Maladies des cultures (DESIGN v0.2, module health). Aucun import net.minecraft.
 * L'identifiant de culture est celui de {@code CropApi.cropIdOf} : id de définition ({@code tomato}) ou
 * identifiant vanilla ({@code minecraft:wheat}).
 */
public enum Disease {
    NONE,
    /** Oïdium : favorisé par la terre saturée d'eau et la pluie ou la chaleur. */
    MILDEW,
    /** Rouille : favorisée par la pluie entre 12 et 24 °C. */
    RUST;

    /** Maladie à laquelle une culture est sensible. Une culture non listée n'a aucune maladie. */
    public static Disease susceptibility(String cropId) {
        return switch (cropId) {
            case "tomato", "cucumber", "strawberry", "lettuce", "bell_pepper", "eggplant", "minecraft:potatoes" -> MILDEW;
            case "barley", "corn", "soybean", "minecraft:wheat" -> RUST;
            default -> NONE;
        };
    }
}
