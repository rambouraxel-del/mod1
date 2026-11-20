package fr.terresvivantes.economy.logic;

/**
 * Saturation du marché par item (v0.7). Chaque vente ajoute 1 point; la saturation décroît de 10 % par jour MC.
 * Logique pure, sans import net.minecraft.
 *
 * @param points saturation à {@code day}
 * @param day    jour MC de la dernière mise à jour
 */
public record MarketLogic(double points, long day) {

    /** Fraction conservée d'un jour à l'autre (décroissance de 10 %). */
    public static final double DAILY_RETENTION = 0.9;
    /** Ticks par jour MC. */
    public static final long TICKS_PER_DAY = 24000L;

    public MarketLogic {
        if (points < 0.0) {
            throw new IllegalArgumentException("saturation négative");
        }
    }

    /** Aucune vente enregistrée. */
    public static MarketLogic empty() {
        return new MarketLogic(0.0, 0L);
    }

    /** Jour MC d'un temps de monde (ticks). */
    public static long dayOf(long dayTime) {
        return Math.max(0L, dayTime) / TICKS_PER_DAY;
    }

    /** Saturation actuelle, décroissance appliquée jusqu'à {@code today}. */
    public double pointsAt(long today) {
        long elapsed = Math.max(0L, today - day);
        return points * Math.pow(DAILY_RETENTION, elapsed);
    }

    /** Nouvel état après une vente le jour {@code today} : décroissance puis +1 point. */
    public MarketLogic sold(long today) {
        return new MarketLogic(pointsAt(today) + 1.0, today);
    }
}
