package fr.terresvivantes.health;

import fr.terresvivantes.climate.ClimateApi;
import fr.terresvivantes.crop.CropApi;
import fr.terresvivantes.crop.CropGrowthHooks;
import fr.terresvivantes.crop.logic.CropDefinition;
import fr.terresvivantes.crop.logic.GrowthDecision;
import fr.terresvivantes.health.logic.CropHealth;
import fr.terresvivantes.health.logic.Disease;
import fr.terresvivantes.health.logic.HealthModel;
import fr.terresvivantes.season.SeasonApi;
import fr.terresvivantes.soil.SoilApi;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Branchement de la santé sur la croissance (contrats {@link CropGrowthHooks}). Les modifiers s'appliquent aux cultures
 * du mod et vanilla; le listener compte chaque tentative une fois.
 */
final class HealthGrowth {

    private HealthGrowth() {
    }

    static void register() {
        CropGrowthHooks.registerModifier(HealthGrowth::growthModifier);
        CropGrowthHooks.registerYieldModifier(HealthGrowth::yieldModifier);
        CropGrowthHooks.registerListener(HealthGrowth::onGrowthAttempt);
    }

    /** Culture malade : croissance x0.3. */
    private static float growthModifier(ServerLevel level, BlockPos pos, BlockState state, CropDefinition def) {
        return HealthModel.growthFactor(HealthApi.isSick(level, pos));
    }

    /** Culture malade : rendement x0.5. */
    private static float yieldModifier(ServerLevel level, BlockPos pos, BlockState state, CropDefinition def) {
        return HealthModel.yieldFactor(HealthApi.isSick(level, pos));
    }

    /**
     * Une tentative : stress selon le facteur, maladie (progression ou infection), puis flétrissement si un seuil est
     * atteint. Une culture remplacée par flétrissement n'est plus une culture : le code de croissance s'arrête (TVCropBlock,
     * CropEvents).
     */
    private static void onGrowthAttempt(ServerLevel level, BlockPos pos, BlockState state, CropDefinition def,
                                        GrowthDecision decision, float factor) {
        CropHealth before = HealthApi.get(level, pos);
        long day = SeasonApi.dayIndex(level);
        String cropId = HealthApi.cropId(state);

        int stress = HealthModel.nextStress(before.stress(), factor, decision != GrowthDecision.DENY);
        Disease disease = before.disease();
        int ticks = before.diseaseTicks();

        if (disease != Disease.NONE) {
            ticks++;
            spores(level, pos, disease);
            spread(level, pos, cropId, disease, day);
        } else if (HealthConfig.diseasesEnabled()) {
            Disease susceptibility = Disease.susceptibility(cropId);
            if (susceptibility != Disease.NONE) {
                boolean immune = before.immune(day);
                double chance = HealthModel.infectionChance(susceptibility, conditions(level, pos, cropId, immune));
                if (HealthModel.infects(chance, level.getRandom().nextDouble())) {
                    disease = susceptibility;
                    ticks = 0;
                }
            }
        }

        HealthApi.put(level, pos, new CropHealth(stress, disease, ticks, before.immuneUntilDay()));

        boolean witherStress = HealthConfig.witheringEnabled() && HealthModel.withersFromStress(stress);
        if (witherStress || HealthModel.withersFromDisease(disease, ticks)) {
            HealthApi.wither(level, pos);
        }
    }

    /** Une culture malade contamine ses voisins identiques (rayon 1) avec 10 % de chance par tentative. */
    private static void spread(ServerLevel level, BlockPos pos, String cropId, Disease disease, long day) {
        for (BlockPos around : BlockPos.betweenClosed(pos.offset(-1, 0, -1), pos.offset(1, 0, 1))) {
            BlockPos neighbour = around.immutable();
            if (neighbour.equals(pos) || !isCropOf(level.getBlockState(neighbour), cropId)) {
                continue;
            }
            CropHealth health = HealthApi.get(level, neighbour);
            if (health.sick() || health.immune(day)) {
                continue;
            }
            if (HealthModel.spreads(level.getRandom().nextDouble())) {
                HealthApi.infect(level, neighbour, disease);
            }
        }
    }

    /** Conditions d'environnement pour le calcul de la chance d'infection. */
    private static HealthModel.Conditions conditions(ServerLevel level, BlockPos pos, String cropId, boolean immune) {
        BlockPos soil = pos.below();
        BlockState soilState = level.getBlockState(soil);
        boolean saturated = soilState.hasProperty(FarmBlock.MOISTURE)
                && soilState.getValue(FarmBlock.MOISTURE) >= FarmBlock.MAX_MOISTURE;
        boolean rain = level.isRainingAt(pos.above());
        float temperature = ClimateApi.temperatureAt(level, pos);
        int identical = countIdentical(level, pos, cropId);
        boolean monoculture = HealthModel.monoculture(SoilApi.getRepeatCount(level, soil),
                cropId.equals(SoilApi.getLastCrop(level, soil)));
        return new HealthModel.Conditions(saturated, rain, temperature, identical, monoculture, immune);
    }

    /** Nombre de cultures identiques dans la zone 5x5 centrée sur la culture (la culture comprise). */
    private static int countIdentical(ServerLevel level, BlockPos pos, String cropId) {
        int count = 0;
        for (BlockPos around : BlockPos.betweenClosed(pos.offset(-2, 0, -2), pos.offset(2, 0, 2))) {
            if (isCropOf(level.getBlockState(around), cropId)) {
                count++;
            }
        }
        return count;
    }

    private static boolean isCropOf(BlockState state, String cropId) {
        return HealthApi.isCrop(state) && cropId.equals(CropApi.cropIdOf(state));
    }

    /** Particules de spores à chaque tentative d'une culture malade. */
    private static void spores(ServerLevel level, BlockPos pos, Disease disease) {
        var type = disease == Disease.MILDEW ? HealthModule.SPORE_MILDEW.get() : HealthModule.SPORE_RUST.get();
        level.sendParticles(type, pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5, 2, 0.25, 0.15, 0.25, 0.01);
    }
}
