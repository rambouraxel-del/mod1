package fr.terresvivantes.insects.logic;

import fr.terresvivantes.season.logic.Season;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ButterflyLogicTest {

    private static final float DELTA = 1e-6f;

    @Test
    void variantsWrapAroundTheFourColours() {
        assertEquals(0, ButterflyLogic.clampVariant(4));
        assertEquals(3, ButterflyLogic.clampVariant(-1));
        assertEquals(2, ButterflyLogic.clampVariant(2));
    }

    @Test
    void daytimeEndsAtNightfall() {
        assertTrue(ButterflyLogic.isDaytime(0L));
        assertTrue(ButterflyLogic.isDaytime(11_999L));
        assertFalse(ButterflyLogic.isDaytime(12_000L));
        assertFalse(ButterflyLogic.isDaytime(23_999L));
        assertTrue(ButterflyLogic.isDaytime(24_000L));
    }

    @Test
    void flyingNeedsWarmSeasonDaylightAndNoRain() {
        assertTrue(ButterflyLogic.isActive(Season.SPRING, true, false));
        assertTrue(ButterflyLogic.isActive(Season.SUMMER, true, false));
        assertFalse(ButterflyLogic.isActive(Season.AUTUMN, true, false));
        assertFalse(ButterflyLogic.isActive(Season.WINTER, true, false));
        assertFalse(ButterflyLogic.isActive(Season.SUMMER, false, false));
        assertFalse(ButterflyLogic.isActive(Season.SUMMER, true, true));
    }

    @Test
    void pollinationContributionIsFivePerCentEachAndCapped() {
        assertEquals(0f, ButterflyLogic.pollinatorContribution(0), DELTA);
        assertEquals(0.05f, ButterflyLogic.pollinatorContribution(1), DELTA);
        assertEquals(0.2f, ButterflyLogic.pollinatorContribution(4), DELTA);
        assertEquals(0.2f, ButterflyLogic.pollinatorContribution(10), DELTA);
    }

    @Test
    void habitatContributionIsCappedAtFifteenPerCent() {
        assertEquals(0.06f, ButterflyLogic.habitatContribution(3), DELTA);
        assertEquals(0.15f, ButterflyLogic.habitatContribution(20), DELTA);
        assertEquals(0f, ButterflyLogic.habitatContribution(-2), DELTA);
    }

    @Test
    void wingsFlapInFlightAndStayAlmostClosedAtRest() {
        assertEquals(0f, ButterflyLogic.flapAngle(0f, false), DELTA);
        float maxFlying = 0f;
        float maxResting = 0f;
        for (int tick = 0; tick < 200; tick++) {
            maxFlying = Math.max(maxFlying, Math.abs(ButterflyLogic.flapAngle(tick, false)));
            maxResting = Math.max(maxResting, Math.abs(ButterflyLogic.flapAngle(tick, true)));
        }
        assertTrue(maxFlying > 0.8f, "le battement en vol doit être visible");
        assertTrue(maxResting <= 0.1f + DELTA, "au repos les ailes restent presque fermées");
    }
}
