package fr.terresvivantes.processing;

/**
 * Description d'une machine de transformation : identifiant (utilisé par les recettes, champ {@code machine}), nombre
 * d'entrées (1 à 4) et présence d'un slot conteneur. Les slots sont numérotés : entrées, puis conteneur (si présent),
 * puis sortie.
 */
public record MachineType(String id, int inputSlots, boolean container) {

    /** Nombre maximal d'entrées d'une machine. */
    public static final int MAX_INPUTS = 4;

    public MachineType {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("identifiant de machine vide");
        }
        if (inputSlots < 1 || inputSlots > MAX_INPUTS) {
            throw new IllegalArgumentException("entrées de " + id + " hors de 1-" + MAX_INPUTS + " : " + inputSlots);
        }
    }

    /** Slot du conteneur, ou -1 s'il n'y en a pas. */
    public int containerSlot() {
        return container ? inputSlots : -1;
    }

    /** Slot de sortie (toujours le dernier). */
    public int outputSlot() {
        return inputSlots + (container ? 1 : 0);
    }

    /** Nombre total de slots internes. */
    public int slotCount() {
        return outputSlot() + 1;
    }
}
