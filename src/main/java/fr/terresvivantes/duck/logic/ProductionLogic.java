package fr.terresvivantes.duck.logic;

/**
 * Qualité du lait, laine supplémentaire et affichage des étoiles de qualité. Aucun import net.minecraft.
 */
public final class ProductionLogic {

    /** Qualité 1 sous ce multiplicateur. */
    public static final float MILK_LOW = 0.9F;
    /** Qualité 2 sous ce multiplicateur, 3 au-delà. */
    public static final float MILK_HIGH = 1.15F;
    /** Nombre d'étoiles maximal. */
    public static final int MAX_QUALITY = 3;

    private ProductionLogic() {
    }

    /** Qualité du lait (1 à 3 étoiles) : 1 si multiplicateur < 0.9, 2 si < 1.15, 3 sinon. */
    public static int milkQuality(float multiplier) {
        if (multiplier < MILK_LOW) {
            return 1;
        }
        return multiplier < MILK_HIGH ? 2 : MAX_QUALITY;
    }

    /**
     * Laine supplémentaire lors d'une tonte : (multiplicateur − 1) × {@code baseCount}, arrondi probabiliste.
     * Un multiplicateur inférieur ou égal à 1 ne retire rien (0).
     */
    public static int extraWool(float multiplier, int baseCount, double roll) {
        if (multiplier <= 1.0F || baseCount <= 0) {
            return 0;
        }
        return DuckLogic.roundProbabilistic((multiplier - 1.0) * baseCount, roll);
    }

    /** Texte des étoiles de qualité : "★★☆" pour 2 (borné entre 1 et 3). */
    public static String stars(int quality) {
        int filled = Math.max(1, Math.min(MAX_QUALITY, quality));
        return "★".repeat(filled) + "☆".repeat(MAX_QUALITY - filled);
    }
}
