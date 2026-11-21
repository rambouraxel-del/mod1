package fr.terresvivantes.health.logic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PestModelTest {

    private static final float DELTA = 1e-6f;

    @Test
    void aphidsAreAPestAndNotAFungus() {
        assertTrue(Disease.APHIDS.isPest());
        assertFalse(Disease.MILDEW.isPest());
        assertFalse(Disease.NONE.isPest());
    }

    @Test
    void aphidsSlowGrowthAndYieldWithoutWithering() {
        assertEquals(0.5f, HealthModel.growthFactor(Disease.APHIDS), DELTA);
        assertEquals(0.8f, HealthModel.yieldFactor(Disease.APHIDS), DELTA);
        assertFalse(HealthModel.withersFromDisease(Disease.APHIDS, 99));
    }

    @Test
    void fungalDiseasesKeepTheirFactors() {
        assertEquals(0.3f, HealthModel.growthFactor(Disease.MILDEW), DELTA);
        assertEquals(0.5f, HealthModel.yieldFactor(Disease.RUST), DELTA);
        assertEquals(1.0f, HealthModel.growthFactor(Disease.NONE), DELTA);
        assertEquals(1.0f, HealthModel.yieldFactor(Disease.NONE), DELTA);
    }

    @Test
    void pestsAreNotInfectedByChance() {
        HealthModel.Conditions calm = new HealthModel.Conditions(false, false, 15.0f, 1, false, false);
        assertEquals(0.0, HealthModel.infectionChance(Disease.APHIDS, calm), 1e-12);
    }
}
