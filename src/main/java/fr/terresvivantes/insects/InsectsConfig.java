package fr.terresvivantes.insects;

import fr.terresvivantes.insects.logic.AphidLogic;
import fr.terresvivantes.insects.logic.FireflyLogic;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Configuration du module insects. A brancher par l'integrateur :
 * {@code container.registerConfig(ModConfig.Type.COMMON, InsectsConfig.SPEC, InsectsConfig.FILE_NAME)}.
 *
 * Tant que la spec n'est pas chargee, les accesseurs renvoient les valeurs par defaut
 * (ne jamais appeler {@code get()} directement hors de ces accesseurs).
 */
public final class InsectsConfig {

    public static final String FILE_NAME = "terresvivantes-insects.toml";
    public static final ModConfigSpec SPEC;

    private static final ModConfigSpec.BooleanValue APHIDS_ENABLED;
    private static final ModConfigSpec.DoubleValue APHID_BASE_CHANCE;
    private static final ModConfigSpec.IntValue FIREFLY_MAX;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        APHIDS_ENABLED = builder
                .comment("Pucerons : apparition sur les cultures au printemps et en été, et nettoyage par les coccinelles.")
                .define("aphidsEnabled", true);

        APHID_BASE_CHANCE = builder
                .comment("Chance de base d'apparition des pucerons par tentative de croissance d'une culture saine (avant biodiversité et coccinelles).")
                .defineInRange("aphidBaseChance", AphidLogic.BASE_CHANCE, 0.0, 1.0);

        FIREFLY_MAX = builder
                .comment("Nombre maximal de lucioles actives autour du joueur (côté client, sans coût serveur).")
                .defineInRange("fireflyMax", FireflyLogic.MAX_ACTIVE, 0, 64);

        SPEC = builder.build();
    }

    private InsectsConfig() {
    }

    /** Vrai si les pucerons apparaissent (défaut vrai tant que la config n'est pas chargée). */
    public static boolean aphidsEnabled() {
        return !SPEC.isLoaded() || APHIDS_ENABLED.get();
    }

    /** Chance de base d'apparition des pucerons (défaut 0.3 % tant que la config n'est pas chargée). */
    public static double aphidBaseChance() {
        return SPEC.isLoaded() ? APHID_BASE_CHANCE.get() : AphidLogic.BASE_CHANCE;
    }

    /** Nombre maximal de lucioles actives (défaut 10 tant que la config n'est pas chargée). */
    public static int fireflyMax() {
        return SPEC.isLoaded() ? FIREFLY_MAX.get() : FireflyLogic.MAX_ACTIVE;
    }
}
