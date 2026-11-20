package fr.terresvivantes.health;

import fr.terresvivantes.crop.CropApi;
import fr.terresvivantes.health.logic.CropHealth;
import fr.terresvivantes.health.logic.Disease;
import fr.terresvivantes.health.logic.HealthModel;
import fr.terresvivantes.season.SeasonApi;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Façade publique du module health. Les autres modules n'appellent que cette classe.
 * Les méthodes qui lisent la saison ou modifient le monde s'appellent côté serveur uniquement.
 */
public final class HealthApi {

    private HealthApi() {
    }

    /**
     * Santé d'une culture. Une position qui n'est plus une culture perd son entrée et rend {@link CropHealth#HEALTHY}.
     */
    public static CropHealth get(Level level, BlockPos pos) {
        Map<Long, CropHealth> entries = chunkData(level, pos).entries;
        CropHealth health = entries.get(pos.asLong());
        if (health == null) {
            return CropHealth.HEALTHY;
        }
        if (!isCrop(level.getBlockState(pos))) {
            entries.remove(pos.asLong());
            markUnsaved(level, pos);
            return CropHealth.HEALTHY;
        }
        return health;
    }

    /** Vrai si la culture est malade. */
    public static boolean isSick(Level level, BlockPos pos) {
        return get(level, pos).sick();
    }

    /** Maladie ou ravageur en cours d'une culture ({@link Disease#NONE} si saine). */
    public static Disease diseaseOf(Level level, BlockPos pos) {
        return get(level, pos).disease();
    }

    /**
     * Multiplicateur de l'apparition des ravageurs, produit des suppresseurs enregistrés (v0.3). 1.0 sans suppresseur.
     * Destiné au module insects : une coccinelle à proximité réduit la chance qu'une culture se couvre de pucerons.
     */
    @FunctionalInterface
    public interface PestSuppressor {
        float multiplier(ServerLevel level, BlockPos pos);
    }

    private static final List<PestSuppressor> PEST_SUPPRESSORS = new CopyOnWriteArrayList<>();

    /** Enregistre un suppresseur de ravageurs. À appeler à l'initialisation du mod. */
    public static void registerPestSuppressor(PestSuppressor suppressor) {
        PEST_SUPPRESSORS.add(suppressor);
    }

    /** Produit des suppresseurs enregistrés à cette position (valeurs négatives ramenées à 0). */
    public static float pestSuppression(ServerLevel level, BlockPos pos) {
        float factor = 1.0f;
        for (PestSuppressor suppressor : PEST_SUPPRESSORS) {
            factor *= Math.max(0f, suppressor.multiplier(level, pos));
        }
        return factor;
    }

    /**
     * Installe des pucerons sur une culture saine (v0.3). Ne fait rien sur une culture déjà malade ou une position
     * sans culture. Le stress et l'immunité sont conservés. Retourne vrai si l'infestation a eu lieu.
     */
    public static boolean infest(Level level, BlockPos pos) {
        if (!isCrop(level.getBlockState(pos))) {
            return false;
        }
        CropHealth health = get(level, pos);
        if (health.sick()) {
            return false;
        }
        put(level, pos, new CropHealth(health.stress(), Disease.APHIDS, 0, health.immuneUntilDay()));
        return true;
    }

    /** Retire les pucerons d'une culture (v0.3). Retourne vrai s'il y en avait. Les maladies ne sont pas touchées. */
    public static boolean cure(Level level, BlockPos pos) {
        CropHealth health = get(level, pos);
        if (!health.disease().isPest()) {
            return false;
        }
        put(level, pos, new CropHealth(health.stress(), Disease.NONE, 0, health.immuneUntilDay()));
        return true;
    }

    /** Vrai si la culture est immunisée à la date courante (serveur). */
    public static boolean isImmune(ServerLevel level, BlockPos pos) {
        return get(level, pos).immune(SeasonApi.dayIndex(level));
    }

    /** Dernier jour d'immunité pour une culture traitée maintenant : fin de la saison courante (serveur). */
    public static long immuneUntilDay(ServerLevel level) {
        return HealthModel.immuneUntilDay(SeasonApi.dayIndex(level), SeasonApi.getDayOfSeason(level),
                SeasonApi.getDaysPerSeason());
    }

