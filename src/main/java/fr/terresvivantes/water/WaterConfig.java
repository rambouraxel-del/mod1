package fr.terresvivantes.water;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Configuration du module water. L'intégrateur enregistre SPEC (type COMMON) sous FILE_NAME.
 */
public final class WaterConfig {

    /** Nom du fichier de configuration (dans config/). */
    public static final String FILE_NAME = "terresvivantes-water.toml";

    /** Valeurs utilisées tant que la configuration n'est pas chargée. */
    static final boolean DEFAULT_DROUGHT_ENABLED = true;
    static final double DEFAULT_DROUGHT_CHANCE = 0.15;

    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue DROUGHT_ENABLED;
    public static final ModConfigSpec.DoubleValue DROUGHT_CHANCE;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.comment("Eau : sécheresse estivale sur les cultures non arrosées.")
                .push("water");
        DROUGHT_ENABLED = builder
                .comment("Active le dessèchement estival (sans pluie et sans arroseur actif).")
                .define("droughtEnabled", DEFAULT_DROUGHT_ENABLED);
        DROUGHT_CHANCE = builder
                .comment("Probabilité, à chaque tentative de croissance, de perdre 1 d'humidité sur la terre.")
                .defineInRange("droughtChance", DEFAULT_DROUGHT_CHANCE, 0.0, 1.0);
        builder.pop();
        SPEC = builder.build();
    }

    private WaterConfig() {
    }

    /** Vrai si le dessèchement est actif, ou la valeur par défaut si la config n'est pas encore chargée. */
    public static boolean droughtEnabled() {
        try {
            return DROUGHT_ENABLED.get();
        } catch (RuntimeException notLoaded) {
            return DEFAULT_DROUGHT_ENABLED;
        }
    }

    /** Probabilité de dessèchement, ou la valeur par défaut si la config n'est pas encore chargée. */
    public static double droughtChance() {
        try {
            return DROUGHT_CHANCE.get();
        } catch (RuntimeException notLoaded) {
            return DEFAULT_DROUGHT_CHANCE;
        }
    }
}
