package fr.terresvivantes.core.quality.logic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QualityLogicTest {

    private static final int ABSENT = QualityLogic.ABSENT;

    @Test
    void starsAreBoundedBetweenOneAndThree() {
        assertEquals("★☆☆", QualityLogic.stars(1));
        assertEquals("★★☆", QualityLogic.stars(2));
        assertEquals("★★★", QualityLogic.stars(3));
        assertEquals("★☆☆", QualityLogic.stars(0));
        assertEquals("★★★", QualityLogic.stars(9));
    }

    @Test
    void inheritMinTakesTheLowestInput() {
        assertEquals(2, QualityLogic.inheritMin(new int[]{3, 2}));
        assertEquals(1, QualityLogic.inheritMin(new int[]{3, 1, 2}));
    }

    @Test
    void absentInputCountsAsDefaultOneWhenAnotherHasQuality() {
        // Entrée sans qualité = 1 : le minimum tombe à 1.
        assertEquals(1, QualityLogic.inheritMin(new int[]{3, ABSENT}));
        // Moyenne de (3, absent=1) = 2.
        assertEquals(2, QualityLogic.inheritAvg(new int[]{3, ABSENT}));
    }

    @Test
    void noQualityInInputsGivesNoQuality() {
        assertEquals(ABSENT, QualityLogic.inheritMin(new int[]{ABSENT, ABSENT}));
        assertEquals(ABSENT, QualityLogic.inheritAvg(new int[]{ABSENT}));
        assertEquals(ABSENT, QualityLogic.inheritMin(new int[0]));
    }

    @Test
    void inheritAvgRoundsToNearestAndStaysInRange() {
        assertEquals(2, QualityLogic.inheritAvg(new int[]{3, 2, 1}));
        assertEquals(3, QualityLogic.inheritAvg(new int[]{3, 3}));
        // (3 + 2) / 2 = 2.5 -> 3 (arrondi au supérieur à .5)
        assertEquals(3, QualityLogic.inheritAvg(new int[]{3, 2}));
    }

    @Test
    void ageQualityGainsAStarEverySixDaysUpToThree() {
        assertEquals(1, QualityLogic.ageQuality(1, 0));
        assertEquals(1, QualityLogic.ageQuality(1, 5));
        assertEquals(2, QualityLogic.ageQuality(1, 6));
        assertEquals(3, QualityLogic.ageQuality(1, 12));
        assertEquals(3, QualityLogic.ageQuality(1, 600));
    }

    @Test
    void ageQualityIgnoresNegativeDaysAndAbsentBase() {
        assertEquals(2, QualityLogic.ageQuality(2, -4));
        assertEquals(1, QualityLogic.ageQuality(ABSENT, 0));
        assertEquals(2, QualityLogic.ageQuality(ABSENT, 6));
    }
}
