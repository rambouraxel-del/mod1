package fr.terresvivantes.flora;

import fr.terresvivantes.climate.ClimateApi;
import fr.terresvivantes.flora.logic.FloraLogic;
import fr.terresvivantes.season.SeasonApi;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Propagation d'une fleur sauvage (côté jeu). La logique de décision est dans {@link FloraLogic}.
 */
public final class FloraSpread {

    private FloraSpread() {
    }

    /**
     * Tente une copie de {@code flower} sur un bloc d'herbe voisin, à au plus {@link FloraLogic#SPREAD_RADIUS} blocs.
     *
     * <p>Conditions : saison de propagation, tirage sous {@code chance}, bloc d'herbe libre au-dessus, fleur
     * capable de survivre à cet endroit, moins de {@link FloraLogic#MAX_SAME_IN_AREA} fleurs du même type dans le
     * 5 x 5 autour de la cible, et température suffisante si la fleur l'exige (lavande).
     *
     * @param chance probabilité de tentative, de 0 à 1 (1 pour un test forcé)
     * @return vrai si une fleur a été posée
     */
    public static boolean attempt(ServerLevel level, BlockPos flowerPos, WildFlowerBlock flower,
                                  RandomSource random, float chance) {
        if (!FloraLogic.isSpreadSeason(SeasonApi.getSeason(level))) {
            return false;
        }
        if (random.nextFloat() >= chance) {
            return false;
        }
        int r = FloraLogic.SPREAD_RADIUS;
        BlockPos ground = flowerPos.below().offset(
                random.nextInt(2 * r + 1) - r, 0, random.nextInt(2 * r + 1) - r);
        BlockPos target = ground.above();
        if (!level.isLoaded(ground) || !level.isLoaded(target)
                || !level.getBlockState(ground).is(Blocks.GRASS_BLOCK)
                || !level.isEmptyBlock(target)) {
            return false;
        }
        BlockState planted = flower.defaultBlockState();
        if (!planted.canSurvive(level, target)) {
            return false;
        }
        if (!FloraLogic.canSpreadTo(countSameFlowers(level, target, flower))) {
            return false;
        }
        float required = FloraLogic.requiredTemperature(flower.id());
        if (required > Float.NEGATIVE_INFINITY
                && !FloraLogic.temperatureAllows(required, ClimateApi.temperatureAt(level, target))) {
            return false;
        }
        return level.setBlock(target, planted, 2);
    }

    /** Fleurs du même type dans le carré 5 x 5 centré sur {@code center} (hauteur ±1). */
    static int countSameFlowers(ServerLevel level, BlockPos center, WildFlowerBlock flower) {
        int r = FloraLogic.SPREAD_RADIUS;
        int count = 0;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    cursor.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    if (level.isLoaded(cursor) && level.getBlockState(cursor).is(flower)) {
                        count++;
                    }
                }
            }
        }
        return count;
    }
}
