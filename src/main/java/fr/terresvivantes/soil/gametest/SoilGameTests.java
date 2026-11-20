package fr.terresvivantes.soil.gametest;

import fr.terresvivantes.TerresVivantes;
import fr.terresvivantes.compost.CompostModule;
import fr.terresvivantes.soil.SoilApi;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Tests de jeu du module soil. */
@GameTestHolder(TerresVivantes.MODID)
@PrefixGameTestTemplate(false)
public final class SoilGameTests {

    private SoilGameTests() {
    }

    /** Un engrais compost sur une terre labouree augmente son azote de 15. */
    @GameTest(template = "")
    public static void fertilizerRaisesNitrogen(GameTestHelper helper) {
        BlockPos relative = new BlockPos(1, 2, 1);
        helper.setBlock(relative, Blocks.FARMLAND);
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(relative);

        int before = SoilApi.get(level, pos).n();
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack stack = new ItemStack(CompostModule.COMPOST.get());
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        CompostModule.COMPOST.get().useOn(new UseOnContext(level, player, InteractionHand.MAIN_HAND, stack, hit));

        int after = SoilApi.get(level, pos).n();
        helper.assertTrue(after == before + 15,
                "azote attendu " + (before + 15) + ", obtenu " + after);
        helper.succeed();
    }
}
