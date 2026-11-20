package fr.terresvivantes.economy.logic;

/** Réputation par joueur et paliers (v0.7). Logique pure, sans import net.minecraft. */
public final class ReputationLogic {

    private ReputationLogic() {
    }

    /**
     * Palier de réputation. {@code label} est le libellé français de référence; {@code langKey} la clé de traduction.
     */
    public enum Tier {
        UNKNOWN(0, "Inconnu", "terresvivantes.economy.tier.unknown"),
        REGULAR(50, "Habitué", "terresvivantes.economy.tier.regular"),
        SUPPLIER(200, "Fournisseur", "terresvivantes.economy.tier.supplier"),
        REPUTED_PRODUCER(500, "Producteur réputé", "terresvivantes.economy.tier.reputed_producer"),
        MASTER_FARMER(1200, "Maître fermier", "terresvivantes.economy.tier.master_farmer");

        private final int threshold;
        private final String label;
        private final String langKey;

        Tier(int threshold, String label, String langKey) {
            this.threshold = threshold;
            this.label = label;
            this.langKey = langKey;
        }

        /** Réputation minimale du palier. */
        public int threshold() {
            return threshold;
        }

        public String label() {
            return label;
        }

        public String langKey() {
            return langKey;
        }
    }

    /** Palier atteint pour une réputation donnée (négative ramenée à 0). */
    public static Tier tierOf(int reputation) {
        Tier result = Tier.UNKNOWN;
        for (Tier tier : Tier.values()) {
            if (Math.max(0, reputation) >= tier.threshold()) {
                result = tier;
            }
        }
        return result;
    }

    /** Réputation après un ajustement, jamais négative. */
    public static int adjusted(int reputation, int delta) {
        long sum = (long) reputation + delta;
        return (int) Math.max(0L, Math.min(Integer.MAX_VALUE, sum));
    }
}
