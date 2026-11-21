package fr.terresvivantes.pollination;

import fr.terresvivantes.orchard.OrchardApi;
import fr.terresvivantes.pollination.logic.BiodiversityModel;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Façade publique de la biodiversité (v0.3) : indice dans [0, 1] à partir des fleurs, du feuillage fruitier, des
 * insectes et des habitats enregistrés (haies, etc.). Utilisé par la santé et l'almanach.
 */
public final class BiodiversityApi {

    /** Habitat enregistré par un autre module (haies, etc.). La contribution est additionnée puis bornée. */
    @FunctionalInterface
    public interface HabitatSource {
        float contribution(ServerLevel level, BlockPos center);
    }

    private static final List<HabitatSource> SOURCES = new CopyOnWriteArrayList<>();
    private static final ChunkScoreCache CACHE = new ChunkScoreCache();

    private BiodiversityApi() {
    }

    /** Enregistre une source d'habitat. À appeler à l'initialisation du mod. */
    public static void registerHabitatSource(HabitatSource source) {
        SOURCES.add(source);
    }

    /** Indice de biodiversité de la position, dans [0, 1], depuis le cache (serveur). */
    public static float index(ServerLevel level, BlockPos pos) {
        return CACHE.get(level, pos, center -> compute(level, center));
    }

    /** Indice exact de la position, sans cache (serveur). */
    public static float indexUncached(ServerLevel level, BlockPos pos) {
        return compute(level, pos);
    }

    /** Vide le cache (arrêt du serveur, tests). */
    static void clearCache() {
        CACHE.clear();
    }

    private static float compute(ServerLevel level, BlockPos center) {
        int flowers = PollinationApi.flowerTypes(level, center, BiodiversityModel.FLOWER_RADIUS);
        int fruitLeaves = countFruitLeaves(level, center, BiodiversityModel.FRUIT_RADIUS);
        int bees = PollinationApi.countBees(level, center);
        float habitat = 0f;
        for (HabitatSource source : SOURCES) {
            habitat += source.contribution(level, center);
        }
        return BiodiversityModel.index(flowers, fruitLeaves, bees, habitat);
    }

    /** Feuilles fruitières dans le rayon horizontal, à 3 blocs près en hauteur. */
    private static int countFruitLeaves(ServerLevel level, BlockPos center, int radius) {
        int count = 0;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = -radius; x <= radius; x++) {
            for (int y = -3; y <= 3; y++) {
                for (int z = -radius; z <= radius; z++) {
                    cursor.set(center.getX() + x, center.getY() + y, center.getZ() + z);
                    if (level.isLoaded(cursor) && OrchardApi.isFruitFoliage(level.getBlockState(cursor))) {
                        count++;
                    }
                }
            }
        }
        return count;
    }
}
