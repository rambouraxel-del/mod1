package fr.terresvivantes.climate;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/** Façade publique du module climate. */
public final class ClimateApi {

    private ClimateApi() {
    }

    /** Température locale en °C. STUB – implémenté par le module climate (biome × 25 uniquement). */
    public static float temperatureAt(Level level, BlockPos pos) {
        return level.getBiome(pos).value().getBaseTemperature() * 25.0f;
    }
}
