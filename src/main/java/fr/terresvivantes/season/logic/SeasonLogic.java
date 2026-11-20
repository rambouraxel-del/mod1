package fr.terresvivantes.season.logic;

/**
 * Logique pure du calendrier saisonnier. Aucun import net.minecraft: testable en JUnit.
 *
 * Formules (docs/DESIGN.md section 2.1):
 * dayIndex = floor(dayTime / 24000) + offset
 * dayOfSeason (1-based) = dayIndex mod daysPerSeason + 1
 */
public final class SeasonLogic {

    /** Duree d'un jour Minecraft en ticks. */
    public static final long TICKS_PER_DAY = 24000L;

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

    /** Index absolu du jour : floor(dayTime / 24000) + offset. */
    public static long dayIndex(long dayTime, long offset) {
        return Math.floorDiv(dayTime, TICKS_PER_DAY) + offset;
    }

    /** Jour dans la saison, 1-based. */
    public static int dayOfSeason(long dayIndex, int daysPerSeason) {
        requirePositive(daysPerSeason);
        return (int) Math.floorMod(dayIndex, (long) daysPerSeason) + 1;
    }

    /**
     * Index absolu visé par {@code /tv season set <season> [day]} : premier jour {@code day}
     * de la saison {@code season} dans l'annee courante (celle qui contient {@code currentDayIndex}).
     *
     * @param day 1-based, entre 1 et daysPerSeason inclus
     */
    public static long targetDayIndex(long currentDayIndex, Season season, int day, int daysPerSeason) {
        requirePositive(daysPerSeason);
        if (day < 1 || day > daysPerSeason) {
            throw new IllegalArgumentException("day doit etre entre 1 et " + daysPerSeason);
        }
        long yearLength = (long) daysPerSeason * Season.values().length;
        long yearStart = Math.floorDiv(currentDayIndex, yearLength) * yearLength;
        return yearStart + (long) season.ordinal() * daysPerSeason + (day - 1);
    }

    /** Offset a persister pour que dayIndex(dayTime, offset) egale {@code targetDayIndex}. */
    public static long offsetFor(long targetDayIndex, long dayTime) {
        return targetDayIndex - Math.floorDiv(dayTime, TICKS_PER_DAY);
    }

    private static void requirePositive(int daysPerSeason) {
        if (daysPerSeason <= 0) {
            throw new IllegalArgumentException("daysPerSeason doit etre > 0");
        }
    }
}
