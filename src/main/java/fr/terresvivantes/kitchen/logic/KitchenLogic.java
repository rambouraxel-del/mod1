package fr.terresvivantes.kitchen.logic;

import fr.terresvivantes.core.quality.logic.QualityLogic;

/** Règles pures de la cuisine (v0.5) : effet de la qualité sur la nourriture d'un plat. Aucun import net.minecraft. */
public final class KitchenLogic {

    /** Bonus de saturation d'un plat de qualité maximale, en fraction de sa saturation de base. */
    public static final float QUALITY_SATURATION_BONUS = 0.3f;

    private KitchenLogic() {
    }

    /**
     * Saturation en plus pour un plat de qualité 3 : 30 % de sa saturation. Toute autre qualité (1, 2 ou absente)
     * ne donne rien.
     */
    public static float saturationBonus(float saturation, int quality) {
        if (quality != QualityLogic.MAX || saturation <= 0f) {
            return 0f;
        }
        return saturation * QUALITY_SATURATION_BONUS;
    }
}
