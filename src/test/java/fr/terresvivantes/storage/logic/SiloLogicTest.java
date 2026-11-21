package fr.terresvivantes.storage.logic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SiloLogicTest {

    @Test
    void capacityIsSixteenStacksPerBlock() {
        assertEquals(16, SiloLogic.capacity(1));
        assertEquals(48, SiloLogic.capacity(3));
        assertEquals(128, SiloLogic.capacity(8));
    }

    @Test
    void heightIsBoundedToEight() {
        assertEquals(128, SiloLogic.capacity(12));
        assertEquals(16, SiloLogic.capacity(0));
    }

    @Test
    void stackingStopsAtEightBlocks() {
        assertTrue(SiloLogic.canJoin(3, 4));
        assertFalse(SiloLogic.canJoin(4, 4));
        assertTrue(SiloLogic.canJoin(0, 0));
    }

    @Test
    void percentIsBoundedBetweenZeroAndHundred() {
        assertEquals(0, SiloLogic.percent(0, 48));
        assertEquals(50, SiloLogic.percent(24, 48));
        assertEquals(100, SiloLogic.percent(48, 48));
        assertEquals(0, SiloLogic.percent(5, 0));
    }
}
