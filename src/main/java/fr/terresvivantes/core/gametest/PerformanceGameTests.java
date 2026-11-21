package fr.terresvivantes.core.gametest;

import com.mojang.logging.LogUtils;
import fr.terresvivantes.TerresVivantes;
import fr.terresvivantes.automation.AutomationModule;
import fr.terresvivantes.crop.CropModule;
import fr.terresvivantes.milling.MillingModule;
import fr.terresvivantes.milling.MillstoneBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Test de charge : le coût moyen d'un tick de niveau serveur, mesuré dans une arène vide puis avec la charge
 * (500 cultures sur farmland, 50 animaux du tag {@code livestock}, 4 récolteuses, 10 meules actives). Le test échoue
 * si la charge ajoute plus de {@link #MAX_OVERHEAD_MS} ms par tick à la ligne de base.
 *
 * <p>Gabarit {@code perf_arena} : 32 x 4 x 32, vide. Batch dédié, pour qu'aucun autre test ne tourne en parallèle.
 * La mesure part des événements {@link LevelTickEvent.Pre} / {@link LevelTickEvent.Post} du niveau de test, donc
 * elle exclut le temps passé par le harnais GameTest entre deux ticks.
 */
@GameTestHolder(TerresVivantes.MODID)
@PrefixGameTestTemplate(false)
public final class PerformanceGameTests {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** Seuil : surcoût maximal par tick de la charge, par rapport à la ligne de base vide. */
    static final double MAX_OVERHEAD_MS = 15.0;
    /** Ticks de mesure par phase (ligne de base, puis charge). */
    static final int SAMPLE_TICKS = 200;
    /** Ticks d'échauffement avant chaque phase de mesure. */
    static final int WARMUP_TICKS = 20;

    private static final String TEMPLATE = "perf_arena";
    private static final String BATCH = "perf_load";
    private static final int CROP_COLUMNS = 25;
    private static final int CROP_ROWS = 20;
    private static final String[] CROP_IDS = {"tomato", "lettuce", "corn", "cabbage"};

    /** Niveau mesuré pendant le test ({@code null} hors test). */
    private static ServerLevel timedLevel;
    private static long tickStartNs;
    private static long sumNs;
    private static int samples;
    private static boolean recording;
    /** Moyenne de la ligne de base (arène vide), en ms par tick. */
    private static double baselineMs;

    static {
        NeoForge.EVENT_BUS.addListener((LevelTickEvent.Pre event) -> {
            if (event.getLevel() == timedLevel) {
                tickStartNs = System.nanoTime();
            }
        });
        NeoForge.EVENT_BUS.addListener((LevelTickEvent.Post event) -> {
            if (recording && event.getLevel() == timedLevel) {
                sumNs += System.nanoTime() - tickStartNs;
                samples++;
            }
        });
    }

    private PerformanceGameTests() {
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 1200)
    public static void loadedLevelOverheadStaysUnder15msPerTick(GameTestHelper helper) {
        timedLevel = helper.getLevel();
        helper.runAfterDelay(WARMUP_TICKS, PerformanceGameTests::startRecording);
        helper.runAfterDelay(WARMUP_TICKS + SAMPLE_TICKS, () -> {
            baselineMs = stopRecording();
            LOGGER.info("PERF baseline (arène vide) : {} ms/tick sur {} ticks", fmt(baselineMs), samples);
            placeLoad(helper);
        });
        helper.runAfterDelay(2 * WARMUP_TICKS + SAMPLE_TICKS, PerformanceGameTests::startRecording);
        helper.runAfterDelay(2 * WARMUP_TICKS + 2 * SAMPLE_TICKS, () -> {
            double loadedMs = stopRecording();
            double overhead = loadedMs - baselineMs;
            LOGGER.info("PERF charge (500 cultures, 50 animaux, 4 récolteuses, 10 meules) : {} ms/tick sur {} ticks, "
                    + "surcoût {} ms/tick", fmt(loadedMs), samples, fmt(overhead));
            timedLevel = null;
            helper.assertTrue(overhead <= MAX_OVERHEAD_MS, "surcoût de la charge " + fmt(overhead)
                    + " ms/tick, maximum " + MAX_OVERHEAD_MS + " ms/tick");
            helper.succeed();
        });
    }

    private static void startRecording() {
        sumNs = 0;
        samples = 0;
        recording = true;
    }

    /** Arrête la mesure et renvoie la moyenne en ms par tick. */
    private static double stopRecording() {
        recording = false;
        return samples == 0 ? 0.0 : sumNs / (double) samples / 1_000_000.0;
    }

    private static String fmt(double ms) {
        return String.format(java.util.Locale.ROOT, "%.3f", ms);
    }

    /** Pose la charge : 500 cultures, 50 animaux, 4 récolteuses, 10 meules actives. */
    private static void placeLoad(GameTestHelper helper) {
        for (int x = 0; x < 32; x++) {
            for (int z = 0; z < 32; z++) {
                helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
            }
        }
        int planted = 0;
        for (int x = 0; x < CROP_COLUMNS; x++) {
            for (int z = 0; z < CROP_ROWS; z++) {
                helper.setBlock(new BlockPos(x, 1, z), Blocks.FARMLAND);
                String id = CROP_IDS[(x + z) % CROP_IDS.length];
                helper.setBlock(new BlockPos(x, 2, z), CropModule.cropBlock(id).defaultBlockState()
                        .setValue(CropBlock.AGE, (x * 3 + z) % 3));
                planted++;
            }
        }
        LOGGER.info("PERF : {} cultures posées", planted);

        EntityType<?>[] livestock = {EntityType.COW, EntityType.SHEEP, EntityType.PIG,
                EntityType.CHICKEN, EntityType.GOAT};
        int animals = 0;
        for (int i = 0; i < 50; i++) {
            EntityType<?> type = livestock[i % livestock.length];
            int x = (i * 7) % 30 + 1;
            int z = 22 + (i * 3) % 9;
            Animal animal = (Animal) helper.spawn(type, new BlockPos(x, 1, z));
            if (animal != null) {
                animals++;
            }
        }
        LOGGER.info("PERF : {} animaux posés", animals);

        for (int z : new int[] {2, 8, 14, 20}) {
            helper.setBlock(new BlockPos(26, 2, z), AutomationModule.HARVESTER.get().defaultBlockState()
                    .setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
        }

        List<MillstoneBlockEntity> mills = new ArrayList<>();
        for (int x : new int[] {28, 30}) {
            for (int z : new int[] {2, 5, 8, 11, 14}) {
                BlockPos pos = new BlockPos(x, 2, z);
                helper.setBlock(pos, MillingModule.MILLSTONE.get().defaultBlockState());
                if (helper.getBlockEntity(pos) instanceof MillstoneBlockEntity mill) {
                    mill.inventory().setStackInSlot(0, new ItemStack(Items.WHEAT, 64));
                    mills.add(mill);
                }
            }
        }
        LOGGER.info("PERF : {} meules actives", mills.size());
    }
}
