package fr.terresvivantes.season.logic;

/**
 * Logique pure du calendrier saisonnier. Aucun import net.minecraft: testable en JUnit.
 */
public final class SeasonLogic {

    /** Saisons dans l'ordre du calendrier. */
    public enum Season {
        SPRING, SUMMER, AUTUMN, WINTER
    }

    private SeasonLogic() {
    }

    /**
     * Retourne la saison d'un jour de l'annee, le cycle complet faisant 4 * daysPerSeason jours.
     *
     * @param dayOfYear     jour (peut etre negatif, le cycle est circulaire)
     * @param daysPerSeason duree d'une saison en jours, strictement positive
     */
    public static Season seasonOf(int dayOfYear, int daysPerSeason) {
        if (daysPerSeason <= 0) {
            throw new IllegalArgumentException("daysPerSeason doit etre > 0");
        }
        int day = Math.floorMod(dayOfYear, daysPerSeason * Season.values().length);
        return Season.values()[day / daysPerSeason];
    }
}
