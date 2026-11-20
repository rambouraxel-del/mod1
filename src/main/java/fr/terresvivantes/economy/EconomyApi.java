package fr.terresvivantes.economy;

import fr.terresvivantes.TerresVivantes;
import fr.terresvivantes.core.quality.QualityModule;
import fr.terresvivantes.crop.logic.CropDefinition;
import fr.terresvivantes.crop.logic.CropDefinitions;
import fr.terresvivantes.economy.logic.BasePrices;
import fr.terresvivantes.economy.logic.CoinLogic;
import fr.terresvivantes.economy.logic.MarketLogic;
import fr.terresvivantes.economy.logic.PriceModel;
import fr.terresvivantes.economy.logic.ReputationLogic;
import fr.terresvivantes.orchard.logic.FruitTreeDefinition;
import fr.terresvivantes.orchard.logic.FruitTreeDefinitions;
import fr.terresvivantes.season.SeasonApi;
import fr.terresvivantes.season.logic.Season;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;

/**
 * Façade de l'économie (v0.7). Prix en cuivre par unité, réputation par joueur, pièces.
 * Les autres modules n'appellent que cette classe.
 */
public final class EconomyApi {

    private static final String PRICES_PATH = "/data/terresvivantes/economy/base_prices.json";

    private static volatile Map<String, Integer> basePrices;

    private EconomyApi() {
    }

    /**
     * Prix actuel d'une unité de l'item, en cuivre : base x qualité x saison x saturation.
     * 0 si l'item n'est pas vendable (outils, graines, objets hors table).
     */
    public static int priceOf(ServerLevel level, ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        ResourceLocation rl = BuiltInRegistries.ITEM.getKey(stack.getItem());
        String id = priceKey(rl);
        int base = BasePrices.basePrice(basePrices(), id);
        if (base <= 0) {
            return 0;
        }
        float seasonFactor = PriceModel.seasonFactor(optimalSeasons(rl), SeasonApi.getSeason(level));
        double points = MarketData.get(level).pointsAt(id, MarketLogic.dayOf(level.getDayTime()));
        return PriceModel.price(base, QualityModule.qualityOf(stack), seasonFactor, points);
    }

    /** Part du prix de base que laisse le marché, en pourcents (100 = marché neuf, 40 = plancher). */
    public static int saturationPercent(ServerLevel level, ItemStack stack) {
        if (stack.isEmpty()) {
            return 100;
        }
        ResourceLocation rl = BuiltInRegistries.ITEM.getKey(stack.getItem());
        double points = MarketData.get(level).pointsAt(priceKey(rl), MarketLogic.dayOf(level.getDayTime()));
        return (int) Math.round(100.0 * PriceModel.saturationFactor(points));
    }

    /** Enregistre une vente (un point de saturation) sur le marché de la dimension. Sans effet si non vendable. */
    public static void recordSale(ServerLevel level, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        ResourceLocation rl = BuiltInRegistries.ITEM.getKey(stack.getItem());
        String id = priceKey(rl);
        if (BasePrices.basePrice(basePrices(), id) <= 0) {
            return;
        }
        MarketData.get(level).sell(id, MarketLogic.dayOf(level.getDayTime()));
    }

    /** Réputation du joueur (0 par défaut, persistante, copiée à la mort). */
    public static int reputation(ServerPlayer player) {
        return player.getData(EconomyModule.REPUTATION.get());
    }

    /** Ajuste la réputation du joueur (jamais en dessous de 0). */
    public static void addReputation(ServerPlayer player, int delta) {
        player.setData(EconomyModule.REPUTATION.get(), ReputationLogic.adjusted(reputation(player), delta));
    }

    /** Palier du joueur, libellé français de référence (voir {@link #reputationTierOf}). */
    public static String reputationTier(ServerPlayer player) {
        return reputationTierOf(player).label();
    }

    /** Palier du joueur sous forme d'enum (pour la traduction). */
    public static ReputationLogic.Tier reputationTierOf(ServerPlayer player) {
        return ReputationLogic.tierOf(reputation(player));
    }

    /** Valeur totale en cuivre d'un lot de pièces (1 argent = 20 cuivre, 1 or = 20 argent). */
    public static long coinsToCopper(long copper, long silver, long gold) {
        return CoinLogic.coinsToCopper(copper, silver, gold);
    }

    /** Décompose une somme en cuivre en piles d'or, d'argent et de cuivre (piles de 64 max, vides omises). */
    public static ItemStack[] copperToCoins(int copper) {
        long[] split = CoinLogic.copperToCoins(copper);
        Item[] coins = {EconomyModule.GOLD_COIN.get(), EconomyModule.SILVER_COIN.get(),
                EconomyModule.COPPER_COIN.get()};
        java.util.List<ItemStack> stacks = new java.util.ArrayList<>();
        for (int i = 0; i < coins.length; i++) {
            long remaining = split[i];
            while (remaining > 0) {
                int n = (int) Math.min(64, remaining);
                stacks.add(new ItemStack(coins[i], n));
                remaining -= n;
            }
        }
        return stacks.toArray(new ItemStack[0]);
    }

    /** Clé du fichier de prix : sans espace de noms pour le mod, complète pour le vanilla. */
    static String priceKey(ResourceLocation rl) {
        return TerresVivantes.MODID.equals(rl.getNamespace()) ? rl.getPath() : rl.toString();
    }

    /** Saisons optimales d'un item : mûrissement d'un fruit, saison optimale d'une culture, sinon aucune. */
    static Set<Season> optimalSeasons(ResourceLocation rl) {
        String full = rl.toString();
        for (FruitTreeDefinition fruit : FruitTreeDefinitions.all()) {
            if (fruit.fruit().equals(full)) {
                return fruit.ripeSeasons();
            }
        }
        if (TerresVivantes.MODID.equals(rl.getNamespace())) {
            return CropDefinitions.byId(rl.getPath()).map(CropDefinition::optimal).orElse(Set.of());
        }
        return Set.of();
    }

    private static Map<String, Integer> basePrices() {
        Map<String, Integer> table = basePrices;
        if (table == null) {
            table = loadBasePrices();
            basePrices = table;
        }
        return table;
    }

    private static Map<String, Integer> loadBasePrices() {
        try (InputStream in = EconomyApi.class.getResourceAsStream(PRICES_PATH)) {
            if (in == null) {
                throw new IllegalStateException("table de prix absente : " + PRICES_PATH);
            }
            return BasePrices.parse(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new IllegalStateException("lecture impossible de " + PRICES_PATH, e);
        }
    }
}
