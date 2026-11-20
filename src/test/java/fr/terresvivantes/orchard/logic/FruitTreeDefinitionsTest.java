package fr.terresvivantes.orchard.logic;

import fr.terresvivantes.season.logic.Season;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FruitTreeDefinitionsTest {

    private static final Map<String, FruitTreeDefinition> BY_ID = FruitTreeDefinitions.all().stream()
            .collect(java.util.stream.Collectors.toMap(FruitTreeDefinition::id, d -> d));

    @Test
    void sixSpeciesWithUniqueIds() {
        List<FruitTreeDefinition> all = FruitTreeDefinitions.all();
        assertEquals(6, all.size());
        Set<String> ids = new HashSet<>();
        all.forEach(d -> ids.add(d.id()));
        assertEquals(6, ids.size());
        assertEquals(Set.of("apple", "cherry", "orange", "olive", "walnut", "peach"), ids);
    }

    @Test
    void everySpeciesFloresInSpring() {
        for (FruitTreeDefinition def : FruitTreeDefinitions.all()) {
            assertEquals(Season.SPRING, def.floweringSeason(), def.id());
        }
    }

    @Test
    void ripeSeasonsMatchDesign() {
        assertEquals(Set.of(Season.AUTUMN), BY_ID.get("apple").ripeSeasons());
        assertEquals(Set.of(Season.SPRING, Season.SUMMER), BY_ID.get("cherry").ripeSeasons());
        assertEquals(Set.of(Season.SUMMER), BY_ID.get("peach").ripeSeasons());
        assertEquals(Set.of(Season.AUTUMN), BY_ID.get("olive").ripeSeasons());
        assertEquals(Set.of(Season.AUTUMN), BY_ID.get("walnut").ripeSeasons());
        assertEquals(Set.of(Season.WINTER), BY_ID.get("orange").ripeSeasons());
    }

    @Test
    void trunksAndFruitsMatchDesign() {
        assertEquals("oak_log", BY_ID.get("apple").trunkLog());
        assertEquals("minecraft:apple", BY_ID.get("apple").fruit());
        assertEquals("cherry_log", BY_ID.get("cherry").trunkLog());
        assertEquals("jungle_log", BY_ID.get("orange").trunkLog());
        assertEquals("acacia_log", BY_ID.get("olive").trunkLog());
        assertEquals("dark_oak_log", BY_ID.get("walnut").trunkLog());
        assertEquals("birch_log", BY_ID.get("peach").trunkLog());
    }

    @Test
    void yieldIsBetweenOneAndThree() {
        for (FruitTreeDefinition def : FruitTreeDefinitions.all()) {
            assertTrue(def.yieldMin() >= 1 && def.yieldMax() <= 3 && def.yieldMin() <= def.yieldMax(), def.id());
        }
    }

    @Test
    void temperatureRangeIsInclusive() {
        FruitTreeDefinition apple = BY_ID.get("apple");
        assertTrue(apple.fruitingTemperature(apple.tempMin()));
        assertTrue(apple.fruitingTemperature(apple.tempMax()));
        assertFalse(apple.fruitingTemperature(apple.tempMax() + 0.1f));
    }

    @Test
    void invalidYieldOrTemperatureIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new FruitTreeDefinition("x", "oak_log", "minecraft:apple",
                0f, 10f, Season.SPRING, Set.of(Season.AUTUMN), 0, 2));
        assertThrows(IllegalArgumentException.class, () -> new FruitTreeDefinition("x", "oak_log", "minecraft:apple",
                0f, 10f, Season.SPRING, Set.of(Season.AUTUMN), 3, 2));
        assertThrows(IllegalArgumentException.class, () -> new FruitTreeDefinition("x", "oak_log", "minecraft:apple",
                10f, 0f, Season.SPRING, Set.of(Season.AUTUMN), 1, 2));
    }
}
