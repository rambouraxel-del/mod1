package fr.terresvivantes.soil.logic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SoilModelTest {

    private static final float DELTA = 1e-5f;

    @Test
    void initialValuesFollowBiome() {
        assertEquals(new SoilData(30, 30, 30, 10), SoilModel.initialFor(true, false));
        assertEquals(new SoilData(60, 50, 50, 80), SoilModel.initialFor(false, true));
        assertEquals(new SoilData(60, 50, 50, 30), SoilModel.initialFor(false, false));
    }

    @Test
    void liebigFactorUsesThresholds() {
        assertEquals(0.25f, SoilModel.liebigFactor(0), DELTA);
        assertEquals(0.25f, SoilModel.liebigFactor(19), DELTA);
        assertEquals(0.6f, SoilModel.liebigFactor(20), DELTA);
        assertEquals(0.6f, SoilModel.liebigFactor(39), DELTA);
        assertEquals(1.0f, SoilModel.liebigFactor(40), DELTA);
        assertEquals(1.0f, SoilModel.liebigFactor(100), DELTA);
    }

    @Test
    void nutrientFactorUsesLowestNutrientAndOrganicBonus() {
        assertEquals(0.25f, SoilModel.nutrientFactor(new SoilData(10, 90, 90, 10)), DELTA);
        assertEquals(1.0f, SoilModel.nutrientFactor(new SoilData(50, 50, 50, 30)), DELTA);
        assertEquals(1.1f, SoilModel.nutrientFactor(new SoilData(50, 50, 50, 61)), DELTA);
        assertEquals(1.0f, SoilModel.nutrientFactor(new SoilData(50, 50, 50, 60)), DELTA);
        assertEquals(0.275f, SoilModel.nutrientFactor(new SoilData(10, 90, 90, 80)), DELTA);
    }

    @Test
    void consumeSubtractsWithMultiplierAndClamps() {
        assertEquals(new SoilData(55, 45, 45, 30),
                SoilModel.consume(SoilData.DEFAULT, 5, 5, 5, 1.0));
        assertEquals(new SoilData(57, 47, 47, 30),
                SoilModel.consume(SoilData.DEFAULT, 5, 5, 5, 0.5));
        assertEquals(new SoilData(0, 0, 0, 30),
                SoilModel.consume(new SoilData(3, 3, 3, 30), 50, 50, 50, 1.0));
        assertEquals(new SoilData(60, 50, 50, 30),
                SoilModel.consume(SoilData.DEFAULT, 5, 5, 5, 0.0));
    }

    @Test
    void fertilizeAddsAndClampsAt100() {
        assertEquals(new SoilData(75, 65, 50, 50),
                SoilModel.fertilize(SoilData.DEFAULT, 15, 15, 0, 20));
        assertEquals(new SoilData(100, 50, 50, 100),
                SoilModel.fertilize(SoilData.DEFAULT, 60, 0, 0, 90));
    }

    @Test
    void aerationAllowedOncePerMcDay() {
        assertEquals(true, SoilModel.canAerate(SoilModel.NEVER_AERATED, 5));
        assertEquals(false, SoilModel.canAerate(5, 5));
        assertEquals(true, SoilModel.canAerate(5, 6));
        assertEquals(false, SoilModel.canAerate(7, 6));
    }

    @Test
    void mcDayUsesTwentyFourThousandTicks() {
        assertEquals(0, SoilModel.mcDay(23_999));
        assertEquals(1, SoilModel.mcDay(24_000));
        assertEquals(-1, SoilModel.mcDay(-1));
    }
}
