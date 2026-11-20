package fr.terresvivantes.crop.logic;

import fr.terresvivantes.season.logic.Season;

import java.util.Set;

/**
 * Définition d'une culture du mod (DESIGN 6.1 et 6.3). Aucun import net.minecraft.
 *
 * @param id          identifiant (snake_case), base des noms de registre (ex. {@code tomato})
 * @param optimal     saisons où la culture est optimale
 * @param possible    saisons où la culture est possible
 * @param unfavorable saisons défavorables
 * @param tempMin     température minimale de la plage de tolérance, en °C
 * @param tempMax     température maximale de la plage de tolérance, en °C
 * @param stageCost   consommation N/P/K par stade gagné
 * @param harvestCost consommation N/P/K à la récolte
 * @param growthDays  durée de maturation en jours MC, à facteur 1
 * @param yieldMin    quantité minimale du produit à maturité
 * @param yieldMax    quantité maximale du produit à maturité
 * @param legume      vrai si la culture fixe l'azote (fertilise +N au lieu de consommer N)
 */
public record CropDefinition(
        String id,
        Set<Season> optimal,
        Set<Season> possible,
        Set<Season> unfavorable,
        float tempMin,
        float tempMax,
        Nutrients stageCost,
        Nutrients harvestCost,
        float growthDays,
        int yieldMin,
        int yieldMax,
        boolean legume) {

    /** Quantités de N, P et K. */
    public record Nutrients(int n, int p, int k) {
    }

    public CropDefinition {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id vide");
        }
        optimal = Set.copyOf(optimal);
        possible = Set.copyOf(possible);
        unfavorable = Set.copyOf(unfavorable);
        if (tempMin > tempMax) {
            throw new IllegalArgumentException("plage de température inversée pour " + id);
        }
        if (growthDays <= 0.0f) {
            throw new IllegalArgumentException("durée de maturation non positive pour " + id);
        }
        if (yieldMin < 1 || yieldMax < yieldMin) {
            throw new IllegalArgumentException("rendement invalide pour " + id);
        }
    }

    /** Affinité de la culture avec une saison. Hors des trois ensembles (hiver), impossible. */
    public Affinity affinityFor(Season season) {
        if (optimal.contains(season)) {
            return Affinity.OPTIMAL;
        }
        if (possible.contains(season)) {
            return Affinity.POSSIBLE;
        }
        if (unfavorable.contains(season)) {
            return Affinity.UNFAVORABLE;
        }
        return Affinity.IMPOSSIBLE;
    }
}
