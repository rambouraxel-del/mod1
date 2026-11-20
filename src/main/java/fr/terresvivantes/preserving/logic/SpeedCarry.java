package fr.terresvivantes.preserving.logic;

/**
 * Accumulateur déterministe d'un facteur de vitesse sur des ticks entiers. La machine avance déjà de 1 tick de
 * progression; pour un facteur f, ce compteur renvoie à chaque tick +1 (progression en plus) ou -1 (progression en
 * moins) pour que la moyenne tende vers f. Pas d'aléatoire : les tests de jeu restent reproductibles. Aucun import
 * net.minecraft.
 */
public final class SpeedCarry {

    private static final double EPS = 1e-9;

    private double carry;

    /** Ajustement de progression (-1, 0 ou +1) pour ce tick, avec un facteur de vitesse (1 = neutre). */
    public int step(double factor) {
        carry += factor - 1.0;
        if (carry >= 1.0 - EPS) {
            carry -= 1.0;
            return 1;
        }
        if (carry <= -1.0 + EPS) {
            carry += 1.0;
            return -1;
        }
        return 0;
    }
}
