package fr.terresvivantes.guild.logic;

import fr.terresvivantes.economy.logic.ReputationLogic.Tier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuildLogicTest {

    @Test
    void levelRequiresTheMatchingTierAndIsClamped() {
        assertEquals(0, GuildLogic.requiredTierIndex(1));
        assertEquals(2, GuildLogic.requiredTierIndex(3));
        assertEquals(4, GuildLogic.requiredTierIndex(5));
        assertEquals(0, GuildLogic.requiredTierIndex(-2));
        assertEquals(4, GuildLogic.requiredTierIndex(9));
    }

    /** Offre de niveau 3 bloquée sous le palier Fournisseur (réputation 200). */
    @Test
    void levelThreeIsBlockedBelowSupplier() {
        assertTrue(GuildLogic.isLocked(3, Tier.UNKNOWN));
        assertTrue(GuildLogic.isLocked(3, Tier.REGULAR));
        assertFalse(GuildLogic.isLocked(3, Tier.SUPPLIER));
        assertFalse(GuildLogic.isLocked(3, Tier.MASTER_FARMER));
    }

    @Test
    void levelOneIsAlwaysOpenAndLevelFiveNeedsMasterTier() {
        assertFalse(GuildLogic.isLocked(1, Tier.UNKNOWN));
        assertTrue(GuildLogic.isLocked(5, Tier.REPUTED_PRODUCER));
        assertFalse(GuildLogic.isLocked(5, Tier.MASTER_FARMER));
    }
}
