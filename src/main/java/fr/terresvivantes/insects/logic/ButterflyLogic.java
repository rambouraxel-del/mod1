package fr.terresvivantes.insects.logic;

import fr.terresvivantes.season.logic.Season;

/**
 * Règles des papillons (DESIGN v0.3 §17, module insects). Aucun import net.minecraft : testable en JUnit.
 *
 * <ul>
 *   <li>Quatre variantes couleur : monarque, piéride, morpho bleu, citron.</li>
 *   <li>Actifs le jour au printemps et en été, hors pluie; sinon ils se cachent (retrait après un délai).</li>
 *   <li>Apport à la pollinisation : 0.05 par papillon dans 12 blocs, plafond 0.2.</li>
 *   <li>Apport à la biodiversité : 0.02 par insecte (papillon ou coccinelle) dans 10 blocs, plafond 0.15.</li>
 * </ul>
 */
public final class ButterflyLogic {

    /** Nombre de variantes couleur. */
    public static final int VARIANT_COUNT = 4;

    /** Contribution d'un papillon à la pollinisation. */
    public static final float POLLINATOR_PER_BUTTERFLY = 0.05f;

    /** Plafond de la contribution des papillons à la pollinisation. */
    public static final float POLLINATOR_CAP = 0.2f;

    /** Rayon (blocs) dans lequel un papillon compte pour la pollinisation. */
    public static final int POLLINATOR_RADIUS = 12;

    /** Contribution d'un insecte à l'indice de biodiversité. */
    public static final float HABITAT_PER_INSECT = 0.02f;

    /** Plafond de la contribution des insectes à l'indice de biodiversité. */
    public static final float HABITAT_CAP = 0.15f;

    /** Rayon (blocs) dans lequel un insecte compte pour la biodiversité. */
    public static final int HABITAT_RADIUS = 10;

    /** Ticks d'inactivité (nuit, hiver, pluie) avant que le papillon se retire. */
    public static final int HIDE_TICKS = 100;

    /** Vitesse du battement d'ailes, en radians par tick. */
    private static final float FLAP_SPEED = 0.8f;

    /** Amplitude du battement en vol, en radians. */
    private static final float FLAP_AMPLITUDE = 0.9f;

    /** Amplitude du battement au repos (ailes presque fermées), en radians. */
    private static final float REST_AMPLITUDE = 0.1f;

    /** Début de la nuit dans le cycle vanilla (ticks de la journée). */
    private static final long NIGHT_START = 12000L;

    private ButterflyLogic() {
    }

    /** Ramène une variante dans [0, VARIANT_COUNT - 1]. */
    public static int clampVariant(int variant) {
        return Math.floorMod(variant, VARIANT_COUNT);
    }

    /** Vrai si le temps de jeu est le jour (cycle vanilla, 24000 ticks). */
    public static boolean isDaytime(long dayTime) {
        return Math.floorMod(dayTime, 24000L) < NIGHT_START;
    }

    /** Vrai au printemps et en été : saisons des papillons et des pucerons. */
    public static boolean isWarmSeason(Season season) {
        return season == Season.SPRING || season == Season.SUMMER;
    }

    /** Vrai si le papillon vole : saison chaude, jour, pas de pluie. */
    public static boolean isActive(Season season, boolean daytime, boolean raining) {
        return isWarmSeason(season) && daytime && !raining;
    }

    /** Apport à la pollinisation de {@code count} papillons, plafonné. */
    public static float pollinatorContribution(int count) {
        if (count <= 0) {
            return 0f;
        }
        return Math.min(POLLINATOR_CAP, count * POLLINATOR_PER_BUTTERFLY);
    }

    /** Apport à la biodiversité de {@code count} insectes (papillons et coccinelles), plafonné. */
    public static float habitatContribution(int count) {
        if (count <= 0) {
            return 0f;
        }
        return Math.min(HABITAT_CAP, count * HABITAT_PER_INSECT);
    }

    /**
     * Angle d'une aile pour le battement, en radians. En vol, l'aile bat en sinusoïde; au repos, elle reste presque
     * fermée.
     */
    public static float flapAngle(float ageInTicks, boolean resting) {
        float amplitude = resting ? REST_AMPLITUDE : FLAP_AMPLITUDE;
        return (float) Math.sin(ageInTicks * FLAP_SPEED) * amplitude;
    }
}
