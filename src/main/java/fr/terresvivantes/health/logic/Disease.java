package fr.terresvivantes.health.logic;

/**
 * Maladies et ravageurs des cultures (DESIGN v0.2 et v0.3, module health). Aucun import net.minecraft.
 * L'identifiant de culture est celui de {@code CropApi.cropIdOf} : id de définition ({@code tomato}) ou
 * identifiant vanilla ({@code minecraft:wheat}).
 */
public enum Disease {
    NONE,
    /** Oïdium : favorisé par la terre saturée d'eau et la pluie ou la chaleur. */
    MILDEW,
    /** Rouille : favorisée par la pluie entre 12 et 24 °C. */
    RUST,
    /**
     * Pucerons (ravageur, v0.3) : croissance ×0.5, rendement ×0.8, pas de flétrissement. L'apparition relève du
     * module insects ({@code HealthApi.infest}), pas d'une susceptibilité de culture.
     */
    APHIDS;

    /** Maladie à laquelle une culture est sensible. Une culture non listée n'a aucune maladie. */
    public static Disease susceptibility(String cropId) {
        return switch (cropId) {
            case "tomato", "cucumber", "strawberry", "lettuce", "bell_pepper", "eggplant", "minecraft:potatoes",
                 "terresvivantes:rice_crop" -> MILDEW; // riz : pyriculariose, traitée comme une maladie fongique
            case "barley", "corn", "soybean", "minecraft:wheat" -> RUST;
            default -> NONE;
        };
    }

    /** Vrai pour un ravageur (pucerons) : il n'est pas une maladie fongique et ne fait pas flétrir la culture. */
    public boolean isPest() {
        return this == APHIDS;
    }
}
