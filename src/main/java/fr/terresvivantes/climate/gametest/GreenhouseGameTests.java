package fr.terresvivantes.climate.gametest;

import fr.terresvivantes.TerresVivantes;
import fr.terresvivantes.climate.ClimateApi;
import fr.terresvivantes.season.SeasonApi;
import fr.terresvivantes.season.logic.Season;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Tests de jeu des serres (abri sous verre). Le blé vanilla sert de culture témoin : en hiver, aucune
 * culture du mod n'a d'affinité avec la saison (voir docs/ARCHITECTURE.md, contrats v0.2).
 */
@GameTestHolder(TerresVivantes.MODID)
@PrefixGameTestTemplate(false)
public final class GreenhouseGameTests {

    private static final BlockPos SOIL = new BlockPos(1, 1, 1);
    private static final BlockPos CROP = SOIL.above();
    private static final BlockPos ROOF = CROP.above(2);
    private static final int MAX_FORCED_TICKS = 4000;

    private GreenhouseGameTests() {
    }

    /** Un verre à deux blocs au-dessus de la culture la rend abritée; sans verre, elle ne l'est pas. */
    @GameTest(template = "crop_arena")
    public static void glassRoofShelters(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        helper.setBlock(ROOF, Blocks.GLASS);
        helper.assertTrue(ClimateApi.isSheltered(level, helper.absolutePos(CROP)),
                "une culture sous un verre à ciel ouvert doit être abritée");
        helper.setBlock(ROOF, Blocks.AIR);
        helper.assertTrue(!ClimateApi.isSheltered(level, helper.absolutePos(CROP)),
                "une culture sans verre ne doit pas être abritée");
        helper.succeed();
    }

    /** En hiver, un blé sous verre pousse : 0.6 au lieu de 0 (randomTicks forcés). */
    @GameTest(template = "crop_arena")
    public static void wheatUnderGlassGrowsInWinter(GameTestHelper helper) {
        setSeason(helper, "winter");
        placeWheat(helper, true);
        int age = forceUntilGrowth(helper);
        helper.assertTrue(age > 0, "sous verre en hiver, le blé doit pousser (âge " + age + ")");
        helper.succeed();
    }

    /** Même blé, même terre, sans verre, en hiver : il ne pousse pas. */
    @GameTest(template = "crop_arena")
    public static void wheatWithoutGlassDoesNotGrowInWinter(GameTestHelper helper) {
        setSeason(helper, "winter");
        placeWheat(helper, false);
        int age = forceUntilGrowth(helper);
        helper.assertTrue(age == 0, "sans verre en hiver, le blé ne doit pas pousser (âge " + age + ")");
        helper.succeed();
    }

    private static void placeWheat(GameTestHelper helper, boolean glass) {
        helper.setBlock(SOIL, Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, FarmBlock.MAX_MOISTURE));
        helper.setBlock(CROP, Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, 0));
        if (glass) {
            helper.setBlock(ROOF, Blocks.GLASS);
        }
    }

    /** Force des randomTicks jusqu'à la première croissance (ou MAX_FORCED_TICKS), retourne l'âge final. */
    private static int forceUntilGrowth(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos crop = helper.absolutePos(CROP);
        for (int i = 0; i < MAX_FORCED_TICKS; i++) {
            BlockState state = level.getBlockState(crop);
            state.randomTick(level, crop, level.getRandom());
            if (level.getBlockState(crop).getValue(CropBlock.AGE) > 0) {
                break;
            }
        }
        return level.getBlockState(crop).getValue(CropBlock.AGE);
    }

    private static void setSeason(GameTestHelper helper, String season) {
        MinecraftServer server = helper.getLevel().getServer();
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tv season set " + season + " 1");
        helper.assertTrue(SeasonApi.getSeason(helper.getLevel()) == Season.WINTER,
                "la saison de test doit être l'hiver");
    }
}
