package fr.terresvivantes.flora.gametest;

import fr.terresvivantes.TerresVivantes;
import fr.terresvivantes.flora.FloraModule;
import fr.terresvivantes.flora.FloraSpread;
import fr.terresvivantes.flora.HawthornHedgeBlock;
import fr.terresvivantes.flora.WildFlowerBlock;
import fr.terresvivantes.pollination.BiodiversityApi;
import fr.terresvivantes.pollination.PollinationApi;
import fr.terresvivantes.season.SeasonApi;
import fr.terresvivantes.season.logic.Season;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Locale;

/** Tests de jeu de la flore. Gabarit orchard_arena (volume d'air de 11 x 12 x 11). */
@GameTestHolder(TerresVivantes.MODID)
@PrefixGameTestTemplate(false)
public final class FloraGameTests {

    private static final String TEMPLATE = "orchard_arena";
    private static final BlockPos CENTER = new BlockPos(5, 2, 5);
    private static final int MAX_FORCED_TICKS = 200;
    private static final float DELTA = 1e-4f;

    private FloraGameTests() {
    }

    /** Au printemps, un trèfle sur herbe propage une copie dans le voisinage sous tentatives forcées. */
    @GameTest(template = TEMPLATE)
    public static void springSpreadsCloverToNeighbouringGrass(GameTestHelper helper) {
        setSeason(helper, "spring");
        ServerLevel level = helper.getLevel();
        WildFlowerBlock clover = FloraModule.flower("clover");
        for (int x = 2; x <= 8; x++) {
            for (int z = 2; z <= 8; z++) {
                helper.setBlock(new BlockPos(x, 1, z), Blocks.GRASS_BLOCK);
            }
        }
        helper.setBlock(CENTER, clover.defaultBlockState());
        BlockPos center = helper.absolutePos(CENTER);
        RandomSource random = RandomSource.create(4242L);

        for (int i = 0; i < MAX_FORCED_TICKS && countFlowers(helper, clover) < 2; i++) {
            FloraSpread.attempt(level, center, clover, random, 1.0f);
        }

        int count = countFlowers(helper, clover);
        helper.assertTrue(count >= 2, "le trèfle doit se propager au printemps, fleurs obtenues : " + count);
        helper.succeed();
    }

    /** La haie fleurit au printemps et perd ses fleurs à l'automne, sous randomTicks. */
    @GameTest(template = TEMPLATE)
    public static void hedgeFloweringFollowsSpring(GameTestHelper helper) {
        setSeason(helper, "spring");
        ServerLevel level = helper.getLevel();
        HawthornHedgeBlock hedge = FloraModule.hedge();
        BlockPos rel = new BlockPos(3, 2, 3);
        helper.setBlock(rel, hedge.defaultBlockState());
        BlockPos pos = helper.absolutePos(rel);
        RandomSource random = RandomSource.create(7L);

        level.getBlockState(pos).randomTick(level, pos, random);
        helper.assertTrue(level.getBlockState(pos).getValue(HawthornHedgeBlock.FLOWERING),
                "la haie doit fleurir au printemps");

        setSeason(helper, "autumn");
        level.getBlockState(pos).randomTick(level, pos, random);
        helper.assertTrue(!level.getBlockState(pos).getValue(HawthornHedgeBlock.FLOWERING),
                "la haie ne doit plus fleurir en automne");
        helper.succeed();
    }

    /** Les quatre fleurs sauvages sont dans le tag des fleurs et font monter le score de pollinisation. */
    @GameTest(template = TEMPLATE)
    public static void fourWildFlowersRaisePollinationScore(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos center = helper.absolutePos(CENTER);
        float empty = PollinationApi.scoreUncached(level, center);

        helper.setBlock(new BlockPos(3, 2, 5), FloraModule.flower("clover"));
        helper.setBlock(new BlockPos(7, 2, 5), FloraModule.flower("lavender"));
        helper.setBlock(new BlockPos(5, 2, 3), FloraModule.flower("chamomile"));
        helper.setBlock(new BlockPos(5, 2, 7), FloraModule.flower("borage"));
        for (BlockPos rel : new BlockPos[] {new BlockPos(3, 2, 5), new BlockPos(7, 2, 5),
                new BlockPos(5, 2, 3), new BlockPos(5, 2, 7)}) {
            BlockState state = level.getBlockState(helper.absolutePos(rel));
            helper.assertTrue(state.is(BlockTags.FLOWERS), "la fleur doit être dans minecraft:flowers : " + state);
        }

        float withFlowers = PollinationApi.scoreUncached(level, center);
        helper.assertTrue(withFlowers > empty + DELTA,
                "quatre fleurs sauvages doivent augmenter le score : " + empty + " -> " + withFlowers);
        helper.succeed();
    }

    /** Deux haies proches augmentent l'indice de biodiversité (habitat). */
    @GameTest(template = TEMPLATE)
    public static void hedgesRaiseBiodiversityIndex(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos center = helper.absolutePos(CENTER);
        float empty = BiodiversityApi.indexUncached(level, center);

        helper.setBlock(new BlockPos(3, 2, 5), FloraModule.hedge().defaultBlockState());
        helper.setBlock(new BlockPos(7, 2, 5), FloraModule.hedge().defaultBlockState());
        float withHedges = BiodiversityApi.indexUncached(level, center);
        helper.assertTrue(withHedges > empty + DELTA,
                "deux haies doivent augmenter l'indice : " + empty + " -> " + withHedges);
        helper.succeed();
    }

    /** Nombre de trèfles dans le carré 5 x 5 autour du centre, à la hauteur de la fleur. */
    private static int countFlowers(GameTestHelper helper, WildFlowerBlock flower) {
        int count = 0;
        for (int x = 3; x <= 7; x++) {
            for (int z = 3; z <= 7; z++) {
                if (helper.getLevel().getBlockState(helper.absolutePos(new BlockPos(x, CENTER.getY(), z)))
                        .is(flower)) {
                    count++;
                }
            }
        }
        return count;
    }

    private static void setSeason(GameTestHelper helper, String season) {
        MinecraftServer server = helper.getLevel().getServer();
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tv season set " + season + " 1");
        helper.assertTrue(SeasonApi.getSeason(helper.getLevel()) == Season.valueOf(season.toUpperCase(Locale.ROOT)),
                "la saison de test doit être " + season);
    }
}
