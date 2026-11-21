package fr.terresvivantes.vineyard.logic;

import fr.terresvivantes.season.logic.Season;

/**
 * Règles pures de la vigne (sans import Minecraft).
 *
 * <p>Stades : 0 (bouture plantée), 1 (jeune), 2 (mature, sans grappes), 3 (grappes, récoltable).
 * Printemps et été : 0 vers 1 et 1 vers 2. Fin d'été et automne : 2 vers 3, une seule fois par an.
 * Hiver : taille naturelle, tout stade 2 ou 3 retombe à 1 et la récolte est à nouveau permise.
 */
public final class VineyardLogic {

    public static final int PLANTED_STAGE = 0;
    public static final int YOUNG_STAGE = 1;
    public static final int MATURE_STAGE = 2;
    public static final int RIPE_STAGE = 3;
    public static final int PRUNED_STAGE = 1;

    /** Nombre de randomTicks par jour MC pour un bloc (randomTickSpeed 3, sous-chunk de 4096 blocs). */
    public static final double RANDOM_TICKS_PER_DAY = 24000.0 * 3.0 / 4096.0;

    /** Plage de température favorable (°C, bornes incluses). */
    public static final float CLIMATE_MIN_C = 10.0f;
    public static final float CLIMATE_MAX_C = 32.0f;
    /** Perte de facteur par degré hors plage. */
    private static final float CLIMATE_SLOPE = 0.1f;
    /** Facteur plancher hors plage : la vigne ralentit mais ne s'arrête pas. */
    private static final float CLIMATE_FLOOR = 0.2f;

    /** Grappes par récolte : de 2 à 4 inclus. */
    public static final int MIN_BUNCHES = 2;
    public static final int MAX_BUNCHES = 4;

    private VineyardLogic() {
    }

    /** Facteur climatique : 1 dans la plage 10 à 32 °C, puis décroît de 0,1 par degré (plancher 0,2). */
    public static float climateFactor(float celsius) {
        if (celsius < CLIMATE_MIN_C) {
            return Math.max(CLIMATE_FLOOR, 1.0f - (CLIMATE_MIN_C - celsius) * CLIMATE_SLOPE);
        }
        if (celsius > CLIMATE_MAX_C) {
            return Math.max(CLIMATE_FLOOR, 1.0f - (celsius - CLIMATE_MAX_C) * CLIMATE_SLOPE);
        }
        return 1.0f;
    }

    /** Facteur de sol : la nutrition de la terre labourée compte, la terre nue ou l'herbe valent 1. */
    public static float soilFactor(boolean farmland, float nutrientFactor) {
        return farmland ? Math.max(0.0f, nutrientFactor) : 1.0f;
    }

    /** Facteur total : saison x climat x sol. */
    public static float growthFactor(float seasonMultiplier, float climate, float soil) {
        return Math.max(0.0f, seasonMultiplier) * climate * soil;
    }

    /**
     * Probabilité qu'un randomTick tente une étape de croissance. Une étape dure {@code stepDays} jours MC à facteur 1.
     */
    public static double attemptChance(float factor, double stepDays) {
        if (stepDays <= 0.0) {
            throw new IllegalArgumentException("stepDays doit être strictement positif");
        }
        return Math.min(1.0, Math.max(0.0, factor) / (stepDays * RANDOM_TICKS_PER_DAY));
    }

    /** Vrai pendant la fenêtre de maturation des grappes : tout l'automne, puis la seconde moitié de l'été. */
    public static boolean ripens(Season season, int dayOfSeason, int daysPerSeason) {
        if (season == Season.AUTUMN) {
            return true;
        }
        return season == Season.SUMMER && daysPerSeason > 0 && dayOfSeason > daysPerSeason / 2;
    }

    /**
     * Stade après une tentative réussie (hors hiver). Une vigne récoltée ({@code harvested}) ne remûrit pas
     * avant la taille d'hiver.
     */
    public static int nextStage(int stage, boolean harvested, Season season, int dayOfSeason, int daysPerSeason) {
        if (stage == PLANTED_STAGE || stage == YOUNG_STAGE) {
            return (season == Season.SPRING || season == Season.SUMMER) ? stage + 1 : stage;
        }
        if (stage == MATURE_STAGE && !harvested && ripens(season, dayOfSeason, daysPerSeason)) {
            return RIPE_STAGE;
        }
        return stage;
    }

    /** Stade après la taille d'hiver : 2 et 3 retombent à 1, 0 et 1 ne bougent pas. */
    public static int winterStage(int stage) {
        return stage >= MATURE_STAGE ? PRUNED_STAGE : stage;
    }

    /** Nombre de grappes à partir d'un tirage uniforme dans [0, 1] : 2, 3 ou 4. */
    public static int harvestCount(double uniform) {
        int span = MAX_BUNCHES - MIN_BUNCHES + 1;
        int offset = (int) Math.floor(Math.max(0.0, Math.min(0.999999, uniform)) * span);
        return MIN_BUNCHES + offset;
    }

    /** Vrai si casser la vigne rend une bouture : stade mature ou plus. */
    public static boolean yieldsCutting(int stage) {
        return stage >= MATURE_STAGE;
    }
}
