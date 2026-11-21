package fr.terresvivantes.preserving.logic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SpeedCarryTest {

    private static int total(double factor, int ticks) {
        SpeedCarry carry = new SpeedCarry();
        int sum = 0;
        for (int i = 0; i < ticks; i++) {
            sum += carry.step(factor);
        }
        return sum;
    }

    @Test
    void neutralFactorAddsNothing() {
        assertEquals(0, total(1.0, 1000));
    }

    @Test
    void fastFactorAddsTheExtraShare() {
        assertEquals(10, total(1.2, 50), 1);
    }

    @Test
    void slowFactorRemovesTheMissingShare() {
        assertEquals(-30, total(0.7, 100), 1);
    }

    @Test
    void verySlowFactorStillAdvancesAboutItsShare() {
        // Progression nette = 1 par tick (base) + ajustements : 0.3 par tick sur 100 ticks.
        assertEquals(30, 100 + total(0.3, 100), 1);
    }
}
