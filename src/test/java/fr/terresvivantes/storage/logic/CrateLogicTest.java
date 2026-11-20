package fr.terresvivantes.storage.logic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrateLogicTest {

    @Test
    void capacityIsEightStacks() {
        assertEquals(512, CrateLogic.capacity(64));
        assertEquals(8, CrateLogic.capacity(1));
    }

    @Test
    void onlyEmptyOrSameItemIsAccepted() {
        assertTrue(CrateLogic.accepts(true, false));
        assertTrue(CrateLogic.accepts(false, true));
        assertFalse(CrateLogic.accepts(false, false));
    }

    @Test
    void insertionStopsAtCapacity() {
        assertEquals(64, CrateLogic.insertable(0, 512, 64));
        assertEquals(12, CrateLogic.insertable(500, 512, 64));
        assertEquals(0, CrateLogic.insertable(512, 512, 64));
    }

    @Test
    void extractionIsBoundedByStackSizeAndContent() {
        assertEquals(64, CrateLogic.extractable(300, 64, 64));
        assertEquals(5, CrateLogic.extractable(5, 64, 64));
        assertEquals(0, CrateLogic.extractable(0, 64, 64));
    }

    @Test
    void leftClickTakesAStackOrOneItemWhenSneaking() {
        assertEquals(64, CrateLogic.attackTake(312, false, 64));
        assertEquals(1, CrateLogic.attackTake(312, true, 64));
        assertEquals(3, CrateLogic.attackTake(3, false, 64));
        assertEquals(0, CrateLogic.attackTake(0, false, 64));
    }

    @Test
    void doubleClickNeedsTwoClicksWithinTenTicks() {
        assertTrue(CrateLogic.isDoubleClick(100, 105));
        assertTrue(CrateLogic.isDoubleClick(100, 109));
        assertFalse(CrateLogic.isDoubleClick(100, 110));
        assertFalse(CrateLogic.isDoubleClick(CrateLogic.NO_CLICK, 105));
    }

    @Test
    void comparatorGoesFromZeroToFifteen() {
        assertEquals(0, CrateLogic.comparatorLevel(0, 512));
        assertEquals(1, CrateLogic.comparatorLevel(1, 512));
        assertEquals(15, CrateLogic.comparatorLevel(512, 512));
        assertEquals(8, CrateLogic.comparatorLevel(256, 512));
    }
}
