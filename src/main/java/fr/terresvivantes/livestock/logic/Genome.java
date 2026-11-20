package fr.terresvivantes.livestock.logic;

/**
 * Génome d'un animal d'élevage : quatre traits entiers de 0 à 10. Les valeurs hors bornes sont ramenées dans l'intervalle.
 * Aucun import net.minecraft.
 *
 * @param productivity influence les productions (lait, œufs, laine) via {@link LivestockLogic#productionMultiplier}
 * @param size         taille de l'animal (réservé aux productions et au rendu)
 * @param hardiness    résistance aux maladies (réservé au module vet)
 * @param fertility    taille de portée : chance d'un second bébé de {@code fertility × 3 %}
 */
public record Genome(int productivity, int size, int hardiness, int fertility) {

    public static final int MIN = 0;
    public static final int MAX = 10;

    public Genome {
        productivity = clamp(productivity);
        size = clamp(size);
        hardiness = clamp(hardiness);
        fertility = clamp(fertility);
    }

    /** Ramène un trait dans [0, 10]. */
    public static int clamp(int value) {
        return Math.max(MIN, Math.min(MAX, value));
    }
}
