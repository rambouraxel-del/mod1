package fr.terresvivantes.market.logic;

import java.util.ArrayList;
import java.util.List;

/** Classement du grand livre (v0.7) : les produits les plus chers de l'inventaire. Logique pure. */
public final class LedgerLogic {

    /** Nombre de lignes affichées par le grand livre. */
    public static final int LINES = 10;

    private LedgerLogic() {
    }

    /**
     * Index des {@code limit} prix les plus élevés, par ordre décroissant (à égalité, l'index le plus petit d'abord).
     * Les prix nuls ou négatifs sont ignorés.
     */
    public static List<Integer> topIndices(long[] prices, int limit) {
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < prices.length; i++) {
            if (prices[i] > 0) {
                indices.add(i);
            }
        }
        indices.sort((a, b) -> {
            int byPrice = Long.compare(prices[b], prices[a]);
            return byPrice != 0 ? byPrice : Integer.compare(a, b);
        });
        return indices.size() > limit ? new ArrayList<>(indices.subList(0, Math.max(0, limit))) : indices;
    }
}
