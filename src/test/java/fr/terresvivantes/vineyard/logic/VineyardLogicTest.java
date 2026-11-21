package fr.terresvivantes.vineyard.logic;

import fr.terresvivantes.season.logic.Season;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VineyardLogicTest {

    @Test
    void climateIsOneInsideTheFavourableRange() {
        assertEquals(1.0f, VineyardLogic.climateFactor(10.0f), 1e-6f);
        assertEquals(1.0f, VineyardLogic.climateFactor(21.0f), 1e-6f);
        assertEquals(1.0f, VineyardLogic.climateFactor(32.0f), 1e-6f);
    }

    @Test
    void climateDropsOutsideTheRangeWithAFloor() {
        assertEquals(0.9f, VineyardLogic.climateFactor(9.0f), 1e-6f);
        assertEquals(0.9f, VineyardLogic.climateFactor(33.0f), 1e-6f);
        assertEquals(0.2f, VineyardLogic.climateFactor(-30.0f), 1e-6f);
        assertEquals(0.2f, VineyardLogic.climateFactor(60.0f), 1e-6f);
    }

    @Test
    void soilCountsOnlyOnFarmland() {
        assertEquals(1.0f, VineyardLogic.soilFactor(false, 0.2f), 1e-6f);
        assertEquals(0.5f, VineyardLogic.soilFactor(true, 0.5f), 1e-6f);
    }

    @Test
    void attemptChanceIsBoundedAndScalesWithFactor() {
        double full = VineyardLogic.attemptChance(1.0f, 4.0);
        assertTrue(full > 0.0 && full < 1.0);
        assertEquals(full * 2, VineyardLogic.attemptChance(2.0f, 4.0), 1e-12);
        assertEquals(1.0, VineyardLogic.attemptChance(1000.0f, 4.0), 1e-12);
        assertEquals(0.0, VineyardLogic.attemptChance(0.0f, 4.0), 1e-12);
        assertThrows(IllegalArgumentException.class, () -> VineyardLogic.attemptChance(1.0f, 0.0));
    }

    @Test
    void springAndSummerGrowTheYoungVine() {
        assertEquals(1, VineyardLogic.nextStage(0, false, Season.SPRING, 1, 7));
        assertEquals(2, VineyardLogic.nextStage(1, false, Season.SUMMER, 1, 7));
        assertEquals(1, VineyardLogic.nextStage(1, false, Season.AUTUMN, 3, 7));
        assertEquals(0, VineyardLogic.nextStage(0, false, Season.WINTER, 3, 7));
    }

    @Test
    void grapesRippenOnlyLateSummerAndAutumn() {
        assertFalse(VineyardLogic.ripens(Season.SUMMER, 3, 7));
        assertTrue(VineyardLogic.ripens(Season.SUMMER, 4, 7));
        assertTrue(VineyardLogic.ripens(Season.AUTUMN, 1, 7));
        assertFalse(VineyardLogic.ripens(Season.SPRING, 7, 7));
        assertEquals(3, VineyardLogic.nextStage(2, false, Season.AUTUMN, 1, 7));
        assertEquals(2, VineyardLogic.nextStage(2, false, Season.SPRING, 1, 7));
    }

    @Test
    void harvestedVineDoesNotRippenAgainUntilPruned() {
        assertEquals(2, VineyardLogic.nextStage(2, true, Season.AUTUMN, 1, 7));
        assertEquals(1, VineyardLogic.winterStage(2));
        assertEquals(1, VineyardLogic.winterStage(3));
        assertEquals(0, VineyardLogic.winterStage(0));
        assertEquals(1, VineyardLogic.winterStage(1));
    }

    @Test
    void harvestGivesTwoToFourBunches() {
        assertEquals(2, VineyardLogic.harvestCount(0.0));
        assertEquals(3, VineyardLogic.harvestCount(0.34));
        assertEquals(4, VineyardLogic.harvestCount(0.7));
        assertEquals(4, VineyardLogic.harvestCount(1.0));
    }

    @Test
    void onlyMatureVinesYieldACutting() {
        assertFalse(VineyardLogic.yieldsCutting(1));
        assertTrue(VineyardLogic.yieldsCutting(2));
        assertTrue(VineyardLogic.yieldsCutting(3));
    }

    @Test
    void redWineGainsOneStarAfterSixDaysOfAgeing() {
        // Recette d'affinage : 144000 ticks = 6 jours MC = une étoile; plafond à 3 étoiles.
        assertEquals(3, VineyardLogic.agedRedWine(2, 144000));
        assertEquals(2, VineyardLogic.agedRedWine(1, 144000));
        assertEquals(3, VineyardLogic.agedRedWine(3, 144000));
    }
}
