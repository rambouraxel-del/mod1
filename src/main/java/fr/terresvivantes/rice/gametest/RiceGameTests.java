package fr.terresvivantes.rice.gametest;

import fr.terresvivantes.TerresVivantes;
import fr.terresvivantes.crop.CropApi;
import fr.terresvivantes.processing.ProcessingRecipe;
import fr.terresvivantes.rice.RiceApi;
import fr.terresvivantes.rice.RiceCropBlock;
import fr.terresvivantes.rice.RiceModule;
import fr.terresvivantes.rice.logic.RiceLogic;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Optional;
import fr.terresvivantes.crop.CropGrowthHooks;
import fr.terresvivantes.health.HealthApi;
import fr.terresvivantes.health.logic.Disease;

/** Tests de jeu de la rizière : plantation sur l'eau, croissance, récolte, casse, recette de meule. */
@GameTestHolder(TerresVivantes.MODID)
@PrefixGameTestTemplate(false)
public final class RiceGameTests {

    /** Terre (dirt) au centre de la rizière; le plant est juste au-dessus, dans l'eau. */
    private static final BlockPos SOIL = new BlockPos(1, 1, 1);
    private static final BlockPos PLANT = SOIL.above();

    private RiceGameTests() {
    }

    /** Plantation sur une source d'eau au-dessus d'une terre : le riz est waterlogged et l'eau est visible. */
    @GameTest(template = "crop_arena")
    public static void plantGoesOnWaterAboveSoilAndIsWaterlogged(GameTestHelper helper) {
        buildPaddy(helper);

        helper.assertTrue(RiceApi.plant(helper.getLevel(), helper.absolutePos(SOIL)), "plantation refusée");
        BlockState state = helper.getBlockState(PLANT);
        helper.assertTrue(state.getBlock() instanceof RiceCropBlock, "pas de riz à la place de l'eau : " + state);
        helper.assertTrue(state.getValue(RiceCropBlock.WATERLOGGED), "riz non waterlogged");
        helper.assertTrue(state.getFluidState().is(FluidTags.WATER), "l'eau doit rester visible dans la rizière");
        helper.succeed();
    }

    /** Une terre sans eau au-dessus (pas de source) refuse la plantation. */
    @GameTest(template = "crop_arena")
    public static void plantRefusedWithoutWaterAbove(GameTestHelper helper) {
        buildPaddy(helper);
        helper.setBlock(SOIL.above(), Blocks.AIR);

        helper.assertTrue(!RiceApi.plant(helper.getLevel(), helper.absolutePos(SOIL)),
                "plantation sans eau ne doit pas réussir");
        helper.assertTrue(helper.getBlockState(PLANT).getBlock() != RiceModule.RICE_CROP.get(),
                "aucun riz attendu sans eau");
        helper.succeed();
    }

    /** En été, des randomTicks forcés font mûrir le plant (7 stades). */
    @GameTest(template = "crop_arena")
    public static void summerRipensPlantByRandomTicks(GameTestHelper helper) {
        buildPaddy(helper);
        ServerLevel level = helper.getLevel();
        setSeason(helper, "summer");
        helper.assertTrue(RiceApi.plant(level, helper.absolutePos(SOIL)), "plantation refusée");

        BlockPos plant = helper.absolutePos(PLANT);
        RandomSource random = RandomSource.create(7L);
        for (int tick = 0; tick < 20000 && age(level, plant) < RiceLogic.MAX_AGE; tick++) {
            if (!(level.getBlockState(plant).getBlock() instanceof RiceCropBlock)) {
                break;
            }
            level.getBlockState(plant).randomTick(level, plant, random);
        }
        helper.assertTrue(age(level, plant) == RiceLogic.MAX_AGE,
                "riz non mûr après les randomTicks d'été, âge " + age(level, plant));
        helper.succeed();
    }

    /** Récolte à maturité : 2 à 3 panicules et un plant, la rizière est replantée à l'âge 0 dans l'eau. */
    @GameTest(template = "crop_arena")
    public static void matureRiceYieldsPaniclesAndReplants(GameTestHelper helper) {
        buildPaddy(helper);
        ServerLevel level = helper.getLevel();
        helper.setBlock(PLANT, RiceModule.RICE_CROP.get().defaultBlockState()
                .setValue(CropBlock.AGE, RiceLogic.MAX_AGE));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);

        List<ItemStack> drops = CropApi.harvestAndReplant(level, helper.absolutePos(PLANT), player);

