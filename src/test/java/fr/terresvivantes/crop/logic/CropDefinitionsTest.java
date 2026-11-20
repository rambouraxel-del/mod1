package fr.terresvivantes.crop.logic;

import fr.terresvivantes.season.logic.Season;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CropDefinitionsTest {

    /** Cultures défavorables en hiver (v0.2) ; les autres sont impossibles en hiver. */
    private static final Set<String> WINTER_UNFAVORABLE = Set.of("garlic", "cabbage", "onion");

    @Test
    void twelveUniqueCrops() {
        List<CropDefinition> all = CropDefinitions.all();
        assertEquals(12, all.size());
        assertEquals(12, all.stream().map(CropDefinition::id).distinct().count());
        assertEquals(Set.of("tomato", "corn", "soybean", "lettuce", "onion", "garlic", "bell_pepper",
                "eggplant", "strawberry", "barley", "cabbage", "cucumber"),
                all.stream().map(CropDefinition::id).collect(java.util.stream.Collectors.toSet()));
    }

    @Test
    void everyCropHasDisjointSeasonsCoveringSpringSummerAutumnOnly() {
        for (CropDefinition def : CropDefinitions.all()) {
            Set<Season> covered = EnumSet.noneOf(Season.class);
            assertEquals(1, def.optimal().size(), def.id() + " : une seule saison optimale");
            assertEquals(1, def.possible().size(), def.id() + " : une seule saison possible");
            for (Set<Season> group : List.of(def.optimal(), def.possible(), def.unfavorable())) {
                for (Season season : group) {
                    assertTrue(covered.add(season), def.id() + " : saison en double");
                }
            }
            Set<Season> expected = WINTER_UNFAVORABLE.contains(def.id())
                    ? EnumSet.allOf(Season.class)
                    : EnumSet.of(Season.SPRING, Season.SUMMER, Season.AUTUMN);
            assertEquals(expected, covered, def.id() + " : hiver impossible, sauf ail, chou et oignon (défavorables)");
        }
    }

    @Test
    void winterIsImpossibleExceptForGarlicCabbageOnion() {
        for (CropDefinition def : CropDefinitions.all()) {
            Affinity expected = WINTER_UNFAVORABLE.contains(def.id()) ? Affinity.UNFAVORABLE : Affinity.IMPOSSIBLE;
            assertEquals(expected, def.affinityFor(Season.WINTER), def.id());
        }
    }

    @Test
    void tomatoValuesMatchDesign() {
        CropDefinition tomato = CropDefinitions.byId("tomato").orElseThrow();
        assertEquals(Affinity.OPTIMAL, tomato.affinityFor(Season.SUMMER));
        assertEquals(Affinity.POSSIBLE, tomato.affinityFor(Season.SPRING));
        assertEquals(Affinity.UNFAVORABLE, tomato.affinityFor(Season.AUTUMN));
        assertEquals(12.0f, tomato.tempMin(), 1e-6f);
        assertEquals(32.0f, tomato.tempMax(), 1e-6f);
        assertEquals(new CropDefinition.Nutrients(2, 1, 2), tomato.stageCost());
        assertEquals(new CropDefinition.Nutrients(3, 2, 3), tomato.harvestCost());
        assertEquals(6.0f, tomato.growthDays(), 1e-6f);
        assertEquals(2, tomato.yieldMin());
        assertEquals(4, tomato.yieldMax());
        assertFalse(tomato.legume());
    }

    @Test
    void garlicIsAutumnOptimalAndSlowest() {
        CropDefinition garlic = CropDefinitions.byId("garlic").orElseThrow();
        assertEquals(Affinity.OPTIMAL, garlic.affinityFor(Season.AUTUMN));
        assertEquals(14.0f, garlic.growthDays(), 1e-6f);
        for (CropDefinition def : CropDefinitions.all()) {
            assertTrue(def.growthDays() <= garlic.growthDays(), def.id() + " plus lent que l'ail");
        }
    }

    @Test
    void onlySoybeanIsLegume() {
        for (CropDefinition def : CropDefinitions.all()) {
            assertEquals(def.id().equals("soybean"), def.legume(), def.id());
        }
    }

    @Test
    void valuesAreConsistent() {
        for (CropDefinition def : CropDefinitions.all()) {
            assertTrue(def.tempMin() < def.tempMax(), def.id() + " plage de température");
            assertTrue(def.growthDays() > 0, def.id() + " durée");
            assertTrue(def.yieldMin() >= 1 && def.yieldMax() >= def.yieldMin(), def.id() + " rendement");
            assertTrue(def.stageCost().n() >= 0 && def.stageCost().p() >= 0 && def.stageCost().k() >= 0, def.id());
            assertTrue(def.harvestCost().n() >= def.stageCost().n(), def.id() + " récolte >= stade (N)");
        }
    }

    @Test
    void optimalSeasonsAreNotWinter() {
        assertTrue(CropDefinitions.all().stream().noneMatch(def -> def.optimal().contains(Season.WINTER)));
    }

    @Test
    void invalidDefinitionIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new CropDefinition("x", Set.of(), Set.of(), Set.of(),
                30, 10, new CropDefinition.Nutrients(0, 0, 0), new CropDefinition.Nutrients(0, 0, 0),
                5, 1, 2, false));
        assertThrows(IllegalArgumentException.class, () -> new CropDefinition("x", Set.of(), Set.of(), Set.of(),
                10, 30, new CropDefinition.Nutrients(0, 0, 0), new CropDefinition.Nutrients(0, 0, 0),
                0, 1, 2, false));
    }
}
