package fr.terresvivantes.rice.logic;

import fr.terresvivantes.crop.logic.Affinity;
import fr.terresvivantes.crop.logic.GrowthModel;
import fr.terresvivantes.season.logic.Season;

/**
 * Logique pure de la rizière : affinité saisonnière, facteur de croissance, survie et plantation.
 * Aucun import net.minecraft : testable en JUnit.
 */
public final class RiceLogic {

    /** Âge maximal (7 stades de croissance, comme les cultures du mod). */
    public static final int MAX_AGE = 7;

    /** Durée de maturation en jours MC à facteur 1. */
    public static final double GROWTH_DAYS = 20.0;

    /** Randomticks moyens par jour MC pour un bloc (randomTickSpeed 3, 24000 ticks, 4096 blocs par section). */
    public static final double RANDOM_TICKS_PER_DAY = 24000.0 * 3.0 / 4096.0;

    /** Plage de température du riz, en °C. Hors plage, le facteur climatique de {@link GrowthModel} s'applique. */
    public static final float TEMP_MIN = 18.0f;
    public static final float TEMP_MAX = 35.0f;

    /** Niveau de lumière minimal pour que la culture pousse. */
    public static final int MIN_GROWTH_LIGHT = 9;

    /** Panicules récoltées à maturité (bornes incluses). */
    public static final int PANICLES_MIN = 2;
    public static final int PANICLES_MAX = 3;

    /** Probabilité de trouver un plant dans une herbe haute des jungles et marais. */
    public static final float SEEDLING_GRASS_CHANCE = 0.06f;

    /** Facteur de sol d'une rizière : pas d'effet de fertilité. */
    public static final float SOIL_FACTOR = 1.0f;

    /** Facteur d'humidité d'une rizière : toujours saturée en eau. */
    public static final float MOISTURE_FACTOR = 1.0f;

    private RiceLogic() {
    }

    /** Affinité du riz avec la saison : été optimal, printemps possible, automne défavorable, hiver impossible. */
    public static Affinity affinity(Season season) {
        return switch (season) {
            case SUMMER -> Affinity.OPTIMAL;
            case SPRING -> Affinity.POSSIBLE;
            case AUTUMN -> Affinity.UNFAVORABLE;
            case WINTER -> Affinity.IMPOSSIBLE;
        };
    }

    /**
     * Facteur total de croissance : saison, affinité, climat, sol (1.0) et humidité (1.0), via {@link GrowthModel}.
     *
     * @param seasonMultiplier multiplicateur saisonnier à la position
     * @param season           saison courante (affinité)
     * @param temperature      température locale en °C
     */
    public static float growthFactor(float seasonMultiplier, Season season, float temperature) {
        return GrowthModel.totalFactor(
                seasonMultiplier,
                affinity(season).getMultiplier(),
                GrowthModel.climateFactor(temperature, TEMP_MIN, TEMP_MAX),
                SOIL_FACTOR,
                MOISTURE_FACTOR);
    }

    /**
     * Probabilité qu'un randomTick tente une croissance, à facteur 1 : {@link #MAX_AGE} stades en {@link #GROWTH_DAYS}.
     */
    public static double attemptChance() {
        return Math.min(1.0, MAX_AGE / (GROWTH_DAYS * RANDOM_TICKS_PER_DAY));
    }

    /** Stade visuel (0 à 3) d'un âge (0 à 7) : 0-1 vers 0, 2-3 vers 1, 4-5 vers 2, 6-7 vers 3. */
    public static int stageForAge(int age) {
        return Math.min(3, clampAge(age) / 2);
    }

    /** Ramène un âge dans [0, MAX_AGE]. */
    public static int clampAge(int age) {
        return Math.max(0, Math.min(MAX_AGE, age));
    }

    /**
     * Condition de survie d'un plant déjà posé : sol valide, plant dans l'eau, et au moins une eau horizontale
     * voisine (la rizière ne tient pas seule au milieu de nulle part).
     *
     * @param soilOk         le bloc sous le plant est une terre admise (dirt, grass, farmland, mud)
     * @param waterlogged    le plant contient de l'eau
     * @param waterNeighbours nombre de voisins horizontaux contenant de l'eau
     */
    public static boolean survives(boolean soilOk, boolean waterlogged, int waterNeighbours) {
        return soilOk && waterlogged && waterNeighbours >= 1;
    }

    /**
     * Plantation possible sur une source d'eau de 1 bloc de profondeur : le bloc visé est une source d'eau,
     * il n'y a pas d'eau au-dessus (profondeur 1), et la position survivrait.
     *
     * @param soilOk            le sol sous le bloc d'eau est admis
     * @param targetIsWaterSource le bloc visé est une source d'eau
     * @param waterAbove        il y a de l'eau au-dessus du bloc visé
     * @param waterNeighbours   nombre de voisins horizontaux contenant de l'eau
     */
    public static boolean canPlant(boolean soilOk, boolean targetIsWaterSource, boolean waterAbove, int waterNeighbours) {
        return !waterAbove && survives(soilOk, targetIsWaterSource, waterNeighbours);
    }
}
