package fr.terresvivantes.orchard.gametest;

import fr.terresvivantes.TerresVivantes;
import fr.terresvivantes.orchard.FruitLeavesBlock;
import fr.terresvivantes.orchard.OrchardModule;
import fr.terresvivantes.orchard.logic.FruitTreeDefinition;
import fr.terresvivantes.orchard.logic.FruitTreeDefinitions;
import fr.terresvivantes.orchard.logic.OrchardLogic;
import fr.terresvivantes.season.SeasonApi;
import fr.terresvivantes.season.logic.Season;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Tests de jeu du module orchard. Le gabarit orchard_arena est un volume d'air de 11 x 12 x 11. */
@GameTestHolder(TerresVivantes.MODID)
@PrefixGameTestTemplate(false)
public final class OrchardGameTests {

    private static final String TEMPLATE = "orchard_arena";
    private static final BlockPos GROUND = new BlockPos(5, 1, 5);
    private static final BlockPos PLANT = GROUND.above();
    private static final int MAX_FORCED_TICKS = 4000;

    private OrchardGameTests() {
    }

    /** Un sapling sur de la terre, fertilisé au bonemeal, devient un arbre : tronc à sa base et feuillage. */
    @GameTest(template = TEMPLATE)
    public static void saplingBonemealGrowsTree(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        helper.setBlock(GROUND, Blocks.DIRT);
        helper.setBlock(PLANT, OrchardModule.saplingBlock("apple").defaultBlockState());
        BlockPos pos = helper.absolutePos(PLANT);

        BonemealableBlock sapling = OrchardModule.saplingBlock("apple");
        sapling.performBonemeal(level, level.getRandom(), pos, level.getBlockState(pos));

        helper.assertTrue(level.getBlockState(pos).is(Blocks.OAK_LOG), "un tronc doit remplacer le sapling");
        helper.assertTrue(level.getBlockState(pos.offset(1, 2, 0)).is(OrchardModule.leavesBlock("apple")),
                "le feuillage doit entourer le tronc");
        helper.succeed();
    }

    /** Une feuille au stade 3 donne 1 à 3 fruits au clic droit et revient au stade 0. */
    @GameTest(template = TEMPLATE)
    public static void ripeLeavesGiveFruitAndReset(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        FruitTreeDefinition cherry = species("cherry");
        FruitLeavesBlock leaves = OrchardModule.leavesBlock("cherry");
        helper.setBlock(PLANT, leaves.defaultBlockState()
                .setValue(FruitLeavesBlock.STAGE, OrchardLogic.MAX_STAGE)
                .setValue(FruitLeavesBlock.PERSISTENT, true));
        BlockPos pos = helper.absolutePos(PLANT);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);

        level.getBlockState(pos).useWithoutItem(level, player, hit);

        Item fruit = OrchardModule.fruitItem(cherry);
        int count = player.getInventory().countItem(fruit);
        // Sans pollinisateur, le rendement vaut x0.6 (module pollination) : la borne basse est abaissée.
        int low = (int) Math.floor(cherry.yieldMin() * 0.6f);
        helper.assertTrue(count >= low && count <= cherry.yieldMax(),
                "récolte attendue entre " + low + " et " + cherry.yieldMax() + ", obtenu " + count);
        int stage = level.getBlockState(pos).getValue(FruitLeavesBlock.STAGE);
        helper.assertTrue(stage == 0, "la feuille doit revenir au stade 0, obtenu " + stage);
        helper.succeed();
    }

    /** Au printemps, une feuille au stade 0 passe à la floraison (stade 1) sous randomTicks forcés. */
    @GameTest(template = TEMPLATE)
    public static void springFlowersFromFoliage(GameTestHelper helper) {
        setSeason(helper, "spring");
        ServerLevel level = helper.getLevel();
        helper.setBlock(PLANT, OrchardModule.leavesBlock("apple").defaultBlockState()
                .setValue(FruitLeavesBlock.PERSISTENT, true));
        BlockPos pos = helper.absolutePos(PLANT);
        RandomSource random = RandomSource.create(4242L);

        for (int i = 0; i < MAX_FORCED_TICKS && level.getBlockState(pos).getValue(FruitLeavesBlock.STAGE) == 0; i++) {
            BlockState state = level.getBlockState(pos);
            state.randomTick(level, pos, random);
        }

        int stage = level.getBlockState(pos).getValue(FruitLeavesBlock.STAGE);
        helper.assertTrue(stage == 1, "la feuille doit fleurir au printemps, stade obtenu " + stage);
        helper.succeed();
    }

    private static FruitTreeDefinition species(String id) {
        return FruitTreeDefinitions.all().stream()
                .filter(def -> def.id().equals(id))
                .findFirst()
                .orElseThrow();
    }

    private static void setSeason(GameTestHelper helper, String season) {
        MinecraftServer server = helper.getLevel().getServer();
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tv season set " + season + " 1");
        helper.assertTrue(SeasonApi.getSeason(helper.getLevel()) == Season.valueOf(season.toUpperCase(java.util.Locale.ROOT)),
                "la saison de test doit être " + season);
    }
}
