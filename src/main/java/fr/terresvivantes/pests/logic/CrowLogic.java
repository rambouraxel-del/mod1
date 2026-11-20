package fr.terresvivantes.pests.logic;

/**
 * Règles pures du corbeau et du picorage (module pests). Aucun import Minecraft.
 *
 * <p>Le corbeau se pose sur une culture mûre (âge au moins {@link #MIN_RIPE_AGE}) hors hiver, de jour, puis picore
 * toutes les {@link #PECK_INTERVAL_TICKS} ticks. Un coup peut faire régresser la culture de deux stades, ou détruire
 * une graine semée (âge 0 à 1).
 */
public final class CrowLogic {

    /** Délai entre deux coups de bec, en ticks, quand le corbeau est posé. */
    public static final int PECK_INTERVAL_TICKS = 60;
    /** Durée maximale de présence sur une même culture, en ticks (dix coups de bec). */
    public static final int MAX_STAY_TICKS = 600;
    /** Rayon de recherche d'une culture à picorer, en blocs. */
    public static final int SEARCH_RADIUS = 16;
    /** Demi-hauteur de recherche autour du corbeau, en blocs. */
    public static final int SEARCH_HEIGHT = 2;
    /** Le corbeau libre cherche une culture une fois toutes les X ticks, au hasard. */
    public static final int SEARCH_INTERVAL_TICKS = 40;
    /** Distance en dessous de laquelle le corbeau s'enfuit d'un joueur, en blocs. */
    public static final double FLEE_DISTANCE = 6.0;
    /** Nombre maximal de corbeaux dans un cube de {@link #CROW_AREA_SIZE} blocs de côté. */
    public static final int MAX_CROWS_PER_AREA = 6;
    /** Côté du cube de comptage des corbeaux, en blocs. */
    public static final int CROW_AREA_SIZE = 64;
    /** Âge minimal d'une culture ciblée (sur 7). */
    public static final int MIN_RIPE_AGE = 4;
    /** Âge maximal d'une graine semée, susceptible d'être détruite. */
    public static final int SEED_MAX_AGE = 1;
    /** Stades perdus par un coup de bec réussi. */
    public static final int REGRESS_STAGES = 2;
    /** Chance de destruction d'une graine semée à chaque coup de bec. */
    public static final double SEED_DESTROY_CHANCE = 0.10;

    /** Issue d'un coup de bec sur une culture. */
    public enum PeckOutcome {
        NONE, REGRESS, DESTROY
    }

    private CrowLogic() {
    }

    /** Vrai si une culture de cet âge est une cible de picorage. */
    public static boolean isTargetCrop(int age) {
        return age >= MIN_RIPE_AGE;
    }

    /** Vrai si le corbeau peut chercher à picorer : de jour et hors hiver. */
    public static boolean mayPeck(boolean daytime, boolean winter) {
        return daytime && !winter;
    }

    /** Vrai à chaque coup de bec, c'est-à-dire tous les {@link #PECK_INTERVAL_TICKS} ticks posé. */
    public static boolean isPeckTick(int perchTicks) {
        return perchTicks > 0 && perchTicks % PECK_INTERVAL_TICKS == 0;
    }

    /**
     * Issue d'un coup de bec. {@code roll} est un tirage uniforme dans [0, 1). Une culture mûre régresse si le
     * tirage est sous {@code regressChance}; une graine (âge 0 à 1) disparaît si le tirage est sous 10 %.
     */
    public static PeckOutcome peckOutcome(int age, double roll, double regressChance) {
        if (age >= MIN_RIPE_AGE) {
            return roll < regressChance ? PeckOutcome.REGRESS : PeckOutcome.NONE;
        }
        if (age <= SEED_MAX_AGE) {
            return roll < SEED_DESTROY_CHANCE ? PeckOutcome.DESTROY : PeckOutcome.NONE;
        }
        return PeckOutcome.NONE;
    }

    /** Âge après régression : deux stades de moins, jamais négatif. */
    public static int regressedAge(int age) {
        return Math.max(0, age - REGRESS_STAGES);
    }

    /** Vrai si un nouveau corbeau peut apparaître malgré {@code crowsNearby} corbeaux déjà présents dans la zone. */
    public static boolean spawnAllowed(int crowsNearby) {
        return crowsNearby < MAX_CROWS_PER_AREA;
    }

    /** Vrai si le corbeau doit quitter la culture : séjour maximal atteint. */
    public static boolean mustLeave(int stayTicks) {
        return stayTicks >= MAX_STAY_TICKS;
    }

    /** Chance clampée dans [0, 1] (les valeurs de config hors bornes sont ramenées). */
    public static double clampChance(double chance) {
        return Math.max(0.0, Math.min(1.0, chance));
    }
}
