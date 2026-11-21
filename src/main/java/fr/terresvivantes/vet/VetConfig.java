package fr.terresvivantes.vet;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Configuration du module vet. A brancher par l'intégrateur :
 * {@code container.registerConfig(ModConfig.Type.COMMON, VetConfig.SPEC, VetConfig.FILE_NAME)}.
 *
 * Tant que la spec n'est pas chargée, les accesseurs renvoient les valeurs par défaut.
 */
public final class VetConfig {

    public static final String FILE_NAME = "terresvivantes-vet.toml";
    public static final ModConfigSpec SPEC;

    private static final ModConfigSpec.BooleanValue KILL_AT_ZERO_HEALTH;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        KILL_AT_ZERO_HEALTH = builder
                .comment("Un animal dont la santé tombe à 0 meurt. Faux : il reste à 0 sans mourir.")
                .define("killAtZeroHealth", true);

        SPEC = builder.build();
    }

    private VetConfig() {
    }

    /** Vrai si un animal à santé 0 meurt (défaut vrai tant que la config n'est pas chargée). */
    public static boolean killAtZeroHealth() {
        return !SPEC.isLoaded() || KILL_AT_ZERO_HEALTH.get();
    }
}
