package fr.terresvivantes.automation.gametest;

import fr.terresvivantes.TerresVivantes;
import fr.terresvivantes.automation.AutomationConfig;
import fr.terresvivantes.automation.AutomationModule;
import fr.terresvivantes.automation.HarvesterBlockEntity;
import fr.terresvivantes.automation.PlanterBlockEntity;
import fr.terresvivantes.orchard.FruitLeavesBlock;
import fr.terresvivantes.orchard.OrchardModule;
import fr.terresvivantes.orchard.logic.OrchardLogic;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests de jeu des machines automatiques. Zone 5x5 devant la machine (x 3..7, z 3..7, Y = 2), terre en Y = 1,
 * machine en (5, 2, 2) orientée vers le sud, conteneur derrière en (5, 2, 1). Gabarit {@code orchard_arena}.
 */
@GameTestHolder(TerresVivantes.MODID)
@PrefixGameTestTemplate(false)
public final class AutomationGameTests {

    private static final String TEMPLATE = "orchard_arena";
    private static final String BATCH = "automation";
    private static final BlockPos MACHINE = new BlockPos(5, 2, 2);
    private static final BlockPos CHEST = new BlockPos(5, 2, 1);
    /** Redstone à côté de la machine, hors de la zone : la machine est alimentée donc en pause. */
    private static final BlockPos REDSTONE = new BlockPos(4, 2, 2);
    /** Le volume 5x5x3 de la récolteuse (75 cases) se parcourt en 15 cycles de 5 cases. */
    private static final int HARVEST_CYCLES = 15;
    private static final int ZONE_CELLS = 25;

    private AutomationGameTests() {
    }

    /** Pose la zone 5x5 : terre labourée en Y = 1, cases de culture en Y = 2 (vides, ou cultures si fournies). */
    private static List<BlockPos> field(GameTestHelper helper) {
        List<BlockPos> cells = new ArrayList<>();
        for (int x = 3; x <= 7; x++) {
            for (int z = 3; z <= 7; z++) {
                helper.setBlock(new BlockPos(x, 1, z), Blocks.FARMLAND);
                BlockPos cell = new BlockPos(x, 2, z);
                helper.setBlock(cell, Blocks.AIR);
                cells.add(cell);
            }
        }
        return cells;
    }

    /** Fait tourner la méthode de tick de la machine pendant {@code ticks} ticks serveur. */
    private static void tickMachine(BlockEntity machine, ServerLevel level, int ticks) {
        for (int i = 0; i < ticks; i++) {
            if (machine instanceof HarvesterBlockEntity harvester) {
                harvester.serverTick(level);
            } else if (machine instanceof PlanterBlockEntity planter) {
                planter.serverTick(level);
            }
        }
    }

