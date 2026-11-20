package fr.terresvivantes.market.logic;

/** Règles de l'étal (v0.7) : cycle d'achat, choix de l'item, paiement. Logique pure. */
public final class StallLogic {

    /** Intervalle entre deux tentatives d'achat, en ticks. */
    public static final int SALE_INTERVAL_TICKS = 2400;

    /** Portée d'un villageois pour qu'il achète, en blocs. */
    public static final double VILLAGER_RANGE = 16.0;

    /** Pourcentage du prix unitaire que paye l'acheteur. */
    public static final int PAYMENT_PERCENT = 90;

    private static final long DAY_TICKS = 24000L;
    private static final long DAY_HALF_TICKS = 12000L;

    private StallLogic() {
    }

    /** Vrai pendant la moitié jour du cycle MC (le marché est ouvert). */
    public static boolean isDay(long dayTime) {
        return Math.floorMod(dayTime, DAY_TICKS) < DAY_HALF_TICKS;
    }

    /** Un achat a lieu si un villageois est proche ou si le marché est ouvert de jour. */
    public static boolean shouldSell(boolean villagerNear, boolean day) {
        return villagerNear || day;
    }

    /** Paiement en cuivre pour un item de prix unitaire {@code unitPrice} : 90 %, au moins 1 cuivre. */
    public static long payment(int unitPrice) {
        if (unitPrice <= 0) {
            return 0;
        }
        return Math.max(1L, (long) unitPrice * PAYMENT_PERCENT / 100L);
    }

    /**
     * Index de la case au prix le plus élevé (à égalité, la première). -1 si aucune case n'a de prix positif.
     */
    public static int pickIndex(int[] prices) {
        int best = -1;
        for (int i = 0; i < prices.length; i++) {
            if (prices[i] > 0 && (best < 0 || prices[i] > prices[best])) {
                best = i;
            }
        }
        return best;
    }
}
