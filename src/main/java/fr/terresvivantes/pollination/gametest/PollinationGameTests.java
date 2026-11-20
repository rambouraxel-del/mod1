package fr.terresvivantes.pollination.gametest;

import fr.terresvivantes.TerresVivantes;
import fr.terresvivantes.pollination.PollinationApi;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Tests de jeu de la pollinisation. Gabarit orchard_arena (volume d'air de 11 x 12 x 11). */
@GameTestHolder(TerresVivantes.MODID)
@PrefixGameTestTemplate(false)
public final class PollinationGameTests {

    private static final String TEMPLATE = "orchard_arena";
    private static final BlockPos CENTER = new BlockPos(5, 2, 5);
    private static final float DELTA = 1e-4f;

    private PollinationGameTests() {
    }

    /**
     * Le score augmente avec des fleurs de types variés, puis avec une abeille vanilla proche.
     * Le score est lu sans cache : le cache de 600 ticks ne doit pas masquer les changements du test.
     */
    @GameTest(template = TEMPLATE)
    public static void varietyOfFlowersAndBeeRaiseScore(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos center = helper.absolutePos(CENTER);
        float empty = PollinationApi.scoreUncached(level, center);

        helper.setBlock(new BlockPos(3, 2, 5), Blocks.DANDELION);
        helper.setBlock(new BlockPos(7, 2, 5), Blocks.POPPY);
        helper.setBlock(new BlockPos(5, 2, 3), Blocks.CORNFLOWER);
        float flowers = PollinationApi.scoreUncached(level, center);
        helper.assertTrue(flowers > empty + DELTA,
                "trois types de fleurs doivent augmenter le score : " + empty + " -> " + flowers);

        Bee bee = helper.spawn(EntityType.BEE, CENTER);
        helper.assertTrue(bee != null, "l'abeille doit être posée");
        float withBee = PollinationApi.scoreUncached(level, center);
        helper.assertTrue(withBee > flowers + DELTA,
                "une abeille proche doit augmenter le score : " + flowers + " -> " + withBee);
        helper.succeed();
    }
}
