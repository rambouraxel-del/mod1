package fr.terresvivantes.livestock.gametest;

import fr.terresvivantes.TerresVivantes;
import fr.terresvivantes.livestock.LivestockApi;
import fr.terresvivantes.livestock.LivestockModule;
import fr.terresvivantes.livestock.logic.Ailment;
import fr.terresvivantes.livestock.logic.Genome;
import fr.terresvivantes.vet.VetApi;
import fr.terresvivantes.livestock.logic.LivestockState;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.BabyEntitySpawnEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Tests de jeu du module livestock. Gabarit {@code orchard_arena} (air 11 x 12 x 11). Les cycles de besoins et les
 * refus de reproduction sont appelés directement (pas d'attente de 1200 ticks).
 */
@GameTestHolder(TerresVivantes.MODID)
@PrefixGameTestTemplate(false)
public final class LivestockGameTests {

    private static final String TEMPLATE = "orchard_arena";

    private LivestockGameTests() {
    }

    /** Enfant d'un couple au génome 4 et 6 (fertilité 0) : chaque trait reste dans [min − 1, max + 1]. */
    @GameTest(template = TEMPLATE, batch = "livestock")
    public static void babyInheritsBoundedGenome(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Cow mother = helper.spawn(EntityType.COW, new BlockPos(3, 2, 3));
        Cow father = helper.spawn(EntityType.COW, new BlockPos(7, 2, 7));
        LivestockApi.ensureGenome(mother);
        LivestockApi.ensureGenome(father);
        mother.setData(LivestockModule.GENOME.get(), new Genome(4, 4, 4, 0));
        father.setData(LivestockModule.GENOME.get(), new Genome(6, 6, 6, 0));

        Cow child = EntityType.COW.create(level);
        helper.assertTrue(child != null, "la vache enfant doit être créée");
        NeoForge.EVENT_BUS.post(new BabyEntitySpawnEvent(mother, father, child));

        Genome genome = child.getData(LivestockModule.GENOME.get());
        assertWithin(helper, genome.productivity(), 3, 7, "productivité");
        assertWithin(helper, genome.size(), 3, 7, "taille");
        assertWithin(helper, genome.hardiness(), 3, 7, "résistance");
        assertWithin(helper, genome.fertility(), 0, 1, "fertilité");
        helper.succeed();
    }

    /** Un animal sans nourriture (satiété 0) perd 5 points de santé au cycle de besoins, satiété reste à 0. */
    @GameTest(template = TEMPLATE, batch = "livestock")
    public static void unfedAnimalLosesHealth(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        helper.setBlock(new BlockPos(5, 1, 5), Blocks.STONE);
        Sheep sheep = helper.spawn(EntityType.SHEEP, new BlockPos(5, 2, 5));
        LivestockApi.setState(sheep, new LivestockState(0, 100, Ailment.NONE, 0L));

        // Le module vet tire des maladies au hasard à chaque cycle : tirage fixé à « pas de maladie » pour ce test.
        VetApi.setRandomForTest(() -> 1.0);
        try {
            LivestockApi.runNeedsTick(level, sheep);
        } finally {
            VetApi.resetRandom();
        }

        LivestockState state = LivestockApi.getState(sheep);
        helper.assertTrue(state.fed() == 0, "satiété attendue 0, obtenue " + state.fed());
        helper.assertTrue(state.health() == 95, "santé attendue 95 (perte de 5), obtenue " + state.health());
        helper.succeed();
    }

    /**
     * Pâturage sur herbe hors hiver : satiété 40 devient 42 (−8 de consommation, +10 de pâturage).
     * Batch propre (voir {@link #breedingRefusedInWinterAllowedInSummer}) : la saison est globale au serveur.
     */
    @GameTest(template = TEMPLATE, batch = "livestock_grazing")
    public static void grazingOnGrassFeedsOutsideWinter(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tv season set summer 1");
        helper.setBlock(new BlockPos(5, 1, 5), Blocks.GRASS_BLOCK);
        Cow cow = helper.spawn(EntityType.COW, new BlockPos(5, 2, 5));
        LivestockApi.setState(cow, new LivestockState(40, 100, Ailment.NONE, 0L));

        LivestockApi.runNeedsTick(level, cow);

        helper.assertTrue(LivestockApi.getState(cow).fed() == 42,
                "satiété attendue 42 au pâturage en été, obtenue " + LivestockApi.getState(cow).fed());
        helper.succeed();
    }

    /**
     * Batches : la batch par défaut reste celle du module core/season (tests sensibles au temps et à la saison);
     * les tests livestock tournent après, en batches séparées, pour ne pas perturber les autres par concurrence.
     * La saison est globale au serveur : les deux tests qui la changent ont chacun leur batch.
     *
     * Reproduction refusée en hiver même avec de la nourriture et une satiété suffisante; permise en été; refusée
     * sous 40 de satiété même en été.
     */
    @GameTest(template = TEMPLATE, batch = "livestock_breeding")
    public static void breedingRefusedInWinterAllowedInSummer(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();
        Cow cow = helper.spawn(EntityType.COW, new BlockPos(5, 2, 5));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WHEAT));
        LivestockApi.setState(cow, new LivestockState(80, 100, Ailment.NONE, 0L));

        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tv season set winter 1");
        PlayerInteractEvent.EntityInteract winter = new PlayerInteractEvent.EntityInteract(player, InteractionHand.MAIN_HAND, cow);
        NeoForge.EVENT_BUS.post(winter);
        helper.assertTrue(winter.isCanceled(), "reproduction doit être refusée en hiver");
        helper.assertTrue(winter.getCancellationResult() == InteractionResult.FAIL, "résultat d'annulation FAIL attendu");

        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tv season set summer 1");
        PlayerInteractEvent.EntityInteract summer = new PlayerInteractEvent.EntityInteract(player, InteractionHand.MAIN_HAND, cow);
        NeoForge.EVENT_BUS.post(summer);
        helper.assertTrue(!summer.isCanceled(), "reproduction doit être permise en été avec satiété 80");

        LivestockApi.setState(cow, new LivestockState(20, 100, Ailment.NONE, 0L));
        PlayerInteractEvent.EntityInteract hungry = new PlayerInteractEvent.EntityInteract(player, InteractionHand.MAIN_HAND, cow);
        NeoForge.EVENT_BUS.post(hungry);
        helper.assertTrue(hungry.isCanceled(), "reproduction doit être refusée avec satiété 20 (< 40)");
        helper.succeed();
    }

    /**
     * Le registre d'élevage est une lecture seule : état et génome sont inchangés après un clic droit.
     * Joueur fictif hors liste des joueurs : la synchro de saison (commandes) ne lui est pas envoyée.
     */
    @GameTest(template = TEMPLATE, batch = "livestock")
    public static void ledgerHasNoSideEffects(GameTestHelper helper) {
        Sheep sheep = helper.spawn(EntityType.SHEEP, new BlockPos(5, 2, 5));
        Genome genome = new Genome(7, 3, 5, 2);
        sheep.setData(LivestockModule.GENOME.get(), genome);
        LivestockState state = new LivestockState(30, 80, Ailment.SICK, 7L);
        LivestockApi.setState(sheep, state);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);

        InteractionResult result = LivestockModule.LEDGER.get().interactLivingEntity(
                new ItemStack(LivestockModule.LEDGER.get()), player, sheep, InteractionHand.MAIN_HAND);

        helper.assertTrue(result.consumesAction(), "le registre doit consommer l'action sur un animal d'élevage");
        helper.assertTrue(LivestockApi.getState(sheep).equals(state), "l'état ne doit pas changer");
        helper.assertTrue(LivestockApi.getGenome(sheep).equals(genome), "le génome ne doit pas changer");
        helper.succeed();
    }

    private static void assertWithin(GameTestHelper helper, int value, int min, int max, String what) {
        helper.assertTrue(value >= min && value <= max,
                what + " hors de [" + min + ", " + max + "] : " + value);
    }
}
