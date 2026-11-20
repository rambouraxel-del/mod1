package fr.terresvivantes.water.logic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WaterLogicTest {

    @Test
    void barrelIsDrawnEveryFourthCycle() {
        assertTrue(WaterLogic.drawsFromBarrel(0));
        assertFalse(WaterLogic.drawsFromBarrel(1));
        assertFalse(WaterLogic.drawsFromBarrel(3));
        assertTrue(WaterLogic.drawsFromBarrel(4));
        assertTrue(WaterLogic.drawsFromBarrel(8));
    }

    @Test
    void copperCoversFiveByFiveAndIronSevenBySeven() {
        assertTrue(WaterLogic.covers(WaterLogic.COPPER_RADIUS, 2, -2));
        assertFalse(WaterLogic.covers(WaterLogic.COPPER_RADIUS, 3, 0));
        assertTrue(WaterLogic.covers(WaterLogic.IRON_RADIUS, 3, 3));
        assertFalse(WaterLogic.covers(WaterLogic.IRON_RADIUS, 0, 4));
    }

    @Test
    void rainFillsBarrelUpToMax() {
        assertEquals(1, WaterLogic.afterRain(0));
        assertEquals(8, WaterLogic.afterRain(7));
        assertEquals(8, WaterLogic.afterRain(8));
    }

    @Test
    void drawNeedsEnoughWater() {
        assertTrue(WaterLogic.canDraw(1, 1));
        assertFalse(WaterLogic.canDraw(0, 1));
        assertFalse(WaterLogic.canDraw(3, 0));
    }

    @Test
    void droughtOnlyInSummerWithoutRainOrSprinkler() {
        assertTrue(WaterLogic.droughtApplies(true, false, false));
        assertFalse(WaterLogic.droughtApplies(false, false, false));
        assertFalse(WaterLogic.droughtApplies(true, true, false));
        assertFalse(WaterLogic.droughtApplies(true, false, true));
    }

    @Test
    void droughtRollUsesChance() {
        assertTrue(WaterLogic.rollDrought(0.10, 0.15));
        assertFalse(WaterLogic.rollDrought(0.15, 0.15));
        assertFalse(WaterLogic.rollDrought(0.0, 0.0));
    }

    @Test
    void driedMoistureStopsAtZero() {
        assertEquals(6, WaterLogic.dried(7));
        assertEquals(0, WaterLogic.dried(0));
    }

    @Test
    void growthBonusOnlyWhenCovered() {
        assertEquals(1.1f, WaterLogic.growthFactor(true), 1e-6f);
        assertEquals(1.0f, WaterLogic.growthFactor(false), 1e-6f);
    }
}