    private static int countItem(Container container, net.minecraft.world.item.Item item) {
        int total = 0;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (stack.is(item)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    /** Vingt-cinq blés mûrs devant la récolteuse, coffre derrière : après cinq cycles, tout est récolté et replanté. */
    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void harvesterHarvestsMatureWheatAndPushesToChest(GameTestHelper helper) {
        helper.setBlock(MACHINE, AutomationModule.HARVESTER.get().defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
        helper.setBlock(CHEST, Blocks.CHEST.defaultBlockState());
        List<BlockPos> cells = field(helper);
        for (BlockPos cell : cells) {
            helper.setBlock(cell, Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, CropBlock.MAX_AGE));
        }
        HarvesterBlockEntity harvester = helper.getBlockEntity(MACHINE);
        tickMachine(harvester, helper.getLevel(), HARVEST_CYCLES * AutomationConfig.intervalTicks());

        for (BlockPos cell : cells) {
            helper.assertTrue(helper.getBlockState(cell).is(Blocks.WHEAT)
                            && helper.getBlockState(cell).getValue(CropBlock.AGE) == 0,
                    "culture " + cell + " attendue replantée à l'âge 0, obtenu " + helper.getBlockState(cell));
        }
        Container chest = (Container) helper.getBlockEntity(CHEST);
        int wheat = countItem(chest, Items.WHEAT);
        helper.assertTrue(wheat >= ZONE_CELLS, "au moins " + ZONE_CELLS + " blés dans le coffre, obtenu " + wheat);
        helper.succeed();
    }

    /**
     * Feuille fruitière mûre à deux couches au-dessus de la case de culture (dans le volume 5x5x3), et feuille
     * non mûre voisine : après le parcours, la mûre est remise au stade 0 et la non mûre n'est pas touchée.
     */
    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void harvesterPicksRipeFruitLeavesAboveTheField(GameTestHelper helper) {
        helper.setBlock(MACHINE, AutomationModule.HARVESTER.get().defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
        helper.setBlock(CHEST, Blocks.CHEST.defaultBlockState());
        field(helper);
        FruitLeavesBlock apple = OrchardModule.leavesBlock("apple");
        BlockPos ripe = new BlockPos(5, 4, 5);
        BlockPos unripe = new BlockPos(6, 3, 6);
        helper.setBlock(ripe, apple.defaultBlockState()
                .setValue(FruitLeavesBlock.STAGE, OrchardLogic.MAX_STAGE)
                .setValue(FruitLeavesBlock.PERSISTENT, true));
        helper.setBlock(unripe, apple.defaultBlockState()
                .setValue(FruitLeavesBlock.STAGE, 2)
                .setValue(FruitLeavesBlock.PERSISTENT, true));
        HarvesterBlockEntity harvester = helper.getBlockEntity(MACHINE);
        tickMachine(harvester, helper.getLevel(), HARVEST_CYCLES * AutomationConfig.intervalTicks());

        int ripeStage = helper.getBlockState(ripe).getValue(FruitLeavesBlock.STAGE);
        helper.assertTrue(ripeStage == 0, "feuille mûre attendue au stade 0 après récolte, obtenu " + ripeStage);
        int unripeStage = helper.getBlockState(unripe).getValue(FruitLeavesBlock.STAGE);
        helper.assertTrue(unripeStage == 2, "feuille non mûre attendue intacte au stade 2, obtenu " + unripeStage);
        helper.succeed();
    }

    /** Graines de blé dans la planteuse : après 25 cycles, les 25 cases de la zone sont plantées. */
    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void planterSowsWheatOnAllFarmlandOfTheZone(GameTestHelper helper) {
        helper.setBlock(MACHINE, AutomationModule.PLANTER.get().defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
        List<BlockPos> cells = field(helper);
        PlanterBlockEntity planter = helper.getBlockEntity(MACHINE);
        planter.container().setItem(0, new ItemStack(Items.WHEAT_SEEDS, 64));
        tickMachine(planter, helper.getLevel(), ZONE_CELLS * AutomationConfig.intervalTicks());

        for (BlockPos cell : cells) {
            helper.assertTrue(helper.getBlockState(cell).is(Blocks.WHEAT),
                    "blé attendu en " + cell + ", obtenu " + helper.getBlockState(cell));
        }
        helper.succeed();
    }

    /** Machine alimentée en redstone : aucun cycle, les cultures mûres restent en place. */
    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void redstonePausesTheHarvester(GameTestHelper helper) {
        helper.setBlock(MACHINE, AutomationModule.HARVESTER.get().defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
        helper.setBlock(CHEST, Blocks.CHEST.defaultBlockState());
        helper.setBlock(REDSTONE, Blocks.REDSTONE_BLOCK);
        List<BlockPos> cells = field(helper);
        for (BlockPos cell : cells) {
            helper.setBlock(cell, Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, CropBlock.MAX_AGE));
        }
        HarvesterBlockEntity harvester = helper.getBlockEntity(MACHINE);
        tickMachine(harvester, helper.getLevel(), HARVEST_CYCLES * AutomationConfig.intervalTicks());

        for (BlockPos cell : cells) {
            helper.assertTrue(helper.getBlockState(cell).getValue(CropBlock.AGE) == CropBlock.MAX_AGE,
                    "culture " + cell + " attendue intacte pendant la pause redstone");
        }
        Container chest = (Container) helper.getBlockEntity(CHEST);
        helper.assertTrue(countItem(chest, Items.WHEAT) == 0, "aucune récolte attendue pendant la pause");
        helper.succeed();
    }
}
