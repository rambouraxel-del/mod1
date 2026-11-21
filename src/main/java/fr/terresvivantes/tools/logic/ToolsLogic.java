package fr.terresvivantes.tools.logic;

/**
 * Logique pure du module Tools (durabilite, utilisations, niveaux affiches par la sonde).
 * Aucun import net.minecraft: testable en JUnit. Source : docs/DESIGN.md section 9.
 */
public final class ToolsLogic {

    /** Durabilite de la faucille (une utilisation par culture recoltee). */
    public static final int SICKLE_DURABILITY = 250;

    /** Utilisations d'un arrosoir plein. */
    public static final int WATERING_CAN_USES = 8;

    /** Humidite vanilla maximale de la terre labouree (FarmBlock.MOISTURE). */
    public static final int MAX_MOISTURE = 7;

    /** Humidite donnee par l'arrosoir. */
    public static final int WATERED_MOISTURE = MAX_MOISTURE;

    /** Rayon de la zone 3x3 (outils faucille et arrosoir). */
    public static final int AREA_RADIUS = 1;

    /** Niveau affiche par la sonde. */
    public enum Tier { LOW, MEDIUM, HIGH }

    private ToolsLogic() {
    }

    /** Niveau d'un nutriment ou de la matiere organique (valeur bornee 0-100). */
    public static Tier nutrientTier(int value) {
        if (value < 30) {
            return Tier.LOW;
        }
        if (value < 60) {
            return Tier.MEDIUM;
        }
        return Tier.HIGH;
    }

    /** Niveau d'humidite vanilla (0-7) : sec, humide partiellement, plein. */
    public static Tier moistureTier(int moisture) {
        if (moisture >= MAX_MOISTURE) {
            return Tier.HIGH;
        }
        if (moisture >= 1) {
            return Tier.MEDIUM;
        }
        return Tier.LOW;
    }
}
