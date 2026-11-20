package fr.terresvivantes.livestock.logic;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GenomeLogicTest {

    @Test
    void initialGenomeStaysInFourToSix() {
        assertEquals(new Genome(4, 4, 4, 4), GenomeLogic.randomInitial(() -> 0.0));
        assertEquals(new Genome(6, 6, 6, 6), GenomeLogic.randomInitial(() -> 0.9999999));
        Random random = new Random(1);
        for (int i = 0; i < 1000; i++) {
            Genome genome = GenomeLogic.randomInitial(random::nextDouble);
            for (int trait : new int[]{genome.productivity(), genome.size(), genome.hardiness(), genome.fertility()}) {
                assertTrue(trait >= 4 && trait <= 6, "trait initial hors de [4, 6] : " + trait);
            }
        }
    }

    @Test
    void childTraitIsWithinParentsPlusMinusOneAndInRange() {
        Random random = new Random(42);
        Genome a = new Genome(4, 0, 10, 2);
        Genome b = new Genome(6, 10, 10, 9);
        for (int i = 0; i < 5000; i++) {
            Genome child = GenomeLogic.inherit(a, b, random::nextDouble);
            assertBounded(child.productivity(), 4, 6);
            assertBounded(child.size(), 0, 10);
            assertBounded(child.hardiness(), 9, 10);
            assertBounded(child.fertility(), 1, 10);
        }
    }

    @Test
    void identicalParentsGiveSameTraitWithoutMutation() {
        // Pas d'arrondi ni de mutation : tirages au-dessus des seuils (0.99 ne déclenche rien).
        assertEquals(5, GenomeLogic.inheritTrait(5, 5, () -> 0.99));
        // Somme impaire 9 : arrondi bas si le tirage d'arrondi est >= 0.5, sans mutation.
        assertEquals(4, GenomeLogic.inheritTrait(4, 5, sequence(0.9, 0.99)));
        // Arrondi haut si le tirage d'arrondi est < 0.5, sans mutation.
        assertEquals(5, GenomeLogic.inheritTrait(4, 5, sequence(0.1, 0.99)));
    }

    @Test
    void mutationMovesTraitByOneOnly() {
        // Pas d'arrondi (somme paire), mutation déclenchée (0.05 < 0.15), sens vers le haut (0.8 >= 0.5).
        assertEquals(6, GenomeLogic.inheritTrait(5, 5, sequence(0.05, 0.8)));
        // Mutation vers le bas (0.2 < 0.5).
        assertEquals(4, GenomeLogic.inheritTrait(5, 5, sequence(0.05, 0.2)));
        // Pas de mutation (0.2 >= 0.15).
        assertEquals(5, GenomeLogic.inheritTrait(5, 5, sequence(0.2)));
    }

    @Test
    void genomeTraitsAreClampedToZeroAndTen() {
        assertEquals(new Genome(0, 10, 10, 10), new Genome(-3, 14, 10, 11));
        // Parents à 10 : la mutation vers le haut est bornée à 10.
        assertEquals(10, GenomeLogic.inheritTrait(10, 10, sequence(0.05, 0.8)));
        // Parents à 0 : la mutation vers le bas est bornée à 0.
        assertEquals(0, GenomeLogic.inheritTrait(0, 0, sequence(0.05, 0.2)));
    }

    /** Générateur déterministe : rend les valeurs données dans l'ordre, puis 0.99 (aucun effet). */
    private static java.util.function.DoubleSupplier sequence(double... values) {
        int[] index = {0};
        return () -> index[0] < values.length ? values[index[0]++] : 0.99;
    }

    private static void assertBounded(int value, int min, int max) {
        assertTrue(value >= min && value <= max, "valeur " + value + " hors de [" + min + ", " + max + "]");
    }
}
