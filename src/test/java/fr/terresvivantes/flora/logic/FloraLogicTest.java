package fr.terresvivantes.flora.logic;

import fr.terresvivantes.season.logic.Season;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FloraLogicTest {

    private static final float DELTA = 1e-6f;

    @Test
    void spreadOnlyInSpringAndSummer() {
        assertTrue(FloraLogic.isSpreadSeason(Season.SPRING));
        assertTrue(FloraLogic.isSpreadSeason(Season.SUMMER));
        assertFalse(FloraLogic.isSpreadSeason(Season.AUTUMN));
        assertFalse(FloraLogic.isSpreadSeason(Season.WINTER));
    }

    @Test
    void spreadStopsAtSixSameFlowersInFiveByFive() {
        assertTrue(FloraLogic.canSpreadTo(0));
        assertTrue(FloraLogic.canSpreadTo(5));
        assertFalse(FloraLogic.canSpreadTo(6));
        assertFalse(FloraLogic.canSpreadTo(25));
    }

    @Test
    void onlyLavenderNeedsWarmth() {
        assertEquals(15f, FloraLogic.requiredTemperature("lavender"), DELTA);
        assertEquals(Float.NEGATIVE_INFINITY, FloraLogic.requiredTemperature("clover"), DELTA);
        assertEquals(Float.NEGATIVE_INFINITY, FloraLogic.requiredTemperature("chamomile"), DELTA);
        assertEquals(Float.NEGATIVE_INFINITY, FloraLogic.requiredTemperature("borage"), DELTA);
    }

    @Test
    void lavenderThresholdIsInclusive() {
        float required = FloraLogic.requiredTemperature("lavender");
        assertFalse(FloraLogic.temperatureAllows(required, 14.9f));
        assertTrue(FloraLogic.temperatureAllows(required, 15f));
        assertTrue(FloraLogic.temperatureAllows(FloraLogic.requiredTemperature("clover"), -30f));
    }

    @Test
    void hedgeFloweringOnlyInSpring() {
        assertTrue(FloraLogic.hedgeFlowers(Season.SPRING));
        assertFalse(FloraLogic.hedgeFlowers(Season.SUMMER));
        assertFalse(FloraLogic.hedgeFlowers(Season.AUTUMN));
        assertFalse(FloraLogic.hedgeFlowers(Season.WINTER));
    }

    @Test
    void habitatContributionIsLinearThenCapped() {
        assertEquals(0f, FloraLogic.habitatContribution(0), DELTA);
        assertEquals(0.05f, FloraLogic.habitatContribution(1), DELTA);
        assertEquals(0.25f, FloraLogic.habitatContribution(5), DELTA);
        assertEquals(0.25f, FloraLogic.habitatContribution(40), DELTA);
        assertEquals(0f, FloraLogic.habitatContribution(-3), DELTA);
    }
}
