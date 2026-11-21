package fr.terresvivantes.breeds.client;

import fr.terresvivantes.breeds.logic.Breed;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cache client : race par identifiant d'entité, alimenté par {@code BreedSyncPayload}. Vidé à la déconnexion.
 * Ne contient que des valeurs de logique pure.
 */
public final class BreedClientCache {

    private static final Map<Integer, String> BREEDS = new ConcurrentHashMap<>();

    private BreedClientCache() {
    }

    public static void put(int entityId, String breedId) {
        BREEDS.put(entityId, breedId);
    }

    public static Optional<Breed> get(int entityId) {
        String id = BREEDS.get(entityId);
        return id == null ? Optional.empty() : Breed.byId(id);
    }

    public static void clear() {
        BREEDS.clear();
    }
}
