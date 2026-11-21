package fr.terresvivantes.duck.gametest;

import fr.terresvivantes.TerresVivantes;
import fr.terresvivantes.core.quality.QualityModule;
import fr.terresvivantes.duck.DuckModule;
import fr.terresvivantes.duck.entity.Duck;
import fr.terresvivantes.livestock.LivestockApi;
import fr.terresvivantes.livestock.LivestockModule;
import fr.terresvivantes.livestock.logic.Ailment;
import fr.terresvivantes.livestock.logic.Genome;
import fr.terresvivantes.livestock.logic.LivestockState;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.BabyEntitySpawnEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Tests de jeu du canard. Gabarit {@code orchard_arena} (air 11 x 12 x 11). Les tests n'attendent pas de cycle
 * réel : la ponte est forcée à une tick, l'héritage et la traite sont déclenchés directement.
 */
@GameTestHolder(TerresVivantes.MODID)
@PrefixGameTestTemplate(false)
public final class DuckGameTests {

    private static final String TEMPLATE = "orchard_arena";
    private static final String BATCH = "duck";
    private static final BlockPos CENTER = new BlockPos(5, 2, 5);

    private DuckGameTests() {
    }

    /**
     * Canard bien nourri (satiété 80, productivité 5, multiplicateur 1.1) avec un délai de ponte à 1 : un œuf de
     * canard apparaît dans la zone de test.
     */
    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void duckLaysEggWhenTimerExpires(GameTestHelper helper) {
        Duck duck = helper.spawn(DuckModule.DUCK.get(), CENTER);
        duck.setData(LivestockModule.GENOME.get(), new Genome(5, 5, 5, 5));
        LivestockApi.setState(duck, new LivestockState(80, 100, Ailment.NONE, 0L));
        duck.setEggTime(1);

        helper.succeedWhen(() -> {
            boolean laid = helper.getEntities(EntityType.ITEM).stream()
                    .anyMatch(item -> item.getItem().is(DuckModule.DUCK_EGG.get()));
            helper.assertTrue(laid, "un œuf de canard doit être pondu après le délai forcé à 1");
        });
    }

    /** Caneton né de deux parents (génome 4 et 6) : chaque trait reste dans [min − 1, max + 1] et le bec accepte les graines. */
    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void ducklingInheritsBoundedGenome(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Duck mother = helper.spawn(DuckModule.DUCK.get(), new BlockPos(3, 2, 3));
        Duck father = helper.spawn(DuckModule.DUCK.get(), new BlockPos(7, 2, 7));
        mother.setData(LivestockModule.GENOME.get(), new Genome(4, 4, 4, 0));
        father.setData(LivestockModule.GENOME.get(), new Genome(6, 6, 6, 0));

        Duck child = DuckModule.DUCK.get().create(level);
        helper.assertTrue(child != null, "le caneton doit être créé");
        NeoForge.EVENT_BUS.post(new BabyEntitySpawnEvent(mother, father, child));

        Genome genome = child.getData(LivestockModule.GENOME.get());
        assertWithin(helper, genome.productivity(), 3, 7, "productivité");
        assertWithin(helper, genome.size(), 3, 7, "taille");
        assertWithin(helper, genome.hardiness(), 3, 7, "résistance");
        assertWithin(helper, genome.fertility(), 0, 1, "fertilité");
        helper.assertTrue(mother.isFood(new ItemStack(Items.WHEAT_SEEDS)), "les graines de blé doivent attirer le canard");
        helper.succeed();
    }

    /**
     * Vache à productivité 10, bien nourrie (multiplicateur 1.4 × 1.1 = 1.54) : un clic au seau donne un seau de lait
     * de qualité 3.
     */
    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void bucketOnWellFedCowGivesTopQualityMilk(GameTestHelper helper) {
        Cow cow = helper.spawn(EntityType.COW, CENTER);
        cow.setData(LivestockModule.GENOME.get(), new Genome(10, 5, 5, 5));
        LivestockApi.setState(cow, new LivestockState(80, 100, Ailment.NONE, 0L));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BUCKET));

        PlayerInteractEvent.EntityInteract event =
                new PlayerInteractEvent.EntityInteract(player, InteractionHand.MAIN_HAND, cow);
        NeoForge.EVENT_BUS.post(event);

        helper.assertTrue(event.isCanceled(), "la traite doit être gérée par le module (action annulée côté vanilla)");
        helper.assertTrue(event.getCancellationResult().consumesAction(), "l'action doit être consommée");
        ItemStack held = player.getItemInHand(InteractionHand.MAIN_HAND);
        helper.assertTrue(held.is(Items.MILK_BUCKET), "le seau doit devenir un seau de lait, obtenu " + held);
        int quality = QualityModule.qualityOf(held);
        helper.assertTrue(quality == 3, "qualité du lait attendue 3, obtenue " + quality);
        helper.succeed();
    }

    private static void assertWithin(GameTestHelper helper, int value, int min, int max, String what) {
        helper.assertTrue(value >= min && value <= max, what + " hors de [" + min + ", " + max + "] : " + value);
    }
}
