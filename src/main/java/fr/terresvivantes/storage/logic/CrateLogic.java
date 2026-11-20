package fr.terresvivantes.storage.logic;

/**
 * Règles pures de la caisse : capacité, insertion, extraction, clic, comparateur. Sans import Minecraft.
 */
public final class CrateLogic {

    /** Nombre de stacks que la caisse peut contenir (multiplié par la taille de stack de l'item). */
    public static final int STACKS = 8;
    /** Fenêtre (en ticks) pour un double clic droit. */
    public static final long DOUBLE_CLICK_TICKS = 10;
    /** Valeur de « pas de clic précédent ». */
    public static final long NO_CLICK = -1;
    /** Niveau maximal du comparateur. */
    public static final int MAX_SIGNAL = 15;

    private CrateLogic() {
    }

    /** Capacité totale en items pour un item de taille de stack donnée (512 pour une stack de 64). */
    public static int capacity(int maxStack) {
        return STACKS * Math.max(1, maxStack);
    }

    /** Une caisse accepte un item si elle est vide ou si elle contient déjà ce même item. */
    public static boolean accepts(boolean empty, boolean sameItem) {
        return empty || sameItem;
    }

    /** Nombre d'items que la caisse peut encore recevoir. */
    public static int insertable(int count, int capacity, int incoming) {
        return Math.min(Math.max(0, incoming), Math.max(0, capacity - count));
    }

    /** Nombre d'items extraits pour une demande donnée (au plus une stack). */
    public static int extractable(int count, int requested, int maxStack) {
        return Math.max(0, Math.min(Math.min(requested, count), Math.max(1, maxStack)));
    }

    /** Clic gauche : une stack entière, ou un seul item accroupi. */
    public static int attackTake(int count, boolean sneaking, int maxStack) {
        if (count <= 0) {
            return 0;
        }
        return sneaking ? 1 : Math.min(count, Math.max(1, maxStack));
    }

    /** Vrai si le clic courant arrive moins de {@link #DOUBLE_CLICK_TICKS} ticks après le précédent. */
    public static boolean isDoubleClick(long previous, long now) {
        return previous != NO_CLICK && now >= previous && now - previous < DOUBLE_CLICK_TICKS;
    }

    /** Signal du comparateur : 0 si vide, puis 1 à 15 selon le remplissage. */
    public static int comparatorLevel(int count, int capacity) {
        if (count <= 0 || capacity <= 0) {
            return 0;
        }
        return Math.min(MAX_SIGNAL, 1 + count * (MAX_SIGNAL - 1) / capacity);
    }
}
