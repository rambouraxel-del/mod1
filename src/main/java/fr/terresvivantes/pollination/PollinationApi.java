package fr.terresvivantes.pollination;

import fr.terresvivantes.pollination.logic.PollinationModel;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.phys.AABB;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Façade publique de la pollinisation (v0.3). Les autres modules n'appellent que cette classe.
 *
 * <p>{@link #score} est la valeur mise en cache (voir {@link ChunkScoreCache}); {@link #scoreUncached} calcule
 * sans cache, à la position exacte (tests, diagnostic).
 */
public final class PollinationApi {

    /**
     * Source de pollinisation enregistrée par un autre module (apiaires, papillons du mod).
     * {@code contribution} renvoie un apport positif, que la source borne elle-même; le total est plafonné à 1.
     */
    @FunctionalInterface
    public interface PollinatorSource {
        float contribution(ServerLevel level, BlockPos center);
    }

    private static final List<PollinatorSource> SOURCES = new CopyOnWriteArrayList<>();
    private static final ChunkScoreCache CACHE = new ChunkScoreCache();

    private PollinationApi() {
    }

    /** Enregistre une source de pollinisation. À appeler à l'initialisation du mod. */
    public static void registerPollinatorSource(PollinatorSource source) {
        SOURCES.add(source);
    }

    /** Score de pollinisation de la position, dans [0, 1], depuis le cache (serveur). */
    public static float score(ServerLevel level, BlockPos pos) {
        return CACHE.get(level, pos, center -> compute(level, center));
    }

    /** Score de pollinisation exact de la position, sans cache (serveur). */
    public static float scoreUncached(ServerLevel level, BlockPos pos) {
        return compute(level, pos);
    }

    /** Vide le cache (arrêt du serveur, tests). */
    static void clearCache() {
        CACHE.clear();
    }

    private static float compute(ServerLevel level, BlockPos center) {
        int bees = countBees(level, center);
        int hives = countHives(level, center);
        int flowers = flowerTypes(level, center, PollinationModel.FLOWER_RADIUS);
        float extra = 0f;
        for (PollinatorSource source : SOURCES) {
            extra += source.contribution(level, center);
        }
        return PollinationModel.score(bees, hives, flowers, extra);
    }

    /** Abeilles vanilla dans le rayon de pollinisation (horizontal), à 8 blocs de haut ou de bas. */
    static int countBees(ServerLevel level, BlockPos center) {
        AABB box = new AABB(center).inflate(PollinationModel.BEE_RADIUS, 8, PollinationModel.BEE_RADIUS);
        return level.getEntitiesOfClass(Bee.class, box).size();
    }

    /** Ruches et nids vanilla dans le rayon 24, à 8 blocs de haut ou de bas. Les apiaires passent par les sources. */
    static int countHives(ServerLevel level, BlockPos center) {
        int radius = PollinationModel.HIVE_RADIUS;
        int count = 0;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = -radius; x <= radius; x++) {
            for (int y = -8; y <= 8; y++) {
                for (int z = -radius; z <= radius; z++) {
                    cursor.set(center.getX() + x, center.getY() + y, center.getZ() + z);
                    if (level.isLoaded(cursor) && level.getBlockState(cursor).getBlock() instanceof BeehiveBlock) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    /** Nombre de types de fleurs distincts (tag {@code minecraft:flowers}) dans le rayon horizontal, à 2 blocs près. */
    static int flowerTypes(ServerLevel level, BlockPos center, int radius) {
        Set<Block> types = new HashSet<>();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = -radius; x <= radius; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -radius; z <= radius; z++) {
                    cursor.set(center.getX() + x, center.getY() + y, center.getZ() + z);
                    if (!level.isLoaded(cursor)) {
                        continue;
                    }
                    var state = level.getBlockState(cursor);
                    if (state.is(BlockTags.FLOWERS)) {
                        types.add(state.getBlock());
                    }
                }
            }
        }
        return types.size();
    }
}
