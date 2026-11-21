package fr.terresvivantes.core.gametest;

import fr.terresvivantes.automation.AutomationModule;
import fr.terresvivantes.automation.PlanterBlockEntity;
import fr.terresvivantes.pests.PestsModule;
import fr.terresvivantes.pests.entity.Crow;
import fr.terresvivantes.storage.CrateBlockEntity;
import fr.terresvivantes.storage.StorageModule;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import fr.terresvivantes.TerresVivantes;
import fr.terresvivantes.compost.CompostBinBlock;
import fr.terresvivantes.compost.CompostModule;
import fr.terresvivantes.core.debug.ShowcaseBuilder;
import fr.terresvivantes.crop.CropModule;
import fr.terresvivantes.dairy.DairyModule;
import fr.terresvivantes.kitchen.KitchenModule;
import fr.terresvivantes.milling.MillingModule;
import fr.terresvivantes.guild.GuildModule;
import fr.terresvivantes.market.MarketModule;
import fr.terresvivantes.market.MarketStallBlockEntity;
import net.minecraft.world.entity.npc.Villager;
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
import fr.terresvivantes.breeds.BreedsApi;
import fr.terresvivantes.breeds.logic.Breed;
import fr.terresvivantes.rice.RiceCropBlock;
import fr.terresvivantes.rice.RiceModule;
import fr.terresvivantes.rice.logic.RiceLogic;
import fr.terresvivantes.vineyard.GrapeVariety;
import fr.terresvivantes.vineyard.GrapevineBlock;
import fr.terresvivantes.vineyard.VineyardModule;
import fr.terresvivantes.vineyard.logic.VineyardLogic;
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
import java.util.Set;
import java.util.stream.Collectors;

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

    private static final int BREED_PEN_MIN_X = ShowcaseBuilder.BREED_PEN_MIN_X;
    private static final int BREED_PEN_MIN_Z = ShowcaseBuilder.BREED_PEN_MIN_Z;

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
        // Une vache, un mouton et une poule de l'enclos v0.4, plus trois vaches et trois poules des races (v0.9).
        helper.assertTrue(count(fauna, Cow.class) == 4 && count(fauna, Sheep.class) == 1
                        && count(fauna, Chicken.class) == 4,
                "quatre vaches, un mouton et quatre poules attendus (enclos v0.4 et enclos des races v0.9)");
        helper.assertTrue(count(fauna, Duck.class) == 2
                        && fauna.stream().filter(Duck.class::isInstance).map(Duck.class::cast)
                        .filter(Duck::isMale).count() == 1,
                "deux canards attendus dans l'enclos, dont un mâle");
        helper.assertTrue(fauna.stream().allMatch(Mob::isPersistenceRequired),
                "les animaux et insectes de la vitrine doivent être persistants");

        // Section v0.9 : deux vignes mûres sur treillis, rizière 3x3 mûre au centre d'une terre 5x5.
        helper.assertTrue(level.getBlockState(at(origin, ShowcaseBuilder.X_VINE_RED, 1, ShowcaseBuilder.ROW_V09))
                        .is(VineyardModule.GRAPEVINE.get())
                        && level.getBlockState(at(origin, ShowcaseBuilder.X_VINE_RED, 1, ShowcaseBuilder.ROW_V09))
                        .getValue(GrapevineBlock.STAGE) == VineyardLogic.RIPE_STAGE
                        && level.getBlockState(at(origin, ShowcaseBuilder.X_VINE_RED, 1, ShowcaseBuilder.ROW_V09))
                        .getValue(GrapevineBlock.VARIETY) == GrapeVariety.RED,
                "vigne rouge mûre attendue sur la rangée z 42, x 1");
        helper.assertTrue(level.getBlockState(at(origin, ShowcaseBuilder.X_VINE_WHITE, 1, ShowcaseBuilder.ROW_V09))
                        .is(VineyardModule.GRAPEVINE.get())
                        && level.getBlockState(at(origin, ShowcaseBuilder.X_VINE_WHITE, 1, ShowcaseBuilder.ROW_V09))
                        .getValue(GrapevineBlock.VARIETY) == GrapeVariety.WHITE,
                "vigne blanche attendue sur la rangée z 42, x 3");
        BlockState paddyCentre = level.getBlockState(at(origin, ShowcaseBuilder.X_PADDY + 2, 1, ShowcaseBuilder.ROW_V09 + 2));
        helper.assertTrue(paddyCentre.is(RiceModule.RICE_CROP.get()) && paddyCentre.getValue(CropBlock.AGE) == RiceLogic.MAX_AGE
                        && paddyCentre.getValue(RiceCropBlock.WATERLOGGED),
                "riz mûr dans l'eau attendu au centre de la rizière, obtenu " + paddyCentre);
        helper.assertTrue(level.getBlockState(at(origin, ShowcaseBuilder.X_PADDY, 1, ShowcaseBuilder.ROW_V09))
                        .isAir(),
                "le bord de la rizière doit rester libre (riz 3x3 seulement)");
        helper.assertTrue(level.getBlockState(at(origin, BREED_PEN_MIN_X, 1, BREED_PEN_MIN_Z)).is(Blocks.OAK_FENCE)
                        && level.getBlockState(at(origin, BREED_PEN_MIN_X + 2, 1, BREED_PEN_MIN_Z + 2)).isAir(),
                "clôture de l'enclos des races attendue sur le pourtour, intérieur libre");
        Set<Breed> cowBreeds = fauna.stream().filter(Cow.class::isInstance).map(BreedsApi::getBreed)
                .collect(Collectors.toSet());
        Set<Breed> henBreeds = fauna.stream().filter(Chicken.class::isInstance).map(BreedsApi::getBreed)
                .collect(Collectors.toSet());
        helper.assertTrue(cowBreeds.containsAll(List.of(Breed.HOLSTEIN, Breed.HIGHLAND, Breed.CHAROLAIS)),
                "les trois races de vache attendues dans la vitrine, reçu " + cowBreeds);
        helper.assertTrue(henBreeds.containsAll(List.of(Breed.MARANS, Breed.LEGHORN, Breed.SUSSEX)),
                "les trois races de poule attendues dans la vitrine, reçu " + henBreeds);

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

        // Section équipements (z 23 à 31) : récolteuse face au sud avec le coffre derrière, champ de blé mûr devant.
        helper.assertTrue(level.getBlockState(at(origin, 7, 1, 24)).is(AutomationModule.HARVESTER.get())
                        && level.getBlockState(at(origin, 7, 1, 24)).getValue(HorizontalDirectionalBlock.FACING) == Direction.SOUTH
                        && level.getBlockState(at(origin, 7, 1, 23)).is(Blocks.CHEST),
                "récolteuse face au sud et coffre derrière attendus en (7,1,24) et (7,1,23)");
        for (int x = 5; x <= 9; x++) {
            helper.assertTrue(level.getBlockState(at(origin, x, 1, 25)).is(Blocks.WHEAT)
                            && level.getBlockState(at(origin, x, 1, 25)).getValue(CropBlock.AGE) == CropBlock.MAX_AGE,
                    "blé mûr attendu en (" + x + ",1,25) devant la récolteuse");
        }
        helper.assertTrue(level.getBlockState(at(origin, 13, 1, 24)).is(AutomationModule.PLANTER.get())
                        && level.getBlockEntity(at(origin, 13, 1, 24)) instanceof PlanterBlockEntity planter
                        && planter.container().getItem(0).is(Items.WHEAT_SEEDS),
                "planteuse avec graines de blé attendue en (13,1,24)");
        helper.assertTrue(level.getBlockState(at(origin, 1, 1, 24)).is(StorageModule.CRATE.get())
                        && level.getBlockEntity(at(origin, 1, 1, 24)) instanceof CrateBlockEntity crate
                        && crate.item() == CropModule.productItem("tomato") && crate.count() == crate.capacity(),
                "caisse pleine de tomates attendue en (1,1,24)");
        for (int y = 1; y <= 3; y++) {
            helper.assertTrue(level.getBlockState(at(origin, 3, y, 27)).is(StorageModule.SILO.get()),
                    "silo attendu en (3," + y + ",27) : colonne de 3 blocs");
        }
        helper.assertTrue(level.getBlockState(at(origin, 10, 1, 27)).is(PestsModule.SCARECROW.get()),
                "épouvantail attendu en (10,1,27)");
        helper.assertTrue(count(fauna, Crow.class) == 2, "deux corbeaux attendus dans la vitrine");

        // Section économie (z 34) : tableau d'annonces, étal avec des produits, bureau de la guilde.
        helper.assertTrue(level.getBlockState(at(origin, 2, 1, 34)).is(MarketModule.MARKET_BOARD.get()),
                "tableau d'annonces attendu en (2,1,34)");
        helper.assertTrue(level.getBlockState(at(origin, 6, 1, 34)).is(MarketModule.MARKET_STALL.get())
                        && level.getBlockEntity(at(origin, 6, 1, 34)) instanceof MarketStallBlockEntity stall
                        && stall.getItem(0).is(CropModule.productItem("tomato")) && !stall.getItem(0).isEmpty()
                        && stall.getItem(1).is(CropModule.productItem("lettuce")) && !stall.getItem(1).isEmpty(),
                "étal avec tomates et laitues attendu en (6,1,34)");
        helper.assertTrue(level.getBlockState(at(origin, 12, 1, 34)).is(GuildModule.GUILD_DESK.get()),
                "bureau de la guilde attendu en (12,1,34)");
        helper.assertTrue(count(fauna, Villager.class) == 1
                        && fauna.stream().filter(Villager.class::isInstance).map(Villager.class::cast)
                        .allMatch(v -> v.getVillagerData().getProfession() == GuildModule.GUILD_MERCHANT.get()),
                "un villageois marchand de la guilde attendu dans la vitrine");

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
