package fr.terresvivantes.insects.logic;

import fr.terresvivantes.health.logic.HealthModel;

/**
 * Règles d'apparition des pucerons et de leur contrôle par les coccinelles (DESIGN v0.3 §17.3). Aucun import
 * net.minecraft.
 *
 * <p>Apparition : une tentative de croissance d'une culture saine au printemps ou en été tire d'abord contre la
 * chance de base (configurable, {@link #BASE_CHANCE} par défaut). Si ce tirage passe, un second tirage utilise
 * {@link #conditionalChance}: biodiversité ({@link HealthModel#biodiversityFactor}) et suppresseurs de ravageurs.
 * Le produit des deux tirages donne exactement {@code base × biodiversité × suppression}.
 */
public final class AphidLogic {

    /** Chance de base d'apparition par tentative de croissance (0.3 %), avant facteurs. Configurable. */
    public static final double BASE_CHANCE = 0.003;

    /** Chance qu'un puceron gagne une culture voisine, par tentative de la culture infestée. */
    public static final double PROPAGATION_CHANCE = 0.08;

    /** Chance qu'une coccinelle proche nettoie une culture infestée, à chaque cycle d'IA. */
    public static final double CURE_CHANCE = 0.15;

    /** Intervalle entre deux cycles de nettoyage d'une coccinelle, en ticks. */
    public static final int CURE_INTERVAL_TICKS = 40;

    /** Rayon (blocs) où une coccinelle réduit l'apparition des pucerons. */
    public static final int LADYBUG_RADIUS = 8;

    /** Multiplicateur d'apparition des pucerons en présence d'une coccinelle. */
    public static final float LADYBUG_SUPPRESSION = 0.2f;

    private AphidLogic() {
    }

    /**
     * Chance du second tirage : biodiversité et suppression, bornées à [0, 1]. Le premier tirage contre la chance
     * de base doit avoir réussi.
     */
    public static double conditionalChance(float biodiversityIndex, float suppression) {
        double chance = HealthModel.biodiversityFactor(biodiversityIndex) * Math.max(0f, suppression);
        return Math.min(1.0, Math.max(0.0, chance));
    }

    /** Vrai si le tirage tombe sous la chance. */
    public static boolean rollSucceeds(double chance, double roll) {
        return roll < chance;
    }
}
