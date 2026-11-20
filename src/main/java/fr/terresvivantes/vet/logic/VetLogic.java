package fr.terresvivantes.vet.logic;

import fr.terresvivantes.livestock.logic.Ailment;
import fr.terresvivantes.livestock.logic.LivestockState;

/**
 * Règles de santé animale : maladies, parasites, contagion, soins, sel. Logique pure, sans import net.minecraft.
 *
 * <p>Toutes les probabilités sont par cycle de besoins (1200 ticks par animal). Les tirages sont faits par l'appelant
 * (comparaison {@code roll < chance}), ce qui permet de les tester sans monde.
 */
public final class VetLogic {

    /** Chance de base d'une maladie par cycle. */
    public static final double SICK_BASE_CHANCE = 0.01;
    /** Chance de base de parasites par cycle (broutard exposé). */
    public static final double PARASITE_BASE_CHANCE = 0.006;
    /** Rayon (blocs) du décompte de surpopulation. */
    public static final int CROWD_RADIUS = 8;
    /** Au-delà de ce nombre d'animaux d'élevage dans le rayon, la surpopulation s'applique. */
    public static final int CROWD_THRESHOLD = 8;
    /** Multiplicateur de maladie en surpopulation. */
    public static final double CROWD_FACTOR = 4.0;
    /** Satiété sous laquelle la sous-alimentation multiplie la chance de maladie. */
    public static final int HUNGER_THRESHOLD = 20;
    /** Multiplicateur de maladie quand l'animal a faim. */
    public static final double HUNGER_FACTOR = 3.0;
    /** Multiplicateur de maladie près d'un bloc de sel. */
    public static final double SALT_LICK_FACTOR = 0.5;
    /** Rayon (blocs) d'action du bloc de sel. */
    public static final int SALT_LICK_RADIUS = 8;
    /** Nombre d'usages (randomTick) après lequel le bloc de sel disparaît. */
    public static final int SALT_LICK_MAX_USES = 4;
    /** Santé gagnée par cycle près d'un bloc de sel. */
    public static final int SALT_HEALTH_GAIN = 1;
    /** Rayon (blocs) de contagion entre animaux. */
    public static final int CONTAGION_RADIUS = 3;
    /** Chance par cycle qu'un voisin sain soit infecté par un animal malade. */
    public static final double CONTAGION_CHANCE = 0.05;
    /** Santé perdue par cycle quand l'animal est malade. */
    public static final int SICK_HEALTH_LOSS = 4;
    /** Satiété perdue en plus, par cycle, quand l'animal a des parasites. */
    public static final int PARASITE_FED_LOSS = 6;
    /** Santé rendue par la trousse vétérinaire. */
    public static final int KIT_HEAL = 20;
    /** Modificateur (multiplicatif sur la base) de vitesse de marche d'un animal malade. */
    public static final double SICK_SPEED_MODIFIER = -0.2;

    private VetLogic() {
    }

    /** Facteur de résistance génétique : 1.5 pour une résistance nulle, 0.5 pour une résistance de 10. */
    public static double hardinessFactor(int hardiness) {
        int clamped = Math.max(0, Math.min(10, hardiness));
        return 1.5 - clamped * 0.1;
    }

    /**
     * Chance de maladie par cycle : base × surpopulation × faim × résistance × sel.
     *
     * @param crowdCount nombre d'animaux d'élevage dans le rayon, animal compris
     */
    public static double sickChance(int crowdCount, int fed, int hardiness, boolean nearSaltLick) {
        double chance = SICK_BASE_CHANCE * hardinessFactor(hardiness);
        if (isCrowded(crowdCount)) {
            chance *= CROWD_FACTOR;
        }
        if (fed < HUNGER_THRESHOLD) {
            chance *= HUNGER_FACTOR;
        }
        if (nearSaltLick) {
            chance *= SALT_LICK_FACTOR;
        }
        return chance;
    }

    /** Chance de parasites par cycle : 0.6 % × (1.5 − résistance × 0.1). */
    public static double parasiteChance(int hardiness) {
        return PARASITE_BASE_CHANCE * hardinessFactor(hardiness);
    }

    /** Vrai s'il y a surpopulation : strictement plus de {@link #CROWD_THRESHOLD} animaux. */
    public static boolean isCrowded(int crowdCount) {
        return crowdCount > CROWD_THRESHOLD;
    }

    /**
     * Exposition aux parasites : un broutard dehors sous la pluie, ou sur herbe humide au printemps ou à l'automne.
     *
     * @param rainOutdoors   il pleut et l'animal voit le ciel
     * @param grassBelow     l'animal est sur un bloc d'herbe
     * @param springOrAutumn saison de printemps ou d'automne
     */
    public static boolean parasiteExposed(boolean grazer, boolean rainOutdoors, boolean grassBelow,
                                          boolean springOrAutumn) {
        return grazer && (rainOutdoors || (grassBelow && springOrAutumn));
    }

    /** Vrai si le tirage {@code roll} (dans [0, 1[) fait arriver l'événement de probabilité {@code chance}. */
    public static boolean hits(double chance, double roll) {
        return roll < chance;
    }

    /** Vrai si le tirage de contagion touche un voisin sain. */
    public static boolean contagionHits(double roll) {
        return hits(CONTAGION_CHANCE, roll);
    }

    /** Effet d'un cycle de maladie : santé −4. */
    public static LivestockState sickStep(LivestockState state) {
        return state.withHealth(state.health() - SICK_HEALTH_LOSS);
    }

    /** Effet d'un cycle de parasites : satiété −6 en plus. */
    public static LivestockState parasiteStep(LivestockState state) {
        return state.withFed(state.fed() - PARASITE_FED_LOSS);
    }

    /** Effet d'un cycle près d'un bloc de sel : santé +1. */
    public static LivestockState saltStep(LivestockState state) {
        return state.withHealth(state.health() + SALT_HEALTH_GAIN);
    }

    /** Soin de la trousse : état sanitaire ramené à NONE, santé +20 (borné à 100). La satiété ne change pas. */
    public static LivestockState treat(LivestockState state) {
        return state.withHealth(state.health() + KIT_HEAL).withAilment(Ailment.NONE);
    }

    /** Vrai si l'animal meurt : santé à 0 et mort à zéro activée. */
    public static boolean shouldKill(LivestockState state, boolean killAtZeroHealth) {
        return killAtZeroHealth && state.health() <= 0;
    }

    /** Nouvel usage d'un bloc de sel : compteur + 1. */
    public static int saltLickAfterUse(int uses) {
        return uses + 1;
    }

    /** Vrai si le bloc de sel est épuisé après {@code usesAfter} usages. */
    public static boolean saltLickGone(int usesAfter) {
        return usesAfter >= SALT_LICK_MAX_USES;
    }
}
