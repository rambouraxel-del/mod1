package fr.terresvivantes.crop;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Configuration du module crop. Spec COMMON propre au module; l'intégrateur l'enregistre avec
 * {@code container.registerConfig(ModConfig.Type.COMMON, CropConfig.SPEC, CropConfig.FILE_NAME)}.
 */
public final class CropConfig {

    /** Nom du fichier de configuration (dans config/). */
    public static final String FILE_NAME = "terresvivantes-crop.toml";

    public static final ModConfigSpec SPEC;

    private static final ModConfigSpec.BooleanValue VANILLA_INTEGRATION;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("crop");
        VANILLA_INTEGRATION = builder
                .comment("Applique saison, climat et sol aux cultures vanilla (blé, carotte, pomme de terre, betterave, tiges).")
                .define("vanillaIntegration", true);
        builder.pop();
        SPEC = builder.build();
    }

    private CropConfig() {
    }

    /** Vrai si l'intégration des cultures vanilla est active. Vrai aussi tant que la config n'est pas chargée. */
    public static boolean vanillaIntegrationEnabled() {
        return !SPEC.isLoaded() || VANILLA_INTEGRATION.get();
    }
}
