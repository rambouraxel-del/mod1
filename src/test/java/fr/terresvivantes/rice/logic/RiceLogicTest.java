package fr.terresvivantes.rice.logic;

import fr.terresvivantes.crop.logic.Affinity;
import fr.terresvivantes.season.logic.Season;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RiceLogicTest {

    private static final float DELTA = 1e-5f;

    @Test
    void affinityFollowsTheSeasons() {
        assertEquals(Affinity.OPTIMAL, RiceLogic.affinity(Season.SUMMER));
        assertEquals(Affinity.POSSIBLE, RiceLogic.affinity(Season.SPRING));
        assertEquals(Affinity.UNFAVORABLE, RiceLogic.affinity(Season.AUTUMN));
        assertEquals(Affinity.IMPOSSIBLE, RiceLogic.affinity(Season.WINTER));
    }

    @Test
    void summerInRangeGivesTheOptimalProduct() {
        // saison 1.2 * optimal 1.25 * climat 1.0 * sol 1.0 * humidité 1.0
        assertEquals(1.2f * 1.25f, RiceLogic.growthFactor(1.2f, Season.SUMMER, 25.0f), DELTA);
    }

    @Test
    void winterStopsGrowthWhateverTheTemperature() {
        assertEquals(0.0f, RiceLogic.growthFactor(1.0f, Season.WINTER, 25.0f), DELTA);
    }

    @Test
    void temperatureOutsideTheRangeWeakensGrowth() {
        // 10 °C : 8 degrés hors plage (18 à 35) -> limite proche, facteur climatique 0.3
        assertEquals(0.3f, RiceLogic.growthFactor(1.0f, Season.SPRING, 10.0f), DELTA);
        // 0 °C : au-delà de 8 degrés -> arrêt
        assertEquals(0.0f, RiceLogic.growthFactor(1.0f, Season.SPRING, 0.0f), DELTA);
    }

    @Test
    void attemptChanceMatchesSevenStepsOverTwentyDays() {
        double expected = RiceLogic.MAX_AGE / (RiceLogic.GROWTH_DAYS * RiceLogic.RANDOM_TICKS_PER_DAY);
        assertEquals(expected, RiceLogic.attemptChance(), 1e-9);
        assertTrue(RiceLogic.attemptChance() > 0.0 && RiceLogic.attemptChance() <= 1.0);
    }

    @Test
    void stageForAgeMapsPairsOfAges() {
        int[] expected = {0, 0, 1, 1, 2, 2, 3, 3};
        for (int age = 0; age < expected.length; age++) {
            assertEquals(expected[age], RiceLogic.stageForAge(age), "âge " + age);
        }
    }

    @Test
    void clampAgeStaysInRange() {
        assertEquals(0, RiceLogic.clampAge(-3));
        assertEquals(RiceLogic.MAX_AGE, RiceLogic.clampAge(12));
        assertEquals(4, RiceLogic.clampAge(4));
    }

    @Test
    void survivalNeedsSoilWaterAndAWaterNeighbour() {
        assertTrue(RiceLogic.survives(true, true, 1));
        assertFalse(RiceLogic.survives(false, true, 3), "sol non admis");
        assertFalse(RiceLogic.survives(true, false, 3), "plus d'eau dans le plant");
        assertFalse(RiceLogic.survives(true, true, 0), "aucune eau autour");
    }

    @Test
    void plantingNeedsASingleBlockOfWaterAboveTheSoil() {
        assertTrue(RiceLogic.canPlant(true, true, false, 2));
        assertFalse(RiceLogic.canPlant(true, true, true, 2), "eau au-dessus : profondeur supérieure à 1");
        assertFalse(RiceLogic.canPlant(true, false, false, 2), "pas une source");
        assertFalse(RiceLogic.canPlant(false, true, false, 2), "sol non admis");
        assertFalse(RiceLogic.canPlant(true, true, false, 0), "aucune eau voisine");
    }
}
