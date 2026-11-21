package fr.terresvivantes.crop.logic;

/** Résultat d'une tentative de croissance d'une culture. */
public enum GrowthDecision {
    /** Croissance annulée pour ce tick. */
    DENY,
    /** Croissance normale d'un stade. */
    NORMAL,
    /** Croissance normale plus un stade supplémentaire. */
    EXTRA_STAGE
}
