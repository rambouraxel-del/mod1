package fr.terresvivantes.crop.logic;

/**
 * Modèle de croissance pur (DESIGN 6.2). Aucun import net.minecraft: testable en JUnit.
 */
public final class GrowthModel {

    /** Plage de tolérance climatique au-delà de laquelle la culture ne pousse plus, en °C. */
    private static final float CLIMATE_NEAR_LIMIT_DEGREES = 8.0f;

    private GrowthModel() {
    }

    /**
     * Facteur de croissance total : produit des cinq facteurs.
     *
     * @param season   multiplicateur saisonnier (0 à 1.2)
     * @param affinity multiplicateur d'affinité (voir {@link Affinity#getMultiplier()})
     * @param climate  facteur climatique (voir {@link #climateFactor(float, float, float)})
     * @param soil     facteur de fertilité (Liebig, bonus organique)
     * @param moisture facteur d'humidité (0.5 si sol sec, 1.0 sinon)
     */
    public static float totalFactor(float season, float affinity, float climate, float soil, float moisture) {
        return season * affinity * climate * soil * moisture;
    }

    /**
     * Produit des modifiers de croissance enregistrés (contrat v0.2). Une valeur négative est ramenée à 0.
     * Sans modifier, le produit vaut 1.0.
     *
     * @param modifiers facteurs de chaque modifier
     */
    public static float combineModifiers(float... modifiers) {
        float product = 1.0f;
        for (float modifier : modifiers) {
            product *= Math.max(0.0f, modifier);
        }
        return product;
    }

    /**
     * Facteur climatique : 1.0 dans la plage, 0.3 à moins de 8 °C hors plage, 0 au-delà.
     *
     * @param temp température courante en °C
     * @param min  température minimale de la culture en °C
     * @param max  température maximale de la culture en °C
     */
    public static float climateFactor(float temp, float min, float max) {
        float distance = Math.max(0.0f, Math.max(min - temp, temp - max));
        if (distance == 0.0f) {
            return 1.0f;
        }
        return distance <= CLIMATE_NEAR_LIMIT_DEGREES ? 0.3f : 0.0f;
    }

    /**
     * Décide de l'issue d'une tentative de croissance.
     * f &lt; 1 : DENY si roll1 &lt; 1 - f. f &gt; 1 : EXTRA_STAGE si roll2 &lt; f - 1. Sinon NORMAL.
     *
     * @param factor facteur total (voir {@link #totalFactor})
     * @param roll1  tirage uniforme dans [0, 1[
     * @param roll2  tirage uniforme dans [0, 1[
     */
    public static GrowthDecision decide(float factor, double roll1, double roll2) {
        if (factor < 1.0f) {
            return roll1 < 1.0 - factor ? GrowthDecision.DENY : GrowthDecision.NORMAL;
        }
        if (factor > 1.0f) {
            return roll2 < factor - 1.0 ? GrowthDecision.EXTRA_STAGE : GrowthDecision.NORMAL;
        }
        return GrowthDecision.NORMAL;
    }
}
