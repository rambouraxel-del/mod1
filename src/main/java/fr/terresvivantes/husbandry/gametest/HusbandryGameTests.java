package fr.terresvivantes.husbandry.gametest;

import fr.terresvivantes.TerresVivantes;
import fr.terresvivantes.husbandry.FeedingTroughBlockEntity;
import fr.terresvivantes.husbandry.HusbandryModule;
import fr.terresvivantes.husbandry.NestingBoxBlockEntity;
import fr.terresvivantes.husbandry.logic.FeedType;
import fr.terresvivantes.livestock.LivestockApi;
import fr.terresvivantes.livestock.logic.Ailment;
import fr.terresvivantes.livestock.logic.LivestockState;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Tests de jeu du module husbandry. Gabarit {@code orchard_arena} (air 11 x 12 x 11). Chaque test pose son propre
 * sol de pierre sous les blocs et les animaux.
 */
@GameTestHolder(TerresVivantes.MODID)
@PrefixGameTestTemplate(false)
public final class HusbandryGameTests {

    private static final String TEMPLATE = "orchard_arena";

    private HusbandryGameTests() {
    }

    /**
     * Auge de foin (une botte, 9 unités) à deux blocs d'une vache affamée (satiété 10) : après un cycle de besoins,
     * la satiété remonte au-dessus de 10 et le stock baisse d'une unité.
     */
    @GameTest(template = TEMPLATE, batch = "husbandry")
    public static void troughHayFeedsHungryCow(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        helper.setBlock(new BlockPos(5, 1, 3), Blocks.STONE);
        helper.setBlock(new BlockPos(5, 1, 5), Blocks.STONE);
        helper.setBlock(new BlockPos(5, 2, 3), HusbandryModule.TROUGH.get());
        FeedingTroughBlockEntity trough = troughAt(helper, new BlockPos(5, 2, 3));
        helper.assertTrue(trough.insert(FeedType.HAY, 1) == 1, "une botte de foin doit entrer dans l'auge");

        Cow cow = helper.spawn(EntityType.COW, new BlockPos(5, 2, 5));
        LivestockApi.setState(cow, new LivestockState(10, 100, Ailment.NONE, 0L));
        LivestockApi.runNeedsTick(level, cow);

        int fed = LivestockApi.getState(cow).fed();
        helper.assertTrue(fed > 10, "satiété attendue au-dessus de 10 après l'auge, obtenue " + fed);
        helper.assertTrue(trough.stock() == 8, "stock attendu 8 (une unité mangée), obtenu " + trough.stock());
        helper.succeed();
    }

    /**
     * Poule adulte dont le décompte de ponte atteint 1, à côté d'un nichoir : l'œuf entre dans le nichoir au lieu
     * de tomber au sol.
     */
    @GameTest(template = TEMPLATE, batch = "husbandry")
    public static void henLaysInNearbyNestingBox(GameTestHelper helper) {
        helper.setBlock(new BlockPos(4, 1, 5), Blocks.STONE);
        helper.setBlock(new BlockPos(5, 1, 5), Blocks.STONE);
        helper.setBlock(new BlockPos(4, 2, 5), HusbandryModule.NEST.get());
        NestingBoxBlockEntity nest = nestAt(helper, new BlockPos(4, 2, 5));

        Chicken hen = helper.spawn(EntityType.CHICKEN, new BlockPos(5, 2, 5));
        hen.eggTime = 1;

        helper.succeedWhen(() -> helper.assertTrue(nest.eggCount() > 0,
                "un œuf doit entrer dans le nichoir voisin, contenu : " + nest.eggCount()));
    }

    /**
     * Auge de foin : une botte de blé (autre type) est refusée, le stock et le type restent inchangés, et le blé
     * garde son nombre d'objets via le clic droit sur le bloc.
     */
    @GameTest(template = TEMPLATE, batch = "husbandry")
    public static void troughRefusesMixedFeed(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        helper.setBlock(new BlockPos(5, 1, 5), Blocks.STONE);
        helper.setBlock(new BlockPos(5, 2, 5), HusbandryModule.TROUGH.get());
        FeedingTroughBlockEntity trough = troughAt(helper, new BlockPos(5, 2, 5));
        helper.assertTrue(trough.insert(FeedType.HAY, 1) == 1, "une botte de foin doit entrer dans l'auge");

        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack wheat = new ItemStack(Items.WHEAT, 4);
        BlockPos absolute = helper.absolutePos(new BlockPos(5, 2, 5));
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absolute), net.minecraft.core.Direction.UP,
                absolute, false);
        level.getBlockState(absolute).useItemOn(wheat, level, player, InteractionHand.MAIN_HAND, hit);

        helper.assertTrue(wheat.getCount() == 4, "le blé ne doit pas être consommé, reste " + wheat.getCount());
        helper.assertTrue(trough.type() == FeedType.HAY, "le type doit rester foin, obtenu " + trough.type());
        helper.assertTrue(trough.stock() == 9, "le stock doit rester 9, obtenu " + trough.stock());
        helper.succeed();
    }

    private static FeedingTroughBlockEntity troughAt(GameTestHelper helper, BlockPos pos) {
        if (helper.getBlockEntity(pos) instanceof FeedingTroughBlockEntity trough) {
            return trough;
        }
        throw new IllegalStateException("auge absente en " + pos);
    }

    private static NestingBoxBlockEntity nestAt(GameTestHelper helper, BlockPos pos) {
        if (helper.getBlockEntity(pos) instanceof NestingBoxBlockEntity nest) {
            return nest;
        }
        throw new IllegalStateException("nichoir absent en " + pos);
    }
}
