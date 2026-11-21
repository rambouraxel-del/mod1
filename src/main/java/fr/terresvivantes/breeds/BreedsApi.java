package fr.terresvivantes.breeds;

import fr.terresvivantes.breeds.logic.Breed;
import fr.terresvivantes.breeds.logic.BreedLogic;
import fr.terresvivantes.breeds.logic.Species;
import fr.terresvivantes.breeds.network.BreedSyncPayload;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * Façade du module breeds. Les autres modules passent par cette classe, jamais par l'attachment directement.
 */
public final class BreedsApi {

    private BreedsApi() {
    }

    /** Espèce à races d'une entité (vache ou poule), {@code null} pour les autres. */
    @Nullable
    public static Species speciesOf(Entity entity) {
        EntityType<?> type = entity.getType();
        if (type == EntityType.COW) {
            return Species.COW;
        }
        if (type == EntityType.CHICKEN) {
            return Species.CHICKEN;
        }
        return null;
    }

    /** Race de l'animal, {@code null} si l'espèce n'a pas de races ou si la race n'a pas encore été attribuée. */
    @Nullable
    public static Breed getBreed(Entity entity) {
        if (speciesOf(entity) == null || !(entity instanceof Animal animal)) {
            return null;
        }
        return animal.hasData(BreedsModule.BREED.get()) ? animal.getData(BreedsModule.BREED.get()) : null;
    }

    /**
     * Écrit la race (commande de test, vitrine). Refusé si la race n'est pas de l'espèce de l'animal.
     * Synchronise les clients qui suivent l'animal.
     */
    public static boolean setBreed(Animal animal, Breed breed) {
        if (speciesOf(animal) != breed.species()) {
            return false;
        }
        animal.setData(BreedsModule.BREED.get(), breed);
        sync(animal);
        return true;
    }

    /** Envoie la race de l'animal aux joueurs qui le suivent (serveur). */
    static void sync(Animal animal) {
        Breed breed = getBreed(animal);
        if (breed == null || !animal.isAddedToLevel() || !(animal.level() instanceof ServerLevel)) {
            return;
        }
        PacketDistributor.sendToPlayersTrackingEntity(animal, new BreedSyncPayload(animal.getId(), breed.id()));
    }

    /** Ligne « Race : Holstein » du registre d'élevage, {@code null} si l'animal n'a pas de race. */
    @Nullable
    public static Component ledgerLine(Animal animal) {
        Breed breed = getBreed(animal);
        if (breed == null) {
            return null;
        }
        return Component.translatable("message.terresvivantes.breeds.ledger", breedName(breed));
    }

    /** Nom traduit d'une race. */
    public static Component breedName(Breed breed) {
        return Component.translatable("message.terresvivantes.breeds.breed." + breed.id());
    }

    /** Multiplicateur de production de la race de l'animal (1.0 sans race). */
    static float productionModifier(Animal animal) {
        return BreedLogic.productionMultiplier(getBreed(animal));
    }
}
