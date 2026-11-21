package fr.terresvivantes.storage.logic;

/**
 * Règles pures du silo : hauteur de colonne, capacité en stacks, pourcentage de remplissage. Sans import Minecraft.
 */
public final class SiloLogic {

    /** Nombre maximal de blocs empilés formant un même silo. */
    public static final int MAX_HEIGHT = 8;
    /** Stacks par bloc de silo. */
    public static final int STACKS_PER_BLOCK = 16;

    private SiloLogic() {
    }

    /** Capacité en stacks d'une colonne de {@code height} blocs (bornée entre 1 et 8). */
    public static int capacity(int height) {
        return Math.min(MAX_HEIGHT, Math.max(1, height)) * STACKS_PER_BLOCK;
    }

    /** Vrai si un silo posé entre {@code below} blocs de silo et {@code above} blocs ne dépasse pas la hauteur max. */
    public static boolean canJoin(int below, int above) {
        return below + above + 1 <= MAX_HEIGHT;
    }

    /** Pourcentage de remplissage en stacks, entre 0 et 100. */
    public static int percent(int usedStacks, int capacity) {
        if (capacity <= 0) {
            return 0;
        }
        return Math.max(0, Math.min(100, usedStacks * 100 / capacity));
    }
}
