package fr.terresvivantes.pests.logic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScarecrowLogicTest {

    @Test
    void freshScarecrowKeepsFullRadius() {
        assertFalse(ScarecrowLogic.isStale(100, 106));
        assertEquals(12, ScarecrowLogic.radiusFor(12, 100, 106));
    }

    @Test
    void scarecrowIsStaleAfterSevenDays() {
        assertTrue(ScarecrowLogic.isStale(100, 107));
        assertEquals(6, ScarecrowLogic.radiusFor(12, 100, 107));
    }

    @Test
    void unknownPlacementDayIsNeverStale() {
        assertFalse(ScarecrowLogic.isStale(ScarecrowLogic.UNKNOWN_DAY, 1_000_000L));
        assertEquals(12, ScarecrowLogic.radiusFor(12, ScarecrowLogic.UNKNOWN_DAY, 1_000_000L));
        assertEquals(0, ScarecrowLogic.daysSince(ScarecrowLogic.UNKNOWN_DAY, 500L));
    }

    @Test
    void daysSinceNeverNegative() {
        assertEquals(4, ScarecrowLogic.daysSince(10, 14));
        assertEquals(0, ScarecrowLogic.daysSince(20, 14));
    }

    @Test
    void radiusBoundaryIsInclusive() {
        assertTrue(ScarecrowLogic.covers(144.0, 12));
        assertFalse(ScarecrowLogic.covers(144.01, 12));
    }
}
