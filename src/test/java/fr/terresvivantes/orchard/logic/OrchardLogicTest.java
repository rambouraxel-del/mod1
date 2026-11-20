package fr.terresvivantes.orchard.logic;

import fr.terresvivantes.season.logic.Season;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrchardLogicTest {

    private static final FruitTreeDefinition APPLE = FruitTreeDefinitions.all().get(0);
    private static final FruitTreeDefinition ORANGE = FruitTreeDefinitions.all().get(2);
    private static final double CHANCE = 0.25;
    private static final double SURE = 0.0;
    private static final double NEVER = 0.99;

    private static OrchardLogic.Step step(FruitTreeDefinition def, int stage, Season season, boolean warm,
                                          double roll, float factor) {
        return OrchardLogic.nextStage(def, stage, season, warm, roll, CHANCE, factor);
    }

    @Test
    void foliageBecomesFloweringOnlyInSpring() {
        assertEquals(1, step(APPLE, 0, Season.SPRING, true, SURE, 1f).stage());
        assertEquals(0, step(APPLE, 0, Season.SUMMER, true, SURE, 1f).stage());
        assertEquals(0, step(APPLE, 0, Season.SPRING, true, NEVER, 1f).stage());
    }

    @Test
    void temperatureOutOfRangeFreezesGrowth() {
        assertEquals(0, step(APPLE, 0, Season.SPRING, false, SURE, 1f).stage());
        assertEquals(1, step(APPLE, 1, Season.SUMMER, false, SURE, 1f).stage());
    }

    @Test
    void floweringBecomesGreenFruitInTheSeasonAfter() {
        assertEquals(2, step(APPLE, 1, Season.SUMMER, true, SURE, 1f).stage());
        assertEquals(1, step(APPLE, 1, Season.SPRING, true, SURE, 1f).stage());
        assertEquals(1, step(APPLE, 1, Season.AUTUMN, true, SURE, 1f).stage());
    }

    @Test
    void pollinationFactorScalesFruitSet() {
        // roll 0.3 : au-dessus de la chance de base 0.25, en dessous de 0.25 * 2
        assertEquals(1, step(APPLE, 1, Season.SUMMER, true, 0.3, 1f).stage());
        assertEquals(2, step(APPLE, 1, Season.SUMMER, true, 0.3, 2f).stage());
        assertEquals(1, step(APPLE, 1, Season.SUMMER, true, 0.3, 0f).stage());
    }

    @Test
    void greenFruitRipensInRipeSeasonOnly() {
        assertEquals(3, step(APPLE, 2, Season.AUTUMN, true, SURE, 1f).stage());
        assertEquals(2, step(APPLE, 2, Season.SUMMER, true, SURE, 1f).stage());
        assertEquals(2, step(APPLE, 2, Season.AUTUMN, true, NEVER, 1f).stage());
    }

    @Test
    void ripeFruitFallsOutOfSeason() {
        OrchardLogic.Step fall = step(APPLE, 3, Season.SUMMER, true, SURE, 1f);
        assertEquals(0, fall.stage());
        assertTrue(fall.fruitFalls());
        OrchardLogic.Step kept = step(APPLE, 3, Season.AUTUMN, true, SURE, 1f);
        assertEquals(3, kept.stage());
        assertFalse(kept.fruitFalls());
    }

    @Test
    void winterResetsAllButOrange() {
        assertEquals(0, step(APPLE, 2, Season.WINTER, true, SURE, 1f).stage());
        assertEquals(0, step(APPLE, 1, Season.WINTER, true, SURE, 1f).stage());
        // l'orange mûrit en hiver : son stade 2 n'est pas remis à zéro et passe à 3
        assertEquals(3, step(ORANGE, 2, Season.WINTER, true, SURE, 1f).stage());
        assertFalse(step(ORANGE, 3, Season.WINTER, true, SURE, 1f).fruitFalls());
        // hors maturation, l'orange tombe au printemps
        assertTrue(step(ORANGE, 3, Season.SPRING, true, SURE, 1f).fruitFalls());
    }

    @Test
    void yieldStaysWithinRangeAndCoversAllValues() {
        Set<Integer> seen = new HashSet<>();
        for (int i = 0; i <= 100; i++) {
            int count = OrchardLogic.yieldCount(APPLE, i / 100.0);
            assertTrue(count >= 1 && count <= 3, "rendement hors plage : " + count);
            seen.add(count);
        }
        assertEquals(Set.of(1, 2, 3), seen);
        assertEquals(3, OrchardLogic.yieldCount(APPLE, 1.0));
    }

    @Test
    void saplingGrowthNeedsSeasonAndChance() {
        assertTrue(OrchardLogic.saplingGrows(0.05, 0.1, 1.0f));
        assertFalse(OrchardLogic.saplingGrows(0.05, 0.1, 0.0f), "pas de croissance en hiver");
        assertFalse(OrchardLogic.saplingGrows(0.5, 0.1, 1.0f));
        assertTrue(OrchardLogic.saplingGrows(0.05, 0.1, 0.6f), "0.05 < 0.1 * 0.6");
        assertFalse(OrchardLogic.saplingGrows(0.07, 0.1, 0.6f), "0.07 >= 0.1 * 0.6");
    }

    @Test
    void saplingAndFallenFruitChances() {
        assertTrue(OrchardLogic.dropsSapling(0.049));
        assertFalse(OrchardLogic.dropsSapling(0.05));
        assertTrue(OrchardLogic.fallenFruitRemains(0.49));
        assertFalse(OrchardLogic.fallenFruitRemains(0.5));
    }

    @Test
    void trunkHeightIsFourOrFive() {
        assertEquals(4, OrchardLogic.trunkHeight(0.0));
        assertEquals(4, OrchardLogic.trunkHeight(0.49));
        assertEquals(5, OrchardLogic.trunkHeight(0.5));
        assertEquals(5, OrchardLogic.trunkHeight(1.0));
    }

    @Test
    void treeTrunkIsStraightAndLeavesNeverOverlapIt() {
        for (int h = OrchardLogic.MIN_TRUNK_HEIGHT; h <= OrchardLogic.MAX_TRUNK_HEIGHT; h++) {
            OrchardLogic.TreeShape shape = OrchardLogic.treeShape(h);
            assertEquals(h, shape.trunk().size());
            Set<OrchardLogic.Offset> trunk = new HashSet<>(shape.trunk());
            for (OrchardLogic.Offset leaf : shape.leaves().keySet()) {
                assertFalse(trunk.contains(leaf), "feuille sur le tronc " + leaf);
                assertTrue(leaf.y() >= 1, "feuille sous la première couche " + leaf);
                assertTrue(leaf.y() <= h, "feuille trop haute " + leaf);
            }
        }
    }

    @Test
    void canopyIsRoundedAndSymmetric() {
        OrchardLogic.TreeShape shape = OrchardLogic.treeShape(4);
        Set<OrchardLogic.Offset> leaves = shape.leaves().keySet();
        // coins retirés de la couche large
        assertFalse(leaves.contains(new OrchardLogic.Offset(2, 2, 2)));
        assertTrue(leaves.contains(new OrchardLogic.Offset(2, 2, 1)));
        for (OrchardLogic.Offset leaf : leaves) {
            assertTrue(leaves.contains(new OrchardLogic.Offset(-leaf.x(), leaf.y(), leaf.z())), "asymétrie en x");
            assertTrue(leaves.contains(new OrchardLogic.Offset(leaf.x(), leaf.y(), -leaf.z())), "asymétrie en z");
        }
    }

    @Test
    void leafDistanceFollowsVanillaPropagation() {
        OrchardLogic.TreeShape shape = OrchardLogic.treeShape(4);
        // collée au tronc : distance 1
        assertEquals(1, shape.leaves().get(new OrchardLogic.Offset(1, 2, 0)));
        // au-dessus du sommet du tronc : distance 1
        assertEquals(1, shape.leaves().get(new OrchardLogic.Offset(0, 4, 0)));
        // plus loin : distance 2 au moins
        assertTrue(shape.leaves().get(new OrchardLogic.Offset(2, 2, 0)) >= 2);
        for (int d : shape.leaves().values()) {
            assertTrue(d >= 1 && d <= OrchardLogic.DECAY_DISTANCE - 1, "distance hors plage : " + d);
        }
    }
}
