package fr.terresvivantes.dairy.logic;

import fr.terresvivantes.core.quality.logic.QualityLogic;

/**
 * Règles pures de la laiterie : vitesse d'affinage selon la lumière, gain de qualité à l'affinage, saturation des
 * fromages affinés. Aucun import net.minecraft.
 */
public final class CheeseLogic {

    /** Durée d'un jour de Minecraft, en ticks. */
    public static final int DAY_TICKS = 24000;
    /** Niveau de lumière (bloc, maximum local) au-dessous duquel la pièce est sombre. */
    public static final int DARK_LIGHT = 7;
    /** Vitesse d'affinage dans une pièce sombre. */
    public static final float DARK_SPEED = 1.5F;
    /** Vitesse d'affinage à la lumière du jour (ciel visible, jour). */
    public static final float SUNLIT_SPEED = 0.75F;
    /** Gain de saturation par étoile au-dessus de la première, pour les fromages affinés. */
    public static final float SATURATION_PER_STAR = 0.2F;
    /**
     * Jours MC d'affinage qui valent une étoile de plus (règle {@link QualityLogic#ageQuality}). La recette
     * fromage jeune -> affiné dure 3 jours (72000 ticks, plafond du framework) et gagne une étoile à sa sortie.
     */
    public static final int RIPEN_DAYS = 6;

    private CheeseLogic() {
    }

    /**
     * Vitesse d'affinage. À la lumière du jour (ciel visible et jour) : 0.75. Dans une pièce sombre (lumière au plus
     * {@link #DARK_LIGHT}) : 1.5. Sinon 1.0. La lumière du jour l'emporte.
     */
    public static float agingSpeed(int light, boolean sunlit) {
        if (sunlit) {
            return SUNLIT_SPEED;
        }
        return light <= DARK_LIGHT ? DARK_SPEED : 1.0F;
    }

    /**
     * Qualité d'un fromage jeune à la fin de son affinage : la base gagne une étoile (règle
     * {@link QualityLogic#ageQuality}). Une base absente compte pour la qualité par défaut.
     */
    public static int ripenedQuality(int baseQuality) {
        return QualityLogic.ageQuality(baseQuality, RIPEN_DAYS);
    }

    /** Multiplicateur de saturation d'un fromage affiné : 1.0 à une étoile, +20 % par étoile au-dessus. */
    public static float saturationFactor(int quality) {
        int stars = QualityLogic.clamp(quality == QualityLogic.ABSENT ? QualityLogic.DEFAULT : quality);
        return 1.0F + SATURATION_PER_STAR * (stars - QualityLogic.MIN);
    }
}
