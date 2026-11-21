package fr.terresvivantes.crop.logic;

/** Affinité d'une culture avec la saison courante. Aucun import net.minecraft. */
public enum Affinity {
    OPTIMAL(1.25f),
    POSSIBLE(1.0f),
    UNFAVORABLE(0.4f),
    IMPOSSIBLE(0.0f);

    private final float multiplier;

    Affinity(float multiplier) {
        this.multiplier = multiplier;
    }

    /** Multiplicateur appliqué au facteur de croissance. */
    public float getMultiplier() {
        return multiplier;
    }
}
