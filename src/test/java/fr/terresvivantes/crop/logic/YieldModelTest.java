package fr.terresvivantes.crop.logic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class YieldModelTest {

    @Test
    void unitFactorKeepsCount() {
        assertEquals(3, YieldModel.scaledCount(3, 1.0f, 0.0));
        assertEquals(3, YieldModel.scaledCount(3, 1.0f, 0.99));
    }

    @Test
    void integerFactorIsExact() {
        assertEquals(6, YieldModel.scaledCount(3, 2.0f, 0.99));
        assertEquals(0, YieldModel.scaledCount(3, 0.0f, 0.0));
    }

    @Test
    void fractionalPartIsRoundedProbabilistically() {
        // 3 * 1.5 = 4.5 : 4 garanti, 5 avec probabilité 0.5.
        assertEquals(5, YieldModel.scaledCount(3, 1.5f, 0.49));
        assertEquals(4, YieldModel.scaledCount(3, 1.5f, 0.5));
        assertEquals(4, YieldModel.scaledCount(3, 1.5f, 0.99));
    }

    @Test
    void negativeFactorAndEmptyCountGiveNothing() {
        assertEquals(0, YieldModel.scaledCount(0, 2.0f, 0.0));
        assertEquals(0, YieldModel.scaledCount(4, -1.0f, 0.0));
    }
}
