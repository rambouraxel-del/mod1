package fr.terresvivantes.water.gametest;

import fr.terresvivantes.TerresVivantes;
import fr.terresvivantes.water.SprinklerBlock;
import fr.terresvivantes.water.WaterModule;
import fr.terresvivantes.water.RainBarrelBlock;
import fr.terresvivantes.water.logic.WaterLogic;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Tests de jeu du module water : arroseur sur eau, tonneau de pluie avec seaux. */
@GameTestHolder(TerresVivantes.MODID)
@PrefixGameTestTemplate(false)
public final class WaterGameTests {

    /** Source d'eau sous l'arroseur (y=1), arroseur en cuivre (y=2). */
    private static final BlockPos WATER = new BlockPos(2, 1, 2);
    private static final BlockPos SPRINKLER = new BlockPos(2, 2, 2);
    /** Terre labourée à deux blocs de l'arroseur, à sa hauteur : hors de portée de l'eau vanilla (y..y+1). */
    private static final BlockPos FARMLAND = new BlockPos(4, 2, 2);
    private static final BlockPos BARREL = new BlockPos(1, 1, 4);
    private static final int SPRINKLER_CHECK_TICKS = 120;
    /** Délai maximal du test : au-delà du contrôle à 120 ticks, pour laisser la main au test. */
    private static final int SPRINKLER_TIMEOUT_TICKS = 200;

    private WaterGameTests() {
    }

    /**
     * Arroseur en cuivre posé sur une source d'eau : après le premier cycle (100 ticks), la terre sèche à deux
     * blocs passe à l'humidité 7. Les randomTicks vanilla sont coupés pendant le test pour qu'ils ne
     * fassent pas baisser l'humidité au moment de la vérification.
     */
    @GameTest(template = "water_arena", timeoutTicks = SPRINKLER_TIMEOUT_TICKS)
    public static void copperSprinklerWetsFarmlandTwoBlocksAway(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        GameRules.IntegerValue randomTicks = level.getGameRules().getRule(GameRules.RULE_RANDOMTICKING);
        int previous = randomTicks.get();
        randomTicks.set(0, level.getServer());

        helper.setBlock(WATER, Blocks.WATER);
        helper.setBlock(SPRINKLER, WaterModule.COPPER_SPRINKLER.get());
        helper.setBlock(FARMLAND, Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, 2));

        helper.runAfterDelay(SPRINKLER_CHECK_TICKS, () -> {
            int moisture = helper.getBlockState(FARMLAND).getValue(FarmBlock.MOISTURE);
            boolean active = helper.getBlockState(SPRINKLER).getValue(SprinklerBlock.ACTIVE);
            randomTicks.set(previous, level.getServer());
            helper.assertTrue(active, "l'arroseur posé sur l'eau doit être actif");
            helper.assertTrue(moisture == WaterLogic.WET_MOISTURE,
                    "humidité attendue " + WaterLogic.WET_MOISTURE + " à deux blocs, obtenu " + moisture);
            helper.succeed();
        });
    }

    /** Seau d'eau sur un tonneau vide : niveau 8 et seau vide en main. Puis seau vide : niveau 0 et seau d'eau. */
    @GameTest(template = "water_arena")
    public static void rainBarrelBucketRoundTrip(GameTestHelper helper) {
        helper.setBlock(BARREL, WaterModule.RAIN_BARREL.get());
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(BARREL);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);

        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
        useHeldItemOn(level, player, pos);
        int filled = level.getBlockState(pos).getValue(RainBarrelBlock.LEVEL);
        helper.assertTrue(filled == WaterLogic.BARREL_MAX_LEVEL,
                "seau d'eau : niveau attendu " + WaterLogic.BARREL_MAX_LEVEL + ", obtenu " + filled);
        helper.assertTrue(player.getItemInHand(InteractionHand.MAIN_HAND).is(Items.BUCKET),
                "le seau d'eau doit devenir un seau vide");

        useHeldItemOn(level, player, pos);
        int emptied = level.getBlockState(pos).getValue(RainBarrelBlock.LEVEL);
        helper.assertTrue(emptied == 0, "seau vide : niveau attendu 0, obtenu " + emptied);
        helper.assertTrue(player.getItemInHand(InteractionHand.MAIN_HAND).is(Items.WATER_BUCKET),
                "le seau vide doit redevenir un seau d'eau");
        helper.succeed();
    }

    /** Clic avec l'objet tenu sur le bloc à pos (position absolue). */
    private static void useHeldItemOn(ServerLevel level, Player player, BlockPos pos) {
        ItemStack stack = player.getItemInHand(InteractionHand.MAIN_HAND);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        level.getBlockState(pos).useItemOn(stack, level, player, InteractionHand.MAIN_HAND, hit);
    }
}
