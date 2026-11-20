package fr.terresvivantes.crop.logic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GrowthModelTest {

    private static final float DELTA = 1e-5f;

    @Test
    void totalFactorMultipliesAllFactors() {
        assertEquals(1.0f * 1.25f * 1.0f * 1.1f * 1.0f,
                GrowthModel.totalFactor(1.0f, 1.25f, 1.0f, 1.1f, 1.0f), DELTA);
        assertEquals(0.0f, GrowthModel.totalFactor(0.0f, 1.25f, 1.0f, 1.0f, 1.0f), DELTA);
        assertEquals(0.5f, GrowthModel.totalFactor(1.0f, 1.0f, 1.0f, 1.0f, 0.5f), DELTA);
    }

    @Test
    void climateFactorInsideRangeIsOne() {
        assertEquals(1.0f, GrowthModel.climateFactor(20.0f, 12.0f, 32.0f), DELTA);
        assertEquals(1.0f, GrowthModel.climateFactor(12.0f, 12.0f, 32.0f), DELTA);
        assertEquals(1.0f, GrowthModel.climateFactor(32.0f, 12.0f, 32.0f), DELTA);
    }

    @Test
    void climateFactorNearOutOfRangeIsReduced() {
        assertEquals(0.3f, GrowthModel.climateFactor(5.0f, 12.0f, 32.0f), DELTA);
        assertEquals(0.3f, GrowthModel.climateFactor(4.0f, 12.0f, 32.0f), DELTA);
        assertEquals(0.3f, GrowthModel.climateFactor(40.0f, 12.0f, 32.0f), DELTA);
    }

    @Test
    void climateFactorFarOutOfRangeIsZero() {
        assertEquals(0.0f, GrowthModel.climateFactor(3.0f, 12.0f, 32.0f), DELTA);
        assertEquals(0.0f, GrowthModel.climateFactor(45.0f, 12.0f, 32.0f), DELTA);
    }

    @Test
    void decideDeniesBelowOneWhenRollIsUnderMissingPart() {
        assertEquals(GrowthDecision.DENY, GrowthModel.decide(0.5f, 0.4, 0.9));
        assertEquals(GrowthDecision.NORMAL, GrowthModel.decide(0.5f, 0.6, 0.9));
        assertEquals(GrowthDecision.DENY, GrowthModel.decide(0.0f, 0.99, 0.0));
    }

    @Test
    void decideGivesExtraStageAboveOne() {
        assertEquals(GrowthDecision.EXTRA_STAGE, GrowthModel.decide(1.5f, 0.99, 0.2));
        assertEquals(GrowthDecision.NORMAL, GrowthModel.decide(1.5f, 0.0, 0.7));
    }

    @Test
    void decideIsNormalAtOne() {
        assertEquals(GrowthDecision.NORMAL, GrowthModel.decide(1.0f, 0.0, 0.0));
        assertEquals(GrowthDecision.NORMAL, GrowthModel.decide(1.0f, 0.99, 0.99));
    }
}
