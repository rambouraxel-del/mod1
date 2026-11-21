package fr.terresvivantes.vineyard.gametest;

import fr.terresvivantes.TerresVivantes;
import fr.terresvivantes.milling.FruitPressBlockEntity;
import fr.terresvivantes.milling.MillingModule;
import fr.terresvivantes.season.SeasonApi;
import fr.terresvivantes.season.logic.Season;
import fr.terresvivantes.vineyard.GrapeVariety;
import fr.terresvivantes.vineyard.GrapevineBlock;
import fr.terresvivantes.vineyard.VineyardModule;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import fr.terresvivantes.vineyard.VineyardApi;
import java.util.List;

/**
 * Tests de jeu du vignoble. Gabarit {@code orchard_arena}. Les saisons se fixent par {@code /tv season set} et la
 * croissance se pilote par appels directs de {@link BlockState#randomTick}, pour ne pas dépendre du hasard du tick.
 */
@GameTestHolder(TerresVivantes.MODID)
@PrefixGameTestTemplate(false)
public final class VineyardGameTests {

    private static final String TEMPLATE = "orchard_arena";
    private static final String BATCH = "vineyard";
    private static final BlockPos SOIL = new BlockPos(5, 1, 5);
    private static final BlockPos PLANT = new BlockPos(5, 2, 5);
    private static final BlockPos PRESS = new BlockPos(5, 2, 5);
    /** Pressage des raisins : la recette dure 120 ticks, vérifiée à 150. */
    private static final int PRESS_CHECK_TICKS = 150;
    /** Borne des randomTicks de croissance (l'attente moyenne par étape est de quelques dizaines d'essais). */
    private static final int GROWTH_LIMIT = 20000;
    /** Essais sans changement en été fin de saison : la vigne ne doit pas mûrir hors fenêtre. */
    private static final int SUMMER_EARLY_CHECKS = 3000;

    private VineyardGameTests() {
    }

