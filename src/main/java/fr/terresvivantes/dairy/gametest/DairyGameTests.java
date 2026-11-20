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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Tests de jeu de la laiterie : baratte, cuve à fromage, étagère d'affinage. Gabarit {@code orchard_arena}. */
@GameTestHolder(TerresVivantes.MODID)
@PrefixGameTestTemplate(false)
public final class DairyGameTests {

    private static final String TEMPLATE = "orchard_arena";
    private static final String BATCH = "dairy";
    private static final BlockPos MACHINE = new BlockPos(5, 2, 5);
    /** Étagère voisine de la cuve, pour la chaîne lait → caillé → affinage. */
    private static final BlockPos SHELF = new BlockPos(7, 2, 5);
    private static final BlockPos GOAT_POS = new BlockPos(9, 2, 9);
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

    /** Un seau de lait sans composant d'espèce est du lait de vache : il donne du caillé, pas du fromage de chèvre. */
    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 500)
    public static void cheeseVatTreatsPlainMilkAsCowMilk(GameTestHelper helper) {
        helper.setBlock(MACHINE, DairyModule.CHEESE_VAT.get());
        CheeseVatBlockEntity vat = helper.getBlockEntity(MACHINE);
        vat.inventory().setStackInSlot(0, new ItemStack(Items.MILK_BUCKET));
        helper.runAfterDelay(VAT_TICKS + MARGIN, () -> {
            ItemStack curd = vat.inventory().getStackInSlot(DairyModule.CHEESE_VAT_TYPE.outputSlot());
            helper.assertTrue(curd.is(DairyModule.CHEESE_CURD.get()) && curd.getCount() == 2,
                    "un seau sans espèce doit donner du caillé (2) comme le lait de vache, obtenu " + curd);
            helper.succeed();
        });
    }

    /**
     * Une chèvre vanilla traite au seau : le seau reçoit le composant {@code milk_source=goat}, et la cuve en fait du
     * fromage de chèvre.
     */
    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 500)
    public static void milkingGoatSetsGoatSourceAndGoatCheeseFollows(GameTestHelper helper) {
        Goat goat = helper.spawn(EntityType.GOAT, GOAT_POS);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BUCKET));
        PlayerInteractEvent.EntityInteract event =
                new PlayerInteractEvent.EntityInteract(player, InteractionHand.MAIN_HAND, goat);
        NeoForge.EVENT_BUS.post(event);

        ItemStack milk = player.getItemInHand(InteractionHand.MAIN_HAND);
        helper.assertTrue(milk.is(Items.MILK_BUCKET), "la traite de chèvre doit donner un seau de lait, obtenu " + milk);
        helper.assertTrue(MilkSource.GOAT.equals(milk.get(DairyModule.MILK_SOURCE.get())),
                "composant milk_source attendu à goat, obtenu " + milk.get(DairyModule.MILK_SOURCE.get()));

        helper.setBlock(MACHINE, DairyModule.CHEESE_VAT.get());
        CheeseVatBlockEntity vat = helper.getBlockEntity(MACHINE);
        vat.inventory().setStackInSlot(0, milk.copy());
        helper.runAfterDelay(VAT_TICKS + MARGIN, () -> {
            ItemStack cheese = vat.inventory().getStackInSlot(DairyModule.CHEESE_VAT_TYPE.outputSlot());
            helper.assertTrue(cheese.is(DairyModule.GOAT_CHEESE.get()) && cheese.getCount() == 1,
                    "fromage de chèvre attendu (1), obtenu " + cheese);
            helper.succeed();
        });
    }

    /**
     * Chaîne complète : lait de vache de qualité 3 → cuve → caillé 3 → étagère (affinage forcé du jeune) → fromage
     * affiné 3. Chaque étape est avancée à la main quand elle dure des jours (24000 et 144000 ticks).
     */
    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 800)
    public static void cowMilkQualityThreeRipensToAgedCheese(GameTestHelper helper) {
        helper.setBlock(MACHINE, DairyModule.CHEESE_VAT.get());
        CheeseVatBlockEntity vat = helper.getBlockEntity(MACHINE);
        vat.inventory().setStackInSlot(0, cowMilk(3));
        helper.runAfterDelay(VAT_TICKS + MARGIN, () -> {
            ItemStack curd = vat.inventory().getStackInSlot(DairyModule.CHEESE_VAT_TYPE.outputSlot());
            helper.assertTrue(curd.is(DairyModule.CHEESE_CURD.get()) && QualityModule.qualityOf(curd) == 3,
                    "caillé de qualité 3 attendu, obtenu " + curd);
            vat.inventory().setStackInSlot(DairyModule.CHEESE_VAT_TYPE.outputSlot(), ItemStack.EMPTY);
            helper.setBlock(SHELF, DairyModule.AGING_SHELF.get());
            AgingShelfBlockEntity shelf = helper.getBlockEntity(SHELF);
            ItemStack oneCurd = new ItemStack(DairyModule.CHEESE_CURD.get());
            QualityModule.setQuality(oneCurd, 3);
            shelf.inventory().setStackInSlot(0, oneCurd);
            helper.runAfterDelay(5, () -> {
                shelf.dataAccess().set(0, shelf.totalTime() - 2);
                helper.runAfterDelay(MARGIN, () -> {
                    ItemStack young = shelf.inventory().getStackInSlot(DairyModule.AGING_SHELF_TYPE.outputSlot());
                    helper.assertTrue(young.is(DairyModule.YOUNG_CHEESE.get()) && QualityModule.qualityOf(young) == 3,
                            "fromage jeune de qualité 3 attendu, obtenu " + young);
                    shelf.inventory().setStackInSlot(DairyModule.AGING_SHELF_TYPE.outputSlot(), ItemStack.EMPTY);
                    shelf.inventory().setStackInSlot(0, young.copy());
                    helper.runAfterDelay(5, () -> {
                        shelf.dataAccess().set(0, shelf.totalTime() - 2);
                        helper.runAfterDelay(MARGIN, () -> {
                            ItemStack aged = shelf.inventory().getStackInSlot(DairyModule.AGING_SHELF_TYPE.outputSlot());
                            helper.assertTrue(aged.is(DairyModule.AGED_CHEESE.get()) && QualityModule.qualityOf(aged) == 3,
                                    "fromage affiné de qualité 3 attendu, obtenu " + aged);
                            helper.succeed();
                        });
                    });
                });
            });
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
