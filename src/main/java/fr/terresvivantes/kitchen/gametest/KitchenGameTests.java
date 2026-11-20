package fr.terresvivantes.kitchen.gametest;

import fr.terresvivantes.TerresVivantes;
import fr.terresvivantes.core.quality.QualityModule;
import fr.terresvivantes.kitchen.CookingPotBlockEntity;
import fr.terresvivantes.kitchen.KitchenModule;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Tests de jeu de la marmite. La recette de test {@code kitchen_mushroom_stew_plus} (deux champignons et un bol, durée
 * 100 ticks) n'utilise que des items vanilla. Gabarit {@code orchard_arena}, comme la meule.
 */
@GameTestHolder(TerresVivantes.MODID)
@PrefixGameTestTemplate(false)
public final class KitchenGameTests {

    private static final String TEMPLATE = "orchard_arena";
    private static final String BATCH = "kitchen";
    /** Durée de la recette de test (100 ticks) avec une marge. */
    private static final int COOK_CHECK_TICKS = 130;
    private static final int IDLE_CHECK_TICKS = 200;
    private static final BlockPos POT = new BlockPos(5, 2, 5);
    private static final BlockPos FIRE = new BlockPos(5, 1, 5);
    private static final int BOWL_SLOT = 4;
    private static final int OUTPUT_SLOT = KitchenModule.COOKING_POT_TYPE.outputSlot();

    private KitchenGameTests() {
    }

    private static CookingPotBlockEntity placePot(GameTestHelper helper) {
        helper.setBlock(POT, KitchenModule.COOKING_POT.get());
        CookingPotBlockEntity pot = helper.getBlockEntity(POT);
        pot.inventory().setStackInSlot(0, new ItemStack(Items.BROWN_MUSHROOM, 1));
        pot.inventory().setStackInSlot(1, new ItemStack(Items.RED_MUSHROOM, 1));
        pot.inventory().setStackInSlot(BOWL_SLOT, new ItemStack(Items.BOWL, 1));
        return pot;
    }

    private static void placeLitCampfire(GameTestHelper helper) {
        helper.setBlock(FIRE, Blocks.CAMPFIRE.defaultBlockState().setValue(BlockStateProperties.LIT, true));
    }

    /** Sans source de chaleur dessous, la marmite ne progresse pas et ne produit rien. */
    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 300)
    public static void potWithoutHeatDoesNotCook(GameTestHelper helper) {
        CookingPotBlockEntity pot = placePot(helper);
        helper.runAfterDelay(IDLE_CHECK_TICKS, () -> {
            helper.assertTrue(pot.inventory().getStackInSlot(OUTPUT_SLOT).isEmpty(),
                    "sans chaleur, aucune sortie attendue, obtenu " + pot.inventory().getStackInSlot(OUTPUT_SLOT));
            helper.assertTrue(pot.progress() == 0,
                    "sans chaleur, progression attendue à 0, obtenu " + pot.progress());
            helper.succeed();
        });
    }

    /** Un feu allumé sous la marmite : la soupe est prête en 130 ticks au plus. */
    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 300)
    public static void potOnLitCampfireCooksMushroomStew(GameTestHelper helper) {
        placeLitCampfire(helper);
        CookingPotBlockEntity pot = placePot(helper);
        helper.runAfterDelay(COOK_CHECK_TICKS, () -> {
            ItemStack made = pot.inventory().getStackInSlot(OUTPUT_SLOT);
            helper.assertTrue(made.is(Items.MUSHROOM_STEW) && made.getCount() == 1,
                    "soupe attendue après " + COOK_CHECK_TICKS + " ticks sur feu, obtenu " + made);
            helper.succeed();
        });
    }

    /** Qualités 3 et 1 en entrée : la moyenne donne 2 pour le plat. */
    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 300)
    public static void potAveragesInputQuality(GameTestHelper helper) {
        placeLitCampfire(helper);
        CookingPotBlockEntity pot = placePot(helper);
        QualityModule.setQuality(pot.inventory().getStackInSlot(0), 3);
        QualityModule.setQuality(pot.inventory().getStackInSlot(1), 1);
        helper.runAfterDelay(COOK_CHECK_TICKS, () -> {
            ItemStack made = pot.inventory().getStackInSlot(OUTPUT_SLOT);
            helper.assertTrue(made.is(Items.MUSHROOM_STEW), "soupe attendue, obtenu " + made);
            helper.assertTrue(QualityModule.qualityOf(made) == 2,
                    "qualité attendue 2 (moyenne de 3 et 1), obtenue " + QualityModule.qualityOf(made));
            helper.succeed();
        });
    }
}
