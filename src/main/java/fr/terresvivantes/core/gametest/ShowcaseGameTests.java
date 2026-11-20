package fr.terresvivantes.core.gametest;

import fr.terresvivantes.TerresVivantes;
import fr.terresvivantes.compost.CompostBinBlock;
import fr.terresvivantes.compost.CompostModule;
import fr.terresvivantes.core.debug.ShowcaseBuilder;
import fr.terresvivantes.crop.CropModule;
import fr.terresvivantes.crop.logic.CropDefinitions;
import fr.terresvivantes.health.HealthModule;
import fr.terresvivantes.orchard.FruitLeavesBlock;
import fr.terresvivantes.orchard.OrchardModule;
import fr.terresvivantes.orchard.logic.FruitTreeDefinitions;
import fr.terresvivantes.water.RainBarrelBlock;
import fr.terresvivantes.water.WaterModule;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Tests de la plateforme {@code /tv showcase} : la construction pose les blocs attendus. */
@GameTestHolder(TerresVivantes.MODID)
@PrefixGameTestTemplate(false)
public final class ShowcaseGameTests {

    /** Hauteur au-dessus de la zone de test, pour ne pas toucher aux autres tests. */
    private static final int ABOVE = 30;

    private ShowcaseGameTests() {
    }

    @GameTest(template = "")
    public static void showcaseBuildsExpectedBlocks(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos origin = helper.absolutePos(BlockPos.ZERO).offset(0, ABOVE, 0);
        int placed = ShowcaseBuilder.build(level, origin);
        helper.assertTrue(placed > 0, "la plateforme doit poser des blocs");

        // Colonne de la première culture du mod : terre humide, âges 0 et 7 aux extrémités.
        String firstCrop = CropDefinitions.all().get(0).id();
        helper.assertTrue(level.getBlockState(at(origin, 0, 0, 0)).is(Blocks.FARMLAND)
                        && level.getBlockState(at(origin, 0, 0, 0)).getValue(FarmBlock.MOISTURE) == 7,
                "terre labourée humide attendue en (0,0,0)");
        assertCrop(helper, level, at(origin, 0, 1, 0), firstCrop, 0);
        assertCrop(helper, level, at(origin, 0, 1, 3), firstCrop, 7);

        // Cultures vanilla : colonne après les cultures du mod.
        int vanillaX = CropDefinitions.all().size();
        helper.assertTrue(level.getBlockState(at(origin, vanillaX, 1, 0)).is(Blocks.WHEAT),
                "blé vanilla attendu en colonne " + vanillaX);

        // Blocs du mod sur la rangée 5.
        BlockState emptyBin = level.getBlockState(at(origin, 0, 1, 5));
        helper.assertTrue(emptyBin.is(CompostModule.COMPOST_BIN.get())
                        && !emptyBin.getValue(CompostBinBlock.READY) && emptyBin.getValue(CompostBinBlock.LEVEL) == 0,
                "compost vide attendu en (0,1,5)");
        BlockState readyBin = level.getBlockState(at(origin, 2, 1, 5));
        helper.assertTrue(readyBin.is(CompostModule.COMPOST_BIN.get()) && readyBin.getValue(CompostBinBlock.READY),
                "compost prêt attendu en (2,1,5)");
        BlockState barrel = level.getBlockState(at(origin, 4, 1, 5));
        helper.assertTrue(barrel.is(WaterModule.RAIN_BARREL.get()) && barrel.getValue(RainBarrelBlock.LEVEL) == 8,
                "tonneau plein attendu en (4,1,5)");
        helper.assertTrue(level.getBlockState(at(origin, 6, 0, 5)).is(Blocks.WATER)
                        && level.getBlockState(at(origin, 6, 1, 5)).is(WaterModule.COPPER_SPRINKLER.get()),
                "arroseur en cuivre sur eau attendu en (6,1,5)");
        helper.assertTrue(level.getBlockState(at(origin, 8, 0, 5)).is(Blocks.WATER)
                        && level.getBlockState(at(origin, 8, 1, 5)).is(WaterModule.IRON_SPRINKLER.get()),
                "arroseur en fer sur eau attendu en (8,1,5)");
        helper.assertTrue(level.getBlockState(at(origin, 10, 1, 5)).is(HealthModule.WITHERED_CROP.get()),
                "culture flétrie attendue en (10,1,5)");

        // Saplings (rangée 8) et feuillages aux stades 0 à 3 (rangées 10 et suivantes).
        String firstSpecies = FruitTreeDefinitions.all().get(0).id();
        helper.assertTrue(level.getBlockState(at(origin, 0, 1, 8)).is(OrchardModule.saplingBlock(firstSpecies)),
                "sapling attendu en (0,1,8)");
        for (int stage = 0; stage <= 3; stage++) {
            BlockState leaves = level.getBlockState(at(origin, stage, 1, 10));
            helper.assertTrue(leaves.is(OrchardModule.leavesBlock(firstSpecies))
                            && leaves.getValue(FruitLeavesBlock.STAGE) == stage
                            && leaves.getValue(LeavesBlock.PERSISTENT),
                    "feuillage stade " + stage + " persistant attendu en (" + stage + ",1,10)");
        }

        // Air au-dessus de la plateforme.
        helper.assertTrue(level.getBlockState(at(origin, 0, ShowcaseBuilder.HEIGHT, 0)).isAir(),
                "air attendu au-dessus de la plateforme");

        ShowcaseBuilder.clear(level, origin);
        helper.succeed();
    }

    private static void assertCrop(GameTestHelper helper, ServerLevel level, BlockPos pos, String id, int age) {
        BlockState state = level.getBlockState(pos);
        helper.assertTrue(state.is(CropModule.cropBlock(id)) && state.getValue(CropBlock.AGE) == age,
                "culture " + id + " à l'âge " + age + " attendue en " + pos);
    }

    private static BlockPos at(BlockPos origin, int dx, int dy, int dz) {
        return origin.offset(dx, dy, dz);
    }
}
