package fr.terresvivantes.guild;

import fr.terresvivantes.TerresVivantes;
import fr.terresvivantes.economy.EconomyModule;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.neoforged.neoforge.common.BasicItemListing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Table des échanges du marchand de la guilde, par niveau (1 à 5). Les quantités de pièces sont fixes (le ratio
 * entre pièces est décidé par le module économie). Chaque item n'apparaît que dans une offre, pour que le niveau
 * d'une offre se déduise de son item ({@link #levelOf}).
 */
public final class GuildTrades {

    private static final int BUY_MAX_USES = 12;
    private static final int SELL_MAX_USES = 8;
    private static final int XP = 2;
    private static final float PRICE_MULT = 0.05f;

    /** Pièce utilisée dans une offre. */
    enum Coin {
        COPPER,
        SILVER,
        GOLD
    }

    /**
     * Une offre. {@code sells} : vrai si le marchand vend {@code item} contre pièces; faux s'il achète {@code item}
     * et paie en pièces. {@code sealed} : offre de niveau 5 qui exige en plus le sceau de la guilde.
     */
    private record Trade(int level, boolean sells, String item, int count, Coin coin, int coinCount, boolean sealed) {
    }

    private static final String SEAL = TerresVivantes.MODID + ":guild_seal";

    private static final List<Trade> TRADES = List.of(
            // Niveau 1 (Inconnu) : achats de légumes, graines courantes, fleurs.
            buy(1, "lettuce", 8, Coin.COPPER, 24),
            buy(1, "cabbage", 4, Coin.COPPER, 15),
            sell(1, "lettuce_seeds", 1, Coin.COPPER, 3),
            sell(1, "onion_seeds", 1, Coin.COPPER, 3),
            sell(1, "cabbage_seeds", 1, Coin.COPPER, 4),
            sell(1, "barley_seeds", 1, Coin.COPPER, 4),
            sell(1, "clover", 1, Coin.COPPER, 4),
            sell(1, "chamomile", 1, Coin.COPPER, 4),
            // Niveau 2 (Habitué) : tomates et maïs, graines et saplings de base.
            buy(2, "tomato", 8, Coin.COPPER, 36),
            buy(2, "corn", 8, Coin.COPPER, 36),
            sell(2, "tomato_seeds", 1, Coin.COPPER, 6),
            sell(2, "corn_seeds", 1, Coin.COPPER, 6),
            sell(2, "cucumber_seeds", 1, Coin.COPPER, 6),
            sell(2, "soybean_seeds", 1, Coin.COPPER, 6),
            sell(2, "cherry_sapling", 1, Coin.SILVER, 1),
            sell(2, "peach_sapling", 1, Coin.SILVER, 1),
            sell(2, "lavender", 1, Coin.COPPER, 8),
            sell(2, "borage", 1, Coin.COPPER, 8),
            // Niveau 3 (Fournisseur) : fromages frais, miel floral, graines rares, saplings.
            buy(3, "cheese_curd", 4, Coin.SILVER, 1),
            buy(3, "wildflower_honey", 4, Coin.SILVER, 2),
            sell(3, "bell_pepper_seeds", 1, Coin.SILVER, 1),
            sell(3, "garlic_seeds", 1, Coin.SILVER, 1),
            sell(3, "eggplant_seeds", 1, Coin.SILVER, 1),
            sell(3, "strawberry_seeds", 1, Coin.SILVER, 1),
            sell(3, "orange_sapling", 1, Coin.SILVER, 2),
            sell(3, "apple_sapling", 1, Coin.SILVER, 2),
            // Niveau 4 (Producteur réputé) : fromage affiné, huile, outils, saplings tardifs.
            buy(4, "aged_cheese", 2, Coin.SILVER, 3),
            buy(4, "olive_oil", 4, Coin.SILVER, 2),
            sell(4, "olive_sapling", 1, Coin.SILVER, 3),
            sell(4, "walnut_sapling", 1, Coin.SILVER, 3),
            sell(4, "broadfork", 1, Coin.SILVER, 4),
            sell(4, "seeder", 1, Coin.SILVER, 4),
            // Niveau 5 (Maître fermier) : miels rares, machines; les ventes exigent le sceau de la guilde.
            buy(5, "orchard_honey", 4, Coin.GOLD, 1),
            buy(5, "lavender_honey", 4, Coin.GOLD, 1),
            sealed(5, "fruit_press", 1, Coin.GOLD, 1),
            sealed(5, "cheese_vat", 1, Coin.GOLD, 1));

