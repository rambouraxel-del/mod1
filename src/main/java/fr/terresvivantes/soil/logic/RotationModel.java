package fr.terresvivantes.soil.logic;

/**
 * Rotation des cultures (logique pure, sans import net.minecraft).
 * Monoculture : la même culture récoltée au moins deux fois de suite (répétition ≥ 2) pénalise sa croissance.
 * Précédent légumineux : une culture non légumineuse qui suit une légumineuse gagne un bonus.
 */
public final class RotationModel {

    /** Répétitions consécutives à partir desquelles la monoculture pénalise. */
    public static final int MONOCULTURE_REPEAT = 2;
    /** Facteur de monoculture. */
    public static final float MONOCULTURE_FACTOR = 0.8f;
    /** Facteur d'un précédent légumineux pour une culture non légumineuse. */
    public static final float LEGUME_PREVIOUS_FACTOR = 1.15f;

    private RotationModel() {
    }

    /**
     * Facteur de rotation d'une culture plantée sur une terre.
     *
     * @param repeat           nombre de récoltes consécutives de la dernière culture (voir SoilApi.getRepeatCount)
     * @param sameCrop         vrai si la culture actuelle est la dernière culture récoltée sur la terre
     * @param previousLegume   vrai si la dernière culture récoltée est une légumineuse
     * @param currentLegume    vrai si la culture actuelle est une légumineuse
     */
    public static float factor(int repeat, boolean sameCrop, boolean previousLegume, boolean currentLegume) {
        float factor = 1.0f;
        if (sameCrop && repeat >= MONOCULTURE_REPEAT) {
            factor *= MONOCULTURE_FACTOR;
        }
        if (previousLegume && !currentLegume) {
            factor *= LEGUME_PREVIOUS_FACTOR;
        }
        return factor;
    }
}
