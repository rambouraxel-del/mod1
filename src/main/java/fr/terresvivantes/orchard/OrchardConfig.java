package fr.terresvivantes.orchard;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Configuration du module orchard. L'intégrateur enregistre SPEC (type COMMON) sous FILE_NAME.
 */
public final class OrchardConfig {

    /** Nom du fichier de configuration (dans config/). */
    public static final String FILE_NAME = "terresvivantes-orchard.toml";

    /** Valeurs utilisées tant que la configuration n'est pas chargée. */
    static final double DEFAULT_TRANSITION_CHANCE = 0.25;
    static final double DEFAULT_SAPLING_CHANCE = 0.1;

    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.DoubleValue TRANSITION_CHANCE;
    public static final ModConfigSpec.DoubleValue SAPLING_CHANCE;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("orchard");
        TRANSITION_CHANCE = builder
                .comment("Probabilité, à chaque randomTick d'une feuille fruitière, d'évoluer vers le stade suivant (si la saison et la température le permettent).")
                .defineInRange("transitionChance", DEFAULT_TRANSITION_CHANCE, 0.0, 1.0);
        SAPLING_CHANCE = builder
                .comment("Probabilité, à chaque randomTick d'un sapling en saison favorable, de pousser en arbre (multipliée par le facteur saisonnier).")
                .defineInRange("saplingChance", DEFAULT_SAPLING_CHANCE, 0.0, 1.0);
        builder.pop();
        SPEC = builder.build();
    }

    private OrchardConfig() {
    }

    /** Probabilité de transition des feuilles, ou la valeur par défaut si la configuration n'est pas chargée. */
    public static double transitionChance() {
        try {
            return TRANSITION_CHANCE.get();
        } catch (RuntimeException notLoaded) {
            return DEFAULT_TRANSITION_CHANCE;
        }
    }

    /** Probabilité de croissance d'un sapling, ou la valeur par défaut si la configuration n'est pas chargée. */
    public static double saplingGrowthChance() {
        try {
            return SAPLING_CHANCE.get();
        } catch (RuntimeException notLoaded) {
            return DEFAULT_SAPLING_CHANCE;
        }
    }
}
