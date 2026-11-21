package fr.terresvivantes.season.logic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SeasonLogicTest {

    @Test
    void firstDayIsSpring() {
        assertEquals(Season.SPRING, SeasonLogic.seasonOf(0, 10));
    }

    @Test
    void seasonsChangeEveryPeriod() {
        assertEquals(Season.SPRING, SeasonLogic.seasonOf(9, 10));
        assertEquals(Season.SUMMER, SeasonLogic.seasonOf(10, 10));
        assertEquals(Season.AUTUMN, SeasonLogic.seasonOf(20, 10));
        assertEquals(Season.WINTER, SeasonLogic.seasonOf(39, 10));
    }

    @Test
    void cycleWrapsAround() {
        assertEquals(Season.SPRING, SeasonLogic.seasonOf(40, 10));
        assertEquals(Season.WINTER, SeasonLogic.seasonOf(-1, 10));
    }

    @Test
    void rejectsNonPositivePeriod() {
        assertThrows(IllegalArgumentException.class, () -> SeasonLogic.seasonOf(0, 0));
    }

    @Test
    void dayIndexUsesFullDaysOfDayTimePlusOffset() {
        assertEquals(0, SeasonLogic.dayIndex(0, 0));
        assertEquals(0, SeasonLogic.dayIndex(23999, 0));
        assertEquals(1, SeasonLogic.dayIndex(24000, 0));
        assertEquals(12, SeasonLogic.dayIndex(24000 * 2 + 5, 10));
        assertEquals(-1, SeasonLogic.dayIndex(0, -1));
    }

    @Test
    void dayOfSeasonIsOneBased() {
        assertEquals(1, SeasonLogic.dayOfSeason(0, 7));
        assertEquals(7, SeasonLogic.dayOfSeason(6, 7));
        assertEquals(1, SeasonLogic.dayOfSeason(7, 7));
        assertEquals(7, SeasonLogic.dayOfSeason(-1, 7));
    }

    @Test
    void targetDayIndexPointsToSeasonStartOfCurrentYear() {
        // Annee 2 (indices 28..55 pour 7 jours), on est en ete (jour 35).
        assertEquals(28, SeasonLogic.targetDayIndex(35, Season.SPRING, 1, 7));
        assertEquals(28 + 7 + 2, SeasonLogic.targetDayIndex(35, Season.SUMMER, 3, 7));
        assertEquals(28 + 21 + 6, SeasonLogic.targetDayIndex(35, Season.WINTER, 7, 7));
    }

    @Test
    void targetDayIndexRejectsDayOutOfRange() {
        assertThrows(IllegalArgumentException.class, () -> SeasonLogic.targetDayIndex(0, Season.SPRING, 0, 7));
        assertThrows(IllegalArgumentException.class, () -> SeasonLogic.targetDayIndex(0, Season.SPRING, 8, 7));
    }

    @Test
    void offsetForMakesTargetReachable() {
        long dayTime = 24000L * 9 + 1200;
        long target = 30;
        long offset = SeasonLogic.offsetFor(target, dayTime);
        assertEquals(target, SeasonLogic.dayIndex(dayTime, offset));
    }

    @Test
    void setThenReadGivesRequestedSeason() {
        long dayTime = 24000L * 100;
        long current = SeasonLogic.dayIndex(dayTime, 0);
        long target = SeasonLogic.targetDayIndex(current, Season.WINTER, 2, 7);
        long offset = SeasonLogic.offsetFor(target, dayTime);
        long now = SeasonLogic.dayIndex(dayTime, offset);
        assertEquals(Season.WINTER, SeasonLogic.seasonOf((int) now, 7));
        assertEquals(2, SeasonLogic.dayOfSeason(now, 7));
    }
}
