package fr.terresvivantes.automation.logic;

/** Cadence et curseur des cycles de travail d'une machine, logique pure. */
public final class CycleLogic {

    private CycleLogic() {
    }

    /** Vrai quand le temps écoulé depuis le dernier cycle atteint l'intervalle. */
    public static boolean isCycleDue(int ticksSinceLastCycle, int intervalTicks) {
        return ticksSinceLastCycle >= intervalTicks;
    }

    /** Curseur suivant, après avoir traité {@code processed} cases à partir de {@code cursor}, modulo {@code total}. */
    public static int nextCursor(int cursor, int processed, int total) {
        if (total <= 0) {
            return 0;
        }
        return Math.floorMod(cursor + processed, total);
    }
}
