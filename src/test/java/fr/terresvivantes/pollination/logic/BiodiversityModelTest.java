package fr.terresvivantes.pollination.logic;

import fr.terresvivantes.health.logic.HealthModel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BiodiversityModelTest {

    private static final float DELTA = 1e-6f;

    @Test
    void emptyPlaceHasNoBiodiversity() {
        assertEquals(0f, BiodiversityModel.index(0, 0, 0, 0f), DELTA);
    }

    @Test
    void componentsAreCappedAndTotalIsBounded() {
        assertEquals(0.3f, BiodiversityModel.index(20, 0, 0, 0f), DELTA);
        assertEquals(0.25f, BiodiversityModel.index(0, 500, 0, 0f), DELTA);
        assertEquals(0.2f, BiodiversityModel.index(0, 0, 50, 0f), DELTA);
        assertEquals(1.0f, BiodiversityModel.index(20, 500, 50, 5f), DELTA);
    }

    @Test
    void moreFlowersMeansMoreBiodiversity() {
        assertTrue(BiodiversityModel.index(4, 0, 0, 0f) > BiodiversityModel.index(1, 0, 0, 0f));
    }

    /** Une chance de maladie est réduite par la biodiversité : ×(1 - 0.5 × indice). */
    @Test
    void diseaseChanceShrinksWithBiodiversity() {
        double base = 0.004;
        assertEquals(base, base * HealthModel.biodiversityFactor(0f), 1e-12);
        assertEquals(base * 0.75, base * HealthModel.biodiversityFactor(0.5f), 1e-12);
        assertEquals(base * 0.5, base * HealthModel.biodiversityFactor(1f), 1e-12);
        assertEquals(base * 0.5, base * HealthModel.biodiversityFactor(3f), 1e-12, "indice borné à 1");
    }
}
