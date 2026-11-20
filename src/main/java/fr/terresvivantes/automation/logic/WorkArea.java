package fr.terresvivantes.automation.logic;

/**
 * Zone de travail carrée devant une machine, logique pure (aucun import net.minecraft).
 * Rayon r : côté 2r+1. Une case est repérée par son index (0 à côté² − 1), en lignes de l'avant vers l'arrière :
 * distance avant de 1 à côté, décalage latéral de −r à +r.
 *
 * @param radius rayon de la zone, entre {@link #MIN_RADIUS} et {@link #MAX_RADIUS}
 */
public record WorkArea(int radius) {

    public static final int MIN_RADIUS = 1;
    public static final int MAX_RADIUS = 4;

    public WorkArea {
        if (radius < MIN_RADIUS || radius > MAX_RADIUS) {
            throw new IllegalArgumentException("rayon hors bornes : " + radius);
        }
    }

    /** Côté de la zone (5 pour le rayon 2). */
    public int side() {
        return 2 * radius + 1;
    }

    /** Nombre de cases de la zone. */
    public int cellCount() {
        int side = side();
        return side * side;
    }

    /** Distance devant la machine (de 1 à côté) de la case d'index donné. */
    public int forwardOf(int index) {
        return index / side() + 1;
    }

    /** Décalage latéral (de −r à +r) de la case d'index donné. */
    public int lateralOf(int index) {
        return index % side() - radius;
    }
}