        int panicles = count(drops, RiceModule.RICE_PANICLE.get());
        int seedlings = count(drops, RiceModule.RICE_SEEDLING.get());
        helper.assertTrue(panicles >= RiceLogic.PANICLES_MIN && panicles <= RiceLogic.PANICLES_MAX,
                "2 à 3 panicules attendues, obtenu " + panicles);
        helper.assertTrue(seedlings >= 1, "un plant de riz attendu à la récolte, obtenu " + seedlings);
        BlockState replanted = helper.getBlockState(PLANT);
        helper.assertTrue(replanted.getBlock() instanceof RiceCropBlock && replanted.getValue(CropBlock.AGE) == 0
                        && replanted.getValue(RiceCropBlock.WATERLOGGED),
                "riz attendu replanté à l'âge 0 dans l'eau, obtenu " + replanted);
        helper.succeed();
    }

    /** Eau retirée autour du plant (plus de voisin aquatique) : le plant casse et rend un plant de riz. */
    @GameTest(template = "crop_arena")
    public static void plantBreaksWhenWaterAroundIsRemoved(GameTestHelper helper) {
        buildPaddy(helper);
        ServerLevel level = helper.getLevel();
        helper.assertTrue(RiceApi.plant(level, helper.absolutePos(SOIL)), "plantation refusée");
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx != 0 || dz != 0) {
                    helper.setBlock(PLANT.offset(dx, 0, dz), Blocks.AIR);
                }
            }
        }
        BlockPos plant = helper.absolutePos(PLANT);
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(!(level.getBlockState(plant).getBlock() instanceof RiceCropBlock),
                    "le riz doit casser sans eau autour");
            boolean seedling = level.getEntitiesOfClass(ItemEntity.class, new AABB(plant).inflate(3)).stream()
                    .anyMatch(item -> item.getItem().is(RiceModule.RICE_SEEDLING.get()));
            helper.assertTrue(seedling, "la casse doit rendre un plant de riz");
            helper.succeed();
        });
    }

    /** Terre remplacée par de l'eau : le plant n'a plus de sol admis et casse. */
    @GameTest(template = "crop_arena")
    public static void plantBreaksWhenWaterReplacesSoil(GameTestHelper helper) {
        buildPaddy(helper);
        ServerLevel level = helper.getLevel();
        helper.assertTrue(RiceApi.plant(level, helper.absolutePos(SOIL)), "plantation refusée");
        helper.setBlock(SOIL, Blocks.WATER);
        BlockPos plant = helper.absolutePos(PLANT);
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(!(level.getBlockState(plant).getBlock() instanceof RiceCropBlock),
                    "le riz doit casser si le sol sous lui est de l'eau");
            helper.succeed();
        });
    }

    /** La recette de meule est chargée : une panicule donne 2 riz. */
    @GameTest(template = "crop_arena")
    public static void millstoneRecipeTurnsPaniclesIntoRice(GameTestHelper helper) {
        RecipeManager manager = helper.getLevel().getServer().getRecipeManager();
        Optional<ProcessingRecipe> recipe = manager
                .byKey(ResourceLocation.fromNamespaceAndPath(TerresVivantes.MODID, "rice_millstone"))
                .map(RecipeHolder::value)
                .filter(ProcessingRecipe.class::isInstance)
                .map(ProcessingRecipe.class::cast);
        helper.assertTrue(recipe.isPresent(), "recette rice_millstone absente");
        helper.assertTrue("millstone".equals(recipe.get().machine()), "machine attendue : meule");
        helper.assertTrue(recipe.get().result().is(RiceModule.RICE.get()) && recipe.get().result().getCount() == 2,
                "2 riz attendus, obtenu " + recipe.get().result());
        helper.succeed();
    }

    /** Rizière de 3x3 : terre au niveau 1, eau au niveau 2 (la terre SOIL est au centre). */
    private static void buildPaddy(GameTestHelper helper) {
        for (int x = 0; x <= 2; x++) {
            for (int z = 0; z <= 2; z++) {
                helper.setBlock(new BlockPos(x, 1, z), Blocks.DIRT);
                helper.setBlock(new BlockPos(x, 2, z), Blocks.WATER);
            }
        }
    }

    private static void setSeason(GameTestHelper helper, String season) {
        MinecraftServer server = helper.getLevel().getServer();
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tv season set " + season + " 1");
    }

    private static int age(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.getBlock() instanceof RiceCropBlock ? state.getValue(CropBlock.AGE) : -1;
    }

    private static int count(List<ItemStack> stacks, Item item) {
        return stacks.stream().filter(stack -> stack.is(item)).mapToInt(ItemStack::getCount).sum();
    }

    /**
     * Branchements v0.9 : la plantation notifie les hooks de plantation, et une rizière malade (mildiou, via
     * CropGrowthHooks.applyModifiers) ne croît qu'à ×0.3.
     */
    @GameTest(template = "crop_arena")
    public static void plantNotifiesHooksAndDiseaseSlowsRice(GameTestHelper helper) {
        buildPaddy(helper);
        ServerLevel level = helper.getLevel();
        BlockPos plant = helper.absolutePos(PLANT);
        CropGrowthHooks.registerPlantListener((l, pos, state) -> {
            if (pos.equals(plant)) {
                RICE_PLANTED.set(true);
            }
        });
        helper.assertTrue(RiceApi.plant(level, helper.absolutePos(SOIL)), "plantation refusée");
        helper.assertTrue(RICE_PLANTED.get(), "le plant de riz doit notifier les PlantListener");

        BlockState state = level.getBlockState(plant);
        float healthy = CropGrowthHooks.applyModifiers(level, plant, state, null);
        helper.assertTrue(Math.abs(healthy - 1.0f) < 1e-4f, "riz sain attendu à ×1, reçu " + healthy);
        HealthApi.infect(level, plant, Disease.MILDEW);
        float sick = CropGrowthHooks.applyModifiers(level, plant, state, null);
        helper.assertTrue(Math.abs(sick - 0.3f) < 1e-4f, "riz malade attendu à ×0.3, reçu " + sick);
        helper.succeed();
    }

    private static final java.util.concurrent.atomic.AtomicBoolean RICE_PLANTED = new java.util.concurrent.atomic.AtomicBoolean();
}
