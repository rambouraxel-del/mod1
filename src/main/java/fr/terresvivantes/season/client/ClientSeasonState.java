package fr.terresvivantes.season.client;

import fr.terresvivantes.season.logic.Season;
import fr.terresvivantes.season.network.SeasonSyncPayload;

/**
 * Cache cote client de la saison recue du serveur. Ne contient aucun import client:
 * il peut etre lu depuis le code commun, il renvoie les valeurs par defaut avant la premiere synchro.
 */
public final class ClientSeasonState {

    private static volatile Season season = Season.SPRING;
    private static volatile int dayOfSeason = 1;
    private static volatile int daysPerSeason = 7;

    private ClientSeasonState() {
    }

    public static void update(SeasonSyncPayload payload) {
        season = payload.season();
        dayOfSeason = payload.dayOfSeason();
        daysPerSeason = payload.daysPerSeason();
    }

    public static Season season() {
        return season;
    }

    public static int dayOfSeason() {
        return dayOfSeason;
    }

    public static int daysPerSeason() {
        return daysPerSeason;
    }
}
