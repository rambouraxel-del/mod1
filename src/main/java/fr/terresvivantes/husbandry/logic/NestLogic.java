package fr.terresvivantes.husbandry.logic;

/**
 * Règles pures du nichoir : interception de la ponte et tirage du prochain délai de ponte.
 */
public final class NestLogic {

    /** Emplacements du nichoir. */
    public static final int SLOTS = 8;

    /** Délai minimal entre deux pontes vanilla (ticks), et étendue du tirage aléatoire. */
    public static final int EGG_TIME_MIN = 6000;
    public static final int EGG_TIME_SPREAD = 6000;

    private NestLogic() {
    }

    /**
     * Vrai si la poule va pondre au prochain tick (le décompte vanilla atteint 0) et peut être interceptée.
     * Les poussins ne pondent pas.
     */
    public static boolean shouldIntercept(int eggTime, boolean baby) {
        return !baby && eggTime <= 1;
    }

    /** Délai avant la prochaine ponte, à partir d'un tirage uniforme (modulo l'étendue). */
    public static int nextEggTime(int roll) {
        return EGG_TIME_MIN + Math.floorMod(roll, EGG_TIME_SPREAD);
    }
}
