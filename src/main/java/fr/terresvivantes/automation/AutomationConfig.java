package fr.terresvivantes.automation;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Configuration du module automation. L'intégrateur enregistre SPEC (type COMMON) sous FILE_NAME.
 */
public final class AutomationConfig {

    /** Nom du fichier de configuration (dans config/). */
    public static final String FILE_NAME = "terresvivantes-automation.toml";

    /** Valeurs utilisées tant que la configuration n'est pas chargée. */
    static final boolean DEFAULT_ENABLED = true;
    static final int DEFAULT_INTERVAL = 40;
    static final int DEFAULT_RADIUS = 2;

    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED;
    public static final ModConfigSpec.IntValue INTERVAL;
    public static final ModConfigSpec.IntValue RADIUS;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("automation");
        ENABLED = builder
                .comment("Active les machines automatiques (récolteuse, planteuse). Désactivées, elles ne scannent plus rien.")
                .define("enabled", DEFAULT_ENABLED);
        INTERVAL = builder
                .comment("Intervalle, en ticks, entre deux cycles de travail d'une machine (40 = 2 secondes).")
                .defineInRange("intervalTicks", DEFAULT_INTERVAL, 10, 1200);
        RADIUS = builder
                .comment("Rayon de la zone de travail devant la machine : 2 donne une zone de 5 x 5.")
                .defineInRange("radius", DEFAULT_RADIUS, 1, 4);
        builder.pop();
        SPEC = builder.build();
    }

    private AutomationConfig() {
    }

    /** Vrai si les machines automatiques sont actives. */
    public static boolean enabled() {
        try {
            return ENABLED.get();
        } catch (RuntimeException notLoaded) {
            return DEFAULT_ENABLED;
        }
    }

    /** Intervalle entre deux cycles, en ticks. */
    public static int intervalTicks() {
        try {
            return INTERVAL.get();
        } catch (RuntimeException notLoaded) {
            return DEFAULT_INTERVAL;
        }
    }

    /** Rayon de la zone de travail. */
    public static int radius() {
        try {
            return RADIUS.get();
        } catch (RuntimeException notLoaded) {
            return DEFAULT_RADIUS;
        }
    }
}
