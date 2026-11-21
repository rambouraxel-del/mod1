package fr.terresvivantes.husbandry;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cache des auges et des nichoirs chargés, par dimension. Mis à jour aux événements de chargement, de déchargement,
 * de pose et de casse (BlockEntity), pour que les recherches ne scannent jamais les blocs.
 */
final class HusbandryRegistry {

    private static final Map<ResourceKey<Level>, Set<FeedingTroughBlockEntity>> TROUGHS = new ConcurrentHashMap<>();
    private static final Map<ResourceKey<Level>, Set<NestingBoxBlockEntity>> NESTS = new ConcurrentHashMap<>();

    private HusbandryRegistry() {
    }

    static void trackTrough(ResourceKey<Level> dimension, FeedingTroughBlockEntity trough) {
        add(TROUGHS, dimension, trough);
    }

    static void untrackTrough(ResourceKey<Level> dimension, FeedingTroughBlockEntity trough) {
        remove(TROUGHS, dimension, trough);
    }

    static void trackNest(ResourceKey<Level> dimension, NestingBoxBlockEntity nest) {
        add(NESTS, dimension, nest);
    }

    static void untrackNest(ResourceKey<Level> dimension, NestingBoxBlockEntity nest) {
        remove(NESTS, dimension, nest);
    }

    /** Auges chargées dans la dimension de ce niveau. */
    static Collection<FeedingTroughBlockEntity> troughs(Level level) {
        return TROUGHS.getOrDefault(level.dimension(), Set.of());
    }

    /** Nichoir le plus proche du point, dans le rayon, qui peut encore recevoir un œuf. Null si aucun. */
    static NestingBoxBlockEntity nearestNestWithRoom(Level level, Vec3 origin, double radius) {
        NestingBoxBlockEntity best = null;
        double bestDistance = radius * radius;
        for (NestingBoxBlockEntity nest : NESTS.getOrDefault(level.dimension(), Set.of())) {
            double distance = nest.getBlockPos().getCenter().distanceToSqr(origin);
            if (distance <= bestDistance && !nest.isFull()) {
                best = nest;
                bestDistance = distance;
            }
        }
        return best;
    }

    private static <T> void add(Map<ResourceKey<Level>, Set<T>> map, ResourceKey<Level> dimension, T item) {
        map.computeIfAbsent(dimension, key -> ConcurrentHashMap.newKeySet()).add(item);
    }

    private static <T> void remove(Map<ResourceKey<Level>, Set<T>> map, ResourceKey<Level> dimension, T item) {
        Set<T> set = map.get(dimension);
        if (set != null) {
            set.remove(item);
        }
    }
}
