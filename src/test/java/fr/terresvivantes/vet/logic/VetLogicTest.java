package fr.terresvivantes.vet.logic;

import fr.terresvivantes.livestock.logic.Ailment;
import fr.terresvivantes.livestock.logic.LivestockState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VetLogicTest {

    private static final double EPS = 1e-9;

    @Test
    void baseSickChanceForHealthyCrowdFedHardyAnimal() {
        assertEquals(0.01, VetLogic.sickChance(1, 50, 5, false), EPS);
    }

    @Test
    void hardinessScalesFromOneAndAHalfToHalf() {
        assertEquals(1.5, VetLogic.hardinessFactor(0), EPS);
        assertEquals(1.0, VetLogic.hardinessFactor(5), EPS);
        assertEquals(0.5, VetLogic.hardinessFactor(10), EPS);
        assertEquals(0.015, VetLogic.sickChance(1, 50, 0, false), EPS);
    }

    @Test
    void crowdThresholdIsStrictlyMoreThanEight() {
        assertFalse(VetLogic.isCrowded(8));
        assertTrue(VetLogic.isCrowded(9));
        assertEquals(0.04, VetLogic.sickChance(9, 50, 5, false), EPS);
        assertEquals(0.01, VetLogic.sickChance(8, 50, 5, false), EPS);
    }

    @Test
    void hungerTriplesSickChanceUnderTwenty() {
        assertEquals(0.03, VetLogic.sickChance(1, 10, 5, false), EPS);
        assertEquals(0.01, VetLogic.sickChance(1, 20, 5, false), EPS);
    }

    @Test
    void saltLickHalvesSickChance() {
        double without = VetLogic.sickChance(9, 10, 5, false);
        double with = VetLogic.sickChance(9, 10, 5, true);
        assertEquals(0.5 * without, with, EPS);
    }

    @Test
    void parasiteChanceFollowsHardiness() {
        assertEquals(0.006, VetLogic.parasiteChance(5), EPS);
        assertEquals(0.009, VetLogic.parasiteChance(0), EPS);
    }

    @Test
    void parasitesNeedAGrazerOutdoorsInRainOrWetGrassInSpringOrAutumn() {
        assertFalse(VetLogic.parasiteExposed(false, true, true, true));
        assertTrue(VetLogic.parasiteExposed(true, true, false, false));
        assertTrue(VetLogic.parasiteExposed(true, false, true, true));
        assertFalse(VetLogic.parasiteExposed(true, false, true, false));
        assertFalse(VetLogic.parasiteExposed(true, false, false, true));
    }

    @Test
    void rollHitsOnlyBelowChance() {
        assertTrue(VetLogic.hits(0.5, 0.49));
        assertFalse(VetLogic.hits(0.5, 0.5));
        assertTrue(VetLogic.contagionHits(0.04));
        assertFalse(VetLogic.contagionHits(0.05));
    }

    @Test
    void sickCycleCostsFourHealthAndStopsAtZero() {
        LivestockState state = new LivestockState(50, 10, Ailment.SICK, 0L);
        assertEquals(6, VetLogic.sickStep(state).health());
        assertEquals(0, VetLogic.sickStep(new LivestockState(50, 2, Ailment.SICK, 0L)).health());
    }

    @Test
    void parasitesCostSixFoodPerCycle() {
        LivestockState state = new LivestockState(50, 80, Ailment.PARASITES, 0L);
        assertEquals(44, VetLogic.parasiteStep(state).fed());
        assertEquals(0, VetLogic.parasiteStep(new LivestockState(3, 80, Ailment.PARASITES, 0L)).fed());
    }

    @Test
    void saltLickGivesOneHealthCappedAtMax() {
        assertEquals(51, VetLogic.saltStep(new LivestockState(50, 50, Ailment.NONE, 0L)).health());
        assertEquals(100, VetLogic.saltStep(new LivestockState(50, 100, Ailment.NONE, 0L)).health());
    }

    @Test
    void kitCuresAndHealsTwentyCappedAtMax() {
        LivestockState treated = VetLogic.treat(new LivestockState(30, 50, Ailment.SICK, 4L));
        assertEquals(Ailment.NONE, treated.ailment());
        assertEquals(70, treated.health());
        assertEquals(30, treated.fed());
        assertEquals(4L, treated.lastFedDay());
        assertEquals(100, VetLogic.treat(new LivestockState(30, 95, Ailment.PARASITES, 0L)).health());
    }

    @Test
    void animalDiesOnlyAtZeroHealthWhenConfigured() {
        LivestockState dead = new LivestockState(0, 0, Ailment.SICK, 0L);
        assertTrue(VetLogic.shouldKill(dead, true));
        assertFalse(VetLogic.shouldKill(dead, false));
        assertFalse(VetLogic.shouldKill(new LivestockState(0, 1, Ailment.SICK, 0L), true));
    }

    @Test
    void saltLickDisappearsOnFourthUse() {
        assertEquals(1, VetLogic.saltLickAfterUse(0));
        assertFalse(VetLogic.saltLickGone(VetLogic.saltLickAfterUse(2)));
        assertTrue(VetLogic.saltLickGone(VetLogic.saltLickAfterUse(3)));
    }
}
