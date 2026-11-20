package fr.terresvivantes.duck.logic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProductionLogicTest {

    @Test
    void milkQualityFollowsTheMultiplierThresholds() {
        assertEquals(1, ProductionLogic.milkQuality(0.5F));
        assertEquals(1, ProductionLogic.milkQuality(0.89F));
        assertEquals(2, ProductionLogic.milkQuality(0.9F));
        assertEquals(2, ProductionLogic.milkQuality(1.0F));
        assertEquals(3, ProductionLogic.milkQuality(1.15F));
        assertEquals(3, ProductionLogic.milkQuality(1.54F));
    }

    @Test
    void lowMultiplierRemovesNoWool() {
        assertEquals(0, ProductionLogic.extraWool(0.8F, 2, 0.0));
        assertEquals(0, ProductionLogic.extraWool(1.0F, 3, 0.0));
    }

    @Test
    void extraWoolIsRoundedProbabilistically() {
        // (1.5 - 1) x 2 = 1 exactement
        assertEquals(1, ProductionLogic.extraWool(1.5F, 2, 0.0));
        assertEquals(1, ProductionLogic.extraWool(1.5F, 2, 0.99));
        // (1.25 - 1) x 2 = 0.5 : une fois sur deux
        assertEquals(1, ProductionLogic.extraWool(1.25F, 2, 0.4));
        assertEquals(0, ProductionLogic.extraWool(1.25F, 2, 0.6));
    }
}
