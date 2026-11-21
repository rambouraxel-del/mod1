package fr.terresvivantes.insects.logic;

import fr.terresvivantes.season.logic.Season;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AphidLogicTest {

    /** Tolérance large de quelques ulp: les suppresseurs sont des float (0.2f n'est pas exact). */
    private static final double DELTA = 1e-6;

    @Test
    void aphidsOnlyAppearInWarmSeasons() {
        assertTrue(ButterflyLogic.isWarmSeason(Season.SPRING));
        assertTrue(ButterflyLogic.isWarmSeason(Season.SUMMER));
        assertFalse(ButterflyLogic.isWarmSeason(Season.AUTUMN));
        assertFalse(ButterflyLogic.isWarmSeason(Season.WINTER));
    }

    @Test
    void biodiversityHalvesTheConditionalChanceAtMaximum() {
        assertEquals(1.0, AphidLogic.conditionalChance(0f, 1f), DELTA);
        assertEquals(0.5, AphidLogic.conditionalChance(1f, 1f), DELTA);
    }

    @Test
    void ladybugSuppressionMultipliesTheChance() {
        assertEquals(0.2, AphidLogic.conditionalChance(0f, AphidLogic.LADYBUG_SUPPRESSION), DELTA);
        assertEquals(0.1, AphidLogic.conditionalChance(1f, AphidLogic.LADYBUG_SUPPRESSION), DELTA);
    }

    @Test
    void conditionalChanceStaysWithinUnitInterval() {
        assertEquals(1.0, AphidLogic.conditionalChance(0f, 5f), DELTA);
        assertEquals(0.0, AphidLogic.conditionalChance(0f, -1f), DELTA);
        assertEquals(0.5, AphidLogic.conditionalChance(2f, 1f), DELTA);
    }

    @Test
    void twoStepRollsGiveTheProductOfTheFactors() {
        // base 0.3 % x biodiversité 0.5 (indice 0.5 -> facteur 0.75) x coccinelle 0.2
        double combined = AphidLogic.BASE_CHANCE * AphidLogic.conditionalChance(0.5f, AphidLogic.LADYBUG_SUPPRESSION);
        assertEquals(0.003 * 0.75 * 0.2, combined, DELTA);
    }

    @Test
    void rollsSucceedBelowTheChanceOnly() {
        assertTrue(AphidLogic.rollSucceeds(0.003, 0.002));
        assertFalse(AphidLogic.rollSucceeds(0.003, 0.003));
        assertFalse(AphidLogic.rollSucceeds(0.0, 0.0));
    }

    @Test
    void designConstantsMatchTheSpec() {
        assertEquals(0.003, AphidLogic.BASE_CHANCE, DELTA);
        assertEquals(0.08, AphidLogic.PROPAGATION_CHANCE, DELTA);
        assertEquals(0.15, AphidLogic.CURE_CHANCE, DELTA);
        assertEquals(40, AphidLogic.CURE_INTERVAL_TICKS);
        assertEquals(0.2f, AphidLogic.LADYBUG_SUPPRESSION, 1e-6f);
    }
}
