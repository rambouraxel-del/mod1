package fr.terresvivantes.core.gametest;

import fr.terresvivantes.TerresVivantes;
import fr.terresvivantes.health.HealthConfig;
import fr.terresvivantes.compost.CompostModule;
import fr.terresvivantes.crop.CropModule;
import fr.terresvivantes.soil.SoilApi;
import fr.terresvivantes.soil.logic.SoilData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Tests d'intégration saison, culture, sol et engrais. Le sol est fertile et humide
 * (N/P/K à 100, humidité 7) pour que seule la saison fasse varier la croissance.
 */
@GameTestHolder(TerresVivantes.MODID)
@PrefixGameTestTemplate(false)
public final class TVIntegrationGameTests {

    private static final BlockPos SOIL = new BlockPos(1, 1, 1);
    private static final BlockPos CROP = SOIL.above();
    private static final int FORCED_TICKS = 200;
    private static final int MAX_FORCED_TICKS = 2000;

    private TVIntegrationGameTests() {
    }

    /**
     * (a) En hiver, une tomate (impossible en hiver) ne pousse pas : 200 randomTicks forcés laissent l'âge à 0.
     * Si elle flétrit (v0.2), le bloc n'a plus d'âge : il compte comme non poussé.
     */
    @GameTest(template = "crop_arena")
    public static void tomatoDoesNotGrowInWinter(GameTestHelper helper) {
        setSeason(helper, "winter");
        placeFertileTomato(helper);
        forceRandomTicks(helper, FORCED_TICKS);
        int age = helper.getBlockState(CROP).hasProperty(CropBlock.AGE)
                ? helper.getBlockState(CROP).getValue(CropBlock.AGE) : 0;
        helper.assertTrue(age == 0, "en hiver la tomate doit rester à l'âge 0, obtenu " + age);
        helper.succeed();
    }

    /** (b) En été, la même tomate sur la même terre atteint un âge supérieur à 0. */
    @GameTest(template = "crop_arena")
    public static void tomatoGrowsInSummer(GameTestHelper helper) {
        setSeason(helper, "summer");
        placeFertileTomato(helper);
        // Sans maladie ni flétrissement aléatoires : le test ne vérifie que la croissance estivale.
        HealthConfig.disableRandomEventsForTest(true);
        forceRandomTicks(helper, FORCED_TICKS);
        HealthConfig.disableRandomEventsForTest(false);
        int age = helper.getBlockState(CROP).getValue(CropBlock.AGE);
        helper.assertTrue(age > 0, "en été la tomate doit pousser, âge obtenu " + age);
        helper.succeed();
    }

    /** (c) Après une croissance en été, le sol a perdu des nutriments (N+P+K diminue). */
    @GameTest(template = "crop_arena")
    public static void growthDepletesSoil(GameTestHelper helper) {
        setSeason(helper, "summer");
        placeFertileTomato(helper);
        ServerLevel level = helper.getLevel();
        BlockPos soil = helper.absolutePos(SOIL);
        int before = nutrientSum(SoilApi.get(level, soil));

        int ticks = 0;
        while (helper.getBlockState(CROP).getValue(CropBlock.AGE) == 0 && ticks < MAX_FORCED_TICKS) {
            forceRandomTicks(helper, 1);
            ticks++;
        }
        int age = helper.getBlockState(CROP).getValue(CropBlock.AGE);
        helper.assertTrue(age > 0, "la tomate doit avoir poussé en " + ticks + " ticks forcés");
        int after = nutrientSum(SoilApi.get(level, soil));
        helper.assertTrue(after < before,
                "N+P+K doit diminuer après croissance : avant " + before + ", après " + after);
        helper.succeed();
    }

    /** (d) Un engrais compost sur une terre labourée augmente N, P et K. */
    @GameTest(template = "crop_arena")
    public static void compostRaisesNPK(GameTestHelper helper) {
        helper.setBlock(SOIL, Blocks.FARMLAND.defaultBlockState());
        ServerLevel level = helper.getLevel();
        BlockPos soil = helper.absolutePos(SOIL);
        // Vide la terre (bornes à 0) pour que l'augmentation soit mesurable sans plafond.
        SoilApi.consume(level, soil, 100, 100, 100);
        SoilData before = SoilApi.get(level, soil);

        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack stack = new ItemStack(CompostModule.COMPOST.get());
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(soil), Direction.UP, soil, false);
        CompostModule.COMPOST.get().useOn(new UseOnContext(level, player, InteractionHand.MAIN_HAND, stack, hit));

        SoilData after = SoilApi.get(level, soil);
        helper.assertTrue(after.n() > before.n(), "azote doit augmenter : " + before.n() + " -> " + after.n());
        helper.assertTrue(after.p() > before.p(), "phosphore doit augmenter : " + before.p() + " -> " + after.p());
        helper.assertTrue(after.k() > before.k(), "potassium doit augmenter : " + before.k() + " -> " + after.k());
        helper.succeed();
    }

    /** Terre fertile et humide, tomate âge 0 au-dessus. */
    private static void placeFertileTomato(GameTestHelper helper) {
        helper.setBlock(SOIL, Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, FarmBlock.MAX_MOISTURE));
        ServerLevel level = helper.getLevel();
        BlockPos soil = helper.absolutePos(SOIL);
        SoilApi.fertilize(level, soil, 100, 100, 100, 100);
        helper.setBlock(CROP, CropModule.cropBlock("tomato").defaultBlockState().setValue(CropBlock.AGE, 0));
    }

    /** Force des randomTicks sur la culture, en relisant l'état à chaque tick. */
    private static void forceRandomTicks(GameTestHelper helper, int count) {
        ServerLevel level = helper.getLevel();
        BlockPos crop = helper.absolutePos(CROP);
        for (int i = 0; i < count; i++) {
            level.getBlockState(crop).randomTick(level, crop, level.getRandom());
        }
    }

    private static void setSeason(GameTestHelper helper, String season) {
        MinecraftServer server = helper.getLevel().getServer();
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tv season set " + season + " 1");
    }

    private static int nutrientSum(SoilData data) {
        return data.n() + data.p() + data.k();
    }
}
