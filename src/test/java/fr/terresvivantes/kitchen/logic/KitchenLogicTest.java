package fr.terresvivantes.kitchen.logic;

import fr.terresvivantes.core.quality.logic.QualityLogic;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KitchenLogicTest {

    @Test
    void topQualityAddsThirtyPercentSaturation() {
        assertEquals(0.3f * 1.2f, KitchenLogic.saturationBonus(1.2f, 3), 1e-6f);
    }

    @Test
    void lowerOrAbsentQualityAddsNothing() {
        assertEquals(0f, KitchenLogic.saturationBonus(1.2f, 2), 0f);
        assertEquals(0f, KitchenLogic.saturationBonus(1.2f, 1), 0f);
        assertEquals(0f, KitchenLogic.saturationBonus(1.2f, QualityLogic.ABSENT), 0f);
    }

    @Test
    void nonPositiveSaturationAddsNothing() {
        assertEquals(0f, KitchenLogic.saturationBonus(0f, 3), 0f);
    }
}
