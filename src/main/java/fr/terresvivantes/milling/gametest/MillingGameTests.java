package fr.terresvivantes.milling.gametest;

import fr.terresvivantes.TerresVivantes;
import fr.terresvivantes.core.quality.QualityModule;
import fr.terresvivantes.milling.MillingModule;
import fr.terresvivantes.milling.MillstoneBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Tests de jeu de la meule (machine de référence de la transformation). Gabarit {@code orchard_arena}. */
@GameTestHolder(TerresVivantes.MODID)
@PrefixGameTestTemplate(false)
public final class MillingGameTests {

    private static final String TEMPLATE = "orchard_arena";
    private static final String BATCH = "processing";
    /** Durée de mouture attendue (100 ticks) avec une marge; le test vérifie la farine à 120 ticks. */
    private static final int GRIND_CHECK_TICKS = 120;
    private static final BlockPos MILL = new BlockPos(5, 2, 5);
    private static final BlockPos HOPPER = new BlockPos(5, 3, 5);

    private MillingGameTests() {
    }

    private static MillstoneBlockEntity placeMill(GameTestHelper helper) {
        helper.setBlock(MILL, MillingModule.MILLSTONE.get());
        return helper.getBlockEntity(MILL);
    }

    /** Un blé dans la meule : la farine est prête en 120 ticks au plus. */
    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 200)
    public static void millstoneGrindsWheatIntoFlourWithin120Ticks(GameTestHelper helper) {
        MillstoneBlockEntity mill = placeMill(helper);
        mill.inventory().setStackInSlot(0, new ItemStack(Items.WHEAT, 1));
        helper.runAfterDelay(GRIND_CHECK_TICKS, () -> {
            ItemStack flour = mill.inventory().getStackInSlot(MillingModule.MILLSTONE_TYPE.outputSlot());
            helper.assertTrue(flour.is(MillingModule.WHEAT_FLOUR.get()) && flour.getCount() == 1,
                    "farine attendue après " + GRIND_CHECK_TICKS + " ticks, obtenu " + flour);
            helper.succeed();
        });
    }

    /** Blé de qualité 3 : la farine garde la qualité 3 (héritage minimum d'une seule entrée). */
    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 200)
    public static void millstoneKeepsTheQualityOfItsWheat(GameTestHelper helper) {
        MillstoneBlockEntity mill = placeMill(helper);
        ItemStack wheat = new ItemStack(Items.WHEAT, 1);
        QualityModule.setQuality(wheat, 3);
        mill.inventory().setStackInSlot(0, wheat);
        helper.runAfterDelay(GRIND_CHECK_TICKS, () -> {
            ItemStack flour = mill.inventory().getStackInSlot(MillingModule.MILLSTONE_TYPE.outputSlot());
            helper.assertTrue(flour.is(MillingModule.WHEAT_FLOUR.get()), "farine attendue, obtenu " + flour);
            helper.assertTrue(QualityModule.qualityOf(flour) == 3,
                    "qualité attendue 3, obtenue " + QualityModule.qualityOf(flour));
            helper.succeed();
        });
    }

    /** Blé sans qualité : la farine n'a pas de composant de qualité. */
    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 200)
    public static void millstoneGivesNoQualityFromPlainWheat(GameTestHelper helper) {
        MillstoneBlockEntity mill = placeMill(helper);
        mill.inventory().setStackInSlot(0, new ItemStack(Items.WHEAT, 1));
        helper.runAfterDelay(GRIND_CHECK_TICKS, () -> {
            ItemStack flour = mill.inventory().getStackInSlot(MillingModule.MILLSTONE_TYPE.outputSlot());
            helper.assertTrue(flour.is(MillingModule.WHEAT_FLOUR.get()), "farine attendue, obtenu " + flour);
            helper.assertTrue(QualityModule.qualityOf(flour) == 0, "aucune qualité attendue sur la farine");
            helper.succeed();
        });
    }

    /** Trémie posée au-dessus de la meule (face basse vers la meule) : le blé entre dans l'entrée. */
    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 200)
    public static void hopperAboveMillstoneFeedsItsInput(GameTestHelper helper) {
        MillstoneBlockEntity mill = placeMill(helper);
        helper.setBlock(HOPPER, Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
        HopperBlockEntity hopper = helper.getBlockEntity(HOPPER);
        hopper.setItem(0, new ItemStack(Items.WHEAT, 4));
        helper.succeedWhen(() -> {
            ItemStack input = mill.inventory().getStackInSlot(0);
            helper.assertTrue(input.is(Items.WHEAT) && !input.isEmpty(),
                    "le blé de la trémie doit entrer dans l'entrée de la meule, obtenu " + input);
        });
    }
}
