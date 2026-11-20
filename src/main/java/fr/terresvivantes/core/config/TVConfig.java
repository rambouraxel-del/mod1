package fr.terresvivantes.core.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Configuration commune du mod. Vide pour l'instant; les modules ajoutent
 * leurs sous-sections avec builder.push("nom") / builder.pop().
 */
public final class TVConfig {
    public static final ModConfigSpec COMMON;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        COMMON = builder.build();
    }

    private TVConfig() {
    }
}
