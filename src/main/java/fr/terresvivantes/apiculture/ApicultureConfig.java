package fr.terresvivantes.apiculture;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Configuration du module apiculture. L'intégrateur enregistre SPEC (type COMMON) sous FILE_NAME.
 */
public final class ApicultureConfig {

    /** Nom du fichier de configuration (dans config/). */
    public static final String FILE_NAME = "terresvivantes-apiculture.toml";

    static final double DEFAULT_ABSORB_CHANCE = 0.25;
    static final int DEFAULT_ANGRY_THRESHOLD = 20;
    static final int DEFAULT_CALM_SECONDS = 30;

    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.DoubleValue ABSORB_CHANCE;
    public static final ModConfigSpec.IntValue ANGRY_THRESHOLD;
    public static final ModConfigSpec.IntValue CALM_SECONDS;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.comment("Apiculture : apiaires, colonies et enfumoir.").push("apiculture");
        ABSORB_CHANCE = builder
                .comment("Chance, à chaque période de 10 s, qu'un apiaire vide absorbe une abeille proche.")
                .defineInRange("absorbChance", DEFAULT_ABSORB_CHANCE, 0.0, 1.0);
        ANGRY_THRESHOLD = builder
                .comment("Population au-delà de laquelle une récolte sans enfumoir énerve les abeilles.")
                .defineInRange("angryThreshold", DEFAULT_ANGRY_THRESHOLD, 0, 100);
        CALM_SECONDS = builder
                .comment("Durée, en secondes, pendant laquelle l'enfumoir permet une récolte calme.")
                .defineInRange("calmSeconds", DEFAULT_CALM_SECONDS, 0, 600);
        builder.pop();
        SPEC = builder.build();
    }

    private ApicultureConfig() {
    }

    /** Chance d'absorption, ou la valeur par défaut si la config n'est pas encore chargée. */
    public static double absorbChance() {
        try {
            return ABSORB_CHANCE.get();
        } catch (RuntimeException notLoaded) {
            return DEFAULT_ABSORB_CHANCE;
        }
    }

    /** Seuil de population, ou la valeur par défaut si la config n'est pas encore chargée. */
    public static int angryThreshold() {
        try {
            return ANGRY_THRESHOLD.get();
        } catch (RuntimeException notLoaded) {
            return DEFAULT_ANGRY_THRESHOLD;
        }
    }

    /** Durée de calme de l'enfumoir en secondes, ou la valeur par défaut si la config n'est pas chargée. */
    public static int calmSeconds() {
        try {
            return CALM_SECONDS.get();
        } catch (RuntimeException notLoaded) {
            return DEFAULT_CALM_SECONDS;
        }
    }
}
