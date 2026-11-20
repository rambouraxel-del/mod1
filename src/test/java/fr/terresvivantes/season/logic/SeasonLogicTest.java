package fr.terresvivantes.season.logic;

import fr.terresvivantes.season.logic.SeasonLogic.Season;
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
}
