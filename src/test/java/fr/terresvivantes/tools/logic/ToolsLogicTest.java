package fr.terresvivantes.tools.logic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ToolsLogicTest {

    @Test
    void nutrientTiersUseThresholds() {
        assertEquals(ToolsLogic.Tier.LOW, ToolsLogic.nutrientTier(0));
        assertEquals(ToolsLogic.Tier.LOW, ToolsLogic.nutrientTier(29));
        assertEquals(ToolsLogic.Tier.MEDIUM, ToolsLogic.nutrientTier(30));
        assertEquals(ToolsLogic.Tier.MEDIUM, ToolsLogic.nutrientTier(59));
        assertEquals(ToolsLogic.Tier.HIGH, ToolsLogic.nutrientTier(60));
        assertEquals(ToolsLogic.Tier.HIGH, ToolsLogic.nutrientTier(100));
    }

    @Test
    void moistureTiersFollowVanillaRange() {
        assertEquals(ToolsLogic.Tier.LOW, ToolsLogic.moistureTier(0));
        assertEquals(ToolsLogic.Tier.MEDIUM, ToolsLogic.moistureTier(3));
        assertEquals(ToolsLogic.Tier.HIGH, ToolsLogic.moistureTier(ToolsLogic.MAX_MOISTURE));
    }

    @Test
    void toolConstantsMatchDesign() {
        assertEquals(250, ToolsLogic.SICKLE_DURABILITY);
        assertEquals(8, ToolsLogic.WATERING_CAN_USES);
        assertEquals(7, ToolsLogic.WATERED_MOISTURE);
    }
}
