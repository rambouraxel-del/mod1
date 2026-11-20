package fr.terresvivantes.crop;

import fr.terresvivantes.TerresVivantes;
import fr.terresvivantes.crop.logic.CropLogic;
import fr.terresvivantes.soil.SoilApi;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

/** Tests de jeu des cultures : récolte à maturité, pas de récolte avant maturité. */
@GameTestHolder(TerresVivantes.MODID)
@PrefixGameTestTemplate(false)
public final class CropGameTests {

    private static final BlockPos SOIL = new BlockPos(1, 1, 1);
    private static final BlockPos CROP = SOIL.above();
    /** Positions absolues notifiées par le PlantListener de ce test. */
    private static final List<BlockPos> PLANTED = new ArrayList<>();

    private CropGameTests() {
    }

    @GameTest(template = "crop_arena")
    public static void matureTomatoIsHarvestedAndReplanted(GameTestHelper helper) {
        helper.setBlock(SOIL, Blocks.FARMLAND.defaultBlockState());
        helper.setBlock(CROP, CropModule.cropBlock("tomato").defaultBlockState()
                .setValue(CropBlock.AGE, CropLogic.MAX_AGE));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);

        List<ItemStack> drops = CropApi.harvestAndReplant(helper.getLevel(), helper.absolutePos(CROP), player);

        int tomatoes = drops.stream()
                .filter(stack -> stack.is(CropModule.productItem("tomato")))
                .mapToInt(ItemStack::getCount)
                .sum();
        helper.assertTrue(tomatoes >= 2, "au moins 2 tomates attendues, obtenu " + tomatoes);
        helper.assertTrue(helper.getBlockState(CROP).getValue(CropBlock.AGE) == 0,
                "la culture doit être replantée à l'âge 0");
        helper.succeed();
    }

    @GameTest(template = "crop_arena")
    public static void immatureCropIsNotHarvested(GameTestHelper helper) {
        helper.setBlock(SOIL, Blocks.FARMLAND.defaultBlockState());
        helper.setBlock(CROP, CropModule.cropBlock("lettuce").defaultBlockState()
                .setValue(CropBlock.AGE, 3));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);

        List<ItemStack> drops = CropApi.harvestAndReplant(helper.getLevel(), helper.absolutePos(CROP), player);

        helper.assertTrue(drops.isEmpty(), "aucun drop attendu avant maturité");
        helper.assertTrue(helper.getBlockState(CROP).getValue(CropBlock.AGE) == 3,
                "la culture immature ne doit pas changer d'âge");
        helper.succeed();
    }

    /** Trois récoltes successives de la même culture sur la même terre : compteur de répétition à 2. */
    @GameTest(template = "crop_arena")
    public static void replantingSameCropThreeTimesGivesRepeatTwo(GameTestHelper helper) {
        helper.setBlock(SOIL, Blocks.FARMLAND.defaultBlockState());
        ServerLevel level = helper.getLevel();
        BlockPos soil = helper.absolutePos(SOIL);
        BlockPos crop = helper.absolutePos(CROP);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);

        for (int harvest = 0; harvest < 3; harvest++) {
            helper.setBlock(CROP, CropModule.cropBlock("tomato").defaultBlockState()
                    .setValue(CropBlock.AGE, CropLogic.MAX_AGE));
            CropApi.harvestAndReplant(level, crop, player);
        }

        helper.assertTrue("tomato".equals(SoilApi.getLastCrop(level, soil)),
                "la dernière culture doit être la tomate, obtenu " + SoilApi.getLastCrop(level, soil));
        int repeat = SoilApi.getRepeatCount(level, soil);
        helper.assertTrue(repeat == 2, "répétition attendue 2 après 3 récoltes, obtenu " + repeat);
        helper.succeed();
    }

    /** Une graine posée sur une terre (culture du mod ou vanilla) notifie le PlantListener à la bonne position. */
    @GameTest(template = "crop_arena")
    public static void seedPlacementNotifiesPlantListener(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        CropGrowthHooks.registerPlantListener((l, pos, state) -> PLANTED.add(pos.immutable()));
        PLANTED.clear();
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);

        helper.setBlock(SOIL, Blocks.FARMLAND.defaultBlockState());
        useSeed(level, player, helper.absolutePos(SOIL), CropModule.seedItem("tomato"));
        helper.assertTrue(PLANTED.contains(helper.absolutePos(CROP)),
                "plantation de tomate non notifiée, positions : " + PLANTED);

        // Même terre, après retrait de la tomate : graine de blé vanilla.
        helper.setBlock(CROP, Blocks.AIR);
        PLANTED.clear();
        useSeed(level, player, helper.absolutePos(SOIL), Items.WHEAT_SEEDS);
        helper.assertTrue(PLANTED.contains(helper.absolutePos(CROP)),
                "plantation de blé vanilla non notifiée, bloc : " + helper.getBlockState(CROP)
                        + ", positions : " + PLANTED);
        helper.succeed();
    }

    private static void useSeed(ServerLevel level, Player player, BlockPos soil, Item seed) {
        ItemStack stack = new ItemStack(seed);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(soil), Direction.UP, soil, false);
        stack.useOn(new UseOnContext(level, player, InteractionHand.MAIN_HAND, stack, hit));
    }
}
