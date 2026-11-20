package fr.terresvivantes.insects.gametest;

import fr.terresvivantes.TerresVivantes;
import fr.terresvivantes.health.HealthApi;
import fr.terresvivantes.health.logic.Disease;
import fr.terresvivantes.insects.InsectsModule;
import fr.terresvivantes.insects.entity.Butterfly;
import fr.terresvivantes.pollination.PollinationApi;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Tests de jeu des insectes. Gabarit orchard_arena (volume d'air de 11 x 12 x 11). */
@GameTestHolder(TerresVivantes.MODID)
@PrefixGameTestTemplate(false)
public final class InsectsGameTests {

    private static final String TEMPLATE = "orchard_arena";
    private static final BlockPos CENTER = new BlockPos(5, 2, 5);
    private static final float DELTA = 1e-4f;
    /** Délai maximal pour qu'une coccinelle nettoie une culture infestée. */
    private static final int CURE_TIMEOUT_TICKS = 400;

    private InsectsGameTests() {
    }

    /** Un papillon posé près de fleurs ajoute 0.05 au score de pollinisation. */
    @GameTest(template = TEMPLATE)
    public static void butterflyNearFlowersRaisesPollination(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos center = helper.absolutePos(CENTER);
        helper.setBlock(new BlockPos(3, 2, 5), Blocks.DANDELION);
        helper.setBlock(new BlockPos(7, 2, 5), Blocks.POPPY);
        float before = PollinationApi.scoreUncached(level, center);

        Butterfly butterfly = helper.spawn(InsectsModule.BUTTERFLY.get(), CENTER);
        helper.assertTrue(butterfly != null, "le papillon doit être posé");
        float after = PollinationApi.scoreUncached(level, center);
        helper.assertTrue(after > before + DELTA,
                "un papillon proche doit augmenter le score : " + before + " -> " + after);
        helper.succeed();
    }

    /**
     * Une culture infestée, avec quatre coccinelles autour, perd ses pucerons dans le délai. Les coccinelles marchent
     * sur un socle de terre labourée 5 x 5 (niveau 1), à la hauteur de la culture.
     */
    @GameTest(template = TEMPLATE, timeoutTicks = CURE_TIMEOUT_TICKS)
    public static void ladybugsCureAphidsWithinFourHundredTicks(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                helper.setBlock(new BlockPos(CENTER.getX() + dx, 1, CENTER.getZ() + dz), Blocks.FARMLAND);
            }
        }
        helper.setBlock(CENTER, Blocks.WHEAT);
        BlockPos crop = helper.absolutePos(CENTER);
        helper.assertTrue(HealthApi.infest(level, crop), "la culture doit recevoir les pucerons");

        // Quatre coccinelles: chacune tente 15 % toutes les 40 ticks, la chance d'échec total est très faible.
        helper.spawn(InsectsModule.LADYBUG.get(), new BlockPos(4, 2, 5));
        helper.spawn(InsectsModule.LADYBUG.get(), new BlockPos(6, 2, 5));
        helper.spawn(InsectsModule.LADYBUG.get(), new BlockPos(5, 2, 4));
        helper.spawn(InsectsModule.LADYBUG.get(), new BlockPos(5, 2, 6));

        helper.succeedWhen(() -> helper.assertTrue(HealthApi.diseaseOf(level, crop) != Disease.APHIDS,
                "les coccinelles doivent nettoyer la culture dans les " + CURE_TIMEOUT_TICKS + " ticks"));
    }

    /** Filet et bouteille en verre sur un papillon : le papillon part dans un bocal, la bouteille est consommée. */
    @GameTest(template = TEMPLATE)
    public static void netAndBottleCaptureButterflyIntoJar(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack net = new ItemStack(InsectsModule.BUTTERFLY_NET.get());
        // Le filet occupe d'abord la main (slot sélectionné), puis la bouteille va dans un autre slot.
        player.setItemInHand(InteractionHand.MAIN_HAND, net);
        player.getInventory().setItem(1, new ItemStack(Items.GLASS_BOTTLE));

        Butterfly butterfly = helper.spawn(InsectsModule.BUTTERFLY.get(), CENTER);
        helper.assertTrue(butterfly != null, "le papillon doit être posé");
        InteractionResult result = InsectsModule.BUTTERFLY_NET.get()
                .interactLivingEntity(net, player, butterfly, InteractionHand.MAIN_HAND);

        helper.assertTrue(result.consumesAction(), "le filet doit capturer le papillon");
        helper.assertTrue(butterfly.isRemoved(), "le papillon capturé doit disparaître du monde");
        helper.assertTrue(count(player.getInventory(), InsectsModule.BUTTERFLY_JAR.get()) == 1,
                "un bocal à papillon doit être dans l'inventaire");
        helper.assertTrue(count(player.getInventory(), Items.GLASS_BOTTLE) == 0,
                "la bouteille en verre doit être consommée");
        helper.succeed();
    }

    private static int count(Inventory inventory, Item item) {
        int total = 0;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.is(item)) {
                total += stack.getCount();
            }
        }
        return total;
    }
}
