package fr.terresvivantes.season;

import fr.terresvivantes.season.logic.Season;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Configuration du module season. A brancher par l'integrateur :
 * {@code container.registerConfig(ModConfig.Type.COMMON, SeasonConfig.SPEC, SeasonConfig.FILE_NAME)}.
 *
 * Tant que la spec n'est pas chargee, les accesseurs renvoient les valeurs par defaut
 * (ne jamais appeler {@code get()} directement hors de ces accesseurs).
 */
public final class SeasonConfig {

    public static final String FILE_NAME = "terresvivantes-season.toml";
    public static final ModConfigSpec SPEC;

    private static final ModConfigSpec.IntValue DAYS_PER_SEASON;
    private static final ModConfigSpec.DoubleValue SPRING_MULTIPLIER;
    private static final ModConfigSpec.DoubleValue SUMMER_MULTIPLIER;
    private static final ModConfigSpec.DoubleValue AUTUMN_MULTIPLIER;
    private static final ModConfigSpec.DoubleValue WINTER_MULTIPLIER;
    private static final ModConfigSpec.DoubleValue GREENHOUSE_WINTER_MULTIPLIER;
    private static final ModConfigSpec.BooleanValue HUD_ENABLED;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        DAYS_PER_SEASON = builder
                .comment("Nombre de jours par saison.")
                .defineInRange("daysPerSeason", 7, 1, 365);

        builder.comment("Multiplicateurs de croissance saisonniers.")
                .push("seasonMultipliers");
        SPRING_MULTIPLIER = builder.defineInRange("spring", 1.0, 0.0, 10.0);
        SUMMER_MULTIPLIER = builder.defineInRange("summer", 1.2, 0.0, 10.0);
        AUTUMN_MULTIPLIER = builder.defineInRange("autumn", 0.8, 0.0, 10.0);
        WINTER_MULTIPLIER = builder.defineInRange("winter", 0.15, 0.0, 10.0);
        builder.pop();

        GREENHOUSE_WINTER_MULTIPLIER = builder
                .comment("Multiplicateur d'hiver pour une culture abritée sous verre (serre).")
                .defineInRange("greenhouseWinterMultiplier", 0.6, 0.0, 10.0);

        builder.push("hud");
        HUD_ENABLED = builder.comment("Affiche la saison en haut a gauche (cote client).")
                .define("enabled", true);
        builder.pop();

        SPEC = builder.build();
    }

    private SeasonConfig() {
    }

    /** Nombre de jours par saison (defaut 7 tant que la config n'est pas chargee). */
    public static int daysPerSeason() {
        return SPEC.isLoaded() ? DAYS_PER_SEASON.get() : 7;
    }

    /** Multiplicateur de croissance d'une saison (defauts 1.0 / 1.2 / 0.8 / 0.15). */
    public static float multiplier(Season season) {
        if (!SPEC.isLoaded()) {
            return switch (season) {
                case SPRING -> 1.0f;
                case SUMMER -> 1.2f;
                case AUTUMN -> 0.8f;
                case WINTER -> 0.15f;
            };
        }
        double value = switch (season) {
            case SPRING -> SPRING_MULTIPLIER.get();
            case SUMMER -> SUMMER_MULTIPLIER.get();
            case AUTUMN -> AUTUMN_MULTIPLIER.get();
            case WINTER -> WINTER_MULTIPLIER.get();
        };
        return (float) value;
    }

    /** Multiplicateur d'hiver sous verre (defaut 0.6 tant que la config n'est pas chargee). */
    public static float greenhouseWinterMultiplier() {
        return SPEC.isLoaded() ? GREENHOUSE_WINTER_MULTIPLIER.get().floatValue() : 0.6f;
    }

    /** Affichage du HUD saison (defaut vrai tant que la config n'est pas chargee). */
    public static boolean hudEnabled() {
        return !SPEC.isLoaded() || HUD_ENABLED.get();
    }
}
