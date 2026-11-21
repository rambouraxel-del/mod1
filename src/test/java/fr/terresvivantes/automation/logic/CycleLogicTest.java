package fr.terresvivantes.automation.logic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CycleLogicTest {

    @Test
    void cycleIsDueOnlyAtTheInterval() {
        assertFalse(CycleLogic.isCycleDue(39, 40));
        assertTrue(CycleLogic.isCycleDue(40, 40));
        assertTrue(CycleLogic.isCycleDue(41, 40));
    }

    @Test
    void cursorAdvancesByProcessedCellsAndWraps() {
        assertEquals(5, CycleLogic.nextCursor(0, 5, 25));
        assertEquals(0, CycleLogic.nextCursor(20, 5, 25));
        assertEquals(3, CycleLogic.nextCursor(23, 5, 25));
    }

    @Test
    void emptyAreaKeepsCursorAtZero() {
        assertEquals(0, CycleLogic.nextCursor(4, 5, 0));
    }

    @Test
    void wholeZoneIsSweptInFiveCycles() {
        int cursor = 0;
        for (int cycle = 0; cycle < 5; cycle++) {
            cursor = CycleLogic.nextCursor(cursor, 5, 25);
        }
        assertEquals(0, cursor);
    }
}