    /** Bouture rouge posée sur un treillis : le treillis devient une vigne rouge au stade 0. */
    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 200)
    public static void cuttingOnTrellisPlantsRedGrapevine(GameTestHelper helper) {
        helper.setBlock(SOIL, Blocks.DIRT);
        helper.setBlock(PLANT, VineyardModule.TRELLIS.get());
        Player player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        ItemStack cutting = new ItemStack(VineyardModule.RED_CUTTING.get());
        BlockPos abs = helper.absolutePos(PLANT);
        UseOnContext context = new UseOnContext(helper.getLevel(), player, InteractionHand.MAIN_HAND, cutting,
                new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false));
        InteractionResult result = VineyardModule.RED_CUTTING.get().useOn(context);
        helper.assertTrue(result.consumesAction(), "la plantation doit être prise en compte");
        BlockState state = helper.getBlockState(PLANT);
        helper.assertTrue(state.is(VineyardModule.GRAPEVINE.get()), "vigne attendue, obtenu " + state);
        helper.assertTrue(state.getValue(GrapevineBlock.VARIETY) == GrapeVariety.RED, "cépage rouge attendu");
        helper.assertTrue(state.getValue(GrapevineBlock.STAGE) == 0, "stade 0 attendu après plantation");
        helper.succeed();
    }

    /** Été : le stade 0 monte à 2 sans mûrir; automne : le stade 2 monte à 3 (grappes). */
    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 200)
    public static void vineRipensInAutumnNotInEarlySummer(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        setSeason(helper, "summer", Season.SUMMER);
        helper.setBlock(SOIL, Blocks.DIRT);
        helper.setBlock(PLANT, VineyardModule.GRAPEVINE.get().defaultBlockState()
                .setValue(GrapevineBlock.VARIETY, GrapeVariety.WHITE)
                .setValue(GrapevineBlock.STAGE, 0));
        BlockPos abs = helper.absolutePos(PLANT);

        growUntil(level, abs, 2);
        helper.assertTrue(stageAt(level, abs) == 2, "stade 2 attendu en été, obtenu " + stageAt(level, abs));
        for (int i = 0; i < SUMMER_EARLY_CHECKS; i++) {
            level.getBlockState(abs).randomTick(level, abs, level.getRandom());
        }
        helper.assertTrue(stageAt(level, abs) == 2, "pas de grappes en début d'été, obtenu " + stageAt(level, abs));

        setSeason(helper, "autumn", Season.AUTUMN);
        growUntil(level, abs, 3);
        helper.assertTrue(stageAt(level, abs) == 3, "stade 3 (grappes) attendu en automne, obtenu " + stageAt(level, abs));
        helper.succeed();
    }

    /** Stade 3 : clic droit à main vide donne 2 à 4 grappes de la variété et revient au stade 2. */
    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 200)
    public static void harvestGivesRedGrapesAndResetsToStageTwo(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        setSeason(helper, "autumn", Season.AUTUMN);
        helper.setBlock(SOIL, Blocks.DIRT);
        helper.setBlock(PLANT, VineyardModule.GRAPEVINE.get().defaultBlockState()
                .setValue(GrapevineBlock.VARIETY, GrapeVariety.RED)
                .setValue(GrapevineBlock.STAGE, 3));
        BlockPos abs = helper.absolutePos(PLANT);
        Player player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        BlockState state = level.getBlockState(abs);
        InteractionResult result = state.useWithoutItem(level, player,
                new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false));
        helper.assertTrue(result.consumesAction(), "la récolte doit être prise en compte");
        int grapes = player.getInventory().countItem(VineyardModule.RED_GRAPES.get());
        helper.assertTrue(grapes >= 2 && grapes <= 4, "2 à 4 raisins rouges attendus, obtenu " + grapes);
        BlockState after = level.getBlockState(abs);
        helper.assertTrue(after.getValue(GrapevineBlock.STAGE) == 2, "retour au stade 2 attendu");
        helper.assertTrue(after.getValue(GrapevineBlock.HARVESTED), "la vigne doit être marquée récoltée");
        helper.succeed();
    }

    /** Taille d'hiver : une vigne mûre retombe au stade 1 et la récolte de l'année est à nouveau permise. */
    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 200)
    public static void winterPrunesMatureVineToStageOne(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        setSeason(helper, "winter", Season.WINTER);
        helper.setBlock(SOIL, Blocks.DIRT);
        helper.setBlock(PLANT, VineyardModule.GRAPEVINE.get().defaultBlockState()
                .setValue(GrapevineBlock.VARIETY, GrapeVariety.RED)
                .setValue(GrapevineBlock.STAGE, 3)
                .setValue(GrapevineBlock.HARVESTED, true));
        BlockPos abs = helper.absolutePos(PLANT);
        level.getBlockState(abs).randomTick(level, abs, level.getRandom());
        BlockState after = level.getBlockState(abs);
        helper.assertTrue(after.getValue(GrapevineBlock.STAGE) == 1, "stade 1 attendu après taille, obtenu " + after);
        helper.assertTrue(!after.getValue(GrapevineBlock.HARVESTED), "la récolte doit être réinitialisée par la taille");
        helper.succeed();
    }

    /** Deux raisins rouges et une bouteille dans le pressoir : le jus de raisin est prêt après la recette. */
    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 200)
    public static void fruitPressMakesGrapeJuiceFromRedGrapes(GameTestHelper helper) {
        helper.setBlock(PRESS, MillingModule.FRUIT_PRESS.get());
        FruitPressBlockEntity press = helper.getBlockEntity(PRESS);
        press.inventory().setStackInSlot(0, new ItemStack(VineyardModule.RED_GRAPES.get(), 2));
        press.inventory().setStackInSlot(MillingModule.FRUIT_PRESS_TYPE.containerSlot(),
                new ItemStack(Items.GLASS_BOTTLE, 1));
        helper.runAfterDelay(PRESS_CHECK_TICKS, () -> {
            ItemStack juice = press.inventory().getStackInSlot(MillingModule.FRUIT_PRESS_TYPE.outputSlot());
            helper.assertTrue(juice.is(VineyardModule.GRAPE_JUICE.get()) && juice.getCount() == 1,
                    "jus de raisin attendu après " + PRESS_CHECK_TICKS + " ticks, obtenu " + juice);
            helper.succeed();
        });
    }

    private static int stageAt(ServerLevel level, BlockPos abs) {
        return level.getBlockState(abs).getValue(GrapevineBlock.STAGE);
    }

    /** Appels de randomTick jusqu'à atteindre le stade voulu, ou la borne. */
    private static void growUntil(ServerLevel level, BlockPos abs, int target) {
        for (int i = 0; i < GROWTH_LIMIT && stageAt(level, abs) < target; i++) {
            level.getBlockState(abs).randomTick(level, abs, level.getRandom());
        }
    }

    private static void setSeason(GameTestHelper helper, String name, Season expected) {
        MinecraftServer server = helper.getLevel().getServer();
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tv season set " + name + " 1");
        helper.assertTrue(SeasonApi.getSeason(helper.getLevel()) == expected, "la saison de test doit être " + name);
    }

    /** API publique de récolte : une vigne non mûre ne donne rien; une vigne mûre donne 2 à 4 grappes de son cépage. */
    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 200)
    public static void apiHarvestOnlyTakesRipeVines(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        helper.setBlock(SOIL, Blocks.DIRT);
        helper.setBlock(PLANT, VineyardModule.GRAPEVINE.get().defaultBlockState()
                .setValue(GrapevineBlock.VARIETY, GrapeVariety.WHITE)
                .setValue(GrapevineBlock.STAGE, 2));
        BlockPos abs = helper.absolutePos(PLANT);
        helper.assertTrue(VineyardApi.harvest(level, abs).isEmpty(), "une vigne au stade 2 ne se récolte pas");

        helper.setBlock(PLANT, VineyardModule.GRAPEVINE.get().defaultBlockState()
                .setValue(GrapevineBlock.VARIETY, GrapeVariety.WHITE)
                .setValue(GrapevineBlock.STAGE, 3));
        List<ItemStack> drops = VineyardApi.harvest(level, abs);
        helper.assertTrue(drops.size() == 1 && drops.get(0).is(VineyardModule.WHITE_GRAPES.get()),
                "une grappe de raisins blancs attendue, reçu " + drops);
        int count = drops.get(0).getCount();
        helper.assertTrue(count >= 2 && count <= 4, "2 à 4 raisins attendus, obtenu " + count);
        helper.assertTrue(level.getBlockState(abs).getValue(GrapevineBlock.STAGE) == 2,
                "la vigne doit revenir au stade 2");
        helper.succeed();
    }
}
