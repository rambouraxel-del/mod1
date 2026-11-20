package fr.terresvivantes.husbandry.logic;

/**
 * Règles pures de l'auge : capacité, acceptation d'un aliment, signal de comparateur.
 * Stock en unités, de 0 à {@link #CAPACITY}; un stock nul n'a pas de type.
 */
public final class TroughLogic {

    /** Capacité de l'auge, en unités. */
    public static final int CAPACITY = 64;

    /** Signal maximal du comparateur. */
    public static final int MAX_SIGNAL = 15;

    private TroughLogic() {
    }

    /**
     * Nombre d'objets acceptés. Refus (0) si l'auge contient déjà un autre type; sinon autant d'objets que la place
     * libre permet, plafonné à {@code requested}.
     *
     * @param current   type actuel (null si l'auge est vide)
     * @param stock     stock actuel en unités
     * @param incoming  type de l'objet proposé
     * @param requested nombre d'objets proposés
     */
    public static int itemsAccepted(FeedType current, int stock, FeedType incoming, int requested) {
        if (incoming == null || requested <= 0) {
            return 0;
        }
        if (stock > 0 && current != incoming) {
            return 0;
        }
        int room = CAPACITY - Math.max(0, Math.min(CAPACITY, stock));
        return Math.min(requested, room / incoming.unitsPerItem());
    }

    /** Signal de comparateur : 0 si vide, 1 à 15 sinon, proportionnel au stock. */
    public static int comparatorSignal(int stock) {
        if (stock <= 0) {
            return 0;
        }
        int clamped = Math.min(CAPACITY, stock);
        return 1 + (clamped * (MAX_SIGNAL - 1)) / CAPACITY;
    }
}
