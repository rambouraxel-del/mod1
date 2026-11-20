package fr.terresvivantes.livestock.logic;

import java.util.function.DoubleSupplier;

/**
 * Règles des besoins, reproduction et productions des animaux d'élevage. Logique pure, sans import net.minecraft.
 *
 * <p>Un cycle de besoins dure {@link #TICK_INTERVAL} ticks par animal. Les valeurs de l'intervalle figurent dans
 * {@code docs/DESIGN.md} (section v0.4 Élevage).
 */
public final class LivestockLogic {

    /** Intervalle entre deux cycles de besoins d'un même animal (1200 ticks = une minute de jeu). */
    public static final int TICK_INTERVAL = 1200;
    /** Consommation de satiété par cycle. */
    public static final int CONSUMPTION = 8;
    /** Multiplicateur de consommation en hiver. */
    public static final double WINTER_CONSUMPTION_FACTOR = 1.5;
    /** Gain de satiété par cycle au pâturage (herbe sous l'animal, hors hiver). */
    public static final int PASTURE_GAIN = 10;
    /** Perte de santé par cycle à satiété nulle. */
    public static final int STARVATION_DAMAGE = 5;
    /** Gain de santé par cycle au-dessus du seuil de satiété. */
    public static final int RECOVERY_HEAL = 2;
    /** Satiété au-delà de laquelle la santé remonte. */
    public static final int RECOVERY_FED = 60;
    /** Satiété minimale pour se reproduire. */
    public static final int BREEDING_MIN_FED = 40;
    /** Satiété sous laquelle la production est réduite. */
    public static final int LOW_FED = 20;
    /** Chance de second bébé par point de fertilité. */
    public static final double TWIN_CHANCE_PER_FERTILITY = 0.03;

    private LivestockLogic() {
    }

    /**
     * Un cycle de besoins. Consommation (×1.5 en hiver), pâturage si {@code onGrass} (un pâturable sur herbe hors
     * hiver; l'appelant le décide), puis santé : satiété nulle fait perdre 5, satiété au-dessus de 60 fait gagner 2.
     * Le jour du dernier repas n'est mis à jour que par le pâturage.
     */
    public static LivestockState needsStep(LivestockState state, boolean winter, boolean onGrass, long day) {
        int consumption = winter ? (int) Math.round(CONSUMPTION * WINTER_CONSUMPTION_FACTOR) : CONSUMPTION;
        int fed = state.fed() - consumption;
        long lastFedDay = state.lastFedDay();
        if (!winter && onGrass) {
            fed += PASTURE_GAIN;
            lastFedDay = day;
        }
        fed = LivestockState.clamp(fed);
        int health = state.health();
        if (fed == 0) {
            health -= STARVATION_DAMAGE;
        } else if (fed > RECOVERY_FED) {
            health += RECOVERY_HEAL;
        }
        return new LivestockState(fed, health, state.ailment(), lastFedDay);
    }

    /**
     * Multiplicateur de production : (0.6 + 0.08 × productivité) × (1.1 si satiété > 60, 0.5 si < 20, sinon 1)
     * × (0.5 si malade, sinon 1). Les modificateurs enregistrés se multiplient par-dessus (côté API).
     */
    public static float productionMultiplier(int productivity, int fed, boolean sick) {
        float genetic = 0.6f + 0.08f * productivity;
        float feeding = fed > RECOVERY_FED ? 1.1f : fed < LOW_FED ? 0.5f : 1.0f;
        float health = sick ? 0.5f : 1.0f;
        return genetic * feeding * health;
    }

    /**
     * Reproduction permise si la satiété atteint 40 et, quand {@code seasonsOnly} est vrai, si la saison est le
     * printemps ou l'été ({@code springOrSummer}).
     */
    public static boolean canBreed(int fed, boolean springOrSummer, boolean seasonsOnly) {
        return fed >= BREEDING_MIN_FED && (!seasonsOnly || springOrSummer);
    }

    /** Chance d'un second bébé : fertilité × 3 %, plafonnée à 1. */
    public static double twinChance(double fertility) {
        return Math.max(0.0, Math.min(1.0, fertility * TWIN_CHANCE_PER_FERTILITY));
    }

    /**
     * Arrondi probabiliste d'une valeur réelle : la partie entière, plus 1 avec la probabilité de la partie fractionnaire
     * (négative ou positive). L'espérance vaut la valeur exacte.
     */
    public static int roundProbabilistic(double value, DoubleSupplier random) {
        double whole = Math.floor(value);
        double fraction = value - whole;
        return (int) whole + (random.getAsDouble() < fraction ? 1 : 0);
    }
}
