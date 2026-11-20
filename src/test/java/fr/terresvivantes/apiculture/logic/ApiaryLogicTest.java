package fr.terresvivantes.apiculture.logic;

import fr.terresvivantes.season.logic.Season;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiaryLogicTest {

    private static final double EPS = 1e-6;

    @Test
    void seasonFactorsGiveDormancyInWinter() {
        assertEquals(1f, ApiaryLogic.seasonFactor(Season.SPRING), EPS);
        assertEquals(1f, ApiaryLogic.seasonFactor(Season.SUMMER), EPS);
        assertEquals(0.5f, ApiaryLogic.seasonFactor(Season.AUTUMN), EPS);
        assertEquals(0f, ApiaryLogic.seasonFactor(Season.WINTER), EPS);
    }

    @Test
    void flowerFactorIsCappedAtOne() {
        assertEquals(0.5f, ApiaryLogic.flowerFactor(4), EPS);
        assertEquals(1f, ApiaryLogic.flowerFactor(50), EPS);
        assertEquals(0f, ApiaryLogic.flowerFactor(0), EPS);
    }

    @Test
    void dominantHoneyIsTheMostCommonType() {
        int[] counts = new int[HoneyType.values().length];
        counts[HoneyType.WILDFLOWER.ordinal()] = 1;
        counts[HoneyType.CLOVER.ordinal()] = 3;
        counts[HoneyType.ORCHARD.ordinal()] = 2;
        assertEquals(HoneyType.CLOVER, ApiaryLogic.dominantHoney(counts));
    }

    @Test
    void dominantHoneyBreaksTiesInEnumOrderAndIsNullWithoutFlowers() {
        int[] tie = new int[HoneyType.values().length];
        tie[HoneyType.LAVENDER.ordinal()] = 2;
        tie[HoneyType.ORCHARD.ordinal()] = 2;
        assertEquals(HoneyType.LAVENDER, ApiaryLogic.dominantHoney(tie));
        assertNull(ApiaryLogic.dominantHoney(new int[HoneyType.values().length]));
    }

    @Test
    void flowerTotalSumsAllTypes() {
        assertEquals(6, ApiaryLogic.flowerTotal(new int[] {1, 2, 0, 3}));
    }

    @Test
    void populationStaysStableInWinter() {
        assertEquals(40, ApiaryLogic.nextPopulation(40, 8, 0f, 0.0));
        assertEquals(40, ApiaryLogic.nextPopulation(40, 0, 0f, 0.0));
    }

    @Test
    void populationGrowsWithFlowersAndCapsAtMax() {
        assertEquals(41, ApiaryLogic.nextPopulation(40, 8, 1f, 0.0));
        assertEquals(40, ApiaryLogic.nextPopulation(40, 8, 1f, 0.99));
        assertEquals(ApiaryLogic.MAX_POPULATION,
                ApiaryLogic.nextPopulation(ApiaryLogic.MAX_POPULATION, 8, 1f, 0.0));
    }

    @Test
    void populationDeclinesWithoutFlowers() {
        assertEquals(9, ApiaryLogic.nextPopulation(10, 0, 1f, 0.0));
        assertEquals(10, ApiaryLogic.nextPopulation(10, 0, 1f, 0.5));
        assertEquals(0, ApiaryLogic.nextPopulation(0, 0, 1f, 0.0));
    }

    @Test
    void productionScalesWithPopulationFlowersAndSeason() {
        assertEquals(0f, ApiaryLogic.productionIncrement(0, 8, 1f), EPS);
        assertEquals(0f, ApiaryLogic.productionIncrement(100, 8, 0f), EPS);
        assertEquals(1f / 60f, ApiaryLogic.productionIncrement(100, 8, 1f), EPS);
        assertEquals(1f / 120f, ApiaryLogic.productionIncrement(100, 8, 0.5f), EPS);
        assertEquals(ApiaryLogic.productionIncrement(100, 8, 1f) / 2f,
                ApiaryLogic.productionIncrement(50, 8, 1f), EPS);
    }

    @Test
    void advanceCompletesAtOne() {
        assertEquals(1f, ApiaryLogic.advance(0.9f, 0.5f), EPS);
        assertTrue(ApiaryLogic.isComplete(1f));
        assertFalse(ApiaryLogic.isComplete(0.99f));
    }

    @Test
    void emptyApiaryAbsorbsBeeOnlyOutsideWinter() {
        assertTrue(ApiaryLogic.shouldAbsorb(0, 1f, 1, 0.1, 0.25));
        assertFalse(ApiaryLogic.shouldAbsorb(0, 1f, 1, 0.3, 0.25));
        assertFalse(ApiaryLogic.shouldAbsorb(0, 0f, 1, 0.1, 0.25));
        assertFalse(ApiaryLogic.shouldAbsorb(5, 1f, 1, 0.1, 0.25));
        assertFalse(ApiaryLogic.shouldAbsorb(0, 1f, 0, 0.1, 0.25));
    }

    @Test
    void crowdedColonyAngersBeesUnlessCalm() {
        assertTrue(ApiaryLogic.angersBees(21, 20, false));
        assertFalse(ApiaryLogic.angersBees(20, 20, false));
        assertFalse(ApiaryLogic.angersBees(50, 20, true));
    }

    @Test
    void pollinatorContributionIsCapped() {
        assertEquals(0f, ApiaryLogic.pollinatorContribution(0), EPS);
        assertEquals(0.25f, ApiaryLogic.pollinatorContribution(1), EPS);
        assertEquals(0.5f, ApiaryLogic.pollinatorContribution(2), EPS);
        assertEquals(0.5f, ApiaryLogic.pollinatorContribution(5), EPS);
    }

    @Test
    void habitatBonusAppliesWithAnyPopulatedApiary() {
        assertEquals(0f, ApiaryLogic.habitatContribution(0), EPS);
        assertEquals(0.05f, ApiaryLogic.habitatContribution(3), EPS);
    }

    @Test
    void honeyTypeIdRoundTripsAndFallsBackToWildflower() {
        for (HoneyType type : HoneyType.values()) {
            assertEquals(type, HoneyType.fromId(type.id()));
        }
        assertEquals(HoneyType.WILDFLOWER, HoneyType.fromId("inconnu"));
    }
}
