package fr.terresvivantes.processing.logic;

/** Mode de qualité d'une recette de transformation (champ {@code quality} du JSON). Aucun import net.minecraft. */
public enum QualityMode {
    /** Pas de qualité sur la sortie. */
    NONE("none"),
    /** Qualité de la sortie = la plus basse des entrées. */
    INHERIT_MIN("inherit_min"),
    /** Qualité de la sortie = moyenne arrondie des entrées. */
    INHERIT_AVG("inherit_avg"),
    /**
     * Affinage : la base est la plus basse des entrées; le gain avec le temps se calcule ensuite par
     * {@code QualityLogic.ageQuality} (étagère d'affinage, module dairy).
     */
    AGE("age");

    private final String id;

    QualityMode(String id) {
        this.id = id;
    }

    /** Identifiant JSON. */
    public String id() {
        return id;
    }

    /** Mode à partir de son identifiant JSON; exception si inconnu. */
    public static QualityMode fromId(String id) {
        for (QualityMode mode : values()) {
            if (mode.id.equals(id)) {
                return mode;
            }
        }
        throw new IllegalArgumentException("mode de qualité inconnu : " + id);
    }
}
