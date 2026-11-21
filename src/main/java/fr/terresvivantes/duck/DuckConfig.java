package fr.terresvivantes.duck;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Configuration du module duck. A brancher par l'intégrateur :
 * {@code container.registerConfig(ModConfig.Type.COMMON, DuckConfig.SPEC, DuckConfig.FILE_NAME)}.
 *
 * Tant que la spec n'est pas chargée, les accesseurs renvoient les valeurs par défaut.
 */
public final class DuckConfig {

    public static final String FILE_NAME = "terresvivantes-duck.toml";
    public static final ModConfigSpec SPEC;

    private static final int DEFAULT_EGG_MIN_TICKS = 6000;
    private static final int DEFAULT_EGG_MAX_TICKS = 12000;

    private static final ModConfigSpec.IntValue EGG_MIN_TICKS;
    private static final ModConfigSpec.IntValue EGG_MAX_TICKS;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.push("eggs");
        EGG_MIN_TICKS = builder
                .comment("Délai minimal entre deux pontes, en ticks (avant modulation par la productivité).")
                .defineInRange("minTicks", DEFAULT_EGG_MIN_TICKS, 1, 1_000_000);
        EGG_MAX_TICKS = builder
                .comment("Délai maximal entre deux pontes, en ticks (avant modulation par la productivité).")
                .defineInRange("maxTicks", DEFAULT_EGG_MAX_TICKS, 1, 1_000_000);
        builder.pop();

        SPEC = builder.build();
    }

    private DuckConfig() {
    }

    /** Délai minimal de ponte (défaut 6000 tant que la config n'est pas chargée). */
    public static int eggMinTicks() {
        return SPEC.isLoaded() ? EGG_MIN_TICKS.get() : DEFAULT_EGG_MIN_TICKS;
    }

    /** Délai maximal de ponte (défaut 12000 tant que la config n'est pas chargée). */
    public static int eggMaxTicks() {
        return SPEC.isLoaded() ? EGG_MAX_TICKS.get() : DEFAULT_EGG_MAX_TICKS;
    }
}
