package fr.terresvivantes.dairy.logic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CheeseLogicTest {

    @Test
    void darkRoomAgesFasterThanTheDefault() {
        assertEquals(CheeseLogic.DARK_SPEED, CheeseLogic.agingSpeed(7, false), 0.0001F);
        assertEquals(CheeseLogic.DARK_SPEED, CheeseLogic.agingSpeed(0, false), 0.0001F);
        assertEquals(1.0F, CheeseLogic.agingSpeed(8, false), 0.0001F);
        assertEquals(1.0F, CheeseLogic.agingSpeed(15, false), 0.0001F);
    }

    @Test
    void daylightAgesSlowerAndWinsOverDarkness() {
        assertEquals(CheeseLogic.SUNLIT_SPEED, CheeseLogic.agingSpeed(15, true), 0.0001F);
        assertEquals(CheeseLogic.SUNLIT_SPEED, CheeseLogic.agingSpeed(2, true), 0.0001F);
    }

    @Test
    void ripeningAddsOneStarAndCapsAtThree() {
        assertEquals(3, CheeseLogic.ripenedQuality(2));
        assertEquals(2, CheeseLogic.ripenedQuality(1));
        assertEquals(3, CheeseLogic.ripenedQuality(3));
        assertEquals(2, CheeseLogic.ripenedQuality(0));
    }

    @Test
    void saturationGrowsTwentyPercentPerStar() {
        assertEquals(1.0F, CheeseLogic.saturationFactor(1), 0.0001F);
        assertEquals(1.2F, CheeseLogic.saturationFactor(2), 0.0001F);
        assertEquals(1.4F, CheeseLogic.saturationFactor(3), 0.0001F);
        assertEquals(1.0F, CheeseLogic.saturationFactor(0), 0.0001F);
    }

    @Test
    void milkSourcesAreCowOrGoat() {
        assertTrue(MilkSource.isKnown(MilkSource.COW));
        assertTrue(MilkSource.isKnown(MilkSource.GOAT));
        assertFalse(MilkSource.isKnown("sheep"));
    }
}
