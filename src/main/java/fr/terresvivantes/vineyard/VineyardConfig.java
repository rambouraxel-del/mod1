package fr.terresvivantes.vineyard;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Configuration du module vineyard. A brancher par l'intégrateur :
 * {@code container.registerConfig(ModConfig.Type.COMMON, VineyardConfig.SPEC, VineyardConfig.FILE_NAME)}.
 *
 * Tant que la spec n'est pas chargée, les accesseurs renvoient les valeurs par défaut.
 */
public final class VineyardConfig {

    public static final String FILE_NAME = "terresvivantes-vineyard.toml";
    public static final ModConfigSpec SPEC;

    private static final double DEFAULT_STEP_DAYS = 4.0;

    private static final ModConfigSpec.DoubleValue STEP_DAYS;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.push("growth");
        STEP_DAYS = builder
                .comment("Durée, en jours MC à facteur 1, d'une étape de croissance de la vigne (0 à 1, puis 1 à 2, puis 2 à 3).")
                .defineInRange("stepDays", DEFAULT_STEP_DAYS, 0.5, 60.0);
        builder.pop();

        SPEC = builder.build();
    }

    private VineyardConfig() {
    }

    /** Durée d'une étape de croissance, en jours MC à facteur 1. */
    public static double stepDays() {
        return SPEC.isLoaded() ? STEP_DAYS.get() : DEFAULT_STEP_DAYS;
    }
}
