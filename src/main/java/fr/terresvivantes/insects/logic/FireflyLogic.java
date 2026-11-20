package fr.terresvivantes.insects.logic;

import fr.terresvivantes.season.logic.Season;

/**
 * Règles d'apparition des lucioles, purement visuelles et côté client (DESIGN v0.3 §17.4). Aucun import
 * net.minecraft.
 */
public final class FireflyLogic {

    /** Nombre maximal de lucioles actives autour du joueur. */
    public static final int MAX_ACTIVE = 10;

    /** Rayon (blocs) autour du joueur où les lucioles apparaissent. */
    public static final int RADIUS = 16;

    /** Chance par tick client qu'une luciole apparaisse, si la limite n'est pas atteinte. */
    public static final double SPAWN_CHANCE = 0.25;

    /** Durée de vie minimale, en ticks. */
    public static final int MIN_LIFETIME = 60;

    /** Durée de vie maximale, en ticks. */
    public static final int MAX_LIFETIME = 100;

    /** Début de la nuit (ticks de la journée, cycle vanilla). */
    private static final long NIGHT_START = 13000L;

    /** Fin de la nuit (ticks de la journée, cycle vanilla). */
    private static final long NIGHT_END = 23000L;

    private FireflyLogic() {
    }

    /** Vrai pendant la nuit vanilla. */
    public static boolean isNight(long dayTime) {
        long t = Math.floorMod(dayTime, 24000L);
        return t >= NIGHT_START && t < NIGHT_END;
    }

    /**
     * Vrai si les lucioles peuvent apparaître : nuit, et été ou fin de printemps (seconde moitié de la saison).
     *
     * @param dayOfSeason   jour dans la saison, à partir de 1
     * @param daysPerSeason nombre de jours par saison
     */
    public static boolean isFireflyTime(Season season, boolean night, int dayOfSeason, int daysPerSeason) {
        if (!night) {
            return false;
        }
        if (season == Season.SUMMER) {
            return true;
        }
        return season == Season.SPRING && dayOfSeason > daysPerSeason / 2;
    }

    /** Vrai s'il faut ajouter une luciole à ce tick. */
    public static boolean shouldSpawn(int activeCount, int maxActive, double roll) {
        return activeCount < maxActive && roll < SPAWN_CHANCE;
    }

    /** Durée de vie d'une luciole, entre {@link #MIN_LIFETIME} et {@link #MAX_LIFETIME} (bornes incluses). */
    public static int lifetime(double roll) {
        int span = MAX_LIFETIME - MIN_LIFETIME + 1;
        int offset = (int) Math.floor(roll * span);
        return MIN_LIFETIME + Math.min(span - 1, Math.max(0, offset));
    }
}
