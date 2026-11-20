package fr.terresvivantes.pests.gametest;

import fr.terresvivantes.TerresVivantes;
import fr.terresvivantes.pests.CropPecking;
import fr.terresvivantes.pests.PestsModule;
import fr.terresvivantes.pests.block.ScarecrowBlock;
import fr.terresvivantes.pests.entity.Crow;
import fr.terresvivantes.pests.logic.CrowLogic;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Tests de jeu du module pests. Gabarit {@code orchard_arena} (air 11 x 12 x 11). Le picorage est appelé directement
 * avec un tirage fixé, pour être déterministe.
 */
@GameTestHolder(TerresVivantes.MODID)
@PrefixGameTestTemplate(false)
public final class PestsGameTests {

    private static final String TEMPLATE = "orchard_arena";
    private static final String BATCH = "pests";
    private static final BlockPos SOIL = new BlockPos(5, 1, 5);
    private static final BlockPos CROP = SOIL.above();

    private PestsGameTests() {
    }

    /** Corbeau posé sur un blé mûr (âge 7) : un coup de bec réussi fait régresser la culture de deux stades. */
    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void crowPeckRegressesRipeWheat(GameTestHelper helper) {
        helper.setBlock(SOIL, Blocks.FARMLAND.defaultBlockState());
        helper.setBlock(CROP, Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, 7));
        Crow crow = helper.spawn(PestsModule.CROW.get(), CROP);
        crow.startPecking(helper.absolutePos(CROP));

        CrowLogic.PeckOutcome outcome = CropPecking.peck(helper.getLevel(), helper.absolutePos(CROP), 0.0);

        helper.assertTrue(outcome == CrowLogic.PeckOutcome.REGRESS, "issue attendue REGRESS, obtenue " + outcome);
        int age = helper.getBlockState(CROP).getValue(CropBlock.AGE);
        helper.assertTrue(age == 5, "blé à l'âge 7 doit redescendre à 5 après un coup réussi, obtenu " + age);
        helper.succeed();
    }

    /** Épouvantail à 4 blocs : le même coup de bec, au même tirage, ne touche pas la culture. */
    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void scarecrowKeepsCropSafeFromPecks(GameTestHelper helper) {
        helper.setBlock(SOIL, Blocks.FARMLAND.defaultBlockState());
        helper.setBlock(CROP, Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, 7));
        placeScarecrow(helper, new BlockPos(9, 2, 5));

        CrowLogic.PeckOutcome outcome = CropPecking.peck(helper.getLevel(), helper.absolutePos(CROP), 0.0);

        helper.assertTrue(outcome == CrowLogic.PeckOutcome.NONE, "culture protégée : aucun coup attendu, obtenu " + outcome);
        int age = helper.getBlockState(CROP).getValue(CropBlock.AGE);
        helper.assertTrue(age == 7, "culture protégée par l'épouvantail : âge 7 attendu, obtenu " + age);
        helper.succeed();
    }

    /** Épouvantail posé : les deux moitiés apparaissent, et casser l'une retire l'autre, sans rien laisser. */
    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void scarecrowPlacesAndBreaksBothHalves(GameTestHelper helper) {
        BlockPos lower = new BlockPos(2, 2, 8);
        placeScarecrow(helper, lower);
        helper.assertTrue(helper.getBlockState(lower).getValue(ScarecrowBlock.HALF) == DoubleBlockHalf.LOWER,
                "la partie basse doit être posée");
        helper.assertTrue(helper.getBlockState(lower.above()).getValue(ScarecrowBlock.HALF) == DoubleBlockHalf.UPPER,
                "la partie haute doit être posée au-dessus");

        helper.destroyBlock(lower);
        helper.succeedWhen(() -> {
            helper.assertTrue(helper.getBlockState(lower).isAir(), "partie basse cassée : air attendu");
            helper.assertTrue(helper.getBlockState(lower.above()).isAir(), "partie haute retirée avec la basse");
        });
    }

    /** Casser la partie haute retire aussi la partie basse. */
    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void scarecrowBreakingTopRemovesBottom(GameTestHelper helper) {
        BlockPos lower = new BlockPos(2, 2, 8);
        placeScarecrow(helper, lower);

        helper.destroyBlock(lower.above());
        helper.succeedWhen(() -> {
            helper.assertTrue(helper.getBlockState(lower.above()).isAir(), "partie haute cassée : air attendu");
            helper.assertTrue(helper.getBlockState(lower).isAir(), "partie basse retirée avec la haute");
        });
    }

    /**
     * Pose comme le ferait un joueur : partie basse, puis {@link Block#setPlacedBy} qui pose la partie haute et
     * enregistre le jour de pose. Le sol sous l'épouvantail est de la pierre.
     */
    private static void placeScarecrow(GameTestHelper helper, BlockPos lower) {
        helper.setBlock(lower.below(), Blocks.STONE.defaultBlockState());
        BlockState state = PestsModule.SCARECROW.get().defaultBlockState().setValue(ScarecrowBlock.HALF, DoubleBlockHalf.LOWER);
        helper.setBlock(lower, state);
        BlockPos absolute = helper.absolutePos(lower);
        state.getBlock().setPlacedBy(helper.getLevel(), absolute, helper.getLevel().getBlockState(absolute), null, ItemStack.EMPTY);
    }
}
