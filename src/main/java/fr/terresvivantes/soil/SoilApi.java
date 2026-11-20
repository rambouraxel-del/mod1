package fr.terresvivantes.soil;

import fr.terresvivantes.soil.logic.SoilData;
import fr.terresvivantes.soil.logic.SoilModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;

import java.util.Map;

/** Façade publique du module soil. */
public final class SoilApi {

    private SoilApi() {
    }

    /**
     * Fertilite d'une terre labouree. Initialisation paresseuse selon le biome.
     * Si le bloc n'est plus de la terre labouree, les donnees sont effacees et les valeurs par defaut sont rendues.
     */
    public static SoilData get(Level level, BlockPos farmland) {
        Map<Long, SoilData> entries = entries(level, farmland);
        long key = farmland.asLong();
        if (!isFarmland(level, farmland)) {
            if (entries.remove(key) != null) {
                markUnsaved(level, farmland);
            }
            return SoilData.DEFAULT;
        }
        SoilData existing = entries.get(key);
        if (existing != null) {
            return existing;
        }
        SoilData created = initialFor(level, farmland);
        entries.put(key, created);
        markUnsaved(level, farmland);
        return created;
    }

    /** Consomme N/P/K sur une terre, multiplie par soilConsumptionMultiplier. */
    public static void consume(Level level, BlockPos farmland, int n, int p, int k) {
        if (!isFarmland(level, farmland)) {
            return;
        }
        SoilData current = get(level, farmland);
        store(level, farmland, SoilModel.consume(current, n, p, k, SoilConfig.consumptionMultiplier()));
    }

    /** Ajoute des nutriments et de la matiere organique, bornes a 100. */
    public static void fertilize(Level level, BlockPos farmland, int dn, int dp, int dk, int dOrganic) {
        if (!isFarmland(level, farmland)) {
            return;
        }
        SoilData current = get(level, farmland);
        store(level, farmland, SoilModel.fertilize(current, dn, dp, dk, dOrganic));
    }

    /** Facteur de Liebig de la terre (0.25 à 1.0), avec bonus organique. */
    public static float nutrientFactor(Level level, BlockPos farmland) {
        return SoilModel.nutrientFactor(get(level, farmland));
    }

    /** Supprime les donnees de sol d'une position. */
    public static void clear(Level level, BlockPos farmland) {
        if (entries(level, farmland).remove(farmland.asLong()) != null) {
            markUnsaved(level, farmland);
        }
    }

    private static boolean isFarmland(Level level, BlockPos pos) {
        return level.getBlockState(pos).is(Blocks.FARMLAND);
    }

    private static SoilData initialFor(Level level, BlockPos pos) {
        Holder<Biome> biome = level.getBiome(pos);
        return SoilModel.initialFor(biome.is(Biomes.DESERT), biome.is(BiomeTags.IS_JUNGLE));
    }

    private static void store(Level level, BlockPos pos, SoilData data) {
        entries(level, pos).put(pos.asLong(), data);
        markUnsaved(level, pos);
    }

    private static Map<Long, SoilData> entries(Level level, BlockPos pos) {
        return chunkData(level, pos).entries;
    }

    private static SoilChunkData chunkData(Level level, BlockPos pos) {
        return level.getChunkAt(pos).getData(SoilModule.CHUNK_DATA.get());
    }

    private static void markUnsaved(Level level, BlockPos pos) {
        level.getChunkAt(pos).setUnsaved(true);
    }
}
