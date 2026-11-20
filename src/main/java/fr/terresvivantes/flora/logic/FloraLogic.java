package fr.terresvivantes.flora.logic;

import fr.terresvivantes.season.logic.Season;

/**
 * Règles pures du module flora (DESIGN section 17.4). Aucun import net.minecraft : testable en JUnit.
 *
 * <p>Propagation des fleurs sauvages : au printemps et en été, une fleur tente de poser une copie sur un bloc
 * d'herbe voisin, à condition que l'espace 5 x 5 autour de la cible contienne moins de {@link #MAX_SAME_IN_AREA}
 * fleurs du même type. La lavande demande en plus une température d'au moins {@link #LAVENDER_MIN_TEMPERATURE}.
 *
 * <p>Haie d'aubépine : elle fleurit au printemps, sinon non. Elle compte comme habitat pour l'indice de
 * biodiversité, à raison de {@link #HEDGE_WEIGHT} par haie dans le rayon {@link #HABITAT_RADIUS}.
 */
public final class FloraLogic {

    /** Rayon de propagation : la copie est posée à au plus 2 blocs de la fleur mère. */
    public static final int SPREAD_RADIUS = 2;
    /** Propagation seulement si l'espace 5 x 5 contient strictement moins de fleurs du même type que ce seuil. */
    public static final int MAX_SAME_IN_AREA = 6;
    /** Température minimale (°C) pour la propagation de la lavande. */
    public static final float LAVENDER_MIN_TEMPERATURE = 15f;

    /** Rayon des haies comptées comme habitat (blocs). */
    public static final int HABITAT_RADIUS = 8;
    /** Contribution d'une haie à l'indice de biodiversité. */
    public static final float HEDGE_WEIGHT = 0.05f;
    /** Plafond de la contribution des haies. */
    public static final float HEDGE_CAP = 0.25f;

    private FloraLogic() {
    }

    /** Vrai si la propagation est active : printemps et été. */
    public static boolean isSpreadSeason(Season season) {
        return season == Season.SPRING || season == Season.SUMMER;
    }

    /** Vrai si la cible peut recevoir une copie, selon le nombre de fleurs du même type dans le 5 x 5. */
    public static boolean canSpreadTo(int sameFlowersInArea) {
        return sameFlowersInArea < MAX_SAME_IN_AREA;
    }

    /** Température minimale de propagation pour une fleur, par identifiant. Sans condition : -infini. */
    public static float requiredTemperature(String flowerId) {
        return "lavender".equals(flowerId) ? LAVENDER_MIN_TEMPERATURE : Float.NEGATIVE_INFINITY;
    }

    /** Vrai si la température locale permet la propagation. */
    public static boolean temperatureAllows(float requiredMin, float temperature) {
        return temperature >= requiredMin;
    }

    /** Une haie fleurit au printemps et seulement au printemps. */
    public static boolean hedgeFlowers(Season season) {
        return season == Season.SPRING;
    }

    /** Contribution des haies à l'indice de biodiversité, bornée par {@link #HEDGE_CAP}. */
    public static float habitatContribution(int hedges) {
        return Math.min(HEDGE_CAP, HEDGE_WEIGHT * Math.max(0, hedges));
    }
}
