package fr.terresvivantes.compost.logic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompostLogicTest {

    private static final double DELTA = 1e-9;

    @Test
    void levelsDependOnMaterial() {
        assertEquals(2, CompostLogic.levelsFor(true, false));
        assertEquals(1, CompostLogic.levelsFor(false, true));
        assertEquals(0, CompostLogic.levelsFor(false, false));
    }

    @Test
    void addLevelsIsCappedAtMax() {
        assertEquals(3, CompostLogic.addLevels(2, 1));
        assertEquals(8, CompostLogic.addLevels(7, 2));
        assertEquals(8, CompostLogic.addLevels(8, 1));
        assertEquals(5, CompostLogic.addLevels(5, 0));
    }

    @Test
    void onlyFullUnreadyBinMatures() {
        assertTrue(CompostLogic.shouldMature(8, false, 0.0));
        assertFalse(CompostLogic.shouldMature(7, false, 0.0));
        assertFalse(CompostLogic.shouldMature(8, true, 0.0));
        assertFalse(CompostLogic.shouldMature(8, false, 0.99));
    }

    @Test
    void maturationChanceGivesAboutOneDay() {
        // 24000 ticks / (3 / 4096) randomTicks attendus par jour
        assertEquals(3.0 / 4096.0 * 24000.0, 1.0 / CompostLogic.MATURATION_CHANCE, 1e-6);
    }

    @Test
    void harvestGivesTwoOrThree() {
        assertEquals(2, CompostLogic.harvestAmount(0.0));
        assertEquals(2, CompostLogic.harvestAmount(0.49));
        assertEquals(3, CompostLogic.harvestAmount(0.5));
        assertEquals(3, CompostLogic.harvestAmount(0.99));
    }

    @Test
    void manureDropsBelowChance() {
        assertTrue(CompostLogic.dropsManure(0.04, 0.05));
        assertFalse(CompostLogic.dropsManure(0.05, 0.05));
        assertFalse(CompostLogic.dropsManure(0.0, 0.0));
    }

    @Test
    void manureCheckHappensEvery1200TicksPerEntity() {
        assertTrue(CompostLogic.isManureCheckTick(0, 0));
        assertTrue(CompostLogic.isManureCheckTick(1200, 0));
        assertFalse(CompostLogic.isManureCheckTick(1, 0));
        assertTrue(CompostLogic.isManureCheckTick(1199, 1));
    }

    @Test
    void fertilizerValuesMatchDesign() {
        CompostLogic.Fertilizer compost = CompostLogic.Fertilizer.COMPOST;
        assertEquals(15, compost.nitrogen());
        assertEquals(15, compost.phosphorus());
        assertEquals(15, compost.potassium());
        assertEquals(20, compost.organic());
        assertEquals(25, CompostLogic.Fertilizer.MANURE.nitrogen());
        assertEquals(10, CompostLogic.Fertilizer.MANURE.organic());
        assertEquals(20, CompostLogic.Fertilizer.WOOD_ASH.potassium());
        assertEquals(15, CompostLogic.BONE_MEAL_PHOSPHORUS);
    }
}
