package fr.terresvivantes.pollination.logic;

/**
 * Indice de biodiversité (DESIGN section 17). Aucun import net.minecraft: testable en JUnit.
 *
 * <p>Indice dans [0, 1] : fleurs (rayon 8), feuillage fruitier (rayon 6), insectes (abeilles, rayon 16) et
 * habitats enregistrés (haies, etc.). Chaque composante est bornée, puis l'indice total est plafonné à 1.
 * Effet dans la santé : chance de maladie ou de ravageur ×(1 − 0.5 × indice), voir {@code HealthModel}.
 */
public final class BiodiversityModel {

    /** Rayon des fleurs pour l'indice (blocs). */
    public static final int FLOWER_RADIUS = 8;
    /** Contribution par type de fleur distinct. */
    public static final float FLOWER_TYPE_WEIGHT = 0.05f;
    /** Plafond de la composante florale. */
    public static final float FLOWER_CAP = 0.3f;

    /** Rayon du feuillage fruitier pour l'indice (blocs). */
    public static final int FRUIT_RADIUS = 6;
    /** Contribution par feuille fruitière. Une cinquantaine de feuilles atteignent le plafond. */
    public static final float FRUIT_LEAF_WEIGHT = 0.005f;
    /** Plafond de la composante fruitière. */
    public static final float FRUIT_CAP = 0.25f;

    /** Contribution par abeille vanilla dans le rayon de pollinisation. */
    public static final float BEE_WEIGHT = 0.05f;
    /** Plafond de la composante insectes. */
    public static final float INSECT_CAP = 0.2f;

    private BiodiversityModel() {
    }

    /**
     * Indice de biodiversité à partir des comptes locaux.
     *
     * @param flowerTypes  types de fleurs distincts dans le rayon 8
     * @param fruitLeaves  feuilles fruitières dans le rayon 6
     * @param bees         abeilles vanilla dans le rayon 16
     * @param habitat      somme des contributions des habitats enregistrés (haies, etc.)
     * @return indice borné dans [0, 1]
     */
    public static float index(int flowerTypes, int fruitLeaves, int bees, float habitat) {
        float flowers = Math.min(FLOWER_CAP, Math.max(0, flowerTypes) * FLOWER_TYPE_WEIGHT);
        float fruit = Math.min(FRUIT_CAP, Math.max(0, fruitLeaves) * FRUIT_LEAF_WEIGHT);
        float insects = Math.min(INSECT_CAP, Math.max(0, bees) * BEE_WEIGHT);
        return PollinationModel.clamp(flowers + fruit + insects + Math.max(0f, habitat));
    }
}
