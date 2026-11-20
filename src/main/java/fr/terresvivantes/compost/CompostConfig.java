package fr.terresvivantes.compost;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Configuration du module compost. L'integrateur enregistre SPEC (type COMMON) sous FILE_NAME.
 */
public final class CompostConfig {

    /** Nom du fichier de configuration. */
    public static final String FILE_NAME = "terresvivantes-compost.toml";

    /** Valeur utilisee tant que la configuration n'est pas chargee : environ un fumier par jour MC. */
    static final double DEFAULT_MANURE_CHANCE = 0.05;

    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.DoubleValue MANURE_CHANCE;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.comment("Compost : probabilite qu'un animal lache du fumier a chaque verification (toutes les 1200 ticks).",
                        "La valeur 0.05 donne environ un fumier par jour Minecraft et par animal.")
                .push("compost");
        MANURE_CHANCE = builder.defineInRange("manureChance", DEFAULT_MANURE_CHANCE, 0.0, 1.0);
        builder.pop();
        SPEC = builder.build();
    }

    private CompostConfig() {
    }

    /** Probabilite de fumier par verification, ou la valeur par defaut si la config n'est pas encore chargee. */
    public static double manureChance() {
        try {
            return MANURE_CHANCE.get();
        } catch (RuntimeException notLoaded) {
            return DEFAULT_MANURE_CHANCE;
        }
    }
}
