package fr.terresvivantes.husbandry.logic;

/**
 * Points de satiété apportés par une unité d'aliment d'auge, selon l'espèce, et règle de consommation.
 */
public final class FeedLogic {

    /** Au-dessus de cette satiété, l'animal ne consomme plus d'aliment d'auge (pas de gaspillage). */
    public static final int SATIATED_LIMIT = 80;

    private FeedLogic() {
    }

    /**
     * Points apportés par une unité. Foin : 15 (0 pour poules et canards). Grain : 12 (18 pour poules, canards,
     * cochons). Ensilage : 20 pour vaches, moutons, chèvres, sinon moitié (10). Aliment composé : 25 pour tous.
     */
    public static int points(Species species, FeedType type) {
        return switch (type) {
            case HAY -> isPoultry(species) ? 0 : 15;
            case GRAIN -> isPoultry(species) || species == Species.PIG ? 18 : 12;
            case SILAGE -> isRuminant(species) ? 20 : 10;
            case MIX -> 25;
        };
    }

    /** Vrai si l'animal a assez faim pour manger dans l'auge. */
    public static boolean wantsFood(int fed) {
        return fed < SATIATED_LIMIT;
    }

    private static boolean isPoultry(Species species) {
        return species == Species.CHICKEN || species == Species.DUCK;
    }

    private static boolean isRuminant(Species species) {
        return species == Species.COW || species == Species.SHEEP || species == Species.GOAT;
    }
}
