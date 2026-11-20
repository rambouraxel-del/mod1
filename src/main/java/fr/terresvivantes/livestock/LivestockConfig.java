package fr.terresvivantes.livestock;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Configuration du module livestock. A brancher par l'intégrateur :
 * {@code container.registerConfig(ModConfig.Type.COMMON, LivestockConfig.SPEC, LivestockConfig.FILE_NAME)}.
 *
 * Tant que la spec n'est pas chargée, les accesseurs renvoient les valeurs par défaut.
 */
public final class LivestockConfig {

    public static final String FILE_NAME = "terresvivantes-livestock.toml";
    public static final ModConfigSpec SPEC;

    private static final ModConfigSpec.BooleanValue BREEDING_SEASONS_ONLY;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        BREEDING_SEASONS_ONLY = builder
                .comment("Reproduction seulement au printemps et en été. Faux : reproduction possible toute l'année (satiété suffisante requise).")
                .define("breedingSeasonsOnly", true);

        SPEC = builder.build();
    }

    private LivestockConfig() {
    }

    /** Vrai si la reproduction est limitée au printemps et à l'été (défaut vrai tant que la config n'est pas chargée). */
    public static boolean breedingSeasonsOnly() {
        return !SPEC.isLoaded() || BREEDING_SEASONS_ONLY.get();
    }
}
