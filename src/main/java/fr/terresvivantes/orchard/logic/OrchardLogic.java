package fr.terresvivantes.orchard.logic;

import fr.terresvivantes.season.logic.Season;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Règles métier du verger : stades des feuilles, rendement, arbre généré. Aucun import net.minecraft.
 *
 * <p>Stades des feuilles : 0 feuillage, 1 floraison, 2 fruits verts, 3 fruits mûrs.
 */
public final class OrchardLogic {

    /** Stade maximal (fruits mûrs). */
    public static final int MAX_STAGE = 3;
    /** Distance vanilla au-delà de laquelle une feuille non persistante se détruit. */
    public static final int DECAY_DISTANCE = 7;
    /** Lumière minimale au-dessus d'un sapling pour qu'il pousse. */
    public static final int MIN_LIGHT = 9;
    /** Chance de lâcher un sapling en cassant une feuille (hors toucher de soie). */
    public static final double SAPLING_DROP_CHANCE = 0.05;
    /** Chance qu'un fruit mûr tombé au sol laisse un fruit à terre. */
    public static final double FALLEN_FRUIT_CHANCE = 0.5;
    /** Hauteur de tronc minimale et maximale d'un arbre généré. */
    public static final int MIN_TRUNK_HEIGHT = 4;
    public static final int MAX_TRUNK_HEIGHT = 5;

    private OrchardLogic() {
    }

    /**
     * Résultat d'une tentative d'évolution d'une feuille.
     *
     * @param stage       nouveau stade
     * @param fruitFalls  vrai si un fruit mûr tombe (stade 3 hors saison de maturation); le module tire alors
     *                    {@link #FALLEN_FRUIT_CHANCE} pour savoir si un fruit reste au sol
     */
    public record Step(int stage, boolean fruitFalls) {
    }

    /** Position relative à la base de l'arbre (le sapling ou le bloc de terre au pied). */
    public record Offset(int x, int y, int z) {
    }

    /** Forme d'un arbre : troncs et feuilles, avec la distance vanilla de chaque feuille. */
    public record TreeShape(List<Offset> trunk, Map<Offset, Integer> leaves) {
    }

    /**
     * Évolution d'une feuille lors d'un randomTick.
     *
     * <p>Règles : en hiver, tout stade non mûr (hors saison de maturation) repasse à 0. Un stade 3 hors
     * saison de maturation tombe (stade 0, fruit au sol possible). Si la température est hors plage, rien
     * d'autre ne change. Sinon : 0 vers 1 au printemps (floraison) ; 1 vers 2 dans la saison suivant la
     * floraison, multiplié par {@code fruitSetFactor} ; 2 vers 3 dans une saison de maturation.
     *
     * @param chance         probabilité de base d'une transition, à chaque randomTick
     * @param fruitSetFactor multiplicateur de la transition 1 vers 2 (pollinisation, voir OrchardApi)
     */
    public static Step nextStage(FruitTreeDefinition def, int stage, Season season, boolean warm,
                                 double roll, double chance, float fruitSetFactor) {
        if (stage >= MAX_STAGE) {
            return def.ripeIn(season) ? new Step(MAX_STAGE, false) : new Step(0, true);
        }
        if (season == Season.WINTER && !def.ripeIn(season)) {
            return new Step(0, false);
        }
        if (!warm) {
            return new Step(stage, false);
        }
        return switch (stage) {
            case 0 -> season == def.floweringSeason() && roll < chance ? new Step(1, false) : new Step(0, false);
            case 1 -> season == def.floweringSeason().next() && roll < chance * Math.max(0f, fruitSetFactor)
                    ? new Step(2, false) : new Step(1, false);
            case 2 -> def.ripeIn(season) && roll < chance ? new Step(3, false) : new Step(2, false);
            default -> new Step(stage, false);
        };
    }

    /** Nombre de fruits récoltés sur un stade 3, entre {@code yieldMin} et {@code yieldMax} inclus. */
    public static int yieldCount(FruitTreeDefinition def, double roll) {
        int span = def.yieldMax() - def.yieldMin() + 1;
        int offset = (int) Math.floor(clamp01(roll) * span);
        return def.yieldMin() + Math.min(offset, span - 1);
    }

