package fr.terresvivantes.season.logic;

/** Saisons dans l'ordre du calendrier. Aucun import net.minecraft. */
public enum Season {
    SPRING, SUMMER, AUTUMN, WINTER;

    /** Saison suivante, cyclique (hiver -> printemps). */
    public Season next() {
        return values()[(ordinal() + 1) % values().length];
    }

    /** Clé de traduction (en_us / fr_fr) du nom de la saison. */
    public String langKey() {
        return "terresvivantes.season." + name().toLowerCase(java.util.Locale.ROOT);
    }
}
