package fr.terresvivantes.compost.gametest;

import fr.terresvivantes.TerresVivantes;
import fr.terresvivantes.compost.CompostBinBlock;
import fr.terresvivantes.compost.CompostModule;
import fr.terresvivantes.compost.logic.CompostLogic;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Tests de jeu du module compost. */
@GameTestHolder(TerresVivantes.MODID)
@PrefixGameTestTemplate(false)
public final class CompostGameTests {

    private CompostGameTests() {
    }

    /** Une matiere organique (ble) remplit le bac d'un niveau et est consommee. */
    @GameTest(template = "")
    public static void binAcceptsOrganicMatter(GameTestHelper helper) {
        BlockPos relative = new BlockPos(1, 2, 1);
        helper.setBlock(relative, CompostModule.COMPOST_BIN.get());
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(relative);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack wheat = new ItemStack(Items.WHEAT, 4);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);

        level.getBlockState(pos).useItemOn(wheat, level, player, InteractionHand.MAIN_HAND, hit);

        int levelAfter = level.getBlockState(pos).getValue(CompostBinBlock.LEVEL);
        helper.assertTrue(levelAfter == 1, "niveau attendu 1, obtenu " + levelAfter);
        helper.assertTrue(wheat.getCount() == 3, "une unite de ble doit etre consommee");
        helper.succeed();
    }

    /** Un bac plein, force a maturer par randomTick, donne 2 a 3 compost a la recolte. */
    @GameTest(template = "")
    public static void fullBinMaturesAndGivesCompost(GameTestHelper helper) {
        BlockPos relative = new BlockPos(1, 2, 1);
        helper.setBlock(relative, CompostModule.COMPOST_BIN.get().defaultBlockState()
                .setValue(CompostBinBlock.LEVEL, CompostLogic.MAX_LEVEL));
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(relative);

        RandomSource random = RandomSource.create(12345L);
        for (int tick = 0; tick < 5000 && !level.getBlockState(pos).getValue(CompostBinBlock.READY); tick++) {
            level.getBlockState(pos).randomTick(level, pos, random);
        }
        helper.assertTrue(level.getBlockState(pos).getValue(CompostBinBlock.READY), "bac non maturé");

        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        level.getBlockState(pos).useWithoutItem(level, player, hit);

        int compost = player.getInventory().countItem(CompostModule.COMPOST.get());
        helper.assertTrue(compost >= 2 && compost <= 3, "compost attendu 2 a 3, obtenu " + compost);
        helper.assertTrue(!level.getBlockState(pos).getValue(CompostBinBlock.READY), "bac non vidé");
        helper.succeed();
    }
}
