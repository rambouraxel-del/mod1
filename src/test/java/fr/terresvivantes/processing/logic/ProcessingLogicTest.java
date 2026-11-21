package fr.terresvivantes.processing.logic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ProcessingLogicTest {

    private static final int ABSENT = 0;

    @Test
    void qualityModeReadsItsJsonIdentifiers() {
        assertEquals(QualityMode.NONE, QualityMode.fromId("none"));
        assertEquals(QualityMode.INHERIT_MIN, QualityMode.fromId("inherit_min"));
        assertEquals(QualityMode.INHERIT_AVG, QualityMode.fromId("inherit_avg"));
        assertEquals(QualityMode.AGE, QualityMode.fromId("age"));
        assertEquals("inherit_min", QualityMode.INHERIT_MIN.id());
    }

    @Test
    void unknownQualityModeIsRejected() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> QualityMode.fromId("best"));
    }

    @Test
    void outputQualityFollowsTheMode() {
        int[] inputs = {3, 2};
        assertEquals(ABSENT, ProcessingLogic.outputQuality(QualityMode.NONE, inputs));
        assertEquals(2, ProcessingLogic.outputQuality(QualityMode.INHERIT_MIN, inputs));
        assertEquals(3, ProcessingLogic.outputQuality(QualityMode.INHERIT_AVG, inputs));
        assertEquals(2, ProcessingLogic.outputQuality(QualityMode.AGE, inputs));
    }

    @Test
    void twoInputsOfThreeAndTwoGiveTwoWithInheritMin() {
        assertEquals(2, ProcessingLogic.outputQuality(QualityMode.INHERIT_MIN, new int[]{3, 2}));
    }

    @Test
    void allocateFindsASlotPerIngredient() {
        boolean[][] canUse = {
                {true, false, false},
                {false, true, false}
        };
        int[] stock = {2, 1, 0};
        int[] need = {1, 1};
        assertArrayEquals(new int[]{0, 1}, ProcessingLogic.allocate(canUse, stock, need));
    }

    @Test
    void allocateSharesASlotWhenItHoldsEnough() {
        boolean[][] canUse = {
                {true, false},
                {true, false}
        };
        assertArrayEquals(new int[]{0, 0}, ProcessingLogic.allocate(canUse, new int[]{3, 0}, new int[]{1, 2}));
        assertNull(ProcessingLogic.allocate(canUse, new int[]{2, 0}, new int[]{1, 2}));
    }

    @Test
    void allocateBacktracksWhenGreedyChoiceBlocksAnother() {
        // Ingrédient 0 accepte les slots 0 et 1; ingrédient 1 n'accepte que le slot 0.
        boolean[][] canUse = {
                {true, true},
                {true, false}
        };
        assertArrayEquals(new int[]{1, 0}, ProcessingLogic.allocate(canUse, new int[]{1, 1}, new int[]{1, 1}));
    }

    @Test
    void allocateFailsWhenAnIngredientHasNoSlot() {
        boolean[][] canUse = {{false, false}};
        assertNull(ProcessingLogic.allocate(canUse, new int[]{5, 5}, new int[]{1}));
    }

    @Test
    void progressCompletesAtTotalTime() {
        assertEquals(false, ProcessingLogic.isComplete(99, 100));
        assertEquals(true, ProcessingLogic.isComplete(100, 100));
        assertEquals(false, ProcessingLogic.isComplete(0, 0));
    }
}
