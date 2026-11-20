package fr.terresvivantes.dairy.gametest;

import fr.terresvivantes.TerresVivantes;
import fr.terresvivantes.core.quality.QualityModule;
import fr.terresvivantes.dairy.DairyModule;
import fr.terresvivantes.dairy.block.AgingShelfBlockEntity;
import fr.terresvivantes.dairy.block.CheeseVatBlockEntity;
import fr.terresvivantes.dairy.block.ChurnBlockEntity;
import fr.terresvivantes.dairy.logic.MilkSource;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Tests de jeu de la laiterie : baratte, cuve à fromage, étagère d'affinage. Gabarit {@code orchard_arena}. */
@GameTestHolder(TerresVivantes.MODID)
@PrefixGameTestTemplate(false)
public final class DairyGameTests {

    private static final String TEMPLATE = "orchard_arena";
    private static final String BATCH = "dairy";
    private static final BlockPos MACHINE = new BlockPos(5, 2, 5);
    /** Durées des recettes (ticks), avec une marge de vérification. */
    private static final int CHURN_TICKS = 200;
    private static final int VAT_TICKS = 400;
    private static final int MARGIN = 20;

    private DairyGameTests() {
    }

    private static ItemStack cowMilk(int quality) {
        ItemStack milk = new ItemStack(Items.MILK_BUCKET);
        milk.set(DairyModule.MILK_SOURCE.get(), MilkSource.COW);
        QualityModule.setQuality(milk, quality);
        return milk;
    }

    /** Deux crèmes dans la baratte : un beurre est prêt après la durée de barattage. */
    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 300)
    public static void churnTurnsCreamIntoButter(GameTestHelper helper) {
        helper.setBlock(MACHINE, DairyModule.CHURN.get());
        ChurnBlockEntity churn = helper.getBlockEntity(MACHINE);
        churn.inventory().setStackInSlot(0, new ItemStack(DairyModule.CREAM.get(), 2));
        helper.runAfterDelay(CHURN_TICKS + MARGIN, () -> {
            ItemStack butter = churn.inventory().getStackInSlot(DairyModule.CHURN_TYPE.outputSlot());
            helper.assertTrue(butter.is(DairyModule.BUTTER.get()) && butter.getCount() == 1,
                    "beurre attendu (1) après " + CHURN_TICKS + " ticks, obtenu " + butter);
            helper.succeed();
        });
    }

    /** Lait de vache de qualité 3 dans la cuve : deux caillés de qualité 3 après la caillure. */
    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 500)
    public static void cheeseVatKeepsMilkQualityInCurd(GameTestHelper helper) {
        helper.setBlock(MACHINE, DairyModule.CHEESE_VAT.get());
        CheeseVatBlockEntity vat = helper.getBlockEntity(MACHINE);
        vat.inventory().setStackInSlot(0, cowMilk(3));
        helper.runAfterDelay(VAT_TICKS + MARGIN, () -> {
            ItemStack curd = vat.inventory().getStackInSlot(DairyModule.CHEESE_VAT_TYPE.outputSlot());
            helper.assertTrue(curd.is(DairyModule.CHEESE_CURD.get()) && curd.getCount() == 2,
                    "caillé attendu (2), obtenu " + curd);
            helper.assertTrue(QualityModule.qualityOf(curd) == 3,
                    "qualité attendue 3, obtenue " + QualityModule.qualityOf(curd));
            helper.succeed();
        });
    }

    /** Lait de chèvre dans la cuve : fromage de chèvre direct, sans affinage. */
    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 500)
    public static void cheeseVatMakesGoatCheeseFromGoatMilk(GameTestHelper helper) {
        helper.setBlock(MACHINE, DairyModule.CHEESE_VAT.get());
        CheeseVatBlockEntity vat = helper.getBlockEntity(MACHINE);
        ItemStack goatMilk = new ItemStack(Items.MILK_BUCKET);
        goatMilk.set(DairyModule.MILK_SOURCE.get(), MilkSource.GOAT);
        vat.inventory().setStackInSlot(0, goatMilk);
        helper.runAfterDelay(VAT_TICKS + MARGIN, () -> {
            ItemStack cheese = vat.inventory().getStackInSlot(DairyModule.CHEESE_VAT_TYPE.outputSlot());
            helper.assertTrue(cheese.is(DairyModule.GOAT_CHEESE.get()) && cheese.getCount() == 1,
                    "fromage de chèvre attendu (1), obtenu " + cheese);
            helper.succeed();
        });
    }

    /**
     * Le lait de vache ne se transforme pas en fromage de chèvre : la cuve reste vide si l'espèce ne correspond pas.
     */
    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 100)
    public static void cheeseVatIgnoresMilkOfAnotherSpecies(GameTestHelper helper) {
        helper.setBlock(MACHINE, DairyModule.CHEESE_VAT.get());
        CheeseVatBlockEntity vat = helper.getBlockEntity(MACHINE);
        ItemStack plainMilk = new ItemStack(Items.MILK_BUCKET);
        vat.inventory().setStackInSlot(0, plainMilk);
        helper.runAfterDelay(MARGIN, () -> {
            ItemStack output = vat.inventory().getStackInSlot(DairyModule.CHEESE_VAT_TYPE.outputSlot());
            helper.assertTrue(output.isEmpty(), "un seau de lait sans espèce ne doit pas donner de caillé, obtenu " + output);
            helper.succeed();
        });
    }

    /**
     * Fromage jeune de qualité 2 dans l'étagère, presque affiné : il gagne une étoile et sort en fromage affiné de
     * qualité 3. La progression est avancée à la main pour éviter les 72000 ticks de la recette.
     */
    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 300)
    public static void agingShelfRipensYoungCheeseOneStar(GameTestHelper helper) {
        helper.setBlock(MACHINE, DairyModule.AGING_SHELF.get());
        AgingShelfBlockEntity shelf = helper.getBlockEntity(MACHINE);
        ItemStack young = new ItemStack(DairyModule.YOUNG_CHEESE.get());
        QualityModule.setQuality(young, 2);
        shelf.inventory().setStackInSlot(0, young);
        helper.runAfterDelay(5, () -> {
            shelf.dataAccess().set(0, shelf.totalTime() - 2);
            helper.runAfterDelay(MARGIN, () -> {
                ItemStack aged = shelf.inventory().getStackInSlot(DairyModule.AGING_SHELF_TYPE.outputSlot());
                helper.assertTrue(aged.is(DairyModule.AGED_CHEESE.get()),
                        "fromage affiné attendu, obtenu " + aged);
                helper.assertTrue(QualityModule.qualityOf(aged) == 3,
                        "qualité attendue 3 après affinage, obtenue " + QualityModule.qualityOf(aged));
                helper.succeed();
            });
        });
    }
}
