package fr.terresvivantes.breeds.logic;

/** Espèces ayant des races : vaches et poules. Aucun import net.minecraft. */
public enum Species {
    COW("cow"),
    CHICKEN("chicken");

    private final String texturePrefix;

    Species(String texturePrefix) {
        this.texturePrefix = texturePrefix;
    }

    /** Préfixe des fichiers de texture de superposition (ex. {@code cow_holstein.png}). */
    public String texturePrefix() {
        return texturePrefix;
    }
}
