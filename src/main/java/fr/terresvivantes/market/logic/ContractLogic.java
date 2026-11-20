package fr.terresvivantes.market.logic;

/** Règles des contrats : taille du tableau, expiration, qualité exigée (v0.7). Logique pure. */
public final class ContractLogic {

    /** Nombre de contrats ouverts que le tableau renouvelle chaque jour. */
    public static final int TARGET_OPEN = 3;

    /** Réputation retirée quand un contrat accepté n'est pas livré à temps. */
    public static final int EXPIRY_PENALTY = 10;

    private ContractLogic() {
    }

    /** Vrai une fois la date limite dépassée (le jour de l'échéance reste valable). */
    public static boolean isExpired(long today, long deadlineDay) {
        return today > deadlineDay;
    }

    /** Vrai si la qualité de l'item atteint la qualité minimale du contrat. */
    public static boolean meetsQuality(int quality, int minQuality) {
        return quality >= minQuality;
    }

    /** Nombre de contrats ouverts à générer pour atteindre {@link #TARGET_OPEN}. */
    public static int openSlotsToFill(int openCount) {
        return Math.max(0, TARGET_OPEN - openCount);
    }
}
