package fr.terresvivantes.water.logic;

/**
 * Logique pure du module Water : arroseurs, tonneau de pluie, sécheresse estivale.
 * Aucun import net.minecraft : testable en JUnit.
 */
public final class WaterLogic {

    /** Période de cycle d'un arroseur, en ticks serveur (jamais plus fréquent). */
    public static final int SPRINKLER_TICKS = 100;

    /** Un arroseur alimenté par un tonneau consomme un niveau tous les N cycles. */
    public static final int BARREL_DRAW_EVERY_CYCLES = 4;

    /** Rayon du carré arrosé par l'arroseur en cuivre (5x5) et en fer (7x7). */
    public static final int COPPER_RADIUS = 2;
    public static final int IRON_RADIUS = 3;

    /** Humidité maximale de la terre labourée (FarmBlock.MOISTURE). */
    public static final int WET_MOISTURE = 7;

    /** Niveau maximal du tonneau de pluie. */
    public static final int BARREL_MAX_LEVEL = 8;

    /** Facteur de croissance d'une culture sur une terre arrosée. */
    public static final float SPRINKLER_GROWTH_BONUS = 1.1f;

    /** Niveaux prélevés par l'arrosoir pour se remplir. */
    public static final int CAN_FILL_LEVELS = 1;

    private WaterLogic() {
    }

    /** Vrai si le cycle n (0 = premier) doit prélever dans le tonneau. */
    public static boolean drawsFromBarrel(int cycle) {
        return cycle % BARREL_DRAW_EVERY_CYCLES == 0;
    }

    /** Vrai si la position relative (dx, dz) est dans le carré de rayon donné. */
    public static boolean covers(int radius, int dx, int dz) {
        return Math.abs(dx) <= radius && Math.abs(dz) <= radius;
    }

    /** Niveau du tonneau après une pluie : +1, plafonné. */
    public static int afterRain(int level) {
        return Math.min(BARREL_MAX_LEVEL, level + 1);
    }

    /** Vrai si le tonneau contient au moins amount niveaux. */
    public static boolean canDraw(int level, int amount) {
        return amount > 0 && level >= amount;
    }

    /** Vrai si la sécheresse s'applique à une tentative de croissance. */
    public static boolean droughtApplies(boolean summer, boolean raining, boolean sprinklerCovers) {
        return summer && !raining && !sprinklerCovers;
    }

    /** Vrai si le tirage aléatoire (0 à 1) déclenche la sécheresse. */
    public static boolean rollDrought(double roll, double chance) {
        return roll < chance;
    }

    /** Humidité après un tour de dessèchement : -1, plancher à 0. */
    public static int dried(int moisture) {
        return Math.max(0, moisture - 1);
    }

    /** Facteur de croissance d'une culture selon qu'elle est arrosée ou non. */
    public static float growthFactor(boolean sprinklerCovers) {
        return sprinklerCovers ? SPRINKLER_GROWTH_BONUS : 1.0f;
    }
}
