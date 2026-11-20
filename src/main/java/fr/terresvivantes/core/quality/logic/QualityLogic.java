package fr.terresvivantes.core.quality.logic;

/**
 * Qualité générique (1 à 3 étoiles) : héritage à la transformation et vieillissement. Aucun import net.minecraft.
 *
 * <p>Convention : {@link #ABSENT} (0) signifie « pas de composant de qualité ». Une entrée sans qualité compte
 * pour {@link #DEFAULT} (1) dans un calcul d'héritage, mais si aucune entrée n'a de qualité, la sortie n'en a pas.
 */
public final class QualityLogic {

    /** Qualité minimale. */
    public static final int MIN = 1;
    /** Qualité maximale (3 étoiles). */
    public static final int MAX = 3;
    /** Qualité d'une entrée sans composant, dans un calcul d'héritage. */
    public static final int DEFAULT = 1;
    /** Valeur signifiant « pas de composant ». */
    public static final int ABSENT = 0;
    /** Jours passés en affinage (mode « age ») pour gagner une étoile. */
    public static final int DAYS_PER_STAR = 6;

    private QualityLogic() {
    }

    /** Borne une qualité entre {@link #MIN} et {@link #MAX}. */
    public static int clamp(int quality) {
        return Math.max(MIN, Math.min(MAX, quality));
    }

    /** Qualité effective d'une entrée : {@link #DEFAULT} si absente, bornée sinon. */
    static int effective(int quality) {
        return quality == ABSENT ? DEFAULT : clamp(quality);
    }

    private static boolean allAbsent(int[] inputs) {
        for (int quality : inputs) {
            if (quality != ABSENT) {
                return false;
            }
        }
        return true;
    }

    /** Héritage « minimum » : la plus basse des qualités effectives; {@link #ABSENT} si aucune entrée n'en a. */
    public static int inheritMin(int[] inputs) {
        if (inputs.length == 0 || allAbsent(inputs)) {
            return ABSENT;
        }
        int min = MAX;
        for (int quality : inputs) {
            min = Math.min(min, effective(quality));
        }
        return min;
    }

    /** Héritage « moyenne » arrondie à l'entier le plus proche; {@link #ABSENT} si aucune entrée n'en a. */
    public static int inheritAvg(int[] inputs) {
        if (inputs.length == 0 || allAbsent(inputs)) {
            return ABSENT;
        }
        long sum = 0;
        for (int quality : inputs) {
            sum += effective(quality);
        }
        return clamp((int) Math.round(sum / (double) inputs.length));
    }

    /**
     * Qualité après affinage : la base gagne une étoile tous les {@link #DAYS_PER_STAR} jours, sans dépasser
     * {@link #MAX}. Une base absente est traitée comme {@link #DEFAULT}; des jours négatifs ne retirent rien.
     */
    public static int ageQuality(int baseQuality, int daysAged) {
        int days = Math.max(0, daysAged);
        return clamp(effective(baseQuality) + days / DAYS_PER_STAR);
    }

    /** Texte des étoiles : "★★☆" pour 2 (borné entre 1 et 3). */
    public static String stars(int quality) {
        int filled = clamp(quality);
        return "★".repeat(filled) + "☆".repeat(MAX - filled);
    }
}