    /** Contamine une culture avec la maladie donnée (sans immunité supplémentaire). */
    public static void infect(Level level, BlockPos pos, Disease disease) {
        CropHealth health = get(level, pos);
        put(level, pos, new CropHealth(health.stress(), disease, 0, health.immuneUntilDay()));
    }

    /**
     * Traitement au fongicide ou au pulvérisateur : guérit la culture et l'immunise jusqu'à la fin de la saison.
     * Le stress n'est pas modifié.
     */
    public static void treat(ServerLevel level, BlockPos pos) {
        CropHealth health = get(level, pos);
        put(level, pos, new CropHealth(health.stress(), Disease.NONE, 0, immuneUntilDay(level)));
    }

    /** Remplace la culture par une culture flétrie et efface sa santé. */
    public static void wither(ServerLevel level, BlockPos pos) {
        put(level, pos, CropHealth.HEALTHY);
        level.setBlock(pos, HealthModule.WITHERED_CROP.get().defaultBlockState(), Block.UPDATE_ALL);
        level.sendParticles(ParticleTypes.SMOKE,
                pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5, 4, 0.2, 0.1, 0.2, 0.01);
        level.playSound(null, pos, SoundEvents.GRASS_BREAK, SoundSource.BLOCKS, 0.6f, 0.9f);
    }

    /** Efface la santé d'une position (casse de la culture). */
    public static void clear(Level level, BlockPos pos) {
        if (chunkData(level, pos).entries.remove(pos.asLong()) != null) {
            markUnsaved(level, pos);
        }
    }

    /** Effets d'un traitement réussi : son et particules de soin. */
    public static void treatmentEffects(ServerLevel level, BlockPos pos) {
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 6, 0.3, 0.2, 0.3, 0.0);
        level.playSound(null, pos, SoundEvents.BONE_MEAL_USE, SoundSource.BLOCKS, 0.8f, 1.1f);
    }

    /** Envoie au joueur la santé d'une culture : stress, maladie, immunité. */
    public static void report(Player player, Level level, BlockPos pos) {
        CropHealth health = get(level, pos);
        player.sendSystemMessage(Component.translatable("message.terresvivantes.health.stress",
                health.stress(), HealthModel.WITHER_STRESS));
        if (health.sick()) {
            player.sendSystemMessage(Component.translatable("message.terresvivantes.health.disease",
                    Component.translatable(diseaseKey(health.disease()))));
        } else {
            player.sendSystemMessage(Component.translatable("message.terresvivantes.health.healthy"));
        }
        boolean immune = level instanceof ServerLevel server && health.immune(SeasonApi.dayIndex(server));
        player.sendSystemMessage(Component.translatable("message.terresvivantes.health.immune",
                Component.translatable(immune ? "message.terresvivantes.health.yes" : "message.terresvivantes.health.no")));
    }

    /** Vrai si le bloc est une culture (vanilla ou du mod). */
    static boolean isCrop(BlockState state) {
        return state.getBlock() instanceof CropBlock;
    }

    /** Identifiant de culture d'un état, pour les règles de maladie. */
    static String cropId(BlockState state) {
        return CropApi.cropIdOf(state);
    }

    private static String diseaseKey(Disease disease) {
        return switch (disease) {
            case MILDEW -> "message.terresvivantes.health.disease.mildew";
            case RUST -> "message.terresvivantes.health.disease.rust";
            case APHIDS -> "message.terresvivantes.health.disease.aphids";
            case NONE -> "message.terresvivantes.health.healthy";
        };
    }

    /** Écrit la santé d'une culture; la santé par défaut n'est pas stockée. */
    static void put(Level level, BlockPos pos, CropHealth health) {
        Map<Long, CropHealth> entries = chunkData(level, pos).entries;
        if (health.equals(CropHealth.HEALTHY)) {
            entries.remove(pos.asLong());
        } else {
            entries.put(pos.asLong(), health);
        }
        markUnsaved(level, pos);
    }

    private static HealthChunkData chunkData(Level level, BlockPos pos) {
        return level.getChunkAt(pos).getData(HealthModule.CHUNK_DATA.get());
    }

    private static void markUnsaved(Level level, BlockPos pos) {
        level.getChunkAt(pos).setUnsaved(true);
    }
}
