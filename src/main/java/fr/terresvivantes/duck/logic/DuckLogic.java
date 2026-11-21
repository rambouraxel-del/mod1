package fr.terresvivantes.duck.logic;

/**
 * Règles pures du canard : ponte, chance de caneton à la projection d'un œuf, battement d'ailes, recherche d'eau.
 * Aucun import net.minecraft. Les tirages reçoivent une valeur dans [0, 1[ pour rester testables.
 */
public final class DuckLogic {

    /** Chance qu'un œuf de canard lancé fasse éclore un caneton (1 sur 8, comme l'œuf de poule). */
    public static final double HATCH_CHANCE = 1.0 / 8.0;
    /** Rayon horizontal de recherche d'eau au spawn naturel. */
    public static final int WATER_SEARCH_RADIUS = 5;
    /** Rayon horizontal de recherche d'un point d'eau pendant l'errance. */
    public static final int WATER_SEEK_RADIUS = 8;
    /** Nombre de positions tirées au hasard pour trouver de l'eau, à chaque recherche. */
    public static final int WATER_SEEK_TRIES = 8;
    /** Une recherche d'eau a lieu une fois sur ce nombre de ticks (hors de l'eau). */
    public static final int WATER_SEEK_CHANCE = 40;

    private static final float FLAP_SPEED = 1.5F;
    private static final float FLAP_AMPLITUDE = 0.9F;

    private DuckLogic() {
    }

    /** Vrai si l'œuf lancé éclôt en caneton (roll < 1/8). */
    public static boolean hatchesFromThrow(double roll) {
        return roll < HATCH_CHANCE;
    }

    /**
     * Délai de ponte tiré uniformément dans [min, max] (entiers, en ticks). Si min > max, les bornes sont échangées.
     */
    public static int randomEggTime(double roll, int min, int max) {
        int low = Math.min(min, max);
        int high = Math.max(min, max);
        int span = high - low + 1;
        int offset = (int) Math.floor(roll * span);
        return low + Math.max(0, Math.min(span - 1, offset));
    }

    /** Angle (en radians) d'une aile. Au sol et hors de l'eau, les ailes restent repliées (0). */
    public static float wingFlap(float ageInTicks, boolean airborne) {
        if (!airborne) {
            return 0.0F;
        }
        return (float) Math.sin(ageInTicks * FLAP_SPEED) * FLAP_AMPLITUDE;
    }

    /**
     * Arrondi probabiliste : partie entière de {@code value}, plus 1 avec la probabilité de la partie fractionnaire.
     * Même règle que {@code LivestockLogic.roundProbabilistic}, recopiée pour ne pas dépendre d'un autre module.
     */
    public static int roundProbabilistic(double value, double roll) {
        double whole = Math.floor(value);
        double fraction = value - whole;
        return (int) whole + (roll < fraction ? 1 : 0);
    }
}
