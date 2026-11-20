package fr.terresvivantes.season;

import fr.terresvivantes.season.client.ClientSeasonState;
import fr.terresvivantes.season.logic.Season;
import fr.terresvivantes.season.logic.SeasonLogic;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/**
 * Facade publique du module season. Les autres modules n'appellent que cette classe.
 * La saison est celle de l'overworld, quelle que soit la dimension passee en argument.
 * Cote client, les valeurs viennent du cache synchronise par le serveur.
 */
public final class SeasonApi {

    private SeasonApi() {
    }

    /** Saison courante de l'overworld. */
    public static Season getSeason(Level level) {
        if (level.isClientSide()) {
            return ClientSeasonState.season();
        }
        return SeasonLogic.seasonOf((int) dayIndex(level), SeasonConfig.daysPerSeason());
    }

    /** Jour dans la saison, 1-based. */
    public static int getDayOfSeason(Level level) {
        if (level.isClientSide()) {
            return ClientSeasonState.dayOfSeason();
        }
        return SeasonLogic.dayOfSeason(dayIndex(level), SeasonConfig.daysPerSeason());
    }

    /** Nombre de jours par saison (config serveur). */
    public static int getDaysPerSeason() {
        return SeasonConfig.daysPerSeason();
    }

    /** Multiplicateur de croissance saisonnier (defauts 1.0 / 1.2 / 0.8 / 0.0, configurables). */
    public static float seasonGrowthMultiplier(Level level) {
        return SeasonConfig.multiplier(getSeason(level));
    }

    /**
     * Index absolu du jour de l'overworld. Appele uniquement cote serveur.
     */
    private static long dayIndex(Level level) {
        ServerLevel overworld = ((ServerLevel) level).getServer().overworld();
        return SeasonLogic.dayIndex(overworld.getDayTime(), SeasonSavedData.get(overworld).getOffset());
    }
}
