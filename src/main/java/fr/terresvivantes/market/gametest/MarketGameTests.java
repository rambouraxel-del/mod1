package fr.terresvivantes.market.gametest;

import fr.terresvivantes.TerresVivantes;
import fr.terresvivantes.core.quality.QualityModule;
import fr.terresvivantes.economy.EconomyApi;
import fr.terresvivantes.economy.EconomyModule;
import fr.terresvivantes.economy.logic.MarketLogic;
import fr.terresvivantes.market.ContractBoard;
import fr.terresvivantes.market.ContractItem;
import fr.terresvivantes.market.ContractBoardData;
import fr.terresvivantes.market.MarketModule;
import fr.terresvivantes.market.MarketStallBlockEntity;
import fr.terresvivantes.market.logic.Contract;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Tests de jeu du module market : livraison de contrats, étal, renouvellement du tableau, ratio des pièces. */
@GameTestHolder(TerresVivantes.MODID)
@PrefixGameTestTemplate(false)
public final class MarketGameTests {

    private static final String TEMPLATE = "orchard_arena";
    private static final String BATCH = "market";
    private static final String TOMATO = "terresvivantes:tomato";

    private MarketGameTests() {
    }

    private static Item item(String id) {
        return ContractItem.itemOf(id);
    }

    /** Donne {@code count} tomates de qualité {@code quality} au joueur. */
    private static void giveTomatoes(Player player, int count, int quality) {
        ItemStack stack = new ItemStack(item(TOMATO), count);
        QualityModule.setQuality(stack, quality);
        player.getInventory().add(stack);
    }

    /** Réputation lue sur l'attachement (le faux joueur n'est pas un joueur serveur). */
    private static int reputationOf(Player player) {
        return player.getData(EconomyModule.REPUTATION.get());
    }

