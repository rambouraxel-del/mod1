package fr.terresvivantes.crop.logic;

import fr.terresvivantes.crop.logic.CropDefinition.Nutrients;
import fr.terresvivantes.season.logic.Season;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static fr.terresvivantes.season.logic.Season.AUTUMN;
import static fr.terresvivantes.season.logic.Season.SPRING;
import static fr.terresvivantes.season.logic.Season.SUMMER;
import static fr.terresvivantes.season.logic.Season.WINTER;

/**
 * Les 12 cultures du mod (DESIGN 6.3; v0.2 : ail, chou et oignon défavorables en hiver). Aucun import net.minecraft.
 */
public final class CropDefinitions {

    /** Valeurs v0.8 (équilibrage) : maturation, consommation à la récolte et rendements. Voir docs/BALANCE.md. */
    private static final List<CropDefinition> ALL = List.of(
            row("tomato", Set.of(SUMMER), Set.of(SPRING), Set.of(AUTUMN), 12, 32,
                    0, 0, 0,
                    3, 2, 3,
                    1.55f, 2, 3, false, false),

            row("corn", Set.of(SUMMER), Set.of(SPRING), Set.of(AUTUMN), 15, 35,
                    0, 0, 0,
                    4, 2, 2,
                    1.55f, 1, 3, false, false),

            row("soybean", Set.of(SUMMER), Set.of(SPRING), Set.of(AUTUMN), 15, 30,
                    0, 0, 0,
                    1, 2, 1,
                    1.55f, 2, 4, true, false),

            row("lettuce", Set.of(SPRING), Set.of(AUTUMN), Set.of(SUMMER), 5, 24,
                    0, 0, 0,
                    2, 1, 1,
                    1.8f, 2, 3, false, false),

            row("onion", Set.of(SPRING), Set.of(AUTUMN), Set.of(SUMMER, WINTER), 8, 28,
                    0, 0, 0,
                    2, 3, 2,
                    1.8f, 1, 3, false, false),

            row("garlic", Set.of(AUTUMN), Set.of(SPRING), Set.of(SUMMER, WINTER), 2, 24,
                    0, 0, 0,
                    2, 3, 2,
                    1.7f, 2, 3, false, false),

            row("bell_pepper", Set.of(SUMMER), Set.of(SPRING), Set.of(AUTUMN), 16, 30,
                    0, 0, 0,
                    3, 2, 3,
                    1.55f, 1, 3, false, true),

            row("eggplant", Set.of(SUMMER), Set.of(SPRING), Set.of(AUTUMN), 18, 32,
                    0, 0, 0,
                    3, 2, 3,
                    1.55f, 1, 2, false, true),

            row("strawberry", Set.of(SPRING), Set.of(SUMMER), Set.of(AUTUMN), 8, 26,
                    0, 0, 0,
                    2, 3, 3,
                    1.55f, 1, 3, false, true),

            row("barley", Set.of(SPRING), Set.of(SUMMER), Set.of(AUTUMN), 5, 25,
                    0, 0, 0,
                    2, 2, 2,
                    1.55f, 2, 4, false, false),

            row("cabbage", Set.of(AUTUMN), Set.of(SPRING), Set.of(SUMMER, WINTER), 4, 22,
                    0, 0, 0,
                    3, 2, 2,
                    1.7f, 2, 3, false, false),

            row("cucumber", Set.of(SUMMER), Set.of(SPRING), Set.of(AUTUMN), 16, 30,
                    0, 0, 0,
                    3, 2, 3,
                    1.55f, 2, 3, false, true));

    private CropDefinitions() {
    }

    /** Les 12 cultures, dans l'ordre du tableau de design. */
    public static List<CropDefinition> all() {
        return ALL;
    }

    /** Culture d'après son identifiant. */
    public static Optional<CropDefinition> byId(String id) {
        return ALL.stream().filter(def -> def.id().equals(id)).findFirst();
    }

    private static CropDefinition row(String id, Set<Season> optimal, Set<Season> possible, Set<Season> unfavorable,
                                      float tempMin, float tempMax,
                                      int stageN, int stageP, int stageK,
                                      int harvestN, int harvestP, int harvestK,
                                      float growthDays, int yieldMin, int yieldMax, boolean legume,
                                      boolean needsPollination) {
        return new CropDefinition(id, optimal, possible, unfavorable, tempMin, tempMax,
                new Nutrients(stageN, stageP, stageK), new Nutrients(harvestN, harvestP, harvestK),
                growthDays, yieldMin, yieldMax, legume, needsPollination);
    }
}
