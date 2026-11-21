package fr.terresvivantes.crop.logic;

import fr.terresvivantes.season.logic.Season;

/**
 * Logique pure du module Crop (stades et croissance des cultures). Aucun import net.minecraft: testable en JUnit.
 */
public final class CropLogic {

    /** Âge maximal d'une culture (vanilla CropBlock). */
    public static final int MAX_AGE = 7;

    /** Nombre de gains de stade entre la graine (âge 0) et la maturité (âge 7). */
    public static final int GROWTH_STEPS = MAX_AGE;

    /**
     * Randomticks moyens par jour MC pour un bloc : randomTickSpeed 3 (défaut), 24000 ticks par jour, 4096 blocs par section.
     */
    public static final double RANDOM_TICKS_PER_DAY = 24000.0 * 3.0 / 4096.0;

    /** Azote fixé par stade pour une légumineuse (DESIGN 6.3, soja). */
    public static final int LEGUME_NITROGEN_FIXATION = 2;

    /** Facteur d'humidité d'une terre sèche (DESIGN 4.4). */
    public static final float DRY_SOIL_FACTOR = 0.5f;

    /** Niveau de lumière minimal pour qu'une culture pousse. */
    public static final int MIN_GROWTH_LIGHT = 9;

    /** Plage de température de référence des cultures vanilla (proposé par le module). */
    public static final float VANILLA_TEMP_MIN = 5.0f;
    public static final float VANILLA_TEMP_MAX = 30.0f;

    private CropLogic() {
    }

    /**
     * Probabilité qu'un randomTick tente une croissance, à facteur 1. Une culture de {@code growthDays} jours
     * franchit les {@link #GROWTH_STEPS} stades en environ {@code growthDays} jours MC à facteur 1.
     *
     * @param growthDays durée de maturation en jours MC
     * @return probabilité dans ]0, 1]
     */
    public static double attemptChance(double growthDays) {
        if (growthDays <= 0.0) {
            throw new IllegalArgumentException("durée non positive");
        }
        return Math.min(1.0, GROWTH_STEPS / (growthDays * RANDOM_TICKS_PER_DAY));
    }

    /** Stade visuel (0 à 3) d'un âge (0 à 7) : 0-1 → 0, 2-3 → 1, 4-5 → 2, 6-7 → 3. */
    public static int stageForAge(int age) {
        return Math.min(3, clampAge(age) / 2);
    }

    /** Ramène un âge dans [0, MAX_AGE]. */
    public static int clampAge(int age) {
        return Math.max(0, Math.min(MAX_AGE, age));
    }

    /**
     * Saison d'après laquelle l'affinité d'une culture est évaluée (DESIGN 6.2, v0.2) : sous serre en hiver,
     * l'affinité est celle du printemps; sinon, la saison réelle.
     */
    public static Season affinitySeason(Season season, boolean sheltered) {
        return season == Season.WINTER && sheltered ? Season.SPRING : season;
    }

    /** Facteur d'humidité d'après la propriété vanilla MOISTURE (0 = sec, 0.5 ; sinon 1.0). */
    public static float moistureFactor(int moisture) {
        return moisture <= 0 ? DRY_SOIL_FACTOR : 1.0f;
    }
}
