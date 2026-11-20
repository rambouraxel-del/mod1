package fr.terresvivantes.livestock.logic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LivestockLogicTest {

    private static final double EPS = 1e-6;

    @Test
    void unfedAnimalLosesHealthAndStaysAtZero() {
        LivestockState state = new LivestockState(0, 100, Ailment.NONE, 0L);
        LivestockState next = LivestockLogic.needsStep(state, false, false, 5L);
        assertEquals(0, next.fed());
        assertEquals(95, next.health());
    }

    @Test
    void wellFedAnimalRecoversHealth() {
        LivestockState state = new LivestockState(70, 80, Ailment.NONE, 0L);
        LivestockState next = LivestockLogic.needsStep(state, false, false, 5L);
        assertEquals(62, next.fed());
        assertEquals(82, next.health());
    }

    @Test
    void winterConsumesOneAndAHalfTimesMoreAndDoesNotGraze() {
        LivestockState state = new LivestockState(50, 100, Ailment.NONE, 0L);
        LivestockState winter = LivestockLogic.needsStep(state, true, true, 9L);
        assertEquals(38, winter.fed());
        assertEquals(0L, winter.lastFedDay());
        LivestockState summer = LivestockLogic.needsStep(state, false, false, 9L);
        assertEquals(42, summer.fed());
    }

    @Test
    void grazingOutOfWinterGivesFoodAndRecordsDay() {
        LivestockState state = new LivestockState(40, 100, Ailment.NONE, 0L);
        LivestockState next = LivestockLogic.needsStep(state, false, true, 12L);
        assertEquals(42, next.fed());
        assertEquals(12L, next.lastFedDay());
    }

    @Test
    void needsCapFoodAt100() {
        LivestockState state = new LivestockState(100, 100, Ailment.NONE, 0L);
        LivestockState next = LivestockLogic.needsStep(state, false, true, 1L);
        assertEquals(100, next.fed());
        assertEquals(100, next.health());
    }

    @Test
    void productionMultiplierFollowsFormula() {
        // productivité 5, satiété moyenne, sain : 0.6 + 0.4 = 1.0
        assertEquals(1.0f, LivestockLogic.productionMultiplier(5, 50, false), EPS);
        // productivité 10, bien nourri : 1.4 × 1.1 = 1.54
        assertEquals(1.54f, LivestockLogic.productionMultiplier(10, 80, false), EPS);
        // productivité 0, affamé (< 20) : 0.6 × 0.5 = 0.3
        assertEquals(0.3f, LivestockLogic.productionMultiplier(0, 10, false), EPS);
        // malade : moitié
        assertEquals(0.5f, LivestockLogic.productionMultiplier(5, 50, true), EPS);
    }

    @Test
    void breedingNeedsFortyFoodAndSpringOrSummerWhenSeasonal() {
        assertTrue(LivestockLogic.canBreed(40, true, true));
        assertFalse(LivestockLogic.canBreed(39, true, true));
        assertFalse(LivestockLogic.canBreed(80, false, true));
        assertTrue(LivestockLogic.canBreed(80, false, false));
    }

    @Test
    void twinChanceIsFertilityTimesThreePercent() {
        assertEquals(0.0, LivestockLogic.twinChance(0), EPS);
        assertEquals(0.09, LivestockLogic.twinChance(3), EPS);
        assertEquals(0.3, LivestockLogic.twinChance(10), EPS);
    }

    @Test
    void probabilisticRoundingKeepsExpectedValue() {
        // -0.4 : valeur entière -1 avec la proba 0.6 (rnd < 0.6 => -1 + 1 = 0), sinon -1.
        assertEquals(0, LivestockLogic.roundProbabilistic(-0.4, () -> 0.1));
        assertEquals(-1, LivestockLogic.roundProbabilistic(-0.4, () -> 0.9));
        assertEquals(2, LivestockLogic.roundProbabilistic(2.0, () -> 0.99));
    }

    @Test
    void stateValuesAreClampedAndAilmentDefaultsToNone() {
        LivestockState state = new LivestockState(150, -4, null, 3L);
        assertEquals(100, state.fed());
        assertEquals(0, state.health());
        assertEquals(Ailment.NONE, state.ailment());
        assertEquals(Ailment.SICK, Ailment.fromName("sick"));
        assertEquals(Ailment.NONE, Ailment.fromName("unknown"));
    }
}
