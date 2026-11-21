package fr.terresvivantes.soil.logic;

/**
 * Fertilité d'une terre labourée. Chaque valeur est bornée à 0-100. Aucun import net.minecraft.
 */
public record SoilData(int n, int p, int k, int organic) {

    /** Valeurs par défaut d'une terre non initialisée. */
    public static final SoilData DEFAULT = new SoilData(60, 50, 50, 30);

    /** Bornes les valeurs à 0-100. */
    public SoilData {
        n = clamp(n);
        p = clamp(p);
        k = clamp(k);
        organic = clamp(organic);
    }

    /** Copie avec N remplacé. */
    public SoilData withN(int value) {
        return new SoilData(value, p, k, organic);
    }

    /** Copie avec P remplacé. */
    public SoilData withP(int value) {
        return new SoilData(n, value, k, organic);
    }

    /** Copie avec K remplacé. */
    public SoilData withK(int value) {
        return new SoilData(n, p, value, organic);
    }

    /** Copie avec la matière organique remplacée. */
    public SoilData withOrganic(int value) {
        return new SoilData(n, p, k, value);
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }
}
