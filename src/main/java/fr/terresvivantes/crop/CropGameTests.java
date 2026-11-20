package fr.terresvivantes.crop;

import fr.terresvivantes.TerresVivantes;
import fr.terresvivantes.crop.logic.CropLogic;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/** Tests de jeu des cultures : récolte à maturité, pas de récolte avant maturité. */
@GameTestHolder(TerresVivantes.MODID)
@PrefixGameTestTemplate(false)
public final class CropGameTests {

    private static final BlockPos SOIL = new BlockPos(1, 1, 1);
    private static final BlockPos CROP = SOIL.above();

    private CropGameTests() {
    }

    @GameTest(template = "crop_arena")
    public static void matureTomatoIsHarvestedAndReplanted(GameTestHelper helper) {
        helper.setBlock(SOIL, Blocks.FARMLAND.defaultBlockState());
        helper.setBlock(CROP, CropModule.cropBlock("tomato").defaultBlockState()
                .setValue(CropBlock.AGE, CropLogic.MAX_AGE));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);

        List<ItemStack> drops = CropApi.harvestAndReplant(helper.getLevel(), helper.absolutePos(CROP), player);

        int tomatoes = drops.stream()
                .filter(stack -> stack.is(CropModule.productItem("tomato")))
                .mapToInt(ItemStack::getCount)
                .sum();
        helper.assertTrue(tomatoes >= 2, "au moins 2 tomates attendues, obtenu " + tomatoes);
        helper.assertTrue(helper.getBlockState(CROP).getValue(CropBlock.AGE) == 0,
                "la culture doit être replantée à l'âge 0");
        helper.succeed();
    }

    @GameTest(template = "crop_arena")
    public static void immatureCropIsNotHarvested(GameTestHelper helper) {
        helper.setBlock(SOIL, Blocks.FARMLAND.defaultBlockState());
        helper.setBlock(CROP, CropModule.cropBlock("lettuce").defaultBlockState()
                .setValue(CropBlock.AGE, 3));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);

        List<ItemStack> drops = CropApi.harvestAndReplant(helper.getLevel(), helper.absolutePos(CROP), player);

        helper.assertTrue(drops.isEmpty(), "aucun drop attendu avant maturité");
        helper.assertTrue(helper.getBlockState(CROP).getValue(CropBlock.AGE) == 3,
                "la culture immature ne doit pas changer d'âge");
        helper.succeed();
    }
}
