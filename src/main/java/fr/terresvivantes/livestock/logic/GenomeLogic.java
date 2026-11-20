package fr.terresvivantes.livestock.logic;

import java.util.function.DoubleSupplier;

/**
 * Tirages de génome : génome initial et hérédité. Aucun import net.minecraft.
 * Le hasard est fourni par un {@link DoubleSupplier} qui rend des valeurs dans [0, 1[ (tests déterministes possibles).
 */
public final class GenomeLogic {

    /** Bornes du génome initial, trait par trait. */
    public static final int INITIAL_MIN = 4;
    public static final int INITIAL_MAX = 6;
    /** Probabilité d'une mutation (±1) sur chaque trait d'un enfant. */
    public static final double MUTATION_CHANCE = 0.15;

    private GenomeLogic() {
    }

    /** Génome initial : chaque trait tiré uniformément dans [4, 6]. */
    public static Genome randomInitial(DoubleSupplier random) {
        return new Genome(initialTrait(random), initialTrait(random), initialTrait(random), initialTrait(random));
    }

    /**
     * Génome d'un enfant : pour chaque trait, moyenne des parents (en cas d'écart impair, l'arrondi est tiré au
     * hasard), puis mutation de ±1 avec la chance {@link #MUTATION_CHANCE}. Le résultat reste dans
     * [min(a, b) − 1, max(a, b) + 1] ∩ [0, 10].
     */
    public static Genome inherit(Genome a, Genome b, DoubleSupplier random) {
        return new Genome(
                inheritTrait(a.productivity(), b.productivity(), random),
                inheritTrait(a.size(), b.size(), random),
                inheritTrait(a.hardiness(), b.hardiness(), random),
                inheritTrait(a.fertility(), b.fertility(), random));
    }

    /** Un trait d'enfant à partir de deux traits parentaux. */
    public static int inheritTrait(int a, int b, DoubleSupplier random) {
        int sum = a + b;
        int value = sum / 2;
        if (sum % 2 != 0 && random.getAsDouble() < 0.5) {
            value++;
        }
        if (random.getAsDouble() < MUTATION_CHANCE) {
            value += random.getAsDouble() < 0.5 ? -1 : 1;
        }
        return Genome.clamp(value);
    }

    private static int initialTrait(DoubleSupplier random) {
        int span = INITIAL_MAX - INITIAL_MIN + 1;
        int offset = (int) Math.floor(random.getAsDouble() * span);
        return INITIAL_MIN + Math.min(span - 1, Math.max(0, offset));
    }
}
