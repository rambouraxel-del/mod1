package fr.terresvivantes.pollination.logic;

import fr.terresvivantes.crop.logic.YieldModel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PollinationModelTest {

    private static final float DELTA = 1e-6f;

    @Test
    void withoutPollinatorsScoreIsZero() {
        assertEquals(0f, PollinationModel.score(0, 0, 0, 0f), DELTA);
    }

    @Test
    void beesAreCappedAtFourTenths() {
        assertEquals(0.1f, PollinationModel.score(1, 0, 0, 0f), DELTA);
        assertEquals(0.4f, PollinationModel.score(4, 0, 0, 0f), DELTA);
        assertEquals(0.4f, PollinationModel.score(40, 0, 0, 0f), DELTA);
    }

    @Test
    void hivesAreCappedAtFourTenths() {
        assertEquals(0.2f, PollinationModel.score(0, 1, 0, 0f), DELTA);
        assertEquals(0.4f, PollinationModel.score(0, 2, 0, 0f), DELTA);
        assertEquals(0.4f, PollinationModel.score(0, 9, 0, 0f), DELTA);
    }

    @Test
    void flowerDiversityIsCappedAtThreeTenths() {
        assertEquals(0.05f, PollinationModel.score(0, 0, 1, 0f), DELTA);
        assertEquals(0.3f, PollinationModel.score(0, 0, 6, 0f), DELTA);
        assertEquals(0.3f, PollinationModel.score(0, 0, 30, 0f), DELTA);
    }

    @Test
    void varietyOfFlowersRaisesScore() {
        assertTrue(PollinationModel.score(0, 0, 3, 0f) > PollinationModel.score(0, 0, 1, 0f));
    }

    @Test
    void registeredSourcesAddAndTotalIsCappedAtOne() {
        assertEquals(0.7f, PollinationModel.score(0, 0, 0, 0.7f), DELTA);
        assertEquals(1.0f, PollinationModel.score(4, 2, 6, 0.5f), DELTA);
        assertEquals(1.0f, PollinationModel.score(10, 5, 20, 1f), DELTA);
    }

    @Test
    void cropWithoutNeedIgnoresPollination() {
        assertEquals(1.0f, PollinationModel.cropYieldFactor(false, 0.0f), DELTA);
        assertEquals(1.0f, PollinationModel.cropYieldFactor(false, 1.0f), DELTA);
    }

    @Test
    void pollinationDependentCropRangesFromSixTenthsToThirteenTenths() {
        assertEquals(0.6f, PollinationModel.cropYieldFactor(true, 0.0f), DELTA);
        assertEquals(1.3f, PollinationModel.cropYieldFactor(true, 1.0f), DELTA);
        assertEquals(0.6f, PollinationModel.fruitYieldFactor(0.0f), DELTA);
        assertEquals(1.3f, PollinationModel.fruitYieldFactor(1.0f), DELTA);
    }

    @Test
    void fruitSetRangesFromHalfToOneAndHalf() {
        assertEquals(0.5f, PollinationModel.fruitSetFactor(0.0f), DELTA);
        assertEquals(1.0f, PollinationModel.fruitSetFactor(0.5f), DELTA);
        assertEquals(1.5f, PollinationModel.fruitSetFactor(2.0f), DELTA);
    }

    /** Cucombre sans pollinisateur : rendement moyen inférieur au rendement avec pollinisateurs (API pure). */
    @Test
    void cucumberYieldIsLowerWithoutPollinators() {
        int samples = 1000;
        double without = averageYield(2, PollinationModel.cropYieldFactor(true, PollinationModel.score(0, 0, 0, 0f)), samples);
        double with = averageYield(2, PollinationModel.cropYieldFactor(true, PollinationModel.score(4, 2, 6, 0f)), samples);
        assertTrue(without < with, "sans pollinisateur " + without + ", avec " + with);
        assertEquals(1.2, without, 0.01);
        assertEquals(2.6, with, 0.01);
    }

    private static double averageYield(int count, float factor, int samples) {
        long total = 0;
        for (int i = 0; i < samples; i++) {
            total += YieldModel.scaledCount(count, factor, (i + 0.5) / samples);
        }
        return (double) total / samples;
    }
}
