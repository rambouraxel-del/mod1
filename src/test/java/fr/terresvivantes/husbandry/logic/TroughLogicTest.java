package fr.terresvivantes.husbandry.logic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TroughLogicTest {

    @Test
    void emptyTroughAcceptsAnyFeedUpToCapacity() {
        // Foin : 9 unités par botte, 64 / 9 = 7 bottes.
        assertEquals(7, TroughLogic.itemsAccepted(null, 0, FeedType.HAY, 20));
        // Grain : 1 unité par objet.
        assertEquals(64, TroughLogic.itemsAccepted(null, 0, FeedType.GRAIN, 64));
    }

    @Test
    void refusesAnotherTypeWhileNotEmpty() {
        assertEquals(0, TroughLogic.itemsAccepted(FeedType.HAY, 9, FeedType.GRAIN, 5));
        assertEquals(0, TroughLogic.itemsAccepted(FeedType.HAY, 9, null, 5));
    }

    @Test
    void sameTypeFillsUpToRemainingRoom() {
        // 50 unités présentes : 14 libres, soit 4 ensilages de 3 unités demandés, 4 acceptés.
        assertEquals(4, TroughLogic.itemsAccepted(FeedType.SILAGE, 50, FeedType.SILAGE, 10));
        // Auge pleine : rien n'entre.
        assertEquals(0, TroughLogic.itemsAccepted(FeedType.MIX, 64, FeedType.MIX, 1));
    }

    @Test
    void comparatorSignalIsZeroWhenEmptyAndFifteenWhenFull() {
        assertEquals(0, TroughLogic.comparatorSignal(0));
        assertEquals(1, TroughLogic.comparatorSignal(1));
        assertEquals(15, TroughLogic.comparatorSignal(64));
    }

    @Test
    void comparatorSignalNeverDecreasesWithStock() {
        int previous = 0;
        for (int stock = 0; stock <= TroughLogic.CAPACITY; stock++) {
            int signal = TroughLogic.comparatorSignal(stock);
            assertTrue(signal >= previous, "signal décroissant à " + stock);
            previous = signal;
        }
    }
}
