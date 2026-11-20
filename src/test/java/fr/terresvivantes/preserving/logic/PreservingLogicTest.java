package fr.terresvivantes.preserving.logic;

import fr.terresvivantes.season.logic.Season;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PreservingLogicTest {

    @Test
    void fermentationSlowsAboveTwentyEightDegrees() {
        assertEquals(0.7f, PreservingLogic.fermentationFactor(28.5f), 1e-6f);
        assertEquals(1f, PreservingLogic.fermentationFactor(28f), 1e-6f);
    }

    @Test
    void fermentationSpeedsUpInCoolCellar() {
        assertEquals(1.2f, PreservingLogic.fermentationFactor(8f), 1e-6f);
        assertEquals(1.2f, PreservingLogic.fermentationFactor(18f), 1e-6f);
        assertEquals(1f, PreservingLogic.fermentationFactor(7.9f), 1e-6f);
        assertEquals(1f, PreservingLogic.fermentationFactor(18.1f), 1e-6f);
    }

    @Test
    void dryingFollowsTheSeason() {
        assertEquals(1.5f, PreservingLogic.dryingSeasonFactor(Season.SUMMER), 1e-6f);
        assertEquals(0.3f, PreservingLogic.dryingSeasonFactor(Season.WINTER), 1e-6f);
        assertEquals(1f, PreservingLogic.dryingSeasonFactor(Season.SPRING), 1e-6f);
        assertEquals(1f, PreservingLogic.dryingSeasonFactor(Season.AUTUMN), 1e-6f);
    }

    @Test
    void dryingNeedsSkyAndNoRain() {
        assertTrue(PreservingLogic.canDry(true, false));
        assertFalse(PreservingLogic.canDry(false, false));
        assertFalse(PreservingLogic.canDry(true, true));
        assertFalse(PreservingLogic.canDry(false, true));
    }
}
