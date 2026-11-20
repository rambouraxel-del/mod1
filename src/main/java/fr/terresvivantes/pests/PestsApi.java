package fr.terresvivantes.pests;

import fr.terresvivantes.pests.block.ScarecrowBlockEntity;
import fr.terresvivantes.pests.logic.ScarecrowLogic;
import fr.terresvivantes.season.SeasonApi;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Façade du module pests : épouvantails connus du serveur et leur zone d'effet.
 *
 * <p>Les positions sont suivies par dimension (partie basse de chaque épouvantail). Une position dont le BlockEntity
 * n'existe plus est retirée à la première requête. Le rayon effectif vient du jour de pose
 * ({@link ScarecrowLogic#radiusFor}).
 */
public final class PestsApi {

    /** Parties basses des épouvantails, par dimension. */
    private static final Map<ResourceKey<Level>, Set<BlockPos>> LOWER_HALVES = new ConcurrentHashMap<>();

    /** Multiplicateur d'apparition des ravageurs à l'intérieur de la zone d'un épouvantail (lu par health). */
    private static final float PEST_MULTIPLIER = 0.6F;

    private PestsApi() {
    }

    /** Suit la partie basse d'un épouvantail posé. */
    public static void track(Level level, BlockPos lower) {
        LOWER_HALVES.computeIfAbsent(level.dimension(), key -> ConcurrentHashMap.newKeySet()).add(lower.immutable());
    }

    /** Ne suit plus la partie basse d'un épouvantail retiré. */
    public static void untrack(Level level, BlockPos lower) {
        Set<BlockPos> set = LOWER_HALVES.get(level.dimension());
        if (set != null) {
            set.remove(lower);
        }
    }

    /** Oublie tous les épouvantails (arrêt du serveur). */
    public static void clear() {
        LOWER_HALVES.clear();
    }

    /**
     * Partie basse d'un épouvantail dont la zone couvre ce point, ou null. Le rayon dépend de l'usure (jour de pose).
     */
    @Nullable
    public static BlockPos coveringScarecrow(ServerLevel level, Vec3 point) {
        Set<BlockPos> set = LOWER_HALVES.get(level.dimension());
        if (set == null || set.isEmpty()) {
            return null;
        }
        long today = SeasonApi.dayIndex(level);
        for (BlockPos lower : set) {
            if (!level.hasChunkAt(lower)) {
                continue;
            }
            if (!(level.getBlockEntity(lower) instanceof ScarecrowBlockEntity be)) {
                set.remove(lower);
                continue;
            }
            int radius = ScarecrowLogic.radiusFor(PestsConfig.scarecrowRadius(), be.placedDay(), today);
            if (ScarecrowLogic.covers(Vec3.atCenterOf(lower).distanceToSqr(point), radius)) {
                return lower;
            }
        }
        return null;
    }

    /** Vrai si une culture à cette position est protégée contre les corbeaux. */
    public static boolean protectsCrop(ServerLevel level, BlockPos crop) {
        return coveringScarecrow(level, Vec3.atCenterOf(crop)) != null;
    }

    /** Multiplicateur d'apparition des ravageurs : 0.6 dans la zone d'un épouvantail, 1.0 ailleurs. */
    public static float pestMultiplier(ServerLevel level, BlockPos pos) {
        return protectsCrop(level, pos) ? PEST_MULTIPLIER : 1.0F;
    }
}
