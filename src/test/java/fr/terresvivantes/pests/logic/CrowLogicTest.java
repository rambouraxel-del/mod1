package fr.terresvivantes.pests.logic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrowLogicTest {

    @Test
    void onlyRipeCropsAreTargets() {
        assertFalse(CrowLogic.isTargetCrop(0));
        assertFalse(CrowLogic.isTargetCrop(3));
        assertTrue(CrowLogic.isTargetCrop(4));
        assertTrue(CrowLogic.isTargetCrop(7));
    }

    @Test
    void pecksOnlyByDayAndOutOfWinter() {
        assertTrue(CrowLogic.mayPeck(true, false));
        assertFalse(CrowLogic.mayPeck(false, false));
        assertFalse(CrowLogic.mayPeck(true, true));
    }

    @Test
    void pecksEverySixtyTicksWhilePerched() {
        assertFalse(CrowLogic.isPeckTick(0));
        assertFalse(CrowLogic.isPeckTick(59));
        assertTrue(CrowLogic.isPeckTick(60));
        assertFalse(CrowLogic.isPeckTick(61));
        assertTrue(CrowLogic.isPeckTick(120));
    }

    @Test
    void ripeCropRegressesOnlyBelowChance() {
        assertEquals(CrowLogic.PeckOutcome.REGRESS, CrowLogic.peckOutcome(7, 0.24, 0.25));
        assertEquals(CrowLogic.PeckOutcome.NONE, CrowLogic.peckOutcome(7, 0.25, 0.25));
        assertEquals(CrowLogic.PeckOutcome.NONE, CrowLogic.peckOutcome(7, 0.0, 0.0));
    }

    @Test
    void youngSeedCanBeDestroyedWithTenPercent() {
        assertEquals(CrowLogic.PeckOutcome.DESTROY, CrowLogic.peckOutcome(0, 0.09, 0.25));
        assertEquals(CrowLogic.PeckOutcome.DESTROY, CrowLogic.peckOutcome(1, 0.0, 0.25));
        assertEquals(CrowLogic.PeckOutcome.NONE, CrowLogic.peckOutcome(1, 0.10, 0.25));
    }

    @Test
    void middleStagesAreNeverPecked() {
        assertEquals(CrowLogic.PeckOutcome.NONE, CrowLogic.peckOutcome(2, 0.0, 1.0));
        assertEquals(CrowLogic.PeckOutcome.NONE, CrowLogic.peckOutcome(3, 0.0, 1.0));
    }

    @Test
    void regressionLosesTwoStagesAndStopsAtZero() {
        assertEquals(5, CrowLogic.regressedAge(7));
        assertEquals(2, CrowLogic.regressedAge(4));
        assertEquals(0, CrowLogic.regressedAge(1));
    }

    @Test
    void flockCapIsSixPerArea() {
        assertTrue(CrowLogic.spawnAllowed(5));
        assertFalse(CrowLogic.spawnAllowed(6));
        assertFalse(CrowLogic.spawnAllowed(9));
    }

    @Test
    void crowLeavesAfterMaxStay() {
        assertFalse(CrowLogic.mustLeave(CrowLogic.MAX_STAY_TICKS - 1));
        assertTrue(CrowLogic.mustLeave(CrowLogic.MAX_STAY_TICKS));
    }

    @Test
    void chanceIsClamped() {
        assertEquals(0.0, CrowLogic.clampChance(-0.5));
        assertEquals(1.0, CrowLogic.clampChance(3.0));
        assertEquals(0.25, CrowLogic.clampChance(0.25));
    }
}
