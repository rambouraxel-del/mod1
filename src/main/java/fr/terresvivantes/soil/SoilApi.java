package fr.terresvivantes.soil;

import fr.terresvivantes.soil.logic.SoilData;
import fr.terresvivantes.soil.logic.SoilModel;
import fr.terresvivantes.soil.SoilChunkData.LastCrop;
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

    /**
     * Aerage d'une terre labouree (v0.6) : ajoute de la matiere organique, au plus une fois par bloc et par jour MC.
     * Renvoie {@code false} sans effet si la terre a deja ete aeree ce jour-la ou si ce n'est pas de la terre labouree.
     */
    public static boolean aerate(Level level, BlockPos farmland, int dOrganic) {
        if (!isFarmland(level, farmland)) {
            return false;
        }
        long today = SoilModel.mcDay(level.getDayTime());
        Map<Long, Long> days = aeratedDays(level, farmland);
        long key = farmland.asLong();
        long last = days.getOrDefault(key, SoilModel.NEVER_AERATED);
        if (!SoilModel.canAerate(last, today)) {
            return false;
        }
        days.put(key, today);
        fertilize(level, farmland, 0, 0, 0, dOrganic);
        markUnsaved(level, farmland);
        return true;
    }

    /** Facteur de Liebig de la terre (0.25 à 1.0), avec bonus organique. */
    public static float nutrientFactor(Level level, BlockPos farmland) {
        return SoilModel.nutrientFactor(get(level, farmland));
    }

    /**
     * Dernière culture récoltée sur cette terre (id de {@code CropApi.cropIdOf}), ou {@code null} si aucune.
     */
    public static String getLastCrop(Level level, BlockPos farmland) {
        LastCrop last = lastCrops(level, farmland).get(farmland.asLong());
        return last == null ? null : last.cropId();
    }

    /**
     * Nombre de récoltes consécutives de la même culture sur cette terre : 0 à la première récolte,
     * +1 à chaque récolte de la même culture, remis à 0 quand la culture change.
     */
    public static int getRepeatCount(Level level, BlockPos farmland) {
        LastCrop last = lastCrops(level, farmland).get(farmland.asLong());
        return last == null ? 0 : last.repeat();
    }

    /**
     * Enregistre la culture récoltée sur une terre labourée. Si c'est la même culture que la précédente,
     * le compteur de répétition augmente de 1; sinon il repart à 0. Sans effet si ce n'est pas de la terre labourée.
     */
    public static void setLastCrop(Level level, BlockPos farmland, String cropId) {
        if (!isFarmland(level, farmland)) {
            return;
        }
        Map<Long, LastCrop> crops = lastCrops(level, farmland);
        LastCrop previous = crops.get(farmland.asLong());
        int repeat = previous != null && previous.cropId().equals(cropId) ? previous.repeat() + 1 : 0;
        crops.put(farmland.asLong(), new LastCrop(cropId, repeat));
        markUnsaved(level, farmland);
    }

    /** Jour MC du dernier aerage de cette terre, ou {@code SoilModel.NEVER_AERATED}. */
    public static long lastAeratedDay(Level level, BlockPos farmland) {
        return aeratedDays(level, farmland).getOrDefault(farmland.asLong(), SoilModel.NEVER_AERATED);
    }

    /** Supprime les donnees de sol (fertilite et dernieres cultures) d'une position. */
    public static void clear(Level level, BlockPos farmland) {
        boolean removed = entries(level, farmland).remove(farmland.asLong()) != null;
        removed |= lastCrops(level, farmland).remove(farmland.asLong()) != null;
        removed |= aeratedDays(level, farmland).remove(farmland.asLong()) != null;
        if (removed) {
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

    private static Map<Long, LastCrop> lastCrops(Level level, BlockPos pos) {
        return chunkData(level, pos).lastCrops;
    }

    private static Map<Long, Long> aeratedDays(Level level, BlockPos pos) {
        return chunkData(level, pos).aeratedDay;
    }

    private static SoilChunkData chunkData(Level level, BlockPos pos) {
        return level.getChunkAt(pos).getData(SoilModule.CHUNK_DATA.get());
    }

    private static void markUnsaved(Level level, BlockPos pos) {
        level.getChunkAt(pos).setUnsaved(true);
    }
}