    /** Vrai si un sapling pousse en un randomTick : chance de base multipliée par le facteur saisonnier. */
    public static boolean saplingGrows(double roll, double baseChance, float seasonMultiplier) {
        return seasonMultiplier > 0f && roll < baseChance * seasonMultiplier;
    }

    /** Vrai si la casse d'une feuille lâche un sapling. */
    public static boolean dropsSapling(double roll) {
        return roll < SAPLING_DROP_CHANCE;
    }

    /** Vrai si un fruit mûr tombé laisse un fruit au sol. */
    public static boolean fallenFruitRemains(double roll) {
        return roll < FALLEN_FRUIT_CHANCE;
    }

    /** Hauteur de tronc tirée uniformément entre {@link #MIN_TRUNK_HEIGHT} et {@link #MAX_TRUNK_HEIGHT}. */
    public static int trunkHeight(double roll) {
        int span = MAX_TRUNK_HEIGHT - MIN_TRUNK_HEIGHT + 1;
        return MIN_TRUNK_HEIGHT + Math.min((int) Math.floor(clamp01(roll) * span), span - 1);
    }

    /**
     * Forme d'un arbre fruitier : tronc de {@code trunkHeight} blocs (y = 0 à h - 1), couronne arrondie de
     * trois couches de 5x5 sans les coins, plus une couche de 3x3 sans les coins au sommet.
     * Les feuilles ne recouvrent jamais le tronc. La distance de chaque feuille est celle de la propagation
     * vanilla depuis les troncs (1 pour une feuille collée à un tronc).
     */
    public static TreeShape treeShape(int trunkHeight) {
        int h = trunkHeight;
        List<Offset> trunk = new ArrayList<>(h);
        for (int y = 0; y < h; y++) {
            trunk.add(new Offset(0, y, 0));
        }
        Set<Offset> leafSet = new HashSet<>();
        for (int y = h - 3; y <= h; y++) {
            int radius = y == h ? 1 : 2;
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.abs(dx) == radius && Math.abs(dz) == radius) {
                        continue;
                    }
                    if (dx == 0 && dz == 0 && y < h) {
                        continue;
                    }
                    leafSet.add(new Offset(dx, y, dz));
                }
            }
        }
        return new TreeShape(trunk, leafDistances(trunk, leafSet));
    }

    /** Propagation en largeur depuis les troncs, par faces adjacentes, plafonnée à {@link #DECAY_DISTANCE}. */
    private static Map<Offset, Integer> leafDistances(List<Offset> trunk, Set<Offset> leafSet) {
        Map<Offset, Integer> distance = new HashMap<>();
        Deque<Offset> queue = new ArrayDeque<>();
        Map<Offset, Integer> reached = new HashMap<>();
        for (Offset log : trunk) {
            reached.put(log, 0);
            queue.add(log);
        }
        while (!queue.isEmpty()) {
            Offset current = queue.poll();
            int next = reached.get(current) + 1;
            for (Offset neighbour : neighbours(current)) {
                if (leafSet.contains(neighbour) && !reached.containsKey(neighbour) && next <= DECAY_DISTANCE - 1) {
                    reached.put(neighbour, next);
                    queue.add(neighbour);
                }
            }
        }
        for (Offset leaf : leafSet) {
            distance.put(leaf, reached.getOrDefault(leaf, DECAY_DISTANCE - 1));
        }
        return distance;
    }

    private static List<Offset> neighbours(Offset o) {
        return List.of(
                new Offset(o.x() + 1, o.y(), o.z()), new Offset(o.x() - 1, o.y(), o.z()),
                new Offset(o.x(), o.y() + 1, o.z()), new Offset(o.x(), o.y() - 1, o.z()),
                new Offset(o.x(), o.y(), o.z() + 1), new Offset(o.x(), o.y(), o.z() - 1));
    }

    private static double clamp01(double value) {
        return Math.max(0.0, Math.min(value, 0.999999));
    }
}
