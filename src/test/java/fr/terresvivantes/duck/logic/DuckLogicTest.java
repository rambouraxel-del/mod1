package fr.terresvivantes.duck.logic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DuckLogicTest {

    @Test
    void eggFromThrowHatchesOneTimeInEight() {
        assertTrue(DuckLogic.hatchesFromThrow(0.124));
        assertFalse(DuckLogic.hatchesFromThrow(0.125));
        assertFalse(DuckLogic.hatchesFromThrow(0.99));
    }

    @Test
    void eggTimeCoversTheConfiguredRange() {
        assertEquals(6000, DuckLogic.randomEggTime(0.0, 6000, 12000));
        assertEquals(12000, DuckLogic.randomEggTime(0.9999, 6000, 12000));
        assertEquals(9000, DuckLogic.randomEggTime(0.5, 6000, 12000));
    }

    @Test
    void eggTimeWithEqualBoundsIsConstant() {
        assertEquals(7000, DuckLogic.randomEggTime(0.0, 7000, 7000));
        assertEquals(7000, DuckLogic.randomEggTime(0.8, 7000, 7000));
    }

    @Test
    void eggTimeSwapsReversedBounds() {
        assertEquals(6000, DuckLogic.randomEggTime(0.0, 12000, 6000));
        assertEquals(12000, DuckLogic.randomEggTime(0.9999, 12000, 6000));
    }

    @Test
    void wingsStayFoldedWhenNotAirborne() {
        assertEquals(0.0F, DuckLogic.wingFlap(37.0F, false), 1e-6F);
    }

    @Test
    void wingsFlapWithinAmplitudeWhenAirborne() {
        float max = 0.0F;
        for (int age = 0; age < 200; age++) {
            float flap = DuckLogic.wingFlap(age, true);
            assertTrue(Math.abs(flap) <= 0.9F + 1e-6F, "amplitude dépassée : " + flap);
            max = Math.max(max, Math.abs(flap));
        }
        assertTrue(max > 0.5F, "les ailes doivent battre en vol");
    }

    @Test
    void probabilisticRoundingKeepsTheExpectedValue() {
        assertEquals(1, DuckLogic.roundProbabilistic(1.3, 0.5));
        assertEquals(2, DuckLogic.roundProbabilistic(1.3, 0.2));
        assertEquals(0, DuckLogic.roundProbabilistic(0.6, 0.7));
        assertEquals(1, DuckLogic.roundProbabilistic(0.6, 0.5));
        assertEquals(1, DuckLogic.roundProbabilistic(1.0, 0.99));
    }
}
