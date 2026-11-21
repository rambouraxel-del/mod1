package fr.terresvivantes.soil;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Configuration du module soil. L'integrateur enregistre SPEC (type COMMON) sous FILE_NAME.
 */
public final class SoilConfig {

    /** Nom du fichier de configuration. */
    public static final String FILE_NAME = "terresvivantes-soil.toml";

    /** Valeur utilisee tant que la configuration n'est pas chargee. */
    static final double DEFAULT_CONSUMPTION_MULTIPLIER = 1.0;

    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.DoubleValue CONSUMPTION_MULTIPLIER;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.comment("Sol : multiplicateur global de consommation N/P/K des cultures (0 a 10).")
                .push("soil");
        CONSUMPTION_MULTIPLIER = builder
                .defineInRange("soilConsumptionMultiplier", DEFAULT_CONSUMPTION_MULTIPLIER, 0.0, 10.0);
        builder.pop();
        SPEC = builder.build();
    }

    private SoilConfig() {
    }

    /** Multiplicateur de consommation, ou la valeur par defaut si la config n'est pas encore chargee. */
    public static double consumptionMultiplier() {
        try {
            return CONSUMPTION_MULTIPLIER.get();
        } catch (RuntimeException notLoaded) {
            return DEFAULT_CONSUMPTION_MULTIPLIER;
        }
    }
}
