package fr.terresvivantes.insects.logic;

import fr.terresvivantes.season.logic.Season;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FireflyLogicTest {

    private static final int DAYS = 10;

    @Test
    void nightIsTheVanillaNight() {
        assertTrue(FireflyLogic.isNight(13_000L));
        assertFalse(FireflyLogic.isNight(12_999L));
        assertTrue(FireflyLogic.isNight(22_999L));
        assertFalse(FireflyLogic.isNight(23_000L));
        assertFalse(FireflyLogic.isNight(-1_000L));
    }

    @Test
    void summerNightsAreFireflyTime() {
        assertTrue(FireflyLogic.isFireflyTime(Season.SUMMER, true, 1, DAYS));
        assertFalse(FireflyLogic.isFireflyTime(Season.SUMMER, false, 1, DAYS));
    }

    @Test
    void lateSpringNightsAreFireflyTimeButEarlySpringIsNot() {
        assertFalse(FireflyLogic.isFireflyTime(Season.SPRING, true, DAYS / 2, DAYS));
        assertTrue(FireflyLogic.isFireflyTime(Season.SPRING, true, DAYS / 2 + 1, DAYS));
    }

    @Test
    void otherSeasonsHaveNoFireflies() {
        assertFalse(FireflyLogic.isFireflyTime(Season.AUTUMN, true, DAYS, DAYS));
        assertFalse(FireflyLogic.isFireflyTime(Season.WINTER, true, DAYS, DAYS));
    }

    @Test
    void spawnStopsAtTheCapAndOnUnlikelyRolls() {
        assertTrue(FireflyLogic.shouldSpawn(0, FireflyLogic.MAX_ACTIVE, 0.1));
        assertFalse(FireflyLogic.shouldSpawn(FireflyLogic.MAX_ACTIVE, FireflyLogic.MAX_ACTIVE, 0.0));
        assertFalse(FireflyLogic.shouldSpawn(3, FireflyLogic.MAX_ACTIVE, 0.25));
    }

    @Test
    void lifetimeCoversSixtyToOneHundredTicks() {
        assertEquals(60, FireflyLogic.lifetime(0.0));
        assertEquals(100, FireflyLogic.lifetime(0.999_999));
        assertEquals(80, FireflyLogic.lifetime(0.5));
        assertEquals(100, FireflyLogic.lifetime(2.0));
    }

    @Test
    void capIsTenFireflies() {
        assertEquals(10, FireflyLogic.MAX_ACTIVE);
    }
}
