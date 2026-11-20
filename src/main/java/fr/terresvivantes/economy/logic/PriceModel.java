package fr.terresvivantes.economy.logic;

import fr.terresvivantes.season.logic.Season;

import java.util.Set;

/**
 * Modèle de prix (v0.7). Prix = base x qualité x saison x saturation, en cuivre par unité.
 * Logique pure, sans import net.minecraft.
 */
public final class PriceModel {

    /** Facteur d'un produit en pleine saison optimale. */
    public static final float IN_SEASON = 0.85f;
    /** Facteur d'un produit hors de sa saison optimale. */
    public static final float OUT_OF_SEASON = 1.3f;
    /** Saturation : chaque point retire 2 % du prix. */
    public static final double SATURATION_STEP = 0.02;
    /** Plancher de la saturation : le prix ne descend pas sous 40 % de la base. */
    public static final double SATURATION_FLOOR = 0.4;

    private PriceModel() {
    }

    /** Qualité 1 à 3 : x1, x1.25, x1.5. Qualité absente (0) traitée comme 1. */
    public static float qualityFactor(int quality) {
        int q = Math.max(1, Math.min(3, quality));
        return 1.0f + 0.25f * (q - 1);
    }

    /**
     * Facteur saisonnier. Vaut {@link #IN_SEASON} si la saison courante est une saison optimale du produit,
     * {@link #OUT_OF_SEASON} sinon. Un produit sans saison (ensemble vide) n'a pas de modulateur saisonnier.
     */
    public static float seasonFactor(Set<Season> optimal, Season current) {
        if (optimal == null || optimal.isEmpty()) {
            return 1.0f;
        }
        return optimal.contains(current) ? IN_SEASON : OUT_OF_SEASON;
    }

    /** Facteur de saturation : max(0.4, 1 - 0.02 x points). */
    public static double saturationFactor(double points) {
        return Math.max(SATURATION_FLOOR, 1.0 - SATURATION_STEP * Math.max(0.0, points));
    }

    /**
     * Prix final en cuivre par unité, arrondi, au moins 1 pour un produit vendable.
     * Un produit de base 0 (non vendable) donne 0.
     */
    public static int price(int base, int quality, float seasonFactor, double saturationPoints) {
        if (base <= 0) {
            return 0;
        }
        double value = base * qualityFactor(quality) * seasonFactor * saturationFactor(saturationPoints);
        return Math.max(1, (int) Math.round(value));
    }
}
