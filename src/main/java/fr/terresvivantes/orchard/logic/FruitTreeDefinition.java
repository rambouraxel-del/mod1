package fr.terresvivantes.orchard.logic;

import fr.terresvivantes.season.logic.Season;

import java.util.Set;

/**
 * Espèce fruitière. Données pures : les blocs et items sont résolus par le module, à partir des identifiants.
 *
 * @param id              identifiant court (ex. {@code apple}); préfixe des blocs {@code <id>_sapling} et {@code <id>_leaves}
 * @param trunkLog        identifiant du tronc vanilla (ex. {@code oak_log})
 * @param fruit           identifiant complet du fruit (ex. {@code minecraft:apple}, {@code terresvivantes:cherry})
 * @param tempMin         température minimale (°C) pour que les stades évoluent
 * @param tempMax         température maximale (°C) pour que les stades évoluent
 * @param floweringSeason saison de floraison (printemps pour toutes les espèces)
 * @param ripeSeasons     saisons où le fruit mûrit; hors de ces saisons, un fruit mûr tombe
 * @param yieldMin        récolte minimale par arbre mûr (au moins 1)
 * @param yieldMax        récolte maximale par arbre mûr
 */
public record FruitTreeDefinition(String id, String trunkLog, String fruit, float tempMin, float tempMax,
                                  Season floweringSeason, Set<Season> ripeSeasons, int yieldMin, int yieldMax) {

    public FruitTreeDefinition {
        if (yieldMin < 1 || yieldMax < yieldMin) {
            throw new IllegalArgumentException("rendement invalide pour " + id);
        }
        if (tempMax < tempMin) {
            throw new IllegalArgumentException("plage de température invalide pour " + id);
        }
        ripeSeasons = Set.copyOf(ripeSeasons);
    }

    /** Vrai si la saison est une saison de maturation. */
    public boolean ripeIn(Season season) {
        return ripeSeasons.contains(season);
    }

    /** Vrai si la température permet la fructification. */
    public boolean fruitingTemperature(float celsius) {
        return celsius >= tempMin && celsius <= tempMax;
    }
}
