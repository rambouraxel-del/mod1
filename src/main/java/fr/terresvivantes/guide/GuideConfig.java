package fr.terresvivantes.guide;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Configuration du guide. Le module l'enregistre lui-même (voir {@link GuideModule}).
 */
public final class GuideConfig {

    /** Nom du fichier de configuration. */
    public static final String FILE_NAME = "terresvivantes-guide.toml";

    static final boolean DEFAULT_GIVE_ON_FIRST_JOIN = true;

    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue GIVE_ON_FIRST_JOIN;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.comment("Manuel du fermier : remise de l'item à la première connexion d'un joueur.",
                        "Une seule remise par joueur, conservée à la mort. Mettre false pour ne jamais la faire.")
                .push("guide");
        GIVE_ON_FIRST_JOIN = builder.define("giveOnFirstJoin", DEFAULT_GIVE_ON_FIRST_JOIN);
        builder.pop();
        SPEC = builder.build();
    }

    private GuideConfig() {
    }

    /** Vrai si le manuel doit être remis à la première connexion (valeur par défaut tant que la config n'est pas chargée). */
    public static boolean giveOnFirstJoin() {
        try {
            return GIVE_ON_FIRST_JOIN.get();
        } catch (RuntimeException notLoaded) {
            return DEFAULT_GIVE_ON_FIRST_JOIN;
        }
    }
}
