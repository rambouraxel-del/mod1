package fr.terresvivantes.core.gametest;

import fr.terresvivantes.TerresVivantes;
import fr.terresvivantes.compost.CompostBinBlock;
import fr.terresvivantes.compost.CompostModule;
import fr.terresvivantes.core.debug.ShowcaseBuilder;
import fr.terresvivantes.crop.CropModule;
import fr.terresvivantes.dairy.DairyModule;
import fr.terresvivantes.kitchen.KitchenModule;
import fr.terresvivantes.milling.MillingModule;
import fr.terresvivantes.preserving.PreservingModule;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import fr.terresvivantes.apiculture.ApiaryBlockEntity;
import fr.terresvivantes.apiculture.ApicultureModule;
import fr.terresvivantes.apiculture.logic.ApiaryLogic;
import fr.terresvivantes.crop.logic.CropDefinitions;
import fr.terresvivantes.duck.entity.Duck;
import fr.terresvivantes.husbandry.FeedingTroughBlockEntity;
import fr.terresvivantes.husbandry.HusbandryModule;
import fr.terresvivantes.husbandry.logic.FeedType;
import fr.terresvivantes.vet.VetModule;
import fr.terresvivantes.flora.FloraModule;
import fr.terresvivantes.flora.HawthornHedgeBlock;
import fr.terresvivantes.health.HealthModule;
import fr.terresvivantes.insects.entity.Butterfly;
import fr.terresvivantes.insects.entity.Ladybug;
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
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

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
        ShowcaseBuilder.Result result = ShowcaseBuilder.build(level, origin);
        helper.assertTrue(result.blocks() > 0, "la plateforme doit poser des blocs");
        verifyShowcase(helper, level, origin, result.fauna());
        // Les animaux et insectes sont vérifiés d'après le retour de la construction : une entité posée dans un chunk
        // qui n'est pas encore « entity ticking » n'est pas visible par requête avant la promotion (asynchrone).
        for (Mob mob : result.fauna()) {
            mob.discard();
        }
        ShowcaseBuilder.clear(level, origin);
        helper.succeed();
    }

    private static void verifyShowcase(GameTestHelper helper, ServerLevel level, BlockPos origin, List<Mob> fauna) {
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

        // Fleurs sauvages (rangée 5, x 12..15), haie d'aubépine (rangée 8, x 6..9; les deux derniers en fleurs).
        for (int i = 0; i < ShowcaseBuilder.FLOWER_IDS.size(); i++) {
            String id = ShowcaseBuilder.FLOWER_IDS.get(i);
            helper.assertTrue(level.getBlockState(at(origin, 12 + i, 1, 5)).is(FloraModule.flower(id)),
                    "fleur " + id + " attendue en (" + (12 + i) + ",1,5)");
        }
        helper.assertTrue(level.getBlockState(at(origin, 6, 1, 8)).is(FloraModule.hedge())
                        && !level.getBlockState(at(origin, 6, 1, 8)).getValue(HawthornHedgeBlock.FLOWERING)
                        && level.getBlockState(at(origin, 9, 1, 8)).getValue(HawthornHedgeBlock.FLOWERING),
                "haie d'aubépine attendue en (6..9,1,8), fleurie aux deux derniers");

        // Apiaire peuplé et extracteur (rangée 10, x 5 et 7).
        helper.assertTrue(level.getBlockState(at(origin, 5, 1, 10)).is(ApicultureModule.APIARY.get()),
                "apiaire attendu en (5,1,10)");
        helper.assertTrue(level.getBlockState(at(origin, 7, 1, 10)).is(ApicultureModule.HONEY_EXTRACTOR.get()),
                "extracteur attendu en (7,1,10)");
        helper.assertTrue(level.getBlockEntity(at(origin, 5, 1, 10)) instanceof ApiaryBlockEntity apiary
                        && apiary.population() == ApiaryLogic.MAX_POPULATION,
                "l'apiaire de la vitrine doit être peuplé au maximum");

        // Deux papillons et une coccinelle, persistants.
        helper.assertTrue(count(fauna, Butterfly.class) == 2, "deux papillons attendus dans la vitrine");
        helper.assertTrue(count(fauna, Ladybug.class) == 1, "une coccinelle attendue dans la vitrine");

        // Enclos d'élevage : auge pleine de foin, nichoir, pierre à sel, clôture sur le pourtour.
        helper.assertTrue(level.getBlockEntity(at(origin, 9, 1, 10)) instanceof FeedingTroughBlockEntity trough
                        && trough.type() == FeedType.HAY && trough.stock() > 0,
                "auge remplie de foin attendue en (9,1,10)");
        helper.assertTrue(level.getBlockState(at(origin, 14, 1, 10)).is(HusbandryModule.NEST.get()),
                "nichoir attendu en (14,1,10)");
        helper.assertTrue(level.getBlockState(at(origin, 9, 1, 14)).is(VetModule.SALT_LICK.get()),
                "pierre à sel attendue en (9,1,14)");
        helper.assertTrue(level.getBlockState(at(origin, 8, 1, 9)).is(Blocks.OAK_FENCE)
                        && level.getBlockState(at(origin, 15, 1, 15)).is(Blocks.OAK_FENCE)
                        && level.getBlockState(at(origin, 11, 1, 12)).isAir(),
                "clôture d'enclos attendue sur le pourtour, intérieur libre");

        // Animaux de l'enclos : une vache, un mouton, une poule, deux canards (un mâle, une femelle), persistants.
        helper.assertTrue(count(fauna, Cow.class) == 1 && count(fauna, Sheep.class) == 1
                        && count(fauna, Chicken.class) == 1,
                "une vache, un mouton et une poule attendus dans l'enclos");
        helper.assertTrue(count(fauna, Duck.class) == 2
                        && fauna.stream().filter(Duck.class::isInstance).map(Duck.class::cast)
                        .filter(Duck::isMale).count() == 1,
                "deux canards attendus dans l'enclos, dont un mâle");
        helper.assertTrue(fauna.stream().allMatch(Mob::isPersistenceRequired),
                "les animaux et insectes de la vitrine doivent être persistants");

        // Rangée de transformation : une machine de chaque sorte, marmite sur feu de camp allumé.
        helper.assertTrue(level.getBlockState(at(origin, 0, 1, 17)).is(MillingModule.MILLSTONE.get())
                        && level.getBlockState(at(origin, 2, 1, 17)).is(MillingModule.FRUIT_PRESS.get())
                        && level.getBlockState(at(origin, 4, 1, 17)).is(DairyModule.CHURN.get())
                        && level.getBlockState(at(origin, 6, 1, 17)).is(DairyModule.CHEESE_VAT.get())
                        && level.getBlockState(at(origin, 8, 1, 17)).is(DairyModule.AGING_SHELF.get())
                        && level.getBlockState(at(origin, 10, 1, 17)).is(PreservingModule.FERMENTATION_BARREL.get())
                        && level.getBlockState(at(origin, 12, 1, 17)).is(PreservingModule.DRYING_RACK.get())
                        && level.getBlockState(at(origin, 14, 1, 17)).is(PreservingModule.SMOKEHOUSE.get()),
                "une machine de transformation de chaque sorte attendue sur la rangée z 17");
        helper.assertTrue(level.getBlockState(at(origin, 2, 1, 20)).is(KitchenModule.COOKING_POT.get())
                        && level.getBlockState(at(origin, 2, 0, 20)).is(Blocks.CAMPFIRE)
                        && level.getBlockState(at(origin, 2, 0, 20)).getValue(BlockStateProperties.LIT),
                "marmite attendue sur un feu de camp allumé (z 20)");

        // Air au-dessus de la plateforme.
        helper.assertTrue(level.getBlockState(at(origin, 0, ShowcaseBuilder.HEIGHT, 0)).isAir(),
                "air attendu au-dessus de la plateforme");

    }

    private static <T extends Mob> long count(List<Mob> fauna, Class<T> type) {
        return fauna.stream().filter(type::isInstance).count();
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
