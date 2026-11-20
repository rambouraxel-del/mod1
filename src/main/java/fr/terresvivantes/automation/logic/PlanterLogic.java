package fr.terresvivantes.automation.logic;

/**
 * Choix de la graine à planter, logique pure (aucun import net.minecraft).
 * Les cases d'inventaire sont décrites par l'identifiant de culture de chaque graine, {@code null} si la case est vide.
 */
public final class PlanterLogic {

    private PlanterLogic() {
    }

    /**
     * Renvoie l'index de la case à planter, ou −1 si aucune graine.
     * On parcourt les cases à partir de {@code start} (rotation des slots). Si la première graine trouvée est la
     * dernière culture de la terre ({@code lastCrop}) et qu'une autre culture est disponible, on préfère l'autre
     * (rotation intelligente). Sinon on garde la première.
     */
    public static int chooseSeedSlot(String[] slotCrops, int start, String lastCrop) {
        int size = slotCrops.length;
        if (size == 0) {
            return -1;
        }
        int first = -1;
        for (int i = 0; i < size; i++) {
            int index = Math.floorMod(start + i, size);
            if (slotCrops[index] != null) {
                first = index;
                break;
            }
        }
        if (first < 0) {
            return -1;
        }
        if (lastCrop == null || !lastCrop.equals(slotCrops[first])) {
            return first;
        }
        for (int i = 1; i < size; i++) {
            int index = Math.floorMod(first + i, size);
            if (slotCrops[index] != null && !lastCrop.equals(slotCrops[index])) {
                return index;
            }
        }
        return first;
    }
}
