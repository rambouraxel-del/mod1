package fr.terresvivantes.water;

import fr.terresvivantes.crop.CropGrowthHooks;
import fr.terresvivantes.season.SeasonApi;
import fr.terresvivantes.season.logic.Season;
import fr.terresvivantes.water.logic.WaterLogic;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Branchements du module water sur la croissance des cultures (contrats {@link CropGrowthHooks}). */
final class WaterGrowth {

    /** Culture dont la terre est dans la zone d'un arroseur actif : facteur de croissance ×1.1. */
    static final CropGrowthHooks.GrowthModifier MODIFIER = (level, cropPos, state, def) ->
            WaterLogic.growthFactor(WaterApi.isCoveredByActiveSprinkler(level, cropPos.below()));

    /**
     * Sécheresse estivale : à chaque tentative de croissance (culture du mod ou vanilla), en été, sans pluie et
     * sans arroseur actif, la terre perd 1 d'humidité avec la probabilité configurée. Les vanilla ne sont notifiées
     * qu'une fois (à Pre), donc une tentative = un tirage.
     */
    static final CropGrowthHooks.GrowthListener DROUGHT = (level, cropPos, state, def, decision, factor) -> {
        if (!WaterConfig.droughtEnabled()) {
            return;
        }
        BlockPos soil = cropPos.below();
        boolean summer = SeasonApi.getSeason(level) == Season.SUMMER;
        boolean raining = level.isRainingAt(cropPos);
        boolean covered = WaterApi.isCoveredByActiveSprinkler(level, soil);
        if (!WaterLogic.droughtApplies(summer, raining, covered)) {
            return;
        }
        if (!WaterLogic.rollDrought(level.getRandom().nextDouble(), WaterConfig.droughtChance())) {
            return;
        }
        dry(level, soil);
    };

    private WaterGrowth() {
    }

    private static void dry(Level level, BlockPos soil) {
        BlockState state = level.getBlockState(soil);
        if (!state.is(Blocks.FARMLAND)) {
            return;
        }
        int moisture = WaterLogic.dried(state.getValue(FarmBlock.MOISTURE));
        level.setBlock(soil, state.setValue(FarmBlock.MOISTURE, moisture), Block.UPDATE_CLIENTS);
    }
}
