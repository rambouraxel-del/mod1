package fr.terresvivantes.health.logic;

/**
 * Santé d'une culture. Aucun import net.minecraft.
 *
 * @param stress         stress accumulé : +1 à chaque tentative de croissance impossible (facteur 0),
 *                       -1 à chaque croissance réussie (minimum 0)
 * @param disease        maladie en cours ({@link Disease#NONE} si saine)
 * @param diseaseTicks   tentatives de croissance subies pendant la maladie en cours
 * @param immuneUntilDay dernier jour (index absolu) où la culture est immunisée; passé ce jour, aucune immunité
 */
public record CropHealth(int stress, Disease disease, int diseaseTicks, long immuneUntilDay) {

    /** Culture saine, sans stress, sans immunité : c'est la valeur par défaut, jamais stockée. */
    public static final CropHealth HEALTHY = new CropHealth(0, Disease.NONE, 0, Long.MIN_VALUE);

    public CropHealth {
        if (stress < 0 || diseaseTicks < 0) {
            throw new IllegalArgumentException("stress et diseaseTicks doivent être positifs");
        }
        if (disease == null) {
            throw new IllegalArgumentException("maladie absente");
        }
    }

    /** Vrai si une maladie est en cours. */
    public boolean sick() {
        return disease != Disease.NONE;
    }

    /** Vrai si la culture est immunisée au jour absolu {@code dayIndex}. */
    public boolean immune(long dayIndex) {
        return HealthModel.isImmune(dayIndex, immuneUntilDay);
    }
}
