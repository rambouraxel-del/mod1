package fr.terresvivantes.apiculture.logic;

/** Miels produits par l'apiaire, selon la fleur dominante du rayon. Aucun import net.minecraft. */
public enum HoneyType {
    WILDFLOWER("wildflower"),
    CLOVER("clover"),
    LAVENDER("lavender"),
    ORCHARD("orchard");

    private final String id;

    HoneyType(String id) {
        this.id = id;
    }

    /** Identifiant sérialisé (composant de données). */
    public String id() {
        return id;
    }

    /** Type correspondant à l'identifiant; {@link #WILDFLOWER} si inconnu. */
    public static HoneyType fromId(String id) {
        for (HoneyType type : values()) {
            if (type.id.equals(id)) {
                return type;
            }
        }
        return WILDFLOWER;
    }
}
