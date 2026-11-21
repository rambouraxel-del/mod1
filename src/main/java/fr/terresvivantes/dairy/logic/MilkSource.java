package fr.terresvivantes.dairy.logic;

/**
 * Espèce d'origine du lait, portée par le composant {@code terresvivantes:milk_source} du seau de lait. Aucun import
 * net.minecraft.
 */
public final class MilkSource {

    /** Lait de vache. */
    public static final String COW = "cow";
    /** Lait de chèvre. */
    public static final String GOAT = "goat";

    private MilkSource() {
    }

    /** Vrai pour une espèce connue. */
    public static boolean isKnown(String source) {
        return COW.equals(source) || GOAT.equals(source);
    }
}
