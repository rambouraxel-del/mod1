package fr.terresvivantes.crop;

import fr.terresvivantes.climate.ClimateApi;
import fr.terresvivantes.crop.logic.Affinity;
import fr.terresvivantes.crop.logic.CropDefinition;
import fr.terresvivantes.crop.logic.CropLogic;
import fr.terresvivantes.crop.logic.GrowthModel;
import fr.terresvivantes.season.SeasonApi;
import fr.terresvivantes.soil.SoilApi;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Calcul du facteur de croissance en jeu (saison, climat, sol, humidité) et effets sur le sol. */
final class CropGrowth {

    private CropGrowth() {
    }

    /** Facteur total d'une culture du mod à sa position. */
    static float tvFactor(Level level, BlockPos cropPos, CropDefinition def) {
        BlockPos soil = cropPos.below();
        return GrowthModel.totalFactor(
                SeasonApi.seasonGrowthMultiplier(level),
                def.affinityFor(SeasonApi.getSeason(level)).getMultiplier(),
                GrowthModel.climateFactor(ClimateApi.temperatureAt(level, cropPos), def.tempMin(), def.tempMax()),
                SoilApi.nutrientFactor(level, soil),
                moisture(level, soil));
    }

    /**
     * Facteur total d'une culture vanilla : affinité possible (1.0) et plage 5 à 30 °C. Sol et humidité
     * ne s'appliquent que si la culture est sur une terre labourée.
     */
    static float vanillaFactor(Level level, BlockPos pos) {
        BlockPos soil = pos.below();
        boolean farmland = level.getBlockState(soil).is(Blocks.FARMLAND);
        return GrowthModel.totalFactor(
                SeasonApi.seasonGrowthMultiplier(level),
                Affinity.POSSIBLE.getMultiplier(),
                GrowthModel.climateFactor(ClimateApi.temperatureAt(level, pos), CropLogic.VANILLA_TEMP_MIN, CropLogic.VANILLA_TEMP_MAX),
                farmland ? SoilApi.nutrientFactor(level, soil) : 1.0f,
                farmland ? moisture(level, soil) : 1.0f);
    }

    /** Consommation N/P/K d'un stade gagné. Une légumineuse fixe de l'azote au lieu d'en consommer. */
    static void consumeStage(ServerLevel level, BlockPos cropPos, CropDefinition def) {
        BlockPos soil = cropPos.below();
        CropDefinition.Nutrients cost = def.stageCost();
        SoilApi.consume(level, soil, def.legume() ? 0 : cost.n(), cost.p(), cost.k());
        if (def.legume()) {
            SoilApi.fertilize(level, soil, CropLogic.LEGUME_NITROGEN_FIXATION, 0, 0, 0);
        }
    }

    /** Consommation N/P/K de la récolte d'une culture. */
    static void consumeHarvest(ServerLevel level, BlockPos cropPos, CropDefinition def) {
        CropDefinition.Nutrients cost = def.harvestCost();
        SoilApi.consume(level, cropPos.below(), cost.n(), cost.p(), cost.k());
    }

    private static float moisture(Level level, BlockPos soil) {
        BlockState state = level.getBlockState(soil);
        return state.hasProperty(FarmBlock.MOISTURE)
                ? CropLogic.moistureFactor(state.getValue(FarmBlock.MOISTURE))
                : 1.0f;
    }
}
