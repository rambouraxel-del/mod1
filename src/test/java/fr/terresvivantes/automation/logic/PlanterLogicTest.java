package fr.terresvivantes.automation.logic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlanterLogicTest {

    private static final String WHEAT = "minecraft:wheat";
    private static final String TOMATO = "tomato";

    @Test
    void noSeedsGivesNoSlot() {
        assertEquals(-1, PlanterLogic.chooseSeedSlot(new String[]{null, null}, 0, null));
        assertEquals(-1, PlanterLogic.chooseSeedSlot(new String[0], 0, null));
    }

    @Test
    void rotationStartsFromTheGivenSlotAndSkipsEmptyOnes() {
        String[] crops = {null, WHEAT, null, TOMATO};
        assertEquals(1, PlanterLogic.chooseSeedSlot(crops, 0, null));
        assertEquals(3, PlanterLogic.chooseSeedSlot(crops, 2, null));
        assertEquals(3, PlanterLogic.chooseSeedSlot(crops, 7, null));
    }

    @Test
    void prefersAnotherCropThanTheLastOneInTheSoil() {
        String[] crops = {WHEAT, null, TOMATO};
        assertEquals(2, PlanterLogic.chooseSeedSlot(crops, 0, WHEAT));
    }

    @Test
    void keepsTheFirstSeedWhenItDiffersFromTheLastCrop() {
        String[] crops = {TOMATO, WHEAT};
        assertEquals(0, PlanterLogic.chooseSeedSlot(crops, 0, WHEAT));
    }

    @Test
    void fallsBackToTheSameCropWhenNothingElseIsAvailable() {
        String[] crops = {WHEAT, null, WHEAT};
        assertEquals(0, PlanterLogic.chooseSeedSlot(crops, 0, WHEAT));
        assertEquals(2, PlanterLogic.chooseSeedSlot(crops, 2, WHEAT));
    }
}
