package fr.terresvivantes.market.logic;

import fr.terresvivantes.economy.logic.ReputationLogic;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MarketLogicTest {

    private static final List<String> SEASONAL = List.of("terresvivantes:tomato", "terresvivantes:corn");
    private static final List<String> OTHERS = List.of("terresvivantes:tomato_soup", "terresvivantes:aged_cheese");

    /** Prix fictif : 6 cuivre par unité pour tout item listé, 0 sinon. */
    private static int price(String id) {
        return id.startsWith("terresvivantes:") ? 6 : 0;
    }

    @Test
    void sameSeedGivesSameContract() {
        long seed = ContractGenerator.seedFor(42L, 7L, 0);
        ContractGenerator.Generated first = ContractGenerator.generate(seed, ReputationLogic.Tier.REGULAR, 42L,
                SEASONAL, OTHERS, MarketLogicTest::price);
        ContractGenerator.Generated second = ContractGenerator.generate(seed, ReputationLogic.Tier.REGULAR, 42L,
                SEASONAL, OTHERS, MarketLogicTest::price);
        assertNotNull(first);
        assertEquals(first, second, "même graine, mêmes entrées : même contrat");
    }

    @Test
    void differentDaysOrDimensionsGiveDifferentSeeds() {
        assertNotEquals(ContractGenerator.seedFor(1L, 0L, 0), ContractGenerator.seedFor(2L, 0L, 0));
        assertNotEquals(ContractGenerator.seedFor(1L, 0L, 0), ContractGenerator.seedFor(1L, 5L, 0));
        assertNotEquals(ContractGenerator.seedFor(1L, 0L, 0), ContractGenerator.seedFor(1L, 0L, 1));
    }

    @Test
    void generatedContractStaysInsideTheDocumentedBounds() {
        for (ReputationLogic.Tier tier : ReputationLogic.Tier.values()) {
            int[] range = ContractGenerator.quantityRange(tier);
            for (long day = 0; day < 200; day++) {
                long today = 100 + day;
                ContractGenerator.Generated c = ContractGenerator.generate(
                        ContractGenerator.seedFor(today, 3L, (int) (day % 3)), tier, today,
                        SEASONAL, OTHERS, MarketLogicTest::price);
                assertNotNull(c);
                assertTrue(c.quantity() >= range[0] && c.quantity() <= range[1], "quantité " + c.quantity());
                assertTrue(c.quantity() >= 8 && c.quantity() <= 64, "quantité hors 8-64 : " + c.quantity());
                assertTrue(c.minQuality() >= 1 && c.minQuality() <= 3, "qualité " + c.minQuality());
                assertTrue(c.deadlineDay() - today >= 2 && c.deadlineDay() - today <= 5, "délai " + c.deadlineDay());
                assertTrue(c.reputation() >= 5 && c.reputation() <= 25, "réputation " + c.reputation());
                assertTrue(c.reward() > 0, "récompense positive");
            }
        }
    }

    @Test
    void supplierTierRequiresQualityAtLeastTwo() {
        for (long day = 0; day < 300; day++) {
            ContractGenerator.Generated c = ContractGenerator.generate(ContractGenerator.seedFor(day, 1L, 0),
                    ReputationLogic.Tier.SUPPLIER, day, SEASONAL, OTHERS, MarketLogicTest::price);
            assertTrue(c.minQuality() >= 2, "palier Fournisseur : qualité " + c.minQuality());
        }
    }

    @Test
    void rewardIsQuantityTimesPriceTimesOneAndAHalf() {
        // Prix unitaire fixe 10 : récompense exacte = quantité x 15.
        ContractGenerator.Generated exact = ContractGenerator.generate(12345L, ReputationLogic.Tier.UNKNOWN, 0L,
                List.of("terresvivantes:tomato"), List.of(), id -> 10);
        assertEquals(exact.quantity() * 15, exact.reward(), "récompense = quantité x prix x 1.5");
        // Prix 7 : 1.5 x 7 = 10.5 par unité; arrondi à l'entier le plus proche, demi vers le haut.
        ContractGenerator.Generated odd = ContractGenerator.generate(12345L, ReputationLogic.Tier.UNKNOWN, 0L,
                List.of("terresvivantes:tomato"), List.of(), id -> 7);
        assertEquals((odd.quantity() * 21 + 1) / 2, odd.reward(), "arrondi demi vers le haut");
    }

    @Test
    void generationSkipsNonSellableCandidatesAndReturnsNullWhenNone() {
        ContractGenerator.Generated none = ContractGenerator.generate(7L, ReputationLogic.Tier.UNKNOWN, 0L,
                List.of("minecraft:stone"), List.of("terresvivantes:shovel"), id -> 0);
        assertNull(none, "aucun candidat vendable");
        ContractGenerator.Generated picked = ContractGenerator.generate(7L, ReputationLogic.Tier.UNKNOWN, 0L,
                List.of("terresvivantes:shovel", "terresvivantes:tomato"), List.of(),
                id -> id.equals("terresvivantes:tomato") ? 4 : 0);
        assertEquals("terresvivantes:tomato", picked.product());
    }

    @Test
    void seasonalProductsDominateWhenBothPoolsExist() {
        int seasonal = 0;
        for (long seed = 0; seed < 1000; seed++) {
            ContractGenerator.Generated c = ContractGenerator.generate(seed, ReputationLogic.Tier.UNKNOWN, 0L,
                    List.of("terresvivantes:corn"), List.of("terresvivantes:aged_cheese"), id -> 5);
            if (c.product().equals("terresvivantes:corn")) {
                seasonal++;
            }
        }
        assertTrue(seasonal > 600 && seasonal < 800, "environ 70 % de produits de saison, obtenu " + seasonal);
    }

    @Test
    void contractLifecycleIsImmutableAndCounted() {
        Contract open = new Contract(1L, "terresvivantes:tomato", 16, 2, 5L, 100, 10,
                Contract.State.OPEN, "", 0);
        Contract accepted = open.accepted("player-1");
        assertEquals(Contract.State.ACCEPTED, accepted.state());
        assertEquals("player-1", accepted.acceptor());
        assertEquals(Contract.State.OPEN, open.state(), "l'original reste inchangé");
        Contract partial = accepted.withDelivered(10);
        assertEquals(6, partial.remaining());
        assertEquals(0, partial.withDelivered(20).remaining(), "reste borné à zéro");
        assertThrows(IllegalArgumentException.class,
                () -> new Contract(2L, "x", 0, 1, 0L, 0, 0, Contract.State.OPEN, "", 0));
    }

    @Test
    void expiryAndQualityRules() {
        assertFalse(ContractLogic.isExpired(5L, 5L), "le jour d'échéance reste valable");
        assertTrue(ContractLogic.isExpired(6L, 5L));
        assertTrue(ContractLogic.meetsQuality(2, 2));
        assertFalse(ContractLogic.meetsQuality(1, 2));
        assertEquals(3, ContractLogic.openSlotsToFill(0));
        assertEquals(0, ContractLogic.openSlotsToFill(5));
        assertEquals(10, ContractLogic.EXPIRY_PENALTY);
    }

    @Test
    void stallPaysNinetyPercentAtLeastOneCopper() {
        assertEquals(0L, StallLogic.payment(0));
        assertEquals(1L, StallLogic.payment(1), "au moins 1 cuivre");
        assertEquals(5L, StallLogic.payment(6), "6 x 0.9 = 5.4 arrondi à 5");
        assertEquals(90L, StallLogic.payment(100));
    }

    @Test
    void stallPicksTheBestPaidSlotAndSkipsEmpty() {
        assertEquals(2, StallLogic.pickIndex(new int[] {0, 4, 9, 9, 0}), "à égalité, la première case");
        assertEquals(-1, StallLogic.pickIndex(new int[] {0, 0, 0}));
    }

    @Test
    void stallSalesNeedAVillagerOrDaylight() {
        assertTrue(StallLogic.shouldSell(true, false));
        assertTrue(StallLogic.shouldSell(false, true));
        assertFalse(StallLogic.shouldSell(false, false));
        assertTrue(StallLogic.isDay(0L));
        assertTrue(StallLogic.isDay(11999L));
        assertFalse(StallLogic.isDay(12000L));
        assertEquals(2400, StallLogic.SALE_INTERVAL_TICKS);
    }

    @Test
    void tillShowsTheLargestCoinThatFits() {
        assertArrayEquals(new int[] {-1, 0}, TillLogic.displayFor(0L));
        assertArrayEquals(new int[] {0, 8}, TillLogic.displayFor(8L));
        assertArrayEquals(new int[] {1, 1}, TillLogic.displayFor(9L), "9 cuivre fusionnent en 1 argent");
        assertArrayEquals(new int[] {1, 8}, TillLogic.displayFor(80L));
        assertArrayEquals(new int[] {2, 1}, TillLogic.displayFor(81L), "81 cuivre fusionnent en 1 or");
        assertArrayEquals(new int[] {2, TillLogic.MAX_STACK}, TillLogic.displayFor(1_000_000L), "pile plafonnée");
        assertEquals(81L, TillLogic.valueOf(2, 1));
        assertEquals(0L, TillLogic.valueOf(-1, 0));
    }

    @Test
    void ledgerKeepsTheMostExpensiveLinesInOrder() {
        long[] prices = {3, 0, 9, 9, 1, 12, 0};
        assertEquals(List.of(5, 2, 3, 0, 4), LedgerLogic.topIndices(prices, 10),
                "ordre décroissant, égalités par index, prix nuls ignorés");
        assertEquals(List.of(5, 2), LedgerLogic.topIndices(prices, 2));
        assertEquals(10, LedgerLogic.LINES);
    }
}
