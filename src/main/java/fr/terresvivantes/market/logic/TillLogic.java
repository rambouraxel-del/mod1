package fr.terresvivantes.market.logic;

import fr.terresvivantes.economy.logic.CoinLogic;

/**
 * Caisse de l'étal (v0.7) : un solde en cuivre, présenté dans une seule case par la plus grosse pièce
 * possible (fusion automatique). Logique pure.
 */
public final class TillLogic {

    /** Index des dénominations : 0 cuivre, 1 argent, 2 or. */
    public static final int COPPER = 0;
    public static final int SILVER = 1;
    public static final int GOLD = 2;

    /** Taille maximale d'une pile de pièces. */
    public static final int MAX_STACK = 64;

    private TillLogic() {
    }

    /** Valeur en cuivre d'une pièce de la dénomination donnée. */
    public static long unitValue(int denomination) {
        return switch (denomination) {
            case SILVER -> CoinLogic.COPPER_PER_SILVER;
            case GOLD -> CoinLogic.COPPER_PER_GOLD;
            default -> 1L;
        };
    }

    /**
     * Pile affichée pour un solde : {dénomination, nombre}. La plus grosse pièce qui tient au moins une fois dans le
     * solde, nombre plafonné à {@link #MAX_STACK}. Solde nul ou négatif : {-1, 0} (case vide).
     */
    public static int[] displayFor(long balance) {
        if (balance <= 0) {
            return new int[] {-1, 0};
        }
        int denomination;
        if (balance >= unitValue(GOLD)) {
            denomination = GOLD;
        } else if (balance >= unitValue(SILVER)) {
            denomination = SILVER;
        } else {
            denomination = COPPER;
        }
        long count = Math.min(MAX_STACK, balance / unitValue(denomination));
        return new int[] {denomination, (int) count};
    }

    /** Valeur en cuivre d'une pile {dénomination, nombre}. */
    public static long valueOf(int denomination, int count) {
        if (denomination < 0 || count <= 0) {
            return 0;
        }
        return unitValue(denomination) * count;
    }
}
