package fr.terresvivantes.market.logic;

import fr.terresvivantes.economy.logic.ReputationLogic;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.ToIntFunction;

/**
 * Génération déterministe d'un contrat (v0.7). Même graine, mêmes entrées : même contrat.
 * Logique pure : les candidats et le prix unitaire sont fournis par l'appelant.
 */
public final class ContractGenerator {

    /** Pourcentage de chances de choisir un produit de saison (sinon un produit transformé ou animal). */
    public static final int SEASONAL_PERCENT = 70;

    /** Délai minimal et maximal, en jours MC après le jour de création. */
    public static final int MIN_DAYS = 2;
    public static final int MAX_DAYS = 5;

    /** Récompense d'un contrat, en pourcentage du prix unitaire (130 = +30 % sur le prix; vente à l'étal : 90 %). */
    public static final int REWARD_PERCENT = 130;
    /** Gain de réputation minimal et maximal à la livraison. */
    public static final int MIN_REPUTATION = 5;
    public static final int MAX_REPUTATION = 25;

    /** Contrat généré : produit, quantité, qualité minimale, échéance, récompense en cuivre, réputation. */
    public record Generated(String product, int quantity, int minQuality, long deadlineDay, int reward,
                            int reputation) {
    }

    private ContractGenerator() {
    }

    /** Plage de quantité selon le palier de réputation (du plus petit au plus grand palier). */
    public static int[] quantityRange(ReputationLogic.Tier tier) {
        return switch (tier) {
            case UNKNOWN -> new int[] {8, 16};
            case REGULAR -> new int[] {12, 24};
            case SUPPLIER -> new int[] {16, 32};
            case REPUTED_PRODUCER -> new int[] {24, 48};
            case MASTER_FARMER -> new int[] {32, 64};
        };
    }

    /**
     * Graine d'un contrat : jour MC, dimension (hachage stable) et rang du contrat dans la journée.
     * Les trois entrées se mélangent pour que deux contrats du même jour diffèrent.
     */
    public static long seedFor(long today, long dimensionHash, int sequence) {
        long seed = today * 0x9E3779B97F4A7C15L;
        seed ^= dimensionHash * 0xC2B2AE3D27D4EB4FL;
        seed ^= (long) sequence * 0x165667B19E3779F9L;
        return seed;
    }

    /**
     * Génère un contrat. {@code seasonal} : produits de saison; {@code others} : autres produits pertinents
     * (transformés, animaux). {@code unitPrice} renvoie le prix unitaire en cuivre (0 si non vendable).
     * Renvoie {@code null} si aucun candidat vendable n'existe.
     */
    public static Generated generate(long seed, ReputationLogic.Tier tier, long today,
                                     List<String> seasonal, List<String> others, ToIntFunction<String> unitPrice) {
        Random rnd = new Random(seed);
        List<String> seasonalPool = sellable(seasonal, unitPrice);
        List<String> otherPool = sellable(others, unitPrice);
        List<String> pool;
        if (!seasonalPool.isEmpty() && (otherPool.isEmpty() || rnd.nextInt(100) < SEASONAL_PERCENT)) {
            pool = seasonalPool;
        } else if (!otherPool.isEmpty()) {
            pool = otherPool;
        } else {
            return null;
        }
        String product = pool.get(rnd.nextInt(pool.size()));
        int price = unitPrice.applyAsInt(product);

        int[] range = quantityRange(tier);
        int quantity = range[0] + rnd.nextInt(range[1] - range[0] + 1);
        int minQuality = 1 + rnd.nextInt(3);
        if (tier.ordinal() >= ReputationLogic.Tier.SUPPLIER.ordinal()) {
            minQuality = Math.max(2, minQuality);
        }
        long deadline = today + MIN_DAYS + rnd.nextInt(MAX_DAYS - MIN_DAYS + 1);
        int reputation = MIN_REPUTATION + rnd.nextInt(MAX_REPUTATION - MIN_REPUTATION + 1);
        // Récompense = quantité x prix x 1.3, arrondie à l'entier le plus proche (demi vers le haut).
        long reward = ((long) quantity * price * REWARD_PERCENT + 50) / 100;
        return new Generated(product, quantity, minQuality, deadline,
                (int) Math.min(Integer.MAX_VALUE, reward), reputation);
    }

    private static List<String> sellable(List<String> candidates, ToIntFunction<String> unitPrice) {
        List<String> result = new ArrayList<>();
        for (String candidate : candidates) {
            if (unitPrice.applyAsInt(candidate) > 0) {
                result.add(candidate);
            }
        }
        return result;
    }
}
