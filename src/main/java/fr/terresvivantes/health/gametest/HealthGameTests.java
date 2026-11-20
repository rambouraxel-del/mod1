package fr.terresvivantes.health.gametest;

import fr.terresvivantes.TerresVivantes;
import fr.terresvivantes.crop.CropModule;
import fr.terresvivantes.health.HealthApi;
import fr.terresvivantes.health.HealthModule;
import fr.terresvivantes.health.logic.Disease;
import fr.terresvivantes.soil.SoilApi;
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
 * Tests de jeu de la santé des cultures : flétrissement, fongicide, maladie et pulvérisateur.
 * Les randomTicks sont forcés; une tentative n'a lieu qu'avec une probabilité d'environ 6 % par tick, donc les
 * boucles vont jusqu'à un plafond généreux.
 */
@GameTestHolder(TerresVivantes.MODID)
@PrefixGameTestTemplate(false)
public final class HealthGameTests {

    private static final BlockPos SOIL = new BlockPos(1, 1, 1);
    private static final BlockPos CROP = SOIL.above();
    private static final int MAX_FORCED_TICKS = 3000;

    private HealthGameTests() {
    }

    /** Hiver extérieur : une tomate (impossible en hiver) accumule du stress et flétrit. */
    @GameTest(template = "crop_arena")
    public static void outdoorTomatoWithersInWinter(GameTestHelper helper) {
        setSeason(helper, "winter");
        placeFertileTomato(helper);
        int ticks = forceUntilWithered(helper);
        helper.assertTrue(helper.getBlockState(CROP).is(HealthModule.WITHERED_CROP.get()),
                "en hiver dehors, la tomate doit flétrir (" + ticks + " ticks forcés)");
        helper.succeed();
    }

    /** Une culture malade traitée au fongicide redevient saine et immunisée. */
    @GameTest(template = "crop_arena")
    public static void fungicideCuresSickCrop(GameTestHelper helper) {
        setSeason(helper, "summer");
        placeFertileTomato(helper);
        ServerLevel level = helper.getLevel();
        BlockPos crop = helper.absolutePos(CROP);
        HealthApi.infect(level, crop, Disease.MILDEW);
        helper.assertTrue(HealthApi.isSick(level, crop), "la culture doit être malade avant le traitement");

        useFungicide(helper, crop);

        helper.assertTrue(!HealthApi.isSick(level, crop), "le fongicide doit guérir la culture");
        helper.assertTrue(HealthApi.isImmune(level, crop), "la culture traitée doit être immunisée");
        helper.succeed();
    }

    /** Une culture malade fait six tentatives de croissance, puis flétrit (été, facteur positif). */
    @GameTest(template = "crop_arena")
    public static void sickCropWithersAfterSixAttempts(GameTestHelper helper) {
        setSeason(helper, "summer");
        placeFertileTomato(helper);
        HealthApi.infect(helper.getLevel(), helper.absolutePos(CROP), Disease.MILDEW);
        int ticks = forceUntilWithered(helper);
        helper.assertTrue(helper.getBlockState(CROP).is(HealthModule.WITHERED_CROP.get()),
                "une culture malade doit flétrir après 6 tentatives (" + ticks + " ticks forcés)");
        helper.succeed();
    }

    /** Le pulvérisateur traite la culture visée et sa voisine (zone 3x3). */
    @GameTest(template = "crop_arena")
    public static void sprayerTreatsNeighbouringCrop(GameTestHelper helper) {
        setSeason(helper, "summer");
        placeFertileTomato(helper);
        BlockPos neighbourSoil = SOIL.east();
        helper.setBlock(neighbourSoil, Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, FarmBlock.MAX_MOISTURE));
        helper.setBlock(CROP.east(), CropModule.cropBlock("tomato").defaultBlockState().setValue(CropBlock.AGE, 0));
        ServerLevel level = helper.getLevel();
        BlockPos crop = helper.absolutePos(CROP);
        BlockPos neighbour = helper.absolutePos(CROP.east());
        HealthApi.infect(level, crop, Disease.MILDEW);
        HealthApi.infect(level, neighbour, Disease.MILDEW);

        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack sprayer = new ItemStack(HealthModule.SPRAYER.get());
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(crop), Direction.UP, crop, false);
        HealthModule.SPRAYER.get().useOn(new UseOnContext(level, player, InteractionHand.MAIN_HAND, sprayer, hit));

        helper.assertTrue(!HealthApi.isSick(level, crop), "le pulvérisateur doit guérir la culture visée");
        helper.assertTrue(!HealthApi.isSick(level, neighbour), "le pulvérisateur doit guérir la voisine");
        helper.assertTrue(HealthApi.isImmune(level, neighbour), "la voisine traitée doit être immunisée");
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

    /** Force des randomTicks sur la culture jusqu'au flétrissement; retourne le nombre de ticks utilisés. */
    private static int forceUntilWithered(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos crop = helper.absolutePos(CROP);
        int ticks = 0;
        while (!level.getBlockState(crop).is(HealthModule.WITHERED_CROP.get()) && ticks < MAX_FORCED_TICKS) {
            level.getBlockState(crop).randomTick(level, crop, level.getRandom());
            ticks++;
        }
        return ticks;
    }

    private static void useFungicide(GameTestHelper helper, BlockPos crop) {
        ServerLevel level = helper.getLevel();
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack stack = new ItemStack(HealthModule.FUNGICIDE.get());
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(crop), Direction.UP, crop, false);
        HealthModule.FUNGICIDE.get().useOn(new UseOnContext(level, player, InteractionHand.MAIN_HAND, stack, hit));
    }

    private static void setSeason(GameTestHelper helper, String season) {
        MinecraftServer server = helper.getLevel().getServer();
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tv season set " + season + " 1");
    }
}
