package fr.terresvivantes.vet.gametest;

import fr.terresvivantes.TerresVivantes;
import fr.terresvivantes.livestock.LivestockApi;
import fr.terresvivantes.livestock.LivestockModule;
import fr.terresvivantes.livestock.logic.Ailment;
import fr.terresvivantes.livestock.logic.Genome;
import fr.terresvivantes.livestock.logic.LivestockState;
import fr.terresvivantes.vet.VetApi;
import fr.terresvivantes.vet.VetModule;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Tests de jeu du module vet. Gabarit {@code orchard_arena} (air 11 x 12 x 11). Les cycles de besoins sont appelés
 * directement, sans attendre 1200 ticks.
 */
@GameTestHolder(TerresVivantes.MODID)
@PrefixGameTestTemplate(false)
public final class VetGameTests {

    private static final String TEMPLATE = "orchard_arena";

    private VetGameTests() {
    }

    /**
     * Dix vaches serrées et affamées, cinq cycles forcés : au moins une tombe malade. Les tirages sont une suite
     * fixe (graine 42) pour que le test soit déterministe. Batch propre : la source de tirages est globale.
     */
    @GameTest(template = TEMPLATE, batch = "vet_sick")
    public static void crowdedStarvingCowsGetSick(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        for (int x = 1; x <= 9; x++) {
            for (int z = 1; z <= 9; z++) {
                helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
            }
        }
        Random seeded = new Random(42);
        VetApi.setRandomForTest(seeded::nextDouble);
        try {
            List<Cow> cows = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                Cow cow = helper.spawn(EntityType.COW, new BlockPos(2 + i % 5, 2, 2 + (i / 5) * 2));
                cow.setData(LivestockModule.GENOME.get(), new Genome(5, 5, 5, 5));
                LivestockApi.setState(cow, new LivestockState(0, 100, Ailment.NONE, 0L));
                cows.add(cow);
            }
            for (int cycle = 0; cycle < 5; cycle++) {
                for (Cow cow : cows) {
                    LivestockApi.runNeedsTick(level, cow);
                }
            }
            long sick = cows.stream().filter(cow -> LivestockApi.getState(cow).ailment() == Ailment.SICK).count();
            helper.assertTrue(sick >= 1, "au moins une vache doit être malade, obtenu " + sick);
        } finally {
            VetApi.resetRandom();
        }
        helper.succeed();
    }

    /** Trousse vétérinaire sur un animal malade : état NONE, santé 50 + 20, une utilisation consommée. */
    @GameTest(template = TEMPLATE, batch = "vet")
    public static void veterinaryKitHealsSickAnimal(GameTestHelper helper) {
        Sheep sheep = helper.spawn(EntityType.SHEEP, new BlockPos(5, 2, 5));
        LivestockApi.setState(sheep, new LivestockState(30, 50, Ailment.SICK, 0L));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack kit = new ItemStack(VetModule.VETERINARY_KIT.get());

        InteractionResult result = VetModule.VETERINARY_KIT.get()
                .interactLivingEntity(kit, player, sheep, InteractionHand.MAIN_HAND);

        helper.assertTrue(result.consumesAction(), "la trousse doit agir sur un animal malade");
        LivestockState state = LivestockApi.getState(sheep);
        helper.assertTrue(state.ailment() == Ailment.NONE, "l'état sanitaire doit revenir à NONE");
        helper.assertTrue(state.health() == 70, "santé attendue 70, obtenue " + state.health());
        helper.assertTrue(kit.getDamageValue() == 1, "une utilisation doit être consommée");
        helper.succeed();
    }

    /** Santé à 0 : l'animal meurt (mort à zéro activée par défaut). Satiété 0 : une perte de 1 par cycle ramène 1 à 0. */
    @GameTest(template = TEMPLATE, batch = "vet")
    public static void zeroHealthKillsAnimal(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        helper.setBlock(new BlockPos(5, 1, 5), Blocks.STONE);
        Sheep sheep = helper.spawn(EntityType.SHEEP, new BlockPos(5, 2, 5));
        sheep.setData(LivestockModule.GENOME.get(), new Genome(5, 5, 5, 5));
        LivestockApi.setState(sheep, new LivestockState(0, 1, Ailment.NONE, 0L));

        LivestockApi.runNeedsTick(level, sheep);

        helper.assertTrue(!sheep.isAlive(), "un animal à santé 0 doit mourir");
        helper.succeed();
    }
}
