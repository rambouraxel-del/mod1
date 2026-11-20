package fr.terresvivantes.insects;

import fr.terresvivantes.health.HealthApi;
import fr.terresvivantes.insects.entity.Butterfly;
import fr.terresvivantes.insects.entity.Ladybug;
import fr.terresvivantes.insects.logic.AphidLogic;
import fr.terresvivantes.insects.logic.ButterflyLogic;
import fr.terresvivantes.pollination.BiodiversityApi;
import fr.terresvivantes.pollination.PollinationApi;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;

/**
 * Branchements des insectes sur les façades des autres modules : papillons comme pollinisateurs, papillons et
 * coccinelles comme habitat, coccinelles comme suppresseurs d'apparition des pucerons.
 */
final class InsectSources {

    private InsectSources() {
    }

    static void register() {
        PollinationApi.registerPollinatorSource(InsectSources::pollination);
        BiodiversityApi.registerHabitatSource(InsectSources::habitat);
        HealthApi.registerPestSuppressor(InsectSources::ladybugSuppression);
    }

    /** Papillons dans le rayon de pollinisation : 0.05 chacun, plafond 0.2. */
    private static float pollination(ServerLevel level, BlockPos center) {
        int r = ButterflyLogic.POLLINATOR_RADIUS;
        int count = level.getEntitiesOfClass(Butterfly.class, box(center, r)).size();
        return ButterflyLogic.pollinatorContribution(count);
    }

    /** Papillons et coccinelles proches : 0.02 chacun, plafond 0.15. */
    private static float habitat(ServerLevel level, BlockPos center) {
        int r = ButterflyLogic.HABITAT_RADIUS;
        int count = level.getEntitiesOfClass(Butterfly.class, box(center, r)).size()
                + level.getEntitiesOfClass(Ladybug.class, box(center, r)).size();
        return ButterflyLogic.habitatContribution(count);
    }

    /** Une coccinelle dans le rayon réduit l'apparition des pucerons à 0.2; sinon 1. */
    private static float ladybugSuppression(ServerLevel level, BlockPos pos) {
        boolean present = !level.getEntitiesOfClass(Ladybug.class, box(pos, AphidLogic.LADYBUG_RADIUS)).isEmpty();
        return present ? AphidLogic.LADYBUG_SUPPRESSION : 1.0f;
    }

    /** Boîte horizontale de rayon {@code r}, hauteur de 4 blocs au-dessus et au-dessous du centre. */
    private static AABB box(BlockPos center, int r) {
        return new AABB(center).inflate(r, 4, r);
    }
}
