package fr.terresvivantes.climate.logic;

import fr.terresvivantes.season.logic.Season;

/**
 * Modele pur de temperature locale (docs/DESIGN.md section 3.1). Aucun import net.minecraft.
 *
 * T = baseBiome * 25 + offsetSaison + offsetAltitude + offsetJourNuit  (en °C)
 */
public final class ClimateModel {

    /** Facteur entre la temperature de base vanilla du biome et les degres Celsius. */
    public static final float BIOME_SCALE = 25.0f;
    /** Ordonnee de reference pour l'altitude : aucun bonus en dessous. */
    public static final int ALTITUDE_REFERENCE_Y = 64;
    /** Degres perdus par bloc au-dessus de la reference (-0.6 °C par tranche de 10 blocs). */
    public static final float ALTITUDE_PER_BLOCK = -0.06f;
    /** Ecart de nuit, en degres. */
    public static final float NIGHT_OFFSET = -4.0f;

    private static final long TICKS_PER_DAY = 24000L;
    private static final long NIGHT_START = 13000L;
    private static final long NIGHT_END = 23000L;

    private ClimateModel() {
    }

    /** Temperature de base d'un biome, en °C. */
    public static float baseTemperature(float biomeBaseTemperature) {
        return biomeBaseTemperature * BIOME_SCALE;
    }

    /** Ecart saisonnier : printemps 0, ete +8, automne -2, hiver -12. */
    public static float seasonOffset(Season season) {
        return switch (season) {
            case SPRING -> 0.0f;
            case SUMMER -> 8.0f;
            case AUTUMN -> -2.0f;
            case WINTER -> -12.0f;
        };
    }

    /** Ecart d'altitude : -0.06 par bloc au-dessus de y=64, zero en dessous. */
    public static float altitudeOffset(int y) {
        return ALTITUDE_PER_BLOCK * Math.max(0, y - ALTITUDE_REFERENCE_Y);
    }

    /** Nuit vanilla : temps de jour dans [13000, 23000[. */
    public static boolean isNight(long dayTime) {
        long timeOfDay = Math.floorMod(dayTime, TICKS_PER_DAY);
        return timeOfDay >= NIGHT_START && timeOfDay < NIGHT_END;
    }

    /** Temperature de l'overworld : biome, saison, altitude et cycle jour/nuit. */
    public static float overworldTemperature(float biomeBaseTemperature, Season season, int y, long dayTime) {
        float nightOffset = isNight(dayTime) ? NIGHT_OFFSET : 0.0f;
        return baseTemperature(biomeBaseTemperature)
                + seasonOffset(season)
                + altitudeOffset(y)
                + nightOffset;
    }

    /**
     * Temperature hors overworld : pas de saison ni de cycle (docs/DESIGN.md, decision proposee).
     * Seuls le biome et l'altitude comptent.
     */
    public static float otherDimensionTemperature(float biomeBaseTemperature, int y) {
        return baseTemperature(biomeBaseTemperature) + altitudeOffset(y);
    }
}
