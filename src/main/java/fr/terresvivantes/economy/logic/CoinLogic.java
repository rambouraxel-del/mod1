package fr.terresvivantes.economy.logic;

/**
 * Monnaie en trois pièces : cuivre, argent (9 cuivre = 1 argent), or (9 argent = 1 or = 81 cuivre).
 * Logique pure, sans import net.minecraft.
 */
public final class CoinLogic {

    public static final int COPPER_PER_SILVER = 9;
    public static final int SILVER_PER_GOLD = 9;
    public static final long COPPER_PER_GOLD = (long) COPPER_PER_SILVER * SILVER_PER_GOLD;

    private CoinLogic() {
    }

    /** Valeur totale en cuivre d'un lot de pièces. */
    public static long coinsToCopper(long copper, long silver, long gold) {
        return copper + silver * COPPER_PER_SILVER + gold * COPPER_PER_GOLD;
    }

    /**
     * Décompose une somme en cuivre en {or, argent, cuivre}, le minimum de pièces possible.
     * Une somme négative est ramenée à 0.
     */
    public static long[] copperToCoins(long copper) {
        long total = Math.max(0, copper);
        long gold = total / COPPER_PER_GOLD;
        long rest = total % COPPER_PER_GOLD;
        long silver = rest / COPPER_PER_SILVER;
        long copperLeft = rest % COPPER_PER_SILVER;
        return new long[] {gold, silver, copperLeft};
    }
}
