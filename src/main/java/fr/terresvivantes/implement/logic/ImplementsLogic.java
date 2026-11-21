package fr.terresvivantes.implement.logic;

/**
 * Logique pure du module Implements (grelinette, semoir, faucille en diamant).
 * Aucun import net.minecraft : testable en JUnit.
 */
public final class ImplementsLogic {

    /** Durabilite de la grelinette (une utilisation par bloc laboure ou aere). */
    public static final int BROADFORK_DURABILITY = 400;

    /** Durabilite du semoir (une utilisation par graine plantee). */
    public static final int SEEDER_DURABILITY = 600;

    /** Durabilite de la faucille en diamant (une utilisation par culture recoltee). */
    public static final int DIAMOND_SICKLE_DURABILITY = 1200;

    /** Rayon de la zone de labour (3x3) : grelinette et semoir. */
    public static final int GRID_RADIUS = 1;

    /** Rayon de la zone de recolte (5x5) : faucille en diamant. */
    public static final int SICKLE_RADIUS = 2;

    /** Longueur de la ligne de semis quand le joueur est accroupi. */
    public static final int ROW_LENGTH = 5;

    /** Matiere organique ajoutee par un aerage de la grelinette. */
    public static final int AERATION_ORGANIC = 5;

    /** Delai entre deux aerages (tick). */
    public static final int AERATION_COOLDOWN_TICKS = 20;

    private ImplementsLogic() {
    }

    /**
     * Decalages (dx, dz) d'un carre de cote {@code 2 * radius + 1} centre sur l'origine, ligne par ligne.
     * Le centre (0, 0) est inclus.
     */
    public static int[][] square(int radius) {
        int side = 2 * radius + 1;
        int[][] offsets = new int[side * side][];
        int index = 0;
        for (int dz = -radius; dz <= radius; dz++) {
            for (int dx = -radius; dx <= radius; dx++) {
                offsets[index++] = new int[] {dx, dz};
            }
        }
        return offsets;
    }

    /**
     * Ligne de {@link #ROW_LENGTH} cases a partir de l'origine, dans la direction horizontale
     * ({@code stepX}, {@code stepZ}) : chaque case est le pas multiplie par son rang.
     */
    public static int[][] row(int stepX, int stepZ) {
        int[][] offsets = new int[ROW_LENGTH][];
        for (int i = 0; i < ROW_LENGTH; i++) {
            offsets[i] = new int[] {i * stepX, i * stepZ};
        }
        return offsets;
    }
}
