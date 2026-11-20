package fr.terresvivantes.crop.logic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CropLogicTest {

    private static final float DELTA = 1e-5f;

    @Test
    void attemptChanceIsHigherForFasterCrops() {
        double lettuce = CropLogic.attemptChance(3.0);
        double tomato = CropLogic.attemptChance(6.0);
        double garlic = CropLogic.attemptChance(14.0);
        assertTrue(lettuce > tomato && tomato > garlic);
        assertTrue(garlic > 0.0);
    }

    @Test
    void attemptChanceMatchesSevenStepsOverMaturationTime() {
        double expected = 7.0 / (6.0 * CropLogic.RANDOM_TICKS_PER_DAY);
        assertEquals(expected, CropLogic.attemptChance(6.0), 1e-9);
    }

    @Test
    void attemptChanceIsCappedAtOne() {
        assertEquals(1.0, CropLogic.attemptChance(0.0001), 1e-9);
    }

    @Test
    void attemptChanceRejectsNonPositiveDuration() {
        assertThrows(IllegalArgumentException.class, () -> CropLogic.attemptChance(0.0));
    }

    @Test
    void stageForAgeMapsPairsOfAges() {
        int[] expected = {0, 0, 1, 1, 2, 2, 3, 3};
        for (int age = 0; age <= 7; age++) {
            assertEquals(expected[age], CropLogic.stageForAge(age), "age " + age);
        }
    }

    @Test
    void clampAgeBoundsToZeroAndMax() {
        assertEquals(0, CropLogic.clampAge(-3));
        assertEquals(7, CropLogic.clampAge(9));
        assertEquals(5, CropLogic.clampAge(5));
    }

    @Test
    void dryFarmlandHalvesGrowthAndWetFarmlandDoesNot() {
        assertEquals(0.5f, CropLogic.moistureFactor(0), DELTA);
        assertEquals(1.0f, CropLogic.moistureFactor(7), DELTA);
        assertEquals(1.0f, CropLogic.moistureFactor(1), DELTA);
    }

    @Test
    void legumeFixesTwoNitrogenPerStage() {
        assertEquals(2, CropLogic.LEGUME_NITROGEN_FIXATION);
        assertTrue(CropLogic.MAX_AGE == CropLogic.GROWTH_STEPS);
    }
}
