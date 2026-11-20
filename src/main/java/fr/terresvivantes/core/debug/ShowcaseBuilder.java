package fr.terresvivantes.core.debug;

import fr.terresvivantes.compost.CompostBinBlock;
import fr.terresvivantes.compost.CompostModule;
import fr.terresvivantes.compost.logic.CompostLogic;
import fr.terresvivantes.crop.CropModule;
import fr.terresvivantes.crop.logic.CropDefinition;
import fr.terresvivantes.crop.logic.CropDefinitions;
import fr.terresvivantes.health.HealthModule;
import fr.terresvivantes.orchard.FruitLeavesBlock;
import fr.terresvivantes.orchard.OrchardModule;
import fr.terresvivantes.orchard.logic.FruitTreeDefinition;
import fr.terresvivantes.orchard.logic.FruitTreeDefinitions;
import fr.terresvivantes.orchard.logic.OrchardLogic;
import fr.terresvivantes.water.RainBarrelBlock;
import fr.terresvivantes.water.WaterModule;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Plateforme de démonstration (/tv showcase) : une colonne par culture (4 terres humides, âges 0, 2, 5, 7),
 * un exemple de chaque bloc du mod, des saplings et des feuillages fruitiers aux quatre stades.
 *
 * <p>Repère relatif à l'origine (surface de la plateforme, herbe) : x de 0 à {@link #width()}-1, z de -{@link #LEDGE}
 * à {@link #DEPTH}-1, y = -1 (pierre), 0 (surface), 1 à {@link #HEIGHT} (air dégagé).
 *
 * <pre>
 *   z 0..3   cultures : colonnes x = 0..11 (mod), 12..15 (vanilla)
 *   z 5      compost vide (x0), compost prêt (x2), tonneau plein (x4), arroseurs sur eau (x6, x8), flétrie (x10)
 *   z 8      saplings des 6 espèces (x0..5)
 *   z 10..15 feuillages: une ligne par espèce, stades 0..3 (x0..3), persistants
 * </pre>
 */
public final class ShowcaseBuilder {

    /** Âges posés sur les 4 terres de chaque colonne de culture. */
    static final int[] AGES = {0, 2, 5, 7};
    /** Cultures vanilla, une colonne chacune après les cultures du mod. */
    static final List<Block> VANILLA_CROPS =
            List.of(Blocks.WHEAT, Blocks.CARROTS, Blocks.POTATOES, Blocks.BEETROOTS);
    /** Rangée de visite devant la plateforme. */
    static final int LEDGE = 4;
    static final int DEPTH = 16;
    public static final int HEIGHT = 6;
    static final int ROW_CROPS = 0;
    static final int ROW_BLOCKS = 5;
    static final int ROW_SAPLINGS = 8;
    static final int ROW_LEAVES = 10;
    static final int FARMLAND_WET = 7;

    private ShowcaseBuilder() {
    }

    /** Nombre de colonnes : une par culture du mod et une par culture vanilla. */
    public static int width() {
        return CropDefinitions.all().size() + VANILLA_CROPS.size();
    }

    /** Point de vue : sur la rampe, au niveau de la surface, à mi-largeur de la plateforme. */
    public static BlockPos viewPoint(BlockPos origin) {
        return origin.offset(width() / 2, 1, -LEDGE / 2);
    }

    /** Pose la plateforme à partir de {@code origin} (surface). Retourne le nombre de blocs posés. */
    public static int build(ServerLevel level, BlockPos origin) {
        Placer placer = new Placer(level, origin);
        int width = width();
        // Dégagement : air au-dessus du sol, puis sol de pierre (y = -1) et herbe (y = 0).
        for (int x = 0; x < width; x++) {
            for (int z = -LEDGE; z < DEPTH; z++) {
                for (int y = 1; y <= HEIGHT; y++) {
                    placer.set(x, y, z, Blocks.AIR.defaultBlockState());
                }
                placer.set(x, -1, z, Blocks.STONE.defaultBlockState());
                placer.set(x, 0, z, Blocks.GRASS_BLOCK.defaultBlockState());
            }
        }
        placeCrops(placer);
        placeBlocks(placer);
        placeSaplingsAndLeaves(placer);
        return placer.count;
    }

    /** Vide la zone de la plateforme (air), pour les tests et pour un nouveau /tv showcase. */
    public static void clear(ServerLevel level, BlockPos origin) {
        Placer placer = new Placer(level, origin);
        for (int x = 0; x < width(); x++) {
            for (int z = -LEDGE; z < DEPTH; z++) {
                for (int y = -1; y <= HEIGHT; y++) {
                    placer.set(x, y, z, Blocks.AIR.defaultBlockState());
                }
            }
        }
    }

    private static void placeCrops(Placer placer) {
        List<CropDefinition> defs = CropDefinitions.all();
        for (int i = 0; i < defs.size(); i++) {
            Block crop = CropModule.cropBlock(defs.get(i).id());
            placeColumn(placer, i, crop);
        }
        for (int j = 0; j < VANILLA_CROPS.size(); j++) {
            placeColumn(placer, defs.size() + j, VANILLA_CROPS.get(j));
        }
    }

    /** Colonne de 4 terres humides le long de z, chacune avec la culture à un âge de {@link #AGES}. */
    private static void placeColumn(Placer placer, int x, Block crop) {
        for (int k = 0; k < AGES.length; k++) {
            placer.set(x, 0, ROW_CROPS + k, farmland());
            CropBlock cropBlock = (CropBlock) crop;
            // La betterave vanilla n'a que 4 stades (0 à 3): l'âge est borné à son maximum.
            int age = Math.min(AGES[k], cropBlock.getMaxAge());
            placer.set(x, 1, ROW_CROPS + k, cropBlock.getStateForAge(age));
        }
    }

    private static void placeBlocks(Placer placer) {
        CompostBinBlock bin = (CompostBinBlock) CompostModule.COMPOST_BIN.get();
        placer.set(0, 1, ROW_BLOCKS, bin.defaultBlockState());
        placer.set(2, 1, ROW_BLOCKS, bin.defaultBlockState()
                .setValue(CompostBinBlock.READY, true)
                .setValue(CompostBinBlock.LEVEL, CompostLogic.MAX_LEVEL));
        placer.set(4, 1, ROW_BLOCKS, WaterModule.RAIN_BARREL.get().defaultBlockState()
                .setValue(RainBarrelBlock.LEVEL, 8));
        placer.set(6, 0, ROW_BLOCKS, Blocks.WATER.defaultBlockState());
        placer.set(6, 1, ROW_BLOCKS, WaterModule.COPPER_SPRINKLER.get().defaultBlockState());
        placer.set(8, 0, ROW_BLOCKS, Blocks.WATER.defaultBlockState());
        placer.set(8, 1, ROW_BLOCKS, WaterModule.IRON_SPRINKLER.get().defaultBlockState());
        placer.set(10, 0, ROW_BLOCKS, farmland());
        placer.set(10, 1, ROW_BLOCKS, HealthModule.WITHERED_CROP.get().defaultBlockState());
    }

    private static void placeSaplingsAndLeaves(Placer placer) {
        List<FruitTreeDefinition> species = FruitTreeDefinitions.all();
        for (int i = 0; i < species.size(); i++) {
            FruitTreeDefinition def = species.get(i);
            placer.set(i, 1, ROW_SAPLINGS, OrchardModule.saplingBlock(def.id()).defaultBlockState());
            FruitLeavesBlock leaves = OrchardModule.leavesBlock(def.id());
            for (int stage = 0; stage <= OrchardLogic.MAX_STAGE; stage++) {
                placer.set(stage, 1, ROW_LEAVES + i, leaves.defaultBlockState()
                        .setValue(FruitLeavesBlock.STAGE, stage)
                        .setValue(LeavesBlock.PERSISTENT, true));
            }
        }
    }

    private static BlockState farmland() {
        return Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, FARMLAND_WET);
    }

    /** Pose des blocs relatifs à l'origine, en comptant les blocs posés. */
    private static final class Placer {
        private final ServerLevel level;
        private final BlockPos origin;
        private int count;

        Placer(ServerLevel level, BlockPos origin) {
            this.level = level;
            this.origin = origin;
        }

        void set(int dx, int dy, int dz, BlockState state) {
            level.setBlock(origin.offset(dx, dy, dz), state, Block.UPDATE_CLIENTS);
            count++;
        }
    }
}
