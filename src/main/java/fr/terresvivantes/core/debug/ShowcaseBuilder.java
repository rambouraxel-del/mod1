package fr.terresvivantes.core.debug;

import fr.terresvivantes.apiculture.ApiaryBlockEntity;
import fr.terresvivantes.apiculture.ApicultureModule;
import fr.terresvivantes.automation.AutomationModule;
import fr.terresvivantes.automation.PlanterBlockEntity;
import fr.terresvivantes.storage.CrateBlockEntity;
import fr.terresvivantes.storage.StorageModule;
import fr.terresvivantes.pests.PestsModule;
import fr.terresvivantes.pests.entity.Crow;
import fr.terresvivantes.apiculture.logic.ApiaryLogic;
import fr.terresvivantes.compost.CompostBinBlock;
import fr.terresvivantes.compost.CompostModule;
import fr.terresvivantes.compost.logic.CompostLogic;
import fr.terresvivantes.crop.CropModule;
import fr.terresvivantes.crop.logic.CropDefinition;
import fr.terresvivantes.crop.logic.CropDefinitions;
import fr.terresvivantes.dairy.DairyModule;
import fr.terresvivantes.duck.DuckModule;
import fr.terresvivantes.duck.entity.Duck;
import fr.terresvivantes.flora.FloraModule;
import fr.terresvivantes.flora.HawthornHedgeBlock;
import fr.terresvivantes.health.HealthModule;
import fr.terresvivantes.husbandry.FeedingTroughBlockEntity;
import fr.terresvivantes.husbandry.HusbandryModule;
import fr.terresvivantes.husbandry.logic.FeedType;
import fr.terresvivantes.husbandry.logic.TroughLogic;
import fr.terresvivantes.insects.InsectsModule;
import fr.terresvivantes.kitchen.KitchenModule;
import fr.terresvivantes.milling.MillingModule;
import fr.terresvivantes.preserving.PreservingModule;
import fr.terresvivantes.insects.entity.Butterfly;
import fr.terresvivantes.insects.entity.Ladybug;
import fr.terresvivantes.orchard.FruitLeavesBlock;
import fr.terresvivantes.orchard.OrchardModule;
import fr.terresvivantes.orchard.logic.FruitTreeDefinition;
import fr.terresvivantes.orchard.logic.FruitTreeDefinitions;
import fr.terresvivantes.orchard.logic.OrchardLogic;
import fr.terresvivantes.vet.VetModule;
import fr.terresvivantes.water.RainBarrelBlock;
import fr.terresvivantes.water.WaterModule;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Plateforme de démonstration (/tv showcase) : une colonne par culture (4 terres humides, âges 0, 2, 5, 7),
 * un exemple de chaque bloc du mod, des saplings et des feuillages fruitiers aux quatre stades, les fleurs
 * sauvages, une haie, un apiaire peuplé avec son extracteur, deux papillons et une coccinelle (persistants), et un
 * enclos d'élevage (clôture de chêne) avec auge pleine de foin, nichoir, pierre à sel, une vache, un mouton, une poule
 * et deux canards (mâle et femelle), persistants, et une rangée de transformation : une machine de chaque sorte
 * (meule, pressoir, baratte, cuve à fromage, étagère d'affinage, tonneau, séchoir, fumoir) et une marmite sur un feu
 * de camp allumé.
 *
 * <p>Repère relatif à l'origine (surface de la plateforme, herbe) : x de 0 à {@link #width()}-1, z de -{@link #LEDGE}
 * à {@link #DEPTH}-1, y = -1 (pierre), 0 (surface), 1 à {@link #HEIGHT} (air dégagé).
 *
 * <pre>
 *   z 0..3   cultures : colonnes x = 0..11 (mod), 12..15 (vanilla)
 *   z 5      compost vide (x0), compost prêt (x2), tonneau plein (x4), arroseurs sur eau (x6, x8), flétrie (x10),
 *            fleurs sauvages (x12..15)
 *   z 8      saplings des 6 espèces (x0..5), haie d'aubépine (x6..9, les deux derniers en fleurs)
 *   z 10..15 feuillages: une ligne par espèce, stades 0..3 (x0..3), persistants;
 *            apiaire peuplé (x5, z 10) et extracteur (x7, z 10)
 *   x 8..15, z 9..15  enclos (clôture); intérieur x 9..14, z 10..14: auge (9,10), nichoir (14,10), sel (9,14)
 *   z 17     transformation: meule (x0), pressoir (x2), baratte (x4), cuve (x6), étagère (x8), tonneau (x10),
 *            séchoir (x12), fumoir (x14)
 *   z 20     marmite (x2, y1) sur feu de camp allumé (x2, y0)
 *   z 23..31 équipements: coffre (x7, z23) derrière la récolteuse (x7, z24, face au sud), dont le champ 5x5
 *            (x5..9, z25..29) a une rangée de blé mûr en z 25; planteuse (x13, z24) face à un champ 5x5 vide
 *            (x11..15, z25..29) avec des graines de blé; caisse de tomates (x1, z24); silo de 3 blocs (x3, z27,
 *            y1..3); épouvantail (x10, z27) entre les deux champs
 *   air      deux corbeaux persistants (x10, z29 et x4, z28)
 *   air      deux papillons et une coccinelle, persistants (ils ne disparaissent pas)
 * </pre>
 */
public final class ShowcaseBuilder {

    /** Âges posés sur les 4 terres de chaque colonne de culture. */
    static final int[] AGES = {0, 2, 5, 7};
    /** Cultures vanilla, une colonne chacune après les cultures du mod. */
    static final List<Block> VANILLA_CROPS =
            List.of(Blocks.WHEAT, Blocks.CARROTS, Blocks.POTATOES, Blocks.BEETROOTS);
    /** Rangée de visite devant la plateforme. */
    public static final int LEDGE = 4;
    public static final int DEPTH = 32;
    public static final int HEIGHT = 6;
    static final int ROW_CROPS = 0;
    static final int ROW_BLOCKS = 5;
    static final int ROW_SAPLINGS = 8;
    static final int ROW_LEAVES = 10;
    /** Rangée des machines de transformation, et rangée de la marmite (feu de camp dessous). */
    static final int ROW_PROCESSING = 17;
    static final int ROW_POT = 20;
    /** Section équipements (z 23 à 31) : récolteuse et planteuse face à leur champ de 5x5, caisse, silo, épouvantail. */
    static final int ROW_EQUIP = 24;
    static final int ROW_FIELD_FIRST = 25;
    static final int X_HARVESTER = 7;
    static final int X_PLANTER = 13;
    static final int FARMLAND_WET = 7;
    /** Fleurs sauvages posées sur la rangée des blocs, x = 12..15 (ordre de {@link #placeFlora}). */
    public static final List<String> FLOWER_IDS = List.of("clover", "lavender", "chamomile", "borage");
    static final int X_FLOWERS = 12;
    static final int X_HEDGE = 6;
    static final int X_APIARY = 5;
    static final int X_EXTRACTOR = 7;
    /** Enclos d'élevage : clôture sur le pourtour x 8..15, z 9..15 (intérieur x 9..14, z 10..14). */
    static final int PEN_MIN_X = 8;
    static final int PEN_MAX_X = 15;
    static final int PEN_MIN_Z = 9;
    static final int PEN_MAX_Z = 15;

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

    /** Résultat d'une construction : blocs posés et animaux ou insectes créés (persistants). */
    public record Result(int blocks, List<Mob> fauna) {
    }

    /** Pose la plateforme à partir de {@code origin} (surface). */
    public static Result build(ServerLevel level, BlockPos origin) {
        Placer placer = new Placer(level, origin);
        List<Mob> fauna = new ArrayList<>();
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
        placeFlora(placer);
        placeApiary(placer, level, origin);
        placeLivestock(placer, level, origin);
        placeProcessing(placer);
        placeEquipment(placer, level, origin);
        removeFauna(level, origin);
        spawnInsects(level, origin, fauna);
        spawnLivestock(level, origin, fauna);
        spawnCrows(level, origin, fauna);
        return new Result(placer.count, List.copyOf(fauna));
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
        removeFauna(level, origin);
    }

    /** Une machine de transformation de chaque sorte, en rangée; la marmite a son feu de camp dessous. */
    private static void placeProcessing(Placer placer) {
        placer.set(0, 1, ROW_PROCESSING, MillingModule.MILLSTONE.get().defaultBlockState());
        placer.set(2, 1, ROW_PROCESSING, MillingModule.FRUIT_PRESS.get().defaultBlockState());
        placer.set(4, 1, ROW_PROCESSING, DairyModule.CHURN.get().defaultBlockState());
        placer.set(6, 1, ROW_PROCESSING, DairyModule.CHEESE_VAT.get().defaultBlockState());
        placer.set(8, 1, ROW_PROCESSING, DairyModule.AGING_SHELF.get().defaultBlockState());
        placer.set(10, 1, ROW_PROCESSING, PreservingModule.FERMENTATION_BARREL.get().defaultBlockState());
        placer.set(12, 1, ROW_PROCESSING, PreservingModule.DRYING_RACK.get().defaultBlockState());
        placer.set(14, 1, ROW_PROCESSING, PreservingModule.SMOKEHOUSE.get().defaultBlockState());
        placer.set(2, 0, ROW_POT, Blocks.CAMPFIRE.defaultBlockState().setValue(BlockStateProperties.LIT, true));
        placer.set(2, 1, ROW_POT, KitchenModule.COOKING_POT.get().defaultBlockState());
    }

    /**
     * Section équipements. La récolteuse et la planteuse sont posées face au sud : leur zone de travail 5x5 est devant
     * elles (z 25..29). Le coffre est derrière la récolteuse, là où elle pousse sa récolte.
     */
    private static void placeEquipment(Placer placer, ServerLevel level, BlockPos origin) {
        placer.set(X_HARVESTER, 1, ROW_EQUIP, AutomationModule.HARVESTER.get().defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
        placer.set(X_HARVESTER, 1, ROW_EQUIP - 1, Blocks.CHEST.defaultBlockState());
        placer.set(X_PLANTER, 1, ROW_EQUIP, AutomationModule.PLANTER.get().defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
        // Champs 5x5 devant les machines : terre labourée; la rangée de blé mûr est la première du champ de récolte.
        for (int x = X_HARVESTER - 2; x <= X_HARVESTER + 2; x++) {
            for (int z = ROW_FIELD_FIRST; z < ROW_FIELD_FIRST + 5; z++) {
                placer.set(x, 0, z, farmland());
            }
            placer.set(x, 1, ROW_FIELD_FIRST, Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, CropBlock.MAX_AGE));
        }
        for (int x = X_PLANTER - 2; x <= X_PLANTER + 2; x++) {
            for (int z = ROW_FIELD_FIRST; z < ROW_FIELD_FIRST + 5; z++) {
                placer.set(x, 0, z, farmland());
            }
        }
        if (level.getBlockEntity(origin.offset(X_PLANTER, 1, ROW_EQUIP)) instanceof PlanterBlockEntity planter) {
            planter.container().setItem(0, new ItemStack(Items.WHEAT_SEEDS, 64));
        }
        placer.set(1, 1, ROW_EQUIP, StorageModule.CRATE.get().defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
        if (level.getBlockEntity(origin.offset(1, 1, ROW_EQUIP)) instanceof CrateBlockEntity crate) {
            Item tomato = CropModule.productItem("tomato");
            crate.setContents(tomato, 1);
            crate.setContents(tomato, crate.capacity());
        }
        for (int y = 1; y <= 3; y++) {
            placer.set(3, y, 27, StorageModule.SILO.get().defaultBlockState());
        }
        placer.set(10, 1, 27, PestsModule.SCARECROW.get().defaultBlockState());
    }

    private static void placeFlora(Placer placer) {
        for (int i = 0; i < FLOWER_IDS.size(); i++) {
            placer.set(X_FLOWERS + i, 1, ROW_BLOCKS, FloraModule.flower(FLOWER_IDS.get(i)).defaultBlockState());
        }
        for (int i = 0; i < 4; i++) {
            placer.set(X_HEDGE + i, 1, ROW_SAPLINGS, FloraModule.hedge().defaultBlockState()
                    .setValue(HawthornHedgeBlock.FLOWERING, i >= 2));
        }
    }

    /** Apiaire peuplé au maximum, avec son extracteur à côté. */
    private static void placeApiary(Placer placer, ServerLevel level, BlockPos origin) {
        placer.set(X_APIARY, 1, ROW_LEAVES, ApicultureModule.APIARY.get().defaultBlockState());
        placer.set(X_EXTRACTOR, 1, ROW_LEAVES, ApicultureModule.HONEY_EXTRACTOR.get().defaultBlockState());
        if (level.getBlockEntity(origin.offset(X_APIARY, 1, ROW_LEAVES)) instanceof ApiaryBlockEntity apiary) {
            apiary.setPopulation(ApiaryLogic.MAX_POPULATION);
        }
    }

    /**
     * Enclos d'élevage : auge pleine de foin, nichoir, pierre à sel, clôture sur le pourtour. Les connexions de la
     * clôture sont posées explicitement (pas de mise à jour de voisinage pendant la construction).
     */
    private static void placeLivestock(Placer placer, ServerLevel level, BlockPos origin) {
        for (int x = PEN_MIN_X; x <= PEN_MAX_X; x++) {
            for (int z = PEN_MIN_Z; z <= PEN_MAX_Z; z++) {
                if (!isPenFence(x, z)) {
                    continue;
                }
                placer.set(x, 1, z, Blocks.OAK_FENCE.defaultBlockState()
                        .setValue(FenceBlock.NORTH, isPenFence(x, z - 1))
                        .setValue(FenceBlock.SOUTH, isPenFence(x, z + 1))
                        .setValue(FenceBlock.WEST, isPenFence(x - 1, z))
                        .setValue(FenceBlock.EAST, isPenFence(x + 1, z)));
            }
        }
        placer.set(9, 1, 10, HusbandryModule.TROUGH.get().defaultBlockState());
        placer.set(14, 1, 10, HusbandryModule.NEST.get().defaultBlockState());
        placer.set(9, 1, 14, VetModule.SALT_LICK.get().defaultBlockState());
        if (level.getBlockEntity(origin.offset(9, 1, 10)) instanceof FeedingTroughBlockEntity trough) {
            // Remplie à ras : la capacité de l'auge plafonne le nombre de bottes acceptées.
            trough.insert(FeedType.HAY, TroughLogic.CAPACITY);
        }
    }

    /** Vrai si la case (x, z) fait partie de la clôture de l'enclos. */
    static boolean isPenFence(int x, int z) {
        boolean inX = x >= PEN_MIN_X && x <= PEN_MAX_X;
        boolean inZ = z >= PEN_MIN_Z && z <= PEN_MAX_Z;
        return inX && inZ && (x == PEN_MIN_X || x == PEN_MAX_X || z == PEN_MIN_Z || z == PEN_MAX_Z);
    }

    /** Boîte de la plateforme et de l'air au-dessus, pour retirer les insectes d'une précédente construction. */
    public static AABB area(BlockPos origin) {
        return new AABB(origin.getX(), origin.getY() - 1, origin.getZ() - LEDGE,
                origin.getX() + width(), origin.getY() + HEIGHT + 1, origin.getZ() + DEPTH);
    }

    /** Deux corbeaux persistants, dans la section équipements. */
    private static void spawnCrows(ServerLevel level, BlockPos origin, List<Mob> fauna) {
        spawnPersistent(level, PestsModule.CROW.get(), origin.offset(10, 3, 29), fauna);
        spawnPersistent(level, PestsModule.CROW.get(), origin.offset(4, 3, 28), fauna);
    }

    /** Retire insectes et animaux d'élevage de la vitrine (une construction précédente, ou un /tv showcase répété). */
    private static void removeFauna(ServerLevel level, BlockPos origin) {
        for (Entity entity : level.getEntitiesOfClass(Entity.class, area(origin),
                e -> e instanceof Butterfly || e instanceof Ladybug || e instanceof Cow || e instanceof Sheep
                        || e instanceof Chicken || e instanceof Duck || e instanceof Crow)) {
            entity.discard();
        }
    }

    /** Deux papillons et une coccinelle, persistants : ils ne disparaissent pas quand le joueur s'éloigne. */
    private static void spawnInsects(ServerLevel level, BlockPos origin, List<Mob> fauna) {
        spawnPersistent(level, InsectsModule.BUTTERFLY.get(), origin.offset(12, 3, 2), fauna);
        spawnPersistent(level, InsectsModule.BUTTERFLY.get(), origin.offset(15, 4, 3), fauna);
        spawnPersistent(level, InsectsModule.LADYBUG.get(), origin.offset(13, 1, 11), fauna);
    }

    /** Animaux de l'enclos : une vache, un mouton, une poule, deux canards (mâle et femelle), persistants. */
    private static void spawnLivestock(ServerLevel level, BlockPos origin, List<Mob> fauna) {
        spawnPersistent(level, EntityType.COW, origin.offset(11, 1, 11), fauna);
        spawnPersistent(level, EntityType.SHEEP, origin.offset(12, 1, 13), fauna);
        spawnPersistent(level, EntityType.CHICKEN, origin.offset(14, 1, 12), fauna);
        Duck male = spawnPersistent(level, DuckModule.DUCK.get(), origin.offset(10, 1, 13), fauna);
        if (male != null) {
            male.setMale(true);
        }
        Duck female = spawnPersistent(level, DuckModule.DUCK.get(), origin.offset(13, 1, 14), fauna);
        if (female != null) {
            female.setMale(false);
        }
    }

    private static <T extends PathfinderMob> T spawnPersistent(ServerLevel level, EntityType<T> type, BlockPos pos,
                                                               List<Mob> fauna) {
        T mob = type.spawn(level, pos, MobSpawnType.COMMAND);
        if (mob != null) {
            mob.setPersistenceRequired();
            fauna.add(mob);
        }
        return mob;
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
