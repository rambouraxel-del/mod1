package fr.terresvivantes.climate.logic;

import fr.terresvivantes.season.logic.Season;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClimateModelTest {

    private static final float DELTA = 1e-4f;

    @Test
    void biomeBaseIsScaledByTwentyFive() {
        assertEquals(20.0f, ClimateModel.baseTemperature(0.8f), DELTA);
        assertEquals(50.0f, ClimateModel.baseTemperature(2.0f), DELTA);
        assertEquals(-12.5f, ClimateModel.baseTemperature(-0.5f), DELTA);
    }

    @Test
    void seasonOffsetsMatchDesign() {
        assertEquals(0.0f, ClimateModel.seasonOffset(Season.SPRING), DELTA);
        assertEquals(8.0f, ClimateModel.seasonOffset(Season.SUMMER), DELTA);
        assertEquals(-2.0f, ClimateModel.seasonOffset(Season.AUTUMN), DELTA);
        assertEquals(-12.0f, ClimateModel.seasonOffset(Season.WINTER), DELTA);
    }

    @Test
    void altitudeCostsPointSixDegreesPerTenBlocksAboveSixtyFour() {
        assertEquals(0.0f, ClimateModel.altitudeOffset(64), DELTA);
        assertEquals(-0.6f, ClimateModel.altitudeOffset(74), DELTA);
        assertEquals(-6.0f, ClimateModel.altitudeOffset(164), DELTA);
    }

    @Test
    void noAltitudeBonusBelowSixtyFour() {
        assertEquals(0.0f, ClimateModel.altitudeOffset(-60), DELTA);
        assertEquals(0.0f, ClimateModel.altitudeOffset(10), DELTA);
    }

    @Test
    void nightIsTheVanillaRangeThirteenThousandToTwentyThreeThousand() {
        assertFalse(ClimateModel.isNight(0));
        assertFalse(ClimateModel.isNight(12999));
        assertTrue(ClimateModel.isNight(13000));
        assertTrue(ClimateModel.isNight(22999));
        assertFalse(ClimateModel.isNight(23000));
        assertTrue(ClimateModel.isNight(24000L * 5 + 15000));
    }

    @Test
    void overworldTemperatureSumsAllTerms() {
        // Plaines (0.8 -> 20 °C), ete, y=74 (-0.6), jour: 20 + 8 - 0.6 = 27.4
        assertEquals(27.4f, ClimateModel.overworldTemperature(0.8f, Season.SUMMER, 74, 1000), DELTA);
        // Meme point la nuit: -4 supplementaires
        assertEquals(23.4f, ClimateModel.overworldTemperature(0.8f, Season.SUMMER, 74, 18000), DELTA);
    }

    @Test
    void winterIsColderThanSummerAtSamePlace() {
        float summer = ClimateModel.overworldTemperature(0.8f, Season.SUMMER, 64, 1000);
        float winter = ClimateModel.overworldTemperature(0.8f, Season.WINTER, 64, 1000);
        assertTrue(winter < summer);
        assertEquals(20.0f, summer - winter, DELTA);
    }

    @Test
    void otherDimensionIgnoresSeasonAndNight() {
        assertEquals(20.0f, ClimateModel.otherDimensionTemperature(0.8f, 64), DELTA);
        assertEquals(14.0f, ClimateModel.otherDimensionTemperature(0.8f, 164), DELTA);
    }
}
