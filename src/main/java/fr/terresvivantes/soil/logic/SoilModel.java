package fr.terresvivantes.soil.logic;

/**
 * Logique pure du sol : valeurs initiales par biome, facteur de Liebig, bonus organique,
 * consommation et fertilisation. Aucun import net.minecraft: testable en JUnit.
 */
public final class SoilModel {

    /** Seuil de matiere organique au-dessus duquel le facteur de sol est bonifie. */
    public static final int ORGANIC_BONUS_THRESHOLD = 60;
    /** Bonus applique au-dessus du seuil organique. */
    public static final float ORGANIC_BONUS = 1.1f;

    private SoilModel() {
    }

    /** Fertilite initiale d'une terre labouree selon son biome (desert, jungle, autres). */
    public static SoilData initialFor(boolean desert, boolean jungle) {
        if (desert) {
            return new SoilData(30, 30, 30, 10);
        }
        if (jungle) {
            return new SoilData(60, 50, 50, 80);
        }
        return SoilData.DEFAULT;
    }

    /** Facteur de Liebig selon le nutriment le plus faible : &lt;20 : 0.25, &lt;40 : 0.6, sinon 1.0. */
    public static float liebigFactor(int lowestNutrient) {
        if (lowestNutrient < 20) {
            return 0.25f;
        }
        if (lowestNutrient < 40) {
            return 0.6f;
        }
        return 1.0f;
    }

    /** Facteur de fertilite total : Liebig sur min(N, P, K), puis bonus organique. */
    public static float nutrientFactor(SoilData data) {
        int lowest = Math.min(data.n(), Math.min(data.p(), data.k()));
        float factor = liebigFactor(lowest);
        if (data.organic() > ORGANIC_BONUS_THRESHOLD) {
            factor *= ORGANIC_BONUS;
        }
        return factor;
    }

    /** Retire N/P/K, multiplies par le multiplicateur global (arrondi), bornes 0-100. */
    public static SoilData consume(SoilData data, int n, int p, int k, double multiplier) {
        return new SoilData(
                data.n() - scale(n, multiplier),
                data.p() - scale(p, multiplier),
                data.k() - scale(k, multiplier),
                data.organic());
    }

    /** Ajoute des nutriments et de la matiere organique, bornes 0-100. */
    public static SoilData fertilize(SoilData data, int dn, int dp, int dk, int dOrganic) {
        return new SoilData(data.n() + dn, data.p() + dp, data.k() + dk, data.organic() + dOrganic);
    }

    private static int scale(int value, double multiplier) {
        return (int) Math.round(value * multiplier);
    }

    /** Jour MC d'une terre jamais aeree (aucun aerage enregistre). */
    public static final long NEVER_AERATED = Long.MIN_VALUE;

    /**
     * Un aerage est permis une fois par bloc et par jour MC : permis si la terre n'a jamais ete aeree,
     * ou si le dernier aerage date d'un jour MC anterieur a {@code today}.
     */
    public static boolean canAerate(long lastAeratedDay, long today) {
        return lastAeratedDay == NEVER_AERATED || today > lastAeratedDay;
    }

    /** Jour MC (24000 ticks) d'un temps de monde; les temps negatifs se rangent avant le jour 0. */
    public static long mcDay(long dayTime) {
        return Math.floorDiv(dayTime, 24000L);
    }
}
