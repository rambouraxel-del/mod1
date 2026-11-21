package fr.terresvivantes.husbandry.logic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NestLogicTest {

    @Test
    void interceptsOnlyLayingAdultHens() {
        assertTrue(NestLogic.shouldIntercept(1, false));
        assertTrue(NestLogic.shouldIntercept(0, false));
        assertFalse(NestLogic.shouldIntercept(2, false));
        assertFalse(NestLogic.shouldIntercept(1, true));
    }

    @Test
    void nextEggTimeStaysInVanillaRange() {
        assertEquals(6000, NestLogic.nextEggTime(0));
        assertEquals(11999, NestLogic.nextEggTime(5999));
        assertEquals(6000, NestLogic.nextEggTime(6000));
    }
}
