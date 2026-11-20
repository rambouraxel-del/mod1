package fr.terresvivantes.processing.logic;

import fr.terresvivantes.core.quality.logic.QualityLogic;

/**
 * Règles pures de la transformation : qualité de sortie, affectation des entrées aux slots, progression.
 * Aucun import net.minecraft.
 */
public final class ProcessingLogic {

    private ProcessingLogic() {
    }

    /**
     * Qualité de la sortie selon le mode. {@code inputQualities} contient la qualité de chaque ingrédient consommé
     * ({@link QualityLogic#ABSENT} si absente). Renvoie {@link QualityLogic#ABSENT} si la sortie n'a pas de qualité.
     */
    public static int outputQuality(QualityMode mode, int[] inputQualities) {
        return switch (mode) {
            case NONE -> QualityLogic.ABSENT;
            case INHERIT_MIN, AGE -> QualityLogic.inheritMin(inputQualities);
            case INHERIT_AVG -> QualityLogic.inheritAvg(inputQualities);
        };
    }

    /**
     * Affecte chaque ingrédient à un slot. {@code canUse[i][s]} indique si le slot {@code s} peut fournir
     * l'ingrédient {@code i}; {@code stock[s]} est le nombre d'items du slot, {@code need[i]} la quantité requise.
     * Un slot peut fournir plusieurs ingrédients tant que son stock suffit. Renvoie le slot de chaque ingrédient,
     * ou {@code null} si ce n'est pas possible. Recherche par retour arrière : petite (4 entrées au plus).
     */
    public static int[] allocate(boolean[][] canUse, int[] stock, int[] need) {
        int[] remaining = stock.clone();
        int[] assigned = new int[need.length];
        return assign(0, canUse, remaining, need, assigned) ? assigned : null;
    }

    private static boolean assign(int ingredient, boolean[][] canUse, int[] remaining, int[] need, int[] assigned) {
        if (ingredient == need.length) {
            return true;
        }
        for (int slot = 0; slot < remaining.length; slot++) {
            if (canUse[ingredient][slot] && remaining[slot] >= need[ingredient]) {
                remaining[slot] -= need[ingredient];
                assigned[ingredient] = slot;
                if (assign(ingredient + 1, canUse, remaining, need, assigned)) {
                    return true;
                }
                remaining[slot] += need[ingredient];
            }
        }
        return false;
    }

    /** Vrai quand la progression a atteint la durée de la recette (durée positive). */
    public static boolean isComplete(int progress, int totalTime) {
        return totalTime > 0 && progress >= totalTime;
    }
}
