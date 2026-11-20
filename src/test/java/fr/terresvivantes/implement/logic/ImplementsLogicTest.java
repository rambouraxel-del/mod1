package fr.terresvivantes.implement.logic;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImplementsLogicTest {

    @Test
    void grid3x3HasNineDistinctCellsIncludingCenter() {
        int[][] grid = ImplementsLogic.square(ImplementsLogic.GRID_RADIUS);
        assertEquals(9, grid.length);
        assertTrue(Arrays.stream(grid).anyMatch(o -> o[0] == 0 && o[1] == 0));
        assertEquals(9, Arrays.stream(grid).map(Arrays::toString).distinct().count());
    }

    @Test
    void sickleAreaIsFiveByFive() {
        assertEquals(25, ImplementsLogic.square(ImplementsLogic.SICKLE_RADIUS).length);
    }

    @Test
    void rowFollowsDirectionAndLength() {
        int[][] row = ImplementsLogic.row(0, -1);
        assertEquals(ImplementsLogic.ROW_LENGTH, row.length);
        assertArrayEquals(new int[] {0, 0}, row[0]);
        assertArrayEquals(new int[] {0, -4}, row[4]);
    }

    @Test
    void toolDurabilitiesMatchDesign() {
        assertEquals(400, ImplementsLogic.BROADFORK_DURABILITY);
        assertEquals(600, ImplementsLogic.SEEDER_DURABILITY);
        assertEquals(1200, ImplementsLogic.DIAMOND_SICKLE_DURABILITY);
    }

    @Test
    void aerationIsRateLimited() {
        assertEquals(5, ImplementsLogic.AERATION_ORGANIC);
        assertEquals(20, ImplementsLogic.AERATION_COOLDOWN_TICKS);
    }
}
