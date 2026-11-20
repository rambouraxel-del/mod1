package fr.terresvivantes.health.logic;

/**
 * Formules de santé des cultures (DESIGN v0.2, module health). Aucun import net.minecraft: testable en JUnit.
 *
 * <ul>
 *   <li>Stress : +1 si le facteur de croissance vaut 0, -1 à chaque croissance réussie; flétrissement à 4.</li>
 *   <li>Maladie : chance de base 0.4 % par tentative, multipliée par les facteurs d'environnement.</li>
 *   <li>Malade : croissance x0.3, rendement x0.5; flétrissement après 6 tentatives.</li>
 *   <li>Immunité : jusqu'à la fin de la saison; aucune infection pendant ce temps.</li>
 * </ul>
 */
public final class HealthModel {

    /** Stress à partir duquel la culture flétrit. */
    public static final int WITHER_STRESS = 4;

    /** Tentatives de croissance d'une culture malade au bout desquelles elle flétrit. */
    public static final int WITHER_DISEASE_TICKS = 6;

    /** Chance de base d'infection par tentative, avant facteurs. */
    public static final double BASE_INFECTION_CHANCE = 0.004;

    /** Chance de contagion vers un voisin identique, par tentative de la culture malade. */
    public static final double SPREAD_CHANCE = 0.10;

    /** Multiplicateur de chance quand les conditions favorisent la maladie. */
    public static final double ENVIRONMENT_FACTOR = 4.0;

    /** Multiplicateur de chance en monoculture (même culture récoltée au moins deux fois de suite). */
    public static final double MONOCULTURE_FACTOR = 2.0;

    /** Multiplicateur de chance quand la culture est en peuplement dense. */
    public static final double DENSITY_FACTOR = 1.5;

    /** Nombre de cultures identiques dans la zone 5x5 à partir duquel la densité compte. */
    public static final int DENSITY_THRESHOLD = 6;

    /** Répétitions de la même culture à partir desquelles il y a monoculture (même seuil que la rotation). */
    public static final int MONOCULTURE_REPEAT = 2;

    /** Température au-delà de laquelle l'oïdium est favorisé, en °C. */
    public static final float MILDEW_TEMPERATURE = 22.0f;

    /** Plage de température favorable à la rouille, en °C (bornes incluses). */
    public static final float RUST_MIN_TEMPERATURE = 12.0f;
    public static final float RUST_MAX_TEMPERATURE = 24.0f;

    /** Facteur de croissance d'une culture malade. */
    public static final float SICK_GROWTH_FACTOR = 0.3f;

    /** Facteur de rendement d'une culture malade. */
    public static final float SICK_YIELD_FACTOR = 0.5f;

    /** Nombre d'utilisations du pulvérisateur avant recharge. */
    public static final int SPRAYER_CHARGES = 16;

    private HealthModel() {
    }

    /** Conditions d'environnement d'une culture au moment d'une tentative. */
    public record Conditions(boolean saturatedSoil, boolean rain, float temperature,
                             int identicalInArea, boolean monoculture, boolean immune) {
    }

    /**
     * Stress après une tentative. Un facteur nul (conditions impossibles) ajoute 1; une croissance réussie
     * retire 1, sans descendre sous 0.
     *
     * @param factor facteur total de croissance de la tentative
     * @param grew   vrai si la tentative a fait progresser la culture
     */
    public static int nextStress(int stress, float factor, boolean grew) {
        if (factor <= 0.0f) {
            return stress + 1;
        }
        return grew ? Math.max(0, stress - 1) : stress;
    }

    /** Vrai si le stress impose le flétrissement. */
    public static boolean withersFromStress(int stress) {
        return stress >= WITHER_STRESS;
    }

    /** Vrai si la maladie impose le flétrissement. */
    public static boolean withersFromDisease(Disease disease, int diseaseTicks) {
        return disease != Disease.NONE && diseaseTicks >= WITHER_DISEASE_TICKS;
    }

    /**
     * Chance d'infection d'une culture saine par la maladie {@code disease} pour une tentative.
     * Immunisée ou sans maladie : 0.
     */
    public static double infectionChance(Disease disease, Conditions conditions) {
        if (disease == Disease.NONE || conditions.immune()) {
            return 0.0;
        }
        double chance = BASE_INFECTION_CHANCE;
        if (disease == Disease.MILDEW
                && conditions.saturatedSoil()
                && (conditions.rain() || conditions.temperature() > MILDEW_TEMPERATURE)) {
            chance *= ENVIRONMENT_FACTOR;
        }
        if (disease == Disease.RUST
                && conditions.rain()
                && conditions.temperature() >= RUST_MIN_TEMPERATURE
                && conditions.temperature() <= RUST_MAX_TEMPERATURE) {
            chance *= ENVIRONMENT_FACTOR;
        }
        if (conditions.monoculture()) {
            chance *= MONOCULTURE_FACTOR;
        }
        return chance * densityFactor(conditions.identicalInArea());
    }

    /** Vrai si le tirage tombe sous la chance d'infection. */
    public static boolean infects(double chance, double roll) {
        return roll < chance;
    }

    /** Vrai si la culture malade contamine un voisin lors de cette tentative. */
    public static boolean spreads(double roll) {
        return roll < SPREAD_CHANCE;
    }

    /** Facteur de densité : x1.5 à partir de 6 cultures identiques dans la zone 5x5, sinon x1. */
    public static double densityFactor(int identicalInArea) {
        return identicalInArea >= DENSITY_THRESHOLD ? DENSITY_FACTOR : 1.0;
    }

    /** Vrai en monoculture : la même culture a été récoltée au moins {@link #MONOCULTURE_REPEAT} fois de suite. */
    public static boolean monoculture(int repeat, boolean sameAsLastCrop) {
        return sameAsLastCrop && repeat >= MONOCULTURE_REPEAT;
    }

    /** Facteur de croissance : x0.3 si malade, sinon x1. */
    public static float growthFactor(boolean sick) {
        return sick ? SICK_GROWTH_FACTOR : 1.0f;
    }

    /** Facteur de rendement : x0.5 si malade, sinon x1. */
    public static float yieldFactor(boolean sick) {
        return sick ? SICK_YIELD_FACTOR : 1.0f;
    }

    /**
     * Dernier jour d'immunité : le jour courant plus les jours restants de la saison.
     *
     * @param dayIndex      index absolu du jour courant
     * @param dayOfSeason   jour dans la saison, 1-based
     * @param daysPerSeason durée d'une saison en jours
     */
    public static long immuneUntilDay(long dayIndex, int dayOfSeason, int daysPerSeason) {
        return dayIndex + Math.max(0, daysPerSeason - dayOfSeason);
    }

    /** Vrai si le jour courant est dans la période d'immunité (bornes incluses). */
    public static boolean isImmune(long dayIndex, long immuneUntilDay) {
        return dayIndex <= immuneUntilDay;
    }
}
