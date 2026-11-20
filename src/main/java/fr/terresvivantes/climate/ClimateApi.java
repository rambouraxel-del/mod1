package fr.terresvivantes.climate;

import fr.terresvivantes.climate.logic.ClimateModel;
import fr.terresvivantes.season.SeasonApi;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/** Facade publique du module climate. */
public final class ClimateApi {

    private ClimateApi() {
    }

    /**
     * Temperature locale en °C. Overworld : biome, saison, altitude et nuit.
     * Autres dimensions : biome et altitude seulement.
     */
    public static float temperatureAt(Level level, BlockPos pos) {
        float biomeBase = level.getBiome(pos).value().getBaseTemperature();
        if (Level.OVERWORLD.equals(level.dimension())) {
            return ClimateModel.overworldTemperature(
                    biomeBase, SeasonApi.getSeason(level), pos.getY(), level.getDayTime());
        }
        return ClimateModel.otherDimensionTemperature(biomeBase, pos.getY());
    }
}
