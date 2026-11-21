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
}
