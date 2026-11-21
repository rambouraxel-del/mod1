package fr.terresvivantes.soil.logic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RotationModelTest {

    private static final float DELTA = 1e-5f;

    @Test
    void neutralWithoutHistory() {
        assertEquals(1.0f, RotationModel.factor(0, false, false, false), DELTA);
        assertEquals(1.0f, RotationModel.factor(1, true, false, false), DELTA);
    }

    @Test
    void monocultureNeedsTwoRepeatsOfTheSameCrop() {
        assertEquals(1.0f, RotationModel.factor(1, true, false, false), DELTA);
        assertEquals(0.8f, RotationModel.factor(2, true, false, false), DELTA);
        assertEquals(0.8f, RotationModel.factor(5, true, false, false), DELTA);
    }

    @Test
    void monocultureDoesNotApplyWhenTheCropChanged() {
        assertEquals(1.0f, RotationModel.factor(3, false, false, false), DELTA);
    }

    @Test
    void legumePreviousGivesBonusOnlyToNonLegume() {
        assertEquals(1.15f, RotationModel.factor(0, false, true, false), DELTA);
        assertEquals(1.0f, RotationModel.factor(0, false, true, true), DELTA);
    }

    @Test
    void factorsCombine() {
        assertEquals(0.8f * 1.15f, RotationModel.factor(2, true, true, false), DELTA);
    }
}
