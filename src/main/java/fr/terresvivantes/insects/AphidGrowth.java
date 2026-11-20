package fr.terresvivantes.insects;

import fr.terresvivantes.crop.logic.CropDefinition;
import fr.terresvivantes.crop.logic.GrowthDecision;
import fr.terresvivantes.health.HealthApi;
import fr.terresvivantes.health.logic.Disease;
import fr.terresvivantes.insects.logic.AphidLogic;
import fr.terresvivantes.insects.logic.ButterflyLogic;
import fr.terresvivantes.pollination.BiodiversityApi;
import fr.terresvivantes.season.SeasonApi;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Apparition et propagation des pucerons, branchées sur chaque tentative de croissance (contrat
 * {@code CropGrowthHooks.GrowthListener}). Côté serveur, à coût faible : un tirage par tentative, le reste seulement
 * si ce tirage passe.
 */
final class AphidGrowth {

    /** Tentatives sur une culture infestée : une particule sur trois. */
    private static final int PARTICLE_ONE_IN = 3;

    private AphidGrowth() {
    }

    static void onGrowthAttempt(ServerLevel level, BlockPos pos, BlockState state, CropDefinition def,
                                GrowthDecision decision, float factor) {
        if (!InsectsConfig.aphidsEnabled()) {
            return;
        }
        if (HealthApi.diseaseOf(level, pos) == Disease.APHIDS) {
            spreadFrom(level, pos);
            return;
        }
        if (!ButterflyLogic.isWarmSeason(SeasonApi.getSeason(level))) {
            return;
        }
        if (!AphidLogic.rollSucceeds(InsectsConfig.aphidBaseChance(), level.getRandom().nextDouble())) {
            return;
        }
        double conditional = AphidLogic.conditionalChance(BiodiversityApi.index(level, pos),
                HealthApi.pestSuppression(level, pos));
        if (AphidLogic.rollSucceeds(conditional, level.getRandom().nextDouble()) && HealthApi.infest(level, pos)) {
            emitParticles(level, pos);
        }
    }

    /** Une culture infestée : particules occasionnelles, puis contamination des voisines au rayon 1. */
    private static void spreadFrom(ServerLevel level, BlockPos pos) {
        if (level.getRandom().nextInt(PARTICLE_ONE_IN) == 0) {
            emitParticles(level, pos);
        }
        for (BlockPos around : BlockPos.betweenClosed(pos.offset(-1, 0, -1), pos.offset(1, 0, 1))) {
            BlockPos neighbour = around.immutable();
            if (neighbour.equals(pos)) {
                continue;
            }
            if (AphidLogic.rollSucceeds(AphidLogic.PROPAGATION_CHANCE, level.getRandom().nextDouble())) {
                HealthApi.infest(level, neighbour);
            }
        }
    }

    private static void emitParticles(ServerLevel level, BlockPos pos) {
        level.sendParticles(InsectsModule.APHID.get(),
                pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5, 2, 0.25, 0.1, 0.25, 0.01);
    }
}