    private static int countTomatoes(Player player) {
        int total = 0;
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.is(item(TOMATO))) {
                total += stack.getCount();
            }
        }
        return total;
    }

    /** Valeur en cuivre des pièces du joueur. */
    private static long coinValue(Player player) {
        long copper = 0;
        long silver = 0;
        long gold = 0;
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.is(EconomyModule.COPPER_COIN.get())) {
                copper += stack.getCount();
            } else if (stack.is(EconomyModule.SILVER_COIN.get())) {
                silver += stack.getCount();
            } else if (stack.is(EconomyModule.GOLD_COIN.get())) {
                gold += stack.getCount();
            }
        }
        return EconomyApi.coinsToCopper(copper, silver, gold);
    }

    /** Seize tomates de qualité 2 contre un contrat de qualité 2 : pièces, réputation, contrat retiré. */
    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void deliveryPaysCoinsAndReputation(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ContractBoardData data = ContractBoardData.get(level);
        long today = MarketLogic.dayOf(level.getDayTime());
        Contract contract = new Contract(data.nextId(), TOMATO, 16, 2, today + 5, 100, 10,
                Contract.State.ACCEPTED, player.getUUID().toString(), 0);
        data.add(contract);
        giveTomatoes(player, 16, 2);

        int moved = ContractBoard.deliver(level, player, contract);
        helper.assertTrue(moved == 16, "16 tomates attendues livrées, obtenu " + moved);
        helper.assertTrue(reputationOf(player) == 10,
                "réputation attendue 10, obtenu " + reputationOf(player));
        helper.assertTrue(coinValue(player) == 100, "100 cuivre de pièces attendus, obtenu " + coinValue(player));
        helper.assertTrue(data.find(contract.id()).isEmpty(), "contrat terminé retiré du tableau");
        helper.assertTrue(countTomatoes(player) == 0, "les tomates livrées sont retirées de l'inventaire");
        helper.succeed();
    }

    /** Tomates de qualité 1 contre un contrat de qualité 2 : refus, aucune récompense, items conservés. */
    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void lowQualityIsRefused(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ContractBoardData data = ContractBoardData.get(level);
        long today = MarketLogic.dayOf(level.getDayTime());
        Contract contract = new Contract(data.nextId(), TOMATO, 16, 2, today + 5, 100, 10,
                Contract.State.ACCEPTED, player.getUUID().toString(), 0);
        data.add(contract);
        giveTomatoes(player, 16, 1);

        int moved = ContractBoard.deliver(level, player, contract);
        helper.assertTrue(moved == 0, "qualité 1 refusée pour un contrat de qualité 2, livré " + moved);
        helper.assertTrue(reputationOf(player) == 0, "aucune réputation sans livraison");
        helper.assertTrue(coinValue(player) == 0, "aucune pièce sans livraison");
        helper.assertTrue(countTomatoes(player) == 16, "les tomates refusées restent dans l'inventaire");
        helper.assertTrue(data.find(contract.id()).isPresent(), "le contrat reste ouvert");
        helper.succeed();
    }

    /** Étal avec un villageois à portée : un cycle forcé vend une tomate et met des pièces en caisse. */
    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void stallSellsToNearbyVillager(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos stallPos = new BlockPos(5, 2, 5);
        helper.setBlock(stallPos, MarketModule.MARKET_STALL.get().defaultBlockState());
        MarketStallBlockEntity stall = helper.getBlockEntity(stallPos);
        ItemStack tomatoes = new ItemStack(item(TOMATO), 4);
        QualityModule.setQuality(tomatoes, 2);
        stall.setItem(0, tomatoes);
        helper.spawn(EntityType.VILLAGER, new BlockPos(7, 2, 5));

        stall.runSaleCycle(level);
        helper.assertTrue(stall.getItem(0).getCount() == 3,
                "une tomate vendue, 3 restantes; obtenu " + stall.getItem(0).getCount());
        helper.assertTrue(stall.tillValue() > 0, "des pièces doivent être en caisse, solde " + stall.tillValue());
        helper.assertTrue(stall.getItem(MarketStallBlockEntity.TILL_SLOT).is(EconomyModule.COPPER_COIN.get())
                        || stall.getItem(MarketStallBlockEntity.TILL_SLOT).is(EconomyModule.SILVER_COIN.get()),
                "la caisse montre une pièce");
        helper.succeed();
    }

    /** Neuf pièces d'un rang donnent une pièce du rang supérieur, et la recette inverse existe. */
    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void nineCoinsMakeOneInTheNextTier(GameTestHelper helper) {
        RecipeManager recipes = helper.getLevel().getServer().getRecipeManager();
        Item copper = EconomyModule.COPPER_COIN.get();
        Item silver = EconomyModule.SILVER_COIN.get();
        Item gold = EconomyModule.GOLD_COIN.get();
        assertCraft(helper, recipes, "copper_from_silver", silver, 1, copper, 9);
        assertCraft(helper, recipes, "silver_from_copper", copper, 9, silver, 1);
        assertCraft(helper, recipes, "silver_from_gold", gold, 1, silver, 9);
        assertCraft(helper, recipes, "gold_from_silver", silver, 9, gold, 1);
        helper.succeed();
    }

    private static void assertCraft(GameTestHelper helper, RecipeManager recipes, String id, Item input,
                                    int inputCount, Item output, int outputCount) {
        Optional<RecipeHolder<?>> holder = recipes.byKey(
                ResourceLocation.fromNamespaceAndPath(TerresVivantes.MODID, id));
        helper.assertTrue(holder.isPresent(), "recette absente : " + id);
        List<ItemStack> stacks = new ArrayList<>();
        for (int i = 0; i < inputCount; i++) {
            stacks.add(new ItemStack(input));
        }
        int side = inputCount == 1 ? 1 : 3;
        CraftingInput grid = CraftingInput.of(side, side, stacks);
        CraftingRecipe recipe = (CraftingRecipe) holder.get().value();
        helper.assertTrue(recipe.matches(grid, helper.getLevel()), "recette " + id + " non applicable");
        ItemStack result = recipe.assemble(grid, helper.getLevel().registryAccess());
        helper.assertTrue(result.is(output) && result.getCount() == outputCount,
                "recette " + id + " : " + outputCount + " attendus, obtenu " + result);
    }

    /** Un nouveau jour MC remplit le tableau à trois contrats ouverts. */
    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void dawnRefillsThreeOpenContracts(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ContractBoardData data = ContractBoardData.get(level);
        data.markRefreshed(-100L);
        ContractBoard.onLevelTick(level);
        long open = data.contracts().stream().filter(c -> c.state() == Contract.State.OPEN).count();
        helper.assertTrue(open >= 3, "au moins 3 contrats ouverts à l'aube, obtenu " + open);
        helper.succeed();
    }
}
