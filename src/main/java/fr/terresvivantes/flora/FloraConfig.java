package fr.terresvivantes.flora;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Configuration du module flora. L'intégrateur enregistre SPEC (type COMMON) sous FILE_NAME.
 */
public final class FloraConfig {

    /** Nom du fichier de configuration (dans config/). */
    public static final String FILE_NAME = "terresvivantes-flora.toml";

    /** Valeur utilisée tant que la configuration n'est pas chargée. */
    static final double DEFAULT_SPREAD_CHANCE = 0.003;

    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.DoubleValue SPREAD_CHANCE;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.comment("Flore sauvage : propagation des fleurs au printemps et en été.")
                .push("flora");
        SPREAD_CHANCE = builder
                .comment("Probabilité, à chaque randomTick d'une fleur sauvage au printemps ou en été, de tenter",
                        "une copie sur un bloc d'herbe voisin (rayon 2).")
                .defineInRange("spreadChance", DEFAULT_SPREAD_CHANCE, 0.0, 1.0);
        builder.pop();
        SPEC = builder.build();
    }

    private FloraConfig() {
    }

    /** Probabilité de propagation, ou la valeur par défaut si la configuration n'est pas encore chargée. */
    public static float spreadChance() {
        try {
            return SPREAD_CHANCE.get().floatValue();
        } catch (RuntimeException notLoaded) {
            return (float) DEFAULT_SPREAD_CHANCE;
        }
    }
}
