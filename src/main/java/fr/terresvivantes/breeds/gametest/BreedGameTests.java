package fr.terresvivantes.breeds.gametest;

import fr.terresvivantes.TerresVivantes;
import fr.terresvivantes.breeds.BreedsApi;
import fr.terresvivantes.breeds.logic.Breed;
import fr.terresvivantes.breeds.logic.Species;
import fr.terresvivantes.livestock.LivestockApi;
import fr.terresvivantes.livestock.LivestockModule;
import fr.terresvivantes.livestock.logic.Genome;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Cow;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.BabyEntitySpawnEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import fr.terresvivantes.vet.VetApi;
import fr.terresvivantes.livestock.logic.LivestockState;
import fr.terresvivantes.livestock.logic.Ailment;
import net.minecraft.server.MinecraftServer;

/**
 * Tests de jeu du module breeds. Gabarit {@code orchard_arena} (air 11 x 12 x 11). Les naissances sont déclenchées
 * par l'événement {@link BabyEntitySpawnEvent}, comme dans le module livestock.
 */
@GameTestHolder(TerresVivantes.MODID)
@PrefixGameTestTemplate(false)
public final class BreedGameTests {

    private static final String TEMPLATE = "orchard_arena";

    private BreedGameTests() {
    }

    /** Une vache posée dans le monde reçoit une race de l'espèce vache. */
    @GameTest(template = TEMPLATE, batch = "breeds")
    public static void spawnedCowGetsCowBreed(GameTestHelper helper) {
        Cow cow = helper.spawn(EntityType.COW, new BlockPos(3, 2, 3));
        Breed breed = BreedsApi.getBreed(cow);
        helper.assertTrue(breed != null, "la vache doit recevoir une race à son arrivée");
        helper.assertTrue(breed.species() == Species.COW, "la race doit être de l'espèce vache, reçu " + breed);
        helper.succeed();
    }

    /** Bébé de deux Holstein : race Holstein (hérédité, les deux parents ont la même race). */
    @GameTest(template = TEMPLATE, batch = "breeds")
    public static void babyOfTwoHolsteinsIsHolstein(GameTestHelper helper) {
        Cow mother = helper.spawn(EntityType.COW, new BlockPos(3, 2, 3));
        Cow father = helper.spawn(EntityType.COW, new BlockPos(7, 2, 7));
        BreedsApi.setBreed(mother, Breed.HOLSTEIN);
        BreedsApi.setBreed(father, Breed.HOLSTEIN);

        Cow child = EntityType.COW.create(helper.getLevel());
        helper.assertTrue(child != null, "la vache enfant doit être créée");
        NeoForge.EVENT_BUS.post(new BabyEntitySpawnEvent(mother, father, child));

        helper.assertTrue(BreedsApi.getBreed(child) == Breed.HOLSTEIN,
                "bébé de deux Holstein attendu Holstein, reçu " + BreedsApi.getBreed(child));
        helper.succeed();
    }

    /** Une Leghorn produit davantage qu'une poule sans race, à génome et état identiques (API, sans attente). */
    @GameTest(template = TEMPLATE, batch = "breeds")
    public static void leghornProducesMoreThanPlainHen(GameTestHelper helper) {
        Chicken plain = EntityType.CHICKEN.create(helper.getLevel());
        Chicken leghorn = EntityType.CHICKEN.create(helper.getLevel());
        helper.assertTrue(plain != null && leghorn != null, "les poules doivent être créées");
        Genome genome = new Genome(5, 5, 5, 0);
        plain.setData(LivestockModule.GENOME.get(), genome);
        leghorn.setData(LivestockModule.GENOME.get(), genome);
        BreedsApi.setBreed(leghorn, Breed.LEGHORN);

        float base = LivestockApi.productionMultiplier(plain);
        float withBreed = LivestockApi.productionMultiplier(leghorn);
        helper.assertTrue(withBreed > base,
                "la Leghorn doit avoir un multiplicateur supérieur (" + withBreed + " contre " + base + ")");
        helper.succeed();
    }

    /** Poule Sussex : chance de maladie ×0.7 (module vet, via VetApi); une Holstein reste à ×1. */
    @GameTest(template = TEMPLATE, batch = "breeds")
    public static void sussexHenHasLowerDiseaseChance(GameTestHelper helper) {
        Chicken hen = helper.spawn(EntityType.CHICKEN, new BlockPos(3, 2, 3));
        Cow cow = helper.spawn(EntityType.COW, new BlockPos(7, 2, 7));
        BreedsApi.setBreed(hen, Breed.SUSSEX);
        BreedsApi.setBreed(cow, Breed.HOLSTEIN);
        helper.assertTrue(Math.abs(VetApi.diseaseChanceFactor(hen) - 0.7f) < 1e-4f,
                "poule Sussex attendue à ×0.7, reçu " + VetApi.diseaseChanceFactor(hen));
        helper.assertTrue(Math.abs(VetApi.diseaseChanceFactor(cow) - 1.0f) < 1e-4f,
                "vache Holstein attendue à ×1, reçu " + VetApi.diseaseChanceFactor(cow));
        helper.succeed();
    }

    /**
     * Jumeau : il reçoit la race d'un parent. Les deux parents sont Charolais, donc le jumeau (dont l'arrivée a tiré une
     * race au hasard) doit être Charolais après l'écouteur de jumeau.
     */
    @GameTest(template = TEMPLATE, batch = "breeds")
    public static void twinInheritsBreedOfItsParents(GameTestHelper helper) {
        Cow parentA = helper.spawn(EntityType.COW, new BlockPos(3, 2, 3));
        Cow parentB = helper.spawn(EntityType.COW, new BlockPos(7, 2, 7));
        BreedsApi.setBreed(parentA, Breed.CHAROLAIS);
        BreedsApi.setBreed(parentB, Breed.CHAROLAIS);
        Cow twin = helper.spawn(EntityType.COW, new BlockPos(5, 2, 5));

        LivestockApi.notifyTwin(twin, parentA, parentB);

        helper.assertTrue(BreedsApi.getBreed(twin) == Breed.CHAROLAIS,
                "jumeau de deux Charolais attendu Charolais, reçu " + BreedsApi.getBreed(twin));
        helper.succeed();
    }

    /**
     * Highland en hiver : la compensation de faim ajoute la satiété annulant le surcroît hivernal (12 - 4 = 8, comme
     * une vache normale), et {@code lastFedDay} ne change pas (ce n'est pas un repas).
     */
    @GameTest(template = TEMPLATE, batch = "breeds")
    public static void highlandWinterKeepsLastFedDay(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tv season set winter 1");
        Cow cow = helper.spawn(EntityType.COW, new BlockPos(3, 2, 3));
        BreedsApi.setBreed(cow, Breed.HIGHLAND);
        LivestockApi.setState(cow, new LivestockState(50, 100, Ailment.NONE, 7L));

        LivestockApi.runNeedsTick(helper.getLevel(), cow);

        LivestockState after = LivestockApi.getState(cow);
        helper.assertTrue(after.fed() == 42, "satiété attendue 42 (50 - 8 net), reçu " + after.fed());
        helper.assertTrue(after.lastFedDay() == 7L, "lastFedDay doit rester 7, reçu " + after.lastFedDay());
        helper.succeed();
    }
}
