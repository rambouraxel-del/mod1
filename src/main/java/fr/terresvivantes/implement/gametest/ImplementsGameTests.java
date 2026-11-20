package fr.terresvivantes.implement.gametest;

import fr.terresvivantes.TerresVivantes;
import fr.terresvivantes.implement.ImplementsModule;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Tests de jeu du module Implements. */
@GameTestHolder(TerresVivantes.MODID)
@PrefixGameTestTemplate(false)
public final class ImplementsGameTests {

    /** Structure vide 5x5x5 (data/terresvivantes/structure/implement_test.nbt). */
    private static final String TEMPLATE = "implement_test";

    private ImplementsGameTests() {
    }

    /** Grelinette sur de l'herbe : 9 blocs labour en 3x3, une utilisation par bloc. */
    @GameTest(template = TEMPLATE)
    public static void broadforkTillsThreeByThreeGrass(GameTestHelper helper) {
        BlockPos center = new BlockPos(2, 1, 2);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                helper.setBlock(center.offset(dx, 0, dz), Blocks.GRASS_BLOCK);
            }
        }

        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack fork = new ItemStack(ImplementsModule.BROADFORK.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, fork);

        fork.useOn(useContext(helper, player, fork, center));

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                helper.assertBlockPresent(Blocks.FARMLAND, center.offset(dx, 0, dz));
            }
        }
        helper.assertTrue(fork.getDamageValue() == 9, "La grelinette doit perdre une utilisation par bloc laboure");
        helper.succeed();
    }

    /** Semoir sur 9 terres labourees en 3x3 : 9 ble plantes, 9 graines consommees, 9 durabilite. */
    @GameTest(template = TEMPLATE)
    public static void seederPlantsThreeByThreeAndConsumesSeeds(GameTestHelper helper) {
        BlockPos center = new BlockPos(2, 1, 2);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                helper.setBlock(center.offset(dx, 0, dz), Blocks.FARMLAND);
            }
        }

        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack seeder = new ItemStack(ImplementsModule.SEEDER.get());
        ItemStack seeds = new ItemStack(Items.WHEAT_SEEDS, 12);
        player.setItemInHand(InteractionHand.MAIN_HAND, seeder);
        player.setItemInHand(InteractionHand.OFF_HAND, seeds);

        seeder.useOn(useContext(helper, player, seeder, center));

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                helper.assertBlockPresent(Blocks.WHEAT, center.offset(dx, 1, dz));
            }
        }
        helper.assertTrue(seeds.getCount() == 3, "Le semoir doit consommer 9 graines, il en reste "
                + seeds.getCount());
        helper.assertTrue(seeder.getDamageValue() == 9, "Le semoir doit perdre une utilisation par case");
        helper.succeed();
    }

    /** Faucille en diamant sur un champ de ble mur 5x5 : 25 cultures recoltees et remises a l'age 0. */
    @GameTest(template = TEMPLATE)
    public static void diamondSickleHarvestsFiveByFive(GameTestHelper helper) {
        BlockPos center = new BlockPos(2, 2, 2);
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                BlockPos soil = center.offset(dx, -1, dz);
                BlockPos crop = center.offset(dx, 0, dz);
                helper.setBlock(soil, Blocks.FARMLAND);
                helper.setBlock(crop, Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, 7));
            }
        }

        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack sickle = new ItemStack(ImplementsModule.DIAMOND_SICKLE.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, sickle);

        sickle.useOn(useContext(helper, player, sickle, center));

        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                helper.assertBlockProperty(center.offset(dx, 0, dz), CropBlock.AGE, 0);
            }
        }
        helper.assertTrue(sickle.getDamageValue() == 25, "La faucille doit perdre une utilisation par culture");
        helper.succeed();
    }

    private static UseOnContext useContext(GameTestHelper helper, Player player, ItemStack stack, BlockPos rel) {
        BlockPos abs = helper.absolutePos(rel);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false);
        return new UseOnContext(helper.getLevel(), player, InteractionHand.MAIN_HAND, stack, hit);
    }
}
