package fr.terresvivantes.kitchen.gametest;

import fr.terresvivantes.TerresVivantes;
import fr.terresvivantes.core.quality.QualityModule;
import fr.terresvivantes.dairy.DairyModule;
import fr.terresvivantes.kitchen.CookingPotBlockEntity;
import fr.terresvivantes.kitchen.KitchenModule;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Tests de jeu de la marmite. Recette de référence : {@code kitchen_cheese_omelette} (deux œufs, une tranche de fromage
 * et du beurre, 160 ticks, qualité moyenne). Gabarit {@code orchard_arena}, comme la meule.
 */
@GameTestHolder(TerresVivantes.MODID)
@PrefixGameTestTemplate(false)
public final class KitchenGameTests {

    private static final String TEMPLATE = "orchard_arena";
    private static final String BATCH = "kitchen";
    /** Durée de la recette de référence (160 ticks) avec une marge. */
    private static final int COOK_CHECK_TICKS = 200;
    private static final int IDLE_CHECK_TICKS = 200;
    private static final BlockPos POT = new BlockPos(5, 2, 5);
    private static final BlockPos FIRE = new BlockPos(5, 1, 5);
    private static final int OUTPUT_SLOT = KitchenModule.COOKING_POT_TYPE.outputSlot();

    private KitchenGameTests() {
    }

    /** Ingrédients de l'omelette au fromage : deux œufs (entrée 0), une tranche de fromage (1), du beurre (2). */
    private static CookingPotBlockEntity placePot(GameTestHelper helper) {
        helper.setBlock(POT, KitchenModule.COOKING_POT.get());
        CookingPotBlockEntity pot = helper.getBlockEntity(POT);
        pot.inventory().setStackInSlot(0, new ItemStack(Items.EGG, 2));
        pot.inventory().setStackInSlot(1, new ItemStack(DairyModule.CHEESE_SLICE.get(), 1));
        pot.inventory().setStackInSlot(2, new ItemStack(DairyModule.BUTTER.get(), 1));
        return pot;
    }

    private static Item omelette() {
        return BuiltInRegistries.ITEM.get(
                ResourceLocation.fromNamespaceAndPath(TerresVivantes.MODID, "cheese_omelette"));
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

    /** Un feu allumé sous la marmite : l'omelette est prête en 200 ticks au plus. */
    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 300)
    public static void potOnLitCampfireCooksCheeseOmelette(GameTestHelper helper) {
        placeLitCampfire(helper);
        CookingPotBlockEntity pot = placePot(helper);
        helper.runAfterDelay(COOK_CHECK_TICKS, () -> {
            ItemStack made = pot.inventory().getStackInSlot(OUTPUT_SLOT);
            helper.assertTrue(made.is(omelette()) && made.getCount() == 1,
                    "omelette attendue après " + COOK_CHECK_TICKS + " ticks sur feu, obtenu " + made);
            helper.succeed();
        });
    }

    /** Qualités 3, 3 et 1 en entrée : la moyenne arrondie donne 2 pour le plat (7 / 3 = 2,33). */
    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 300)
    public static void potAveragesInputQuality(GameTestHelper helper) {
        placeLitCampfire(helper);
        CookingPotBlockEntity pot = placePot(helper);
        QualityModule.setQuality(pot.inventory().getStackInSlot(0), 3);
        QualityModule.setQuality(pot.inventory().getStackInSlot(1), 3);
        QualityModule.setQuality(pot.inventory().getStackInSlot(2), 1);
        helper.runAfterDelay(COOK_CHECK_TICKS, () -> {
            ItemStack made = pot.inventory().getStackInSlot(OUTPUT_SLOT);
            helper.assertTrue(made.is(omelette()), "omelette attendue, obtenu " + made);
            helper.assertTrue(QualityModule.qualityOf(made) == 2,
                    "qualité attendue 2 (moyenne de 3, 3 et 1), obtenue " + QualityModule.qualityOf(made));
            helper.succeed();
        });
    }
}
