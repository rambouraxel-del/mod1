package fr.terresvivantes.pollination;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cache des scores locaux, par (dimension, chunk, tranche verticale de 16 blocs). La valeur est calculée au centre
 * de la tranche et reste valable {@link #TTL_TICKS} ticks : toutes les positions de la tranche partagent le même
 * score pendant ce délai, ce qui évite un balayage du monde à chaque tentative de croissance.
 * Serveur uniquement; vidé à l'arrêt du serveur.
 */
final class ChunkScoreCache {

    /** Durée de validité d'une entrée, en ticks (30 secondes). */
    static final long TTL_TICKS = 600L;

    /** Au-delà, le cache est vidé : il se reconstruit à la demande. */
    private static final int MAX_ENTRIES = 4096;

    /** Calcul d'un score au centre d'une tranche. */
    @FunctionalInterface
    interface Compute {
        float at(BlockPos center);
    }

    private record Key(ResourceKey<Level> dimension, int chunkX, int sectionY, int chunkZ) {
    }

    private record Entry(float value, long tick) {
    }

    private final Map<Key, Entry> entries = new ConcurrentHashMap<>();

    /** Score de la tranche contenant {@code pos}, calculé par {@code compute} si absent ou périmé. */
    float get(ServerLevel level, BlockPos pos, Compute compute) {
        long now = level.getGameTime();
        int chunkX = pos.getX() >> 4;
        int sectionY = pos.getY() >> 4;
        int chunkZ = pos.getZ() >> 4;
        Key key = new Key(level.dimension(), chunkX, sectionY, chunkZ);
        Entry cached = entries.get(key);
        if (cached != null && now - cached.tick() >= 0 && now - cached.tick() < TTL_TICKS) {
            return cached.value();
        }
        BlockPos center = new BlockPos((chunkX << 4) + 8, (sectionY << 4) + 8, (chunkZ << 4) + 8);
        float value = compute.at(center);
        if (entries.size() >= MAX_ENTRIES) {
            entries.clear();
        }
        entries.put(key, new Entry(value, now));
        return value;
    }

    void clear() {
        entries.clear();
    }
}
