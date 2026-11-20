package fr.terresvivantes.preserving.logic;

import fr.terresvivantes.season.logic.Season;

/**
 * Règles pures de la conservation (v0.5) : facteurs de vitesse du tonneau et du séchoir, et condition de séchage.
 * Aucun import net.minecraft.
 */
public final class PreservingLogic {

    /** Au-dessus de cette température (°C, stricte), la fermentation ralentit. */
    public static final float TOO_HOT_ABOVE_C = 28f;
    /** Plage de cave fraîche (°C, bornes incluses) : la fermentation accélère. */
    public static final float COOL_MIN_C = 8f;
    public static final float COOL_MAX_C = 18f;
    public static final float TOO_HOT_FACTOR = 0.7f;
    public static final float COOL_CELLAR_FACTOR = 1.2f;

    public static final float SUMMER_DRYING_FACTOR = 1.5f;
    public static final float WINTER_DRYING_FACTOR = 0.3f;

    private PreservingLogic() {
    }

    /** Facteur de vitesse d'une fermentation selon la température locale. */
    public static float fermentationFactor(float celsius) {
        if (celsius > TOO_HOT_ABOVE_C) {
            return TOO_HOT_FACTOR;
        }
        if (celsius >= COOL_MIN_C && celsius <= COOL_MAX_C) {
            return COOL_CELLAR_FACTOR;
        }
        return 1f;
    }

    /** Facteur de vitesse du séchage selon la saison. */
    public static float dryingSeasonFactor(Season season) {
        return switch (season) {
            case SUMMER -> SUMMER_DRYING_FACTOR;
            case WINTER -> WINTER_DRYING_FACTOR;
            default -> 1f;
        };
    }

    /** Le séchoir avance seulement s'il voit le ciel et qu'il ne pleut pas sur lui. */
    public static boolean canDry(boolean seesSky, boolean raining) {
        return seesSky && !raining;
    }
}
