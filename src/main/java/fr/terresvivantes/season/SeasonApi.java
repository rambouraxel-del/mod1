package fr.terresvivantes.season;

import fr.terresvivantes.season.logic.Season;
import fr.terresvivantes.season.logic.SeasonLogic;
import net.minecraft.world.level.Level;

/** Façade publique du module season. Les autres modules n'appellent que cette classe. */
public final class SeasonApi {

    /** Durée provisoire d'une saison en jours (config à venir). */
    private static final int DAYS_PER_SEASON = 7;

    private SeasonApi() {
    }

    /** Saison courante de l'overworld. STUB – implémenté par le module season. */
    public static Season getSeason(Level level) {
        return SeasonLogic.seasonOf(dayIndex(level), DAYS_PER_SEASON);
    }

    /** Jour dans la saison, 1-based. STUB – implémenté par le module season. */
    public static int getDayOfSeason(Level level) {
        return dayIndex(level) % DAYS_PER_SEASON + 1;
    }

    /** Nombre de jours par saison. STUB – implémenté par le module season. */
    public static int getDaysPerSeason() {
        return DAYS_PER_SEASON;
    }

    /** Multiplicateur de croissance saisonnier (1.0 / 1.2 / 0.8 / 0.0). STUB – implémenté par le module season. */
    public static float seasonGrowthMultiplier(Level level) {
        return switch (getSeason(level)) {
            case SPRING -> 1.0f;
            case SUMMER -> 1.2f;
            case AUTUMN -> 0.8f;
            case WINTER -> 0.0f;
        };
    }

    /** Index de jour dans l'année complète. STUB – implémenté par le module season. */
    private static int dayIndex(Level level) {
        return (int) Math.floorMod(Math.floorDiv(level.getDayTime(), 24000L), 4L * DAYS_PER_SEASON);
    }
}
