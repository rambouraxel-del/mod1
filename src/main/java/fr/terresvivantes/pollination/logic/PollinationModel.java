package fr.terresvivantes.pollination.logic;

/**
 * Pollinisation (DESIGN section 17). Aucun import net.minecraft: testable en JUnit.
 *
 * <p>Score de pollinisation d'une position, dans [0, 1] : somme bornée de quatre composantes, puis plafond à 1.
 * <ul>
 *   <li>abeilles vanilla dans un rayon de 16 : 0.1 chacune, plafonnée à 0.4 (4 abeilles);</li>
 *   <li>ruches et nids vanilla dans un rayon de 24 : 0.2 chacun, plafonné à 0.4 (2 ruches);</li>
 *   <li>diversité florale dans un rayon de 8 : 0.05 par type de fleur (tag {@code minecraft:flowers}), plafonnée à 0.3;</li>
 *   <li>sources supplémentaires enregistrées (apiaires, papillons du mod) : leur somme, bornée par chaque source.</li>
 * </ul>
 *
 * <p>Effets : culture dépendante des pollinisateurs ×(0.6 + 0.7 × score), soit ×0.6 sans pollinisateur à ×1.3 au max;
 * fructification d'un arbre fruitier ×(0.5 + score).
 */
public final class PollinationModel {

    /** Rayon de recherche des abeilles vanilla (blocs, horizontal). */
    public static final int BEE_RADIUS = 16;
    /** Contribution d'une abeille vanilla. */
    public static final float BEE_WEIGHT = 0.1f;
    /** Nombre maximal d'abeilles comptées (0.4 au plafond). */
    public static final int BEE_CAP_COUNT = 4;

    /** Rayon de recherche des ruches et nids vanilla (blocs, horizontal). */
    public static final int HIVE_RADIUS = 24;
    /** Contribution d'une ruche ou d'un nid vanilla. */
    public static final float HIVE_WEIGHT = 0.2f;
    /** Nombre maximal de ruches comptées (0.4 au plafond). */
    public static final int HIVE_CAP_COUNT = 2;

    /** Rayon de recherche de la diversité florale (blocs, horizontal). */
    public static final int FLOWER_RADIUS = 8;
    /** Contribution par type de fleur distinct. */
    public static final float FLOWER_TYPE_WEIGHT = 0.05f;
    /** Plafond de la composante florale. */
    public static final float FLOWER_CAP = 0.3f;

    /** Plafond du score total. */
    public static final float MAX_SCORE = 1.0f;

    /** Facteur de rendement d'une culture dépendante des pollinisateurs, sans pollinisation. */
    public static final float YIELD_BASE = 0.6f;
    /** Gain de rendement apporté par un score de 1. */
    public static final float YIELD_RANGE = 0.7f;

    /** Facteur de fructification d'un arbre fruitier sans pollinisation. */
    public static final float FRUIT_SET_BASE = 0.5f;

    private PollinationModel() {
    }

    /**
     * Score de pollinisation à partir des comptes locaux.
     *
     * @param bees         abeilles vanilla dans le rayon 16
     * @param hives        ruches et nids vanilla dans le rayon 24
     * @param flowerTypes  types de fleurs distincts dans le rayon 8
     * @param extra        somme des contributions des sources enregistrées (pour un plafonnement global)
     * @return score borné dans [0, 1]
     */
    public static float score(int bees, int hives, int flowerTypes, float extra) {
        float beeTerm = Math.min(BEE_CAP_COUNT, Math.max(0, bees)) * BEE_WEIGHT;
        float hiveTerm = Math.min(HIVE_CAP_COUNT, Math.max(0, hives)) * HIVE_WEIGHT;
        float flowerTerm = Math.min(FLOWER_CAP, Math.max(0, flowerTypes) * FLOWER_TYPE_WEIGHT);
        float total = beeTerm + hiveTerm + flowerTerm + Math.max(0f, extra);
        return clamp(total);
    }

    /** Borne une valeur dans [0, 1]. */
    public static float clamp(float value) {
        return Math.max(0f, Math.min(MAX_SCORE, value));
    }

    /**
     * Facteur de rendement d'une culture. Une culture sans besoin de pollinisateurs garde 1.0.
     *
     * @param needsPollination vrai si la culture dépend des pollinisateurs
     * @param score            score de pollinisation dans [0, 1]
     */
    public static float cropYieldFactor(boolean needsPollination, float score) {
        if (!needsPollination) {
            return 1.0f;
        }
        return YIELD_BASE + YIELD_RANGE * clamp(score);
    }

    /** Facteur de rendement d'un arbre fruitier : toujours dépendant de la pollinisation. */
    public static float fruitYieldFactor(float score) {
        return YIELD_BASE + YIELD_RANGE * clamp(score);
    }

    /** Facteur de fructification d'un arbre fruitier : 0.5 sans pollinisation, 1.5 au max. */
    public static float fruitSetFactor(float score) {
        return FRUIT_SET_BASE + clamp(score);
    }
}
