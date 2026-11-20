package fr.terresvivantes.health;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Configuration du module health. A brancher par l'integrateur :
 * {@code container.registerConfig(ModConfig.Type.COMMON, HealthConfig.SPEC, HealthConfig.FILE_NAME)}.
 *
 * Tant que la spec n'est pas chargee, les accesseurs renvoient les valeurs par defaut
 * (ne jamais appeler {@code get()} directement hors de ces accesseurs).
 */
public final class HealthConfig {

    public static final String FILE_NAME = "terresvivantes-health.toml";
    public static final ModConfigSpec SPEC;

    private static final ModConfigSpec.BooleanValue WITHERING_ENABLED;
    private static final ModConfigSpec.BooleanValue DISEASES_ENABLED;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        WITHERING_ENABLED = builder
                .comment("Flétrissement : une culture dont le stress atteint 4 devient une culture flétrie.")
                .define("witheringEnabled", true);

        DISEASES_ENABLED = builder
                .comment("Maladies (oïdium, rouille) : infection, effets sur la croissance et le rendement.")
                .define("diseasesEnabled", true);

        SPEC = builder.build();
    }

    /** Bascule de test : coupe maladies et flétrissement sans toucher au TOML. Toujours faux hors GameTest. */
    private static volatile boolean disabledForTest;

    private HealthConfig() {
    }

    /**
     * Coupe (ou rétablit) maladies et flétrissement pour un GameTest qui force des randomTicks. Le test doit
     * rétablir la valeur {@code false} avant de finir.
     */
    public static void disableRandomEventsForTest(boolean disabled) {
        disabledForTest = disabled;
    }

    /** Vrai si le flétrissement par stress est actif (défaut vrai tant que la config n'est pas chargée). */
    public static boolean witheringEnabled() {
        return !disabledForTest && (!SPEC.isLoaded() || WITHERING_ENABLED.get());
    }

    /** Vrai si les maladies sont actives (défaut vrai tant que la config n'est pas chargée). */
    public static boolean diseasesEnabled() {
        return !disabledForTest && (!SPEC.isLoaded() || DISEASES_ENABLED.get());
    }
}
