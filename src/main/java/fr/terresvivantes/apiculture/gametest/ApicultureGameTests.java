package fr.terresvivantes.apiculture.gametest;

import fr.terresvivantes.TerresVivantes;
import fr.terresvivantes.apiculture.ApiaryBlockEntity;
import fr.terresvivantes.apiculture.ApicultureModule;
import fr.terresvivantes.apiculture.logic.ApiaryLogic;
import fr.terresvivantes.apiculture.logic.HoneyType;
import fr.terresvivantes.flora.FloraModule;
import fr.terresvivantes.pollination.PollinationApi;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Tests de jeu du module apiculture. Gabarit orchard_arena (air 11 x 12 x 11).
 *
 * <p>Les fleurs de trèfle sont les vrais blocs du module flore ({@code terresvivantes:clover}), déclarés comme
 * source de nectar trèfle par {@code TerresVivantes} à l'initialisation.
 */
@GameTestHolder(TerresVivantes.MODID)
@PrefixGameTestTemplate(false)
public final class ApicultureGameTests {

    private static final String TEMPLATE = "orchard_arena";
    private static final BlockPos APIARY = new BlockPos(5, 2, 5);
    private static final BlockPos EXTRACTOR = new BlockPos(7, 2, 5);
    /** Fleurs à 2 à 3 blocs de l'apiaire, posées sur de la terre (y = 1). */
    private static final BlockPos[] FLOWERS = {
            new BlockPos(3, 2, 3), new BlockPos(7, 2, 3), new BlockPos(3, 2, 7),
            new BlockPos(7, 2, 7), new BlockPos(5, 2, 2), new BlockPos(5, 2, 8)
    };
    /** Cycles maximum avant de vérifier la production : largement au-delà de la durée nominale. */
    private static final int MAX_CYCLES = 400;

    private ApicultureGameTests() {
    }

    /**
     * Apiaire peuplé (100), six fleurs de trèfle autour et un cadre vide : après quelques cycles, le cadre devient
     * un cadre de miel de type trèfle.
     */
    @GameTest(template = TEMPLATE)
    public static void apiaryWithCloverFlowersMakesCloverFrame(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        placeFlowerField(helper, FloraModule.flower("clover"));
        helper.setBlock(APIARY.below(), Blocks.DIRT);
        helper.setBlock(APIARY, ApicultureModule.APIARY.get());
        ApiaryBlockEntity apiary = apiaryAt(helper, APIARY);
        apiary.setPopulation(ApiaryLogic.MAX_POPULATION);
        helper.assertTrue(apiary.insertEmptyFrame(), "un cadre vide doit entrer dans l'apiaire");

        for (int cycle = 0; cycle < MAX_CYCLES && !isHoneyFrame(apiary); cycle++) {
            apiary.cycle(level, 1f);
        }

        ItemStack frame = apiary.frame(0);
        helper.assertTrue(frame.is(ApicultureModule.HONEY_FRAME.get()),
                "le cadre doit devenir un cadre de miel après " + MAX_CYCLES + " cycles");
        helper.assertTrue(frame.get(ApicultureModule.HONEY_TYPE.get()) == HoneyType.CLOVER,
                "le type de miel doit être trèfle, obtenu " + frame.get(ApicultureModule.HONEY_TYPE.get()));
        helper.succeed();
    }

    /** Extracteur : un cadre de miel de trèfle et une bouteille en verre donnent du miel de trèfle et un cadre vide. */
    @GameTest(template = TEMPLATE)
    public static void honeyExtractorTurnsCloverFrameIntoBottle(GameTestHelper helper) {
        helper.setBlock(EXTRACTOR, ApicultureModule.HONEY_EXTRACTOR.get());
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(EXTRACTOR);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        // Emplacement 1 : l'emplacement 0 est celui de la main tenue, écrasé par setItemInHand.
        player.getInventory().setItem(1, new ItemStack(Items.GLASS_BOTTLE));

        ItemStack frame = new ItemStack(ApicultureModule.HONEY_FRAME.get());
        frame.set(ApicultureModule.HONEY_TYPE.get(), HoneyType.CLOVER);
        player.setItemInHand(InteractionHand.MAIN_HAND, frame);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        level.getBlockState(pos).useItemOn(player.getItemInHand(InteractionHand.MAIN_HAND), level, player,
                InteractionHand.MAIN_HAND, hit);

        helper.assertTrue(player.getInventory().countItem(ApicultureModule.CLOVER_HONEY.get()) == 1,
                "une bouteille de miel de trèfle doit être obtenue");
        helper.assertTrue(player.getInventory().countItem(ApicultureModule.EMPTY_FRAME.get()) == 1,
                "un cadre vide doit être rendu");
        helper.assertTrue(player.getInventory().countItem(Items.GLASS_BOTTLE) == 0,
                "la bouteille en verre doit être consommée");
        helper.succeed();
    }

    /** Un apiaire peuplé augmente le score de pollinisation de sa position, par rapport au même apiaire vide. */
    @GameTest(template = TEMPLATE)
    public static void populatedApiaryRaisesPollinationScore(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        helper.setBlock(APIARY.below(), Blocks.DIRT);
        helper.setBlock(APIARY, ApicultureModule.APIARY.get());
        BlockPos center = helper.absolutePos(new BlockPos(5, 2, 5));
        ApiaryBlockEntity apiary = apiaryAt(helper, APIARY);

        apiary.setPopulation(0);
        float empty = PollinationApi.scoreUncached(level, center);
        apiary.setPopulation(40);
        float populated = PollinationApi.scoreUncached(level, center);

        helper.assertTrue(populated > empty,
                "le score doit augmenter avec un apiaire peuplé : vide " + empty + ", peuplé " + populated);
        helper.succeed();
    }

    /** Pose de la terre et des fleurs autour de l'apiaire, chaque fleur sur sa propre terre. */
    private static void placeFlowerField(GameTestHelper helper, Block flower) {
        for (BlockPos pos : FLOWERS) {
            helper.setBlock(pos.below(), Blocks.DIRT);
            helper.setBlock(pos, flower);
        }
    }

    private static ApiaryBlockEntity apiaryAt(GameTestHelper helper, BlockPos relative) {
        return (ApiaryBlockEntity) helper.getBlockEntity(relative);
    }

    private static boolean isHoneyFrame(ApiaryBlockEntity apiary) {
        return apiary.frame(0).is(ApicultureModule.HONEY_FRAME.get());
    }
}
