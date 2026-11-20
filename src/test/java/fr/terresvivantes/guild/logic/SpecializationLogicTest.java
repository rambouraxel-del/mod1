package fr.terresvivantes.guild.logic;

import fr.terresvivantes.guild.logic.SpecializationLogic.Filiere;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpecializationLogicTest {

    @Test
    void itemsBelongToTheirFiliere() {
        assertEquals(Optional.of(Filiere.MARAICHER), SpecializationLogic.filiereOf("terresvivantes:tomato"));
        assertEquals(Optional.of(Filiere.ARBORICULTEUR), SpecializationLogic.filiereOf("minecraft:apple"));
        assertEquals(Optional.of(Filiere.ELEVEUR), SpecializationLogic.filiereOf("terresvivantes:cream"));
        assertEquals(Optional.of(Filiere.FROMAGER), SpecializationLogic.filiereOf("terresvivantes:aged_cheese"));
        assertEquals(Optional.of(Filiere.APICULTEUR), SpecializationLogic.filiereOf("terresvivantes:orchard_honey"));
        assertEquals(Optional.empty(), SpecializationLogic.filiereOf("terresvivantes:copper_coin"));
        assertEquals(Optional.empty(), SpecializationLogic.filiereOf("terresvivantes:tomato_seeds"));
    }

    /** 256 tomates vendues au marchand : titre Maraîcher, et pas avant. */
    @Test
    void twoHundredFiftySixUnitsGrantTheTitle() {
        int count = 0;
        for (int i = 0; i < 255; i++) {
            count = SpecializationLogic.add(count, 1);
        }
        assertFalse(SpecializationLogic.hasTitle(count));
        int before = count;
        count = SpecializationLogic.add(count, 1);
        assertEquals(SpecializationLogic.THRESHOLD, count);
        assertTrue(SpecializationLogic.hasTitle(count));
        assertTrue(SpecializationLogic.reachedThreshold(before, count));
    }

    @Test
    void thresholdIsCrossedOnlyOnce() {
        assertTrue(SpecializationLogic.reachedThreshold(255, 256));
        assertTrue(SpecializationLogic.reachedThreshold(200, 300));
        assertFalse(SpecializationLogic.reachedThreshold(256, 257));
        assertFalse(SpecializationLogic.reachedThreshold(10, 20));
    }

    @Test
    void addIsBoundedAndNeverNegative() {
        assertEquals(Integer.MAX_VALUE, SpecializationLogic.add(Integer.MAX_VALUE, 5));
        assertEquals(2, SpecializationLogic.add(-3, 2));
        assertEquals(5, SpecializationLogic.add(5, -4));
    }

    @Test
    void filiereIdsResolve() {
        assertEquals(Optional.of(Filiere.ELEVEUR), SpecializationLogic.byId("eleveur"));
        assertEquals(Optional.empty(), SpecializationLogic.byId("inconnu"));
        assertEquals(0.05f, SpecializationLogic.QUALITY_BONUS_CHANCE, 1e-6f);
    }
}
