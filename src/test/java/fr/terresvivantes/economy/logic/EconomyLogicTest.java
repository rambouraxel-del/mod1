package fr.terresvivantes.economy.logic;

import fr.terresvivantes.season.logic.Season;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EconomyLogicTest {

    private static final float EPS = 1e-6f;

    @Test
    void coinsConvertBothWaysWithMinimalPieces() {
        assertEquals(445L, CoinLogic.coinsToCopper(5, 2, 1));
        assertArrayEquals(new long[] {1, 2, 5}, CoinLogic.copperToCoins(445));
        assertArrayEquals(new long[] {0, 0, 0}, CoinLogic.copperToCoins(-3));
        assertArrayEquals(new long[] {0, 1, 0}, CoinLogic.copperToCoins(20));
    }

    @Test
    void qualityMultiplies1To1_5() {
        assertEquals(1.0f, PriceModel.qualityFactor(0), EPS, "qualité absente");
        assertEquals(1.0f, PriceModel.qualityFactor(1), EPS);
        assertEquals(1.25f, PriceModel.qualityFactor(2), EPS);
        assertEquals(1.5f, PriceModel.qualityFactor(3), EPS);
        assertEquals(1.5f, PriceModel.qualityFactor(9), EPS, "bornée à 3");
    }

    @Test
    void seasonFactorRewardsOutOfSeasonAndDiscountsFullSeason() {
        Set<Season> summer = Set.of(Season.SUMMER);
        assertEquals(0.85f, PriceModel.seasonFactor(summer, Season.SUMMER), EPS, "pleine saison");
        assertEquals(1.3f, PriceModel.seasonFactor(summer, Season.WINTER), EPS, "hors saison");
        assertEquals(1.0f, PriceModel.seasonFactor(Set.of(), Season.WINTER), EPS, "sans saison");
    }

    @Test
    void tomatoOutOfSeasonIsMoreExpensiveThanInSeason() {
        int inSeason = PriceModel.price(6, 1, PriceModel.seasonFactor(Set.of(Season.SUMMER), Season.SUMMER), 0);
        int outOfSeason = PriceModel.price(6, 1, PriceModel.seasonFactor(Set.of(Season.SUMMER), Season.WINTER), 0);
        assertTrue(outOfSeason > inSeason, "hors saison " + outOfSeason + " doit dépasser en saison " + inSeason);
    }

    @Test
    void saturationLowersPriceDownToFloor() {
        assertEquals(1.0, PriceModel.saturationFactor(0.0), 1e-9);
        assertEquals(0.8, PriceModel.saturationFactor(10.0), 1e-9);
        assertEquals(0.4, PriceModel.saturationFactor(50.0), 1e-9);
        assertEquals(0.4, PriceModel.saturationFactor(500.0), 1e-9);
    }

    @Test
    void priceIsZeroForUnsellableAndAtLeastOneOtherwise() {
        assertEquals(0, PriceModel.price(0, 1, 1f, 0));
        assertEquals(1, PriceModel.price(1, 1, 0.4f, 100));
    }

    @Test
    void fiftySalesThenFiveDaysRecoverPrice() {
        MarketLogic market = MarketLogic.empty();
        for (int i = 0; i < 50; i++) {
            market = market.sold(0L);
        }
        int afterSales = PriceModel.price(100, 1, 1f, market.pointsAt(0L));
        assertEquals(40, afterSales, "50 ventes : plancher à 40 % de la base");
        int fiveDaysLater = PriceModel.price(100, 1, 1f, market.pointsAt(5L));
        assertTrue(fiveDaysLater > afterSales, "après 5 jours le prix doit remonter (" + fiveDaysLater + ")");
        assertTrue(market.pointsAt(5L) < market.pointsAt(0L));
    }

    @Test
    void marketDecaysTenPercentPerDay() {
        MarketLogic market = new MarketLogic(10.0, 0L);
        assertEquals(9.0, market.pointsAt(1L), 1e-9);
        assertEquals(10.0 * 0.9 * 0.9, market.pointsAt(2L), 1e-9);
        assertEquals(10.0, market.pointsAt(0L), 1e-9);
    }

    @Test
    void dayIndexFromDayTime() {
        assertEquals(0L, MarketLogic.dayOf(23999L));
        assertEquals(1L, MarketLogic.dayOf(24000L));
        assertEquals(0L, MarketLogic.dayOf(-5L));
        assertThrows(IllegalArgumentException.class, () -> new MarketLogic(-1.0, 0L));
    }

    @Test
    void reputationTiersAtThresholds() {
        assertEquals(ReputationLogic.Tier.UNKNOWN, ReputationLogic.tierOf(0));
        assertEquals(ReputationLogic.Tier.UNKNOWN, ReputationLogic.tierOf(49));
        assertEquals(ReputationLogic.Tier.REGULAR, ReputationLogic.tierOf(50));
        assertEquals(ReputationLogic.Tier.REGULAR, ReputationLogic.tierOf(199));
        assertEquals(ReputationLogic.Tier.SUPPLIER, ReputationLogic.tierOf(200));
        assertEquals(ReputationLogic.Tier.REPUTED_PRODUCER, ReputationLogic.tierOf(500));
        assertEquals(ReputationLogic.Tier.MASTER_FARMER, ReputationLogic.tierOf(1200));
        assertEquals(ReputationLogic.Tier.UNKNOWN, ReputationLogic.tierOf(-5));
    }

    @Test
    void reputationNeverNegative() {
        assertEquals(0, ReputationLogic.adjusted(10, -50));
        assertEquals(60, ReputationLogic.adjusted(10, 50));
    }

    @Test
    void basePriceJsonIsParsed() {
        Map<String, Integer> table = BasePrices.parse("{\"tomato\": 6, \"minecraft:wheat\": 4}");
        assertEquals(6, BasePrices.basePrice(table, "tomato"));
        assertEquals(4, BasePrices.basePrice(table, "minecraft:wheat"));
        assertEquals(0, BasePrices.basePrice(table, "unknown"));
    }

    @Test
    void shippedPriceTableHasProcessedAboveRawAndCoversCrops() throws IOException {
        String json = Files.readString(
                Paths.get("src/main/resources/data/terresvivantes/economy/base_prices.json"),
                StandardCharsets.UTF_8);
        Map<String, Integer> t = BasePrices.parse(json);
        String[] crops = {"tomato", "corn", "soybean", "lettuce", "onion", "garlic", "bell_pepper", "eggplant",
                "strawberry", "barley", "cabbage", "cucumber", "cherry", "olive", "orange", "peach", "walnut"};
        for (String crop : crops) {
            assertTrue(BasePrices.basePrice(t, crop) > 0, "prix manquant pour " + crop);
        }
        assertTrue(t.get("tomato_soup") > t.get("tomato"));
        assertTrue(t.get("barley_bread") > t.get("barley_flour"));
        assertTrue(t.get("barley_flour") > t.get("barley"));
        assertTrue(t.get("olive_oil") > t.get("olive"));
        assertTrue(t.get("strawberry_jam") > t.get("strawberry"));
        assertTrue(t.get("aged_cheese") > t.get("cream"));
        assertFalse(t.containsKey("copper_coin"), "les pièces ne sont pas vendables");
    }
}
