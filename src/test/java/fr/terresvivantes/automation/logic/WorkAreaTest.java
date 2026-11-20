package fr.terresvivantes.automation.logic;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkAreaTest {

    @Test
    void radiusTwoIsAFiveByFiveZone() {
        WorkArea area = new WorkArea(2);
        assertEquals(5, area.side());
        assertEquals(25, area.cellCount());
    }

    @Test
    void forwardRunsFromOneToSideAndLateralIsCentred() {
        WorkArea area = new WorkArea(2);
        assertEquals(1, area.forwardOf(0));
        assertEquals(-2, area.lateralOf(0));
        assertEquals(5, area.forwardOf(24));
        assertEquals(2, area.lateralOf(24));
    }

    @Test
    void everyIndexMapsToADistinctCell() {
        WorkArea area = new WorkArea(2);
        Set<String> cells = new HashSet<>();
        for (int i = 0; i < area.cellCount(); i++) {
            int forward = area.forwardOf(i);
            int lateral = area.lateralOf(i);
            assertTrue(forward >= 1 && forward <= 5, "avant hors zone : " + forward);
            assertTrue(lateral >= -2 && lateral <= 2, "latéral hors zone : " + lateral);
            cells.add(forward + ":" + lateral);
        }
        assertEquals(25, cells.size());
    }

    @Test
    void radiusOutOfBoundsIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new WorkArea(0));
        assertThrows(IllegalArgumentException.class, () -> new WorkArea(5));
    }
}
