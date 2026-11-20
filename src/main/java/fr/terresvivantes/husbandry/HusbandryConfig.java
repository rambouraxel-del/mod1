package fr.terresvivantes.husbandry;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Configuration du module husbandry. A brancher par l'intégrateur :
 * {@code container.registerConfig(ModConfig.Type.COMMON, HusbandryConfig.SPEC, HusbandryConfig.FILE_NAME)}.
 *
 * Tant que la spec n'est pas chargée, les accesseurs renvoient les valeurs par défaut.
 */
public final class HusbandryConfig {

    public static final String FILE_NAME = "terresvivantes-husbandry.toml";
    public static final ModConfigSpec SPEC;

    private static final int DEFAULT_TROUGH_RADIUS = 6;
    private static final int DEFAULT_NESTING_RADIUS = 4;

    private static final ModConfigSpec.IntValue TROUGH_RADIUS;
    private static final ModConfigSpec.IntValue NESTING_RADIUS;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        TROUGH_RADIUS = builder
                .comment("Rayon (en blocs) dans lequel une auge nourrit les animaux d'élevage.")
                .defineInRange("troughRadius", DEFAULT_TROUGH_RADIUS, 1, 16);
        NESTING_RADIUS = builder
                .comment("Rayon (en blocs) dans lequel une poule pond dans un nichoir.")
                .defineInRange("nestingRadius", DEFAULT_NESTING_RADIUS, 1, 16);

        SPEC = builder.build();
    }

    private HusbandryConfig() {
    }

    /** Rayon d'alimentation d'une auge (défaut 6 tant que la config n'est pas chargée). */
    public static int troughRadius() {
        return SPEC.isLoaded() ? TROUGH_RADIUS.get() : DEFAULT_TROUGH_RADIUS;
    }

    /** Rayon de ponte vers un nichoir (défaut 4 tant que la config n'est pas chargée). */
    public static int nestingRadius() {
        return SPEC.isLoaded() ? NESTING_RADIUS.get() : DEFAULT_NESTING_RADIUS;
    }
}