    private static final Map<String, Integer> LEVEL_BY_ITEM = indexLevels();

    private GuildTrades() {
    }

    /** Listings (un par offre) du niveau donné, pour {@code VillagerTradesEvent}. */
    public static List<VillagerTrades.ItemListing> listingsAt(int level) {
        List<VillagerTrades.ItemListing> listings = new ArrayList<>();
        for (Trade trade : TRADES) {
            if (trade.level() == level) {
                listings.add(toListing(trade));
            }
        }
        return listings;
    }

    /** Niveau d'échange d'une offre de la guilde, ou 0 si l'offre n'est pas de la guilde. */
    public static int levelOf(MerchantOffer offer) {
        for (ItemStack stack : new ItemStack[] {offer.getResult(), offer.getCostA(), offer.getCostB()}) {
            if (stack.isEmpty()) {
                continue;
            }
            Integer level = LEVEL_BY_ITEM.get(itemId(stack.getItem()));
            if (level != null) {
                return level;
            }
        }
        return 0;
    }

    /** Identifiant complet d'un item ({@code terresvivantes:tomato}). */
    public static String itemId(Item item) {
        return BuiltInRegistries.ITEM.getKey(item).toString();
    }

    private static Trade buy(int level, String item, int count, Coin coin, int coinCount) {
        return new Trade(level, false, item(item), count, coin, coinCount, false);
    }

    private static Trade sell(int level, String item, int count, Coin coin, int coinCount) {
        return new Trade(level, true, item(item), count, coin, coinCount, false);
    }

    private static Trade sealed(int level, String item, int count, Coin coin, int coinCount) {
        return new Trade(level, true, item(item), count, coin, coinCount, true);
    }

    private static String item(String path) {
        return TerresVivantes.MODID + ":" + path;
    }

    private static VillagerTrades.ItemListing toListing(Trade trade) {
        ItemStack coins = new ItemStack(coin(trade.coin()), trade.coinCount());
        ItemStack goods = new ItemStack(stackItem(trade.item()), trade.count());
        if (!trade.sells()) {
            // Achat : le joueur donne le produit, le marchand paie en pièces.
            return new BasicItemListing(goods, coins, BUY_MAX_USES, XP, PRICE_MULT);
        }
        if (trade.sealed()) {
            ItemStack seal = new ItemStack(stackItem(SEAL), 1);
            return new BasicItemListing(coins, seal, goods, SELL_MAX_USES, XP, PRICE_MULT);
        }
        return new BasicItemListing(coins, goods, SELL_MAX_USES, XP, PRICE_MULT);
    }

    private static Item coin(Coin coin) {
        return switch (coin) {
            case COPPER -> EconomyModule.COPPER_COIN.get();
            case SILVER -> EconomyModule.SILVER_COIN.get();
            case GOLD -> EconomyModule.GOLD_COIN.get();
        };
    }

    private static Item stackItem(String id) {
        return BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(id)).orElseThrow(
                () -> new IllegalStateException("item de guilde absent : " + id));
    }

    private static Map<String, Integer> indexLevels() {
        Map<String, Integer> index = new HashMap<>();
        for (Trade trade : TRADES) {
            index.put(trade.item(), trade.level());
        }
        return Map.copyOf(index);
    }
}
