package fr.terresvivantes.tools.gametest;

import fr.terresvivantes.TerresVivantes;
import fr.terresvivantes.tools.ToolsModule;
import fr.terresvivantes.tools.logic.ToolsLogic;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Tests de jeu du module Tools. */
@GameTestHolder(TerresVivantes.MODID)
@PrefixGameTestTemplate(false)
public final class ToolsGameTests {

    /** Structure vide 5x5x5 (data/terresvivantes/structure/tools_test.nbt). */
    private static final String TEMPLATE = "tools_test";

    private ToolsGameTests() {
    }

    /** Arrosoir plein sur une terre seche : humidite 7 sur la zone, une utilisation consommee. */
    @GameTest(template = TEMPLATE)
    public static void wateringCanSetsFarmlandMoisture(GameTestHelper helper) {
        BlockPos rel = new BlockPos(2, 1, 2);
        helper.setBlock(rel, Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, 0));

        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack can = new ItemStack(ToolsModule.WATERING_CAN_FILLED.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, can);

        UseOnContext context = useContext(helper, player, can, rel);
        can.useOn(context);

        helper.assertBlockProperty(rel, FarmBlock.MOISTURE, ToolsLogic.WATERED_MOISTURE);
        helper.assertTrue(can.getDamageValue() == 1, "L'arrosoir doit perdre une utilisation");
        helper.succeed();
    }

    /** Faucille sur un ble mur (age 7) : recolte, remise a l'age 0, une utilisation consommee. */
    @GameTest(template = TEMPLATE)
    public static void sickleHarvestsAndReplantsWheat(GameTestHelper helper) {
        BlockPos crop = new BlockPos(2, 2, 2);
        helper.setBlock(crop.below(), Blocks.FARMLAND);
        helper.setBlock(crop, Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, 7));

        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack sickle = new ItemStack(ToolsModule.SICKLE.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, sickle);

        UseOnContext context = useContext(helper, player, sickle, crop);
        sickle.useOn(context);

        helper.assertBlockProperty(crop, CropBlock.AGE, 0);
        helper.assertTrue(sickle.getDamageValue() == 1, "La faucille doit perdre une utilisation par culture");
        helper.succeed();
    }

    private static UseOnContext useContext(GameTestHelper helper, Player player, ItemStack stack, BlockPos rel) {
        BlockPos abs = helper.absolutePos(rel);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false);
        return new UseOnContext(helper.getLevel(), player, InteractionHand.MAIN_HAND, stack, hit);
    }
}
