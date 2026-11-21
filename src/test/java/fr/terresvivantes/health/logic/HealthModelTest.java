package fr.terresvivantes.health.logic;

import fr.terresvivantes.crop.logic.GrowthModel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HealthModelTest {

    private static final float DELTA = 1e-6f;
    private static final double CHANCE_DELTA = 1e-9;

    /** Conditions neutres : rien ne favorise la maladie. */
    private static HealthModel.Conditions calm() {
        return new HealthModel.Conditions(false, false, 15.0f, 1, false, false);
    }

    @Test
    void zeroFactorAddsStressAndGrowthRemovesIt() {
        assertEquals(1, HealthModel.nextStress(0, 0.0f, false));
        assertEquals(3, HealthModel.nextStress(2, 0.0f, false));
        assertEquals(1, HealthModel.nextStress(2, 0.5f, true));
        assertEquals(0, HealthModel.nextStress(0, 0.5f, true), "le stress ne descend pas sous 0");
        assertEquals(2, HealthModel.nextStress(2, 0.5f, false), "une tentative sans croissance ne change rien");
    }

    @Test
    void witheringThresholdsAreStressFourAndSixSickTicks() {
        assertFalse(HealthModel.withersFromStress(3));
        assertTrue(HealthModel.withersFromStress(4));
        assertFalse(HealthModel.withersFromDisease(Disease.MILDEW, 5));
        assertTrue(HealthModel.withersFromDisease(Disease.MILDEW, 6));
        assertFalse(HealthModel.withersFromDisease(Disease.NONE, 99));
    }

    @Test
    void sickCropGrowsAndYieldsAtHalfOrLessOfHealthy() {
        assertEquals(0.3f, HealthModel.growthFactor(true), DELTA);
        assertEquals(1.0f, HealthModel.growthFactor(false), DELTA);
        assertEquals(0.5f, HealthModel.yieldFactor(true), DELTA);
        assertEquals(1.0f, HealthModel.yieldFactor(false), DELTA);
        // Via le produit des modifiers (contrat v0.2) : la croissance malade vaut 0.3 du total.
        assertEquals(0.3f, GrowthModel.combineModifiers(HealthModel.growthFactor(true)), DELTA);
    }

    @Test
    void baseChanceIsZeroPointTwentyFivePerMille() {
        // v0.8 : 0,4 % -> 0,25 % (équilibrage des maladies, docs/BALANCE.md).
        assertEquals(0.0025, HealthModel.infectionChance(Disease.MILDEW, calm()), CHANCE_DELTA);
        assertEquals(0.0025, HealthModel.infectionChance(Disease.RUST, calm()), CHANCE_DELTA);
    }

    @Test
    void immuneOrHealthyDiseaseHasNoChance() {
        HealthModel.Conditions immune = new HealthModel.Conditions(true, true, 20.0f, 9, true, true);
        assertEquals(0.0, HealthModel.infectionChance(Disease.MILDEW, immune), CHANCE_DELTA);
        assertEquals(0.0, HealthModel.infectionChance(Disease.NONE, calm()), CHANCE_DELTA);
    }

    @Test
    void mildewIsFourTimesMoreLikelyOnSaturatedSoilWithRainOrHeat() {
        HealthModel.Conditions rainy = new HealthModel.Conditions(true, true, 15.0f, 1, false, false);
        HealthModel.Conditions hot = new HealthModel.Conditions(true, false, 23.0f, 1, false, false);
        HealthModel.Conditions dry = new HealthModel.Conditions(false, true, 23.0f, 1, false, false);
        assertEquals(0.01, HealthModel.infectionChance(Disease.MILDEW, rainy), CHANCE_DELTA);
        assertEquals(0.01, HealthModel.infectionChance(Disease.MILDEW, hot), CHANCE_DELTA);
        assertEquals(0.0025, HealthModel.infectionChance(Disease.MILDEW, dry), CHANCE_DELTA,
                "sol non saturé : pas de bonus d'oïdium");
    }

    @Test
    void rustNeedsRainBetweenTwelveAndTwentyFourDegrees() {
        HealthModel.Conditions inRange = new HealthModel.Conditions(false, true, 12.0f, 1, false, false);
        HealthModel.Conditions tooHot = new HealthModel.Conditions(false, true, 25.0f, 1, false, false);
        HealthModel.Conditions noRain = new HealthModel.Conditions(false, false, 18.0f, 1, false, false);
        assertEquals(0.01, HealthModel.infectionChance(Disease.RUST, inRange), CHANCE_DELTA);
        assertEquals(0.0025, HealthModel.infectionChance(Disease.RUST, tooHot), CHANCE_DELTA);
        assertEquals(0.0025, HealthModel.infectionChance(Disease.RUST, noRain), CHANCE_DELTA);
    }

    @Test
    void monocultureDoublesAndDenseFieldsMultiplyByOneAndAHalf() {
        assertEquals(1.5, HealthModel.densityFactor(6), CHANCE_DELTA);
        assertEquals(1.0, HealthModel.densityFactor(5), CHANCE_DELTA);
        assertTrue(HealthModel.monoculture(2, true));
        assertFalse(HealthModel.monoculture(1, true));
        assertFalse(HealthModel.monoculture(3, false), "même répétition, autre culture : pas de monoculture");
        HealthModel.Conditions mono = new HealthModel.Conditions(false, false, 15.0f, 6, true, false);
        // v0.8 : monoculture x2.5 (était x2).
        assertEquals(0.0025 * 2.5 * 1.5, HealthModel.infectionChance(Disease.MILDEW, mono), CHANCE_DELTA);
    }

    @Test
    void infectionAndContagionUseStrictThresholds() {
        assertTrue(HealthModel.infects(0.004, 0.003));
        assertFalse(HealthModel.infects(0.004, 0.004));
        assertTrue(HealthModel.spreads(0.09));
        assertFalse(HealthModel.spreads(0.10));
    }

    @Test
    void immunityLastsUntilTheEndOfTheSeason() {
        // Jour absolu 10, jour 3 d'une saison de 7 jours : il reste 4 jours, immunité jusqu'au jour 14.
        assertEquals(14L, HealthModel.immuneUntilDay(10L, 3, 7));
        // Dernier jour de la saison : immunité jusqu'à aujourd'hui.
        assertEquals(10L, HealthModel.immuneUntilDay(10L, 7, 7));
        assertTrue(HealthModel.isImmune(14L, 14L));
        assertFalse(HealthModel.isImmune(15L, 14L));
    }

    @Test
    void cropHealthReportsSicknessAndImmunity() {
        CropHealth sick = new CropHealth(0, Disease.RUST, 2, 20L);
        assertTrue(sick.sick());
        assertTrue(sick.immune(20L));
        assertFalse(sick.immune(21L));
        assertFalse(CropHealth.HEALTHY.sick());
        assertFalse(CropHealth.HEALTHY.immune(0L));
    }

    @Test
    void susceptibilityMatchesDesignLists() {
        assertEquals(Disease.MILDEW, Disease.susceptibility("tomato"));
        assertEquals(Disease.MILDEW, Disease.susceptibility("minecraft:potatoes"));
        assertEquals(Disease.RUST, Disease.susceptibility("barley"));
        assertEquals(Disease.RUST, Disease.susceptibility("minecraft:wheat"));
        assertEquals(Disease.NONE, Disease.susceptibility("onion"));
        assertEquals(Disease.NONE, Disease.susceptibility("garlic"));
    }
}
