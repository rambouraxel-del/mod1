package fr.terresvivantes.food.logic;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FoodLogicTest {

    @Test
    void hasTenUniqueDishes() {
        assertEquals(10, FoodLogic.DISHES.size());
        Set<String> ids = new HashSet<>();
        FoodLogic.DISHES.forEach(d -> ids.add(d.id()));
        assertEquals(10, ids.size());
    }

    @Test
    void saturationIsPositiveAndBounded() {
        for (FoodLogic.Dish dish : FoodLogic.DISHES) {
            assertTrue(dish.saturation() > 0f && dish.saturation() <= 1f, dish.id());
            assertTrue(dish.nutrition() > 0, dish.id());
        }
    }

    @Test
    void bowlDishesAreTheFourSoupsAndStackToOne() {
        for (FoodLogic.Dish dish : FoodLogic.DISHES) {
            boolean bowl = dish.vessel() == FoodLogic.Vessel.BOWL;
            assertEquals(bowl, dish.stacksToOne(), dish.id());
        }
        long bowls = FoodLogic.DISHES.stream().filter(FoodLogic.Dish::stacksToOne).count();
        assertEquals(4, bowls);
    }

    @Test
    void jamReturnsBottleAndRegenerates() {
        FoodLogic.Dish jam = FoodLogic.DISHES.stream()
                .filter(d -> d.id().equals("strawberry_jam")).findFirst().orElseThrow();
        assertEquals(FoodLogic.Vessel.GLASS_BOTTLE, jam.vessel());
        assertTrue(jam.regeneration());
        assertFalse(FoodLogic.DISHES.stream().filter(d -> !d.id().equals("strawberry_jam"))
                .anyMatch(FoodLogic.Dish::regeneration));
    }
}
