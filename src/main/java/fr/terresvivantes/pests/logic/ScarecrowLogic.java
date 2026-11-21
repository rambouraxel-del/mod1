package fr.terresvivantes.pests.logic;

/**
 * Règles pures de l'épouvantail (module pests). Aucun import Minecraft.
 *
 * <p>L'effet est divisé par deux quand l'épouvantail n'a pas été déplacé depuis {@link #STALE_DAYS} jours de Minecraft :
 * les corbeaux s'habituent à lui.
 */
public final class ScarecrowLogic {

    /** Jours de Minecraft au-delà desquels l'épouvantail est usé. */
    public static final long STALE_DAYS = 7;
    /** Valeur de jour de pose quand elle n'est pas encore connue : traitée comme un épouvantail neuf. */
    public static final long UNKNOWN_DAY = Long.MIN_VALUE;

    private ScarecrowLogic() {
    }

    /** Vrai si l'épouvantail posé le {@code placedDay} est usé au jour {@code currentDay}. */
    public static boolean isStale(long placedDay, long currentDay) {
        return placedDay != UNKNOWN_DAY && currentDay - placedDay >= STALE_DAYS;
    }

    /** Rayon effectif : le rayon de base, divisé par deux (arrondi inférieur) quand il est usé. */
    public static int effectiveRadius(int baseRadius, boolean stale) {
        return stale ? baseRadius / 2 : baseRadius;
    }

    /** Rayon effectif à partir des jours. */
    public static int radiusFor(int baseRadius, long placedDay, long currentDay) {
        return effectiveRadius(baseRadius, isStale(placedDay, currentDay));
    }

    /** Jours écoulés depuis la pose, 0 si la pose est inconnue ou dans le futur. */
    public static long daysSince(long placedDay, long currentDay) {
        if (placedDay == UNKNOWN_DAY) {
            return 0;
        }
        return Math.max(0, currentDay - placedDay);
    }

    /** Vrai si la distance au carré est dans le rayon (bords inclus). */
    public static boolean covers(double distanceSq, int radius) {
        return distanceSq <= (double) radius * radius;
    }
}
