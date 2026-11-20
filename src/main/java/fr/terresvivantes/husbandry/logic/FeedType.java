package fr.terresvivantes.husbandry.logic;

/**
 * Type d'aliment d'une auge. Une auge ne contient qu'un seul type à la fois.
 * {@link #unitsPerItem} : unités de stock apportées par un objet (foin : 9, blé ou orge : 1, ensilage : 3, aliment : 4).
 */
public enum FeedType {
    HAY(9),
    GRAIN(1),
    SILAGE(3),
    MIX(4);

    private final int unitsPerItem;

    FeedType(int unitsPerItem) {
        this.unitsPerItem = unitsPerItem;
    }

    public int unitsPerItem() {
        return unitsPerItem;
    }
}
