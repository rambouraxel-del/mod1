package fr.terresvivantes.pests;

import fr.terresvivantes.pests.logic.CrowLogic;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Configuration du module pests. A brancher par l'intégrateur :
 * {@code container.registerConfig(ModConfig.Type.COMMON, PestsConfig.SPEC, PestsConfig.FILE_NAME)}.
 *
 * Tant que la spec n'est pas chargée, les accesseurs renvoient les valeurs par défaut.
 */
public final class PestsConfig {

    public static final String FILE_NAME = "terresvivantes-pests.toml";
    public static final ModConfigSpec SPEC;

    private static final boolean DEFAULT_CROWS_ENABLED = true;
    private static final double DEFAULT_PECK_CHANCE = 0.25;
    private static final int DEFAULT_SCARECROW_RADIUS = 12;

    private static final ModConfigSpec.BooleanValue CROWS_ENABLED;
    private static final ModConfigSpec.DoubleValue PECK_CHANCE;
    private static final ModConfigSpec.IntValue SCARECROW_RADIUS;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.push("crows");
        CROWS_ENABLED = builder
                .comment("Active les corbeaux : false désactive leur apparition naturelle et les corbeaux présents restent inoffensifs.")
                .define("enabled", DEFAULT_CROWS_ENABLED);
        PECK_CHANCE = builder
                .comment("Chance (0 à 1) qu'un coup de bec fasse régresser une culture mûre de deux stades.")
                .defineInRange("peckChance", DEFAULT_PECK_CHANCE, 0.0, 1.0);
        builder.pop();

        builder.push("scarecrow");
        SCARECROW_RADIUS = builder
                .comment("Rayon, en blocs, dans lequel un épouvantail neuf éloigne les corbeaux (divisé par deux une fois usé).")
                .defineInRange("radius", DEFAULT_SCARECROW_RADIUS, 1, 64);
        builder.pop();

        SPEC = builder.build();
    }

    private PestsConfig() {
    }

    /** Corbeaux actifs (défaut vrai tant que la config n'est pas chargée). */
    public static boolean crowsEnabled() {
        return SPEC.isLoaded() ? CROWS_ENABLED.get() : DEFAULT_CROWS_ENABLED;
    }

    /** Chance de régression par coup de bec, clampée dans [0, 1]. */
    public static double peckChance() {
        double value = SPEC.isLoaded() ? PECK_CHANCE.get() : DEFAULT_PECK_CHANCE;
        return CrowLogic.clampChance(value);
    }

    /** Rayon de base de l'épouvantail, en blocs. */
    public static int scarecrowRadius() {
        return SPEC.isLoaded() ? SCARECROW_RADIUS.get() : DEFAULT_SCARECROW_RADIUS;
    }
}
