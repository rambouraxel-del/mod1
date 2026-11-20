package fr.terresvivantes.soil;

import fr.terresvivantes.soil.logic.SoilData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/** Façade publique du module soil. */
public final class SoilApi {

    private SoilApi() {
    }

    /** Fertilité d'une terre labourée. STUB – implémenté par le module soil (défauts 60/50/50/30). */
    public static SoilData get(Level level, BlockPos farmland) {
        return SoilData.DEFAULT;
    }

    /** Consomme N/P/K sur une terre. STUB – implémenté par le module soil (no-op). */
    public static void consume(Level level, BlockPos farmland, int n, int p, int k) {
    }

    /** Ajoute des nutriments et de la matière organique. STUB – implémenté par le module soil (no-op). */
    public static void fertilize(Level level, BlockPos farmland, int dn, int dp, int dk, int dOrganic) {
    }

    /** Facteur de Liebig de la terre (0.25 à 1.0). STUB – implémenté par le module soil (retourne 1.0). */
    public static float nutrientFactor(Level level, BlockPos farmland) {
        return 1.0f;
    }

    /** Supprime les données de sol d'une position. STUB – implémenté par le module soil (no-op). */
    public static void clear(Level level, BlockPos farmland) {
    }
}
