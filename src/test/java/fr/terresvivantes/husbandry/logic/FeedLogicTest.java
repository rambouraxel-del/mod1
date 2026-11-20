package fr.terresvivantes.husbandry.logic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FeedLogicTest {

    @Test
    void hayIsRefusedByPoultry() {
        assertEquals(0, FeedLogic.points(Species.CHICKEN, FeedType.HAY));
        assertEquals(0, FeedLogic.points(Species.DUCK, FeedType.HAY));
        assertEquals(15, FeedLogic.points(Species.COW, FeedType.HAY));
    }

    @Test
    void grainIsRicherForPoultryAndPigs() {
        assertEquals(18, FeedLogic.points(Species.CHICKEN, FeedType.GRAIN));
        assertEquals(18, FeedLogic.points(Species.PIG, FeedType.GRAIN));
        assertEquals(12, FeedLogic.points(Species.SHEEP, FeedType.GRAIN));
    }

    @Test
    void silageIsBestForRuminantsAndHalfForOthers() {
        assertEquals(20, FeedLogic.points(Species.COW, FeedType.SILAGE));
        assertEquals(20, FeedLogic.points(Species.SHEEP, FeedType.SILAGE));
        assertEquals(20, FeedLogic.points(Species.GOAT, FeedType.SILAGE));
        assertEquals(10, FeedLogic.points(Species.PIG, FeedType.SILAGE));
        assertEquals(10, FeedLogic.points(Species.OTHER, FeedType.SILAGE));
    }

    @Test
    void mixGivesTwentyFiveToEverySpecies() {
        for (Species species : Species.values()) {
            assertEquals(25, FeedLogic.points(species, FeedType.MIX));
        }
    }

    @Test
    void satiatedAnimalsDoNotEatFromTroughs() {
        assertTrue(FeedLogic.wantsFood(10));
        assertTrue(FeedLogic.wantsFood(79));
        assertFalse(FeedLogic.wantsFood(FeedLogic.SATIATED_LIMIT));
    }
}
