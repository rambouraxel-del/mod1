package fr.terresvivantes.preserving.gametest;

import fr.terresvivantes.TerresVivantes;
import fr.terresvivantes.crop.CropModule;
import fr.terresvivantes.preserving.DryingRackBlockEntity;
import fr.terresvivantes.preserving.FermentationBarrelBlockEntity;
import fr.terresvivantes.preserving.PreservingModule;
import fr.terresvivantes.preserving.SmokehouseBlockEntity;
import fr.terresvivantes.processing.MachineType;
import fr.terresvivantes.processing.ProcessingBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Tests de jeu de la conservation (v0.5). Les durées longues (jours MC) sont franchies en appelant directement
 * {@code serverTick} : c'est le même chemin que le ticker, sans attendre des milliers de ticks réels.
 */
@GameTestHolder(TerresVivantes.MODID)
@PrefixGameTestTemplate(false)
public final class PreservingGameTests {

    private static final String TEMPLATE = "orchard_arena";
    private static final String BATCH = "preserving";
    private static final BlockPos MACHINE = new BlockPos(5, 2, 5);
    /** Borne de sécurité : 2 jours MC à facteur 0.7 tiennent en environ 68 600 ticks. */
    private static final int MAX_FORCED_TICKS = 100_000;

    private PreservingGameTests() {
    }

    /** Appelle le tick serveur de la machine jusqu'à ce que la sortie soit remplie, au plus {@code maxTicks} fois. */
    private static void forceUntilOutput(ServerLevel level, BlockPos abs, ProcessingBlockEntity machine,
                                         MachineType type, int maxTicks) {
        for (int i = 0; i < maxTicks; i++) {
            if (!machine.inventory().getStackInSlot(type.outputSlot()).isEmpty()) {
                return;
            }
            machine.serverTick(level, abs, level.getBlockState(abs));
        }
    }

    /** Jambon : 600 ticks de fumaison avec un charbon dans le conteneur, qui est consommé. */
    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 200)
    public static void smokehouseSmokesPorkchopWithCharcoal(GameTestHelper helper) {
        helper.setBlock(MACHINE, PreservingModule.SMOKEHOUSE.get());
        SmokehouseBlockEntity smokehouse = helper.getBlockEntity(MACHINE);
        ServerLevel level = helper.getLevel();
        BlockPos abs = helper.absolutePos(MACHINE);
        MachineType type = PreservingModule.SMOKEHOUSE_TYPE;
        smokehouse.inventory().setStackInSlot(0, new ItemStack(Items.PORKCHOP));
        smokehouse.inventory().setStackInSlot(type.containerSlot(), new ItemStack(Items.CHARCOAL, 2));

        forceUntilOutput(level, abs, smokehouse, type, 700);

        ItemStack ham = smokehouse.inventory().getStackInSlot(type.outputSlot());
        helper.assertTrue(ham.is(PreservingModule.SMOKED_HAM.get()),
                "jambon fumé attendu après 600 ticks, obtenu " + ham);
        helper.assertTrue(smokehouse.inventory().getStackInSlot(type.containerSlot()).getCount() == 1,
                "un charbon doit être consommé par cycle");
        helper.succeed();
    }

    /** Chou x3 dans le tonneau : deux choucroutes, en 2 jours MC au plus (facteur de température compris). */
    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 200)
    public static void barrelTurnsThreeCabbagesIntoSauerkraut(GameTestHelper helper) {
        helper.setBlock(MACHINE, PreservingModule.FERMENTATION_BARREL.get());
        FermentationBarrelBlockEntity barrel = helper.getBlockEntity(MACHINE);
        ServerLevel level = helper.getLevel();
        BlockPos abs = helper.absolutePos(MACHINE);
        MachineType type = PreservingModule.BARREL_TYPE;
        barrel.inventory().setStackInSlot(0, new ItemStack(CropModule.productItem("cabbage"), 3));

        forceUntilOutput(level, abs, barrel, type, MAX_FORCED_TICKS);

        ItemStack kraut = barrel.inventory().getStackInSlot(type.outputSlot());
        helper.assertTrue(kraut.is(PreservingModule.SAUERKRAUT.get()) && kraut.getCount() == 2,
                "deux choucroutes attendues, obtenu " + kraut);
        helper.succeed();
    }

    /** Séchoir sous la pluie : aucune progression. */
    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 200)
    public static void dryingRackDoesNotProgressInRain(GameTestHelper helper) {
        helper.setBlock(MACHINE, PreservingModule.DRYING_RACK.get());
        DryingRackBlockEntity rack = helper.getBlockEntity(MACHINE);
        ServerLevel level = helper.getLevel();
        BlockPos abs = helper.absolutePos(MACHINE);
        rack.inventory().setStackInSlot(0, new ItemStack(Items.APPLE, 2));
        level.setWeatherParameters(0, 100_000, true, false);
        level.setRainLevel(1.0f);
        try {
            for (int i = 0; i < 200; i++) {
                rack.serverTick(level, abs, level.getBlockState(abs));
            }
            helper.assertTrue(rack.progress() == 0,
                    "le séchoir sous la pluie ne doit pas progresser, progression " + rack.progress());
        } finally {
            level.setWeatherParameters(6000, 0, false, false);
            level.setRainLevel(0.0f);
        }
        helper.succeed();
    }

    /** Témoin : sans pluie, le même séchoir progresse (sinon le test de pluie serait vide de sens). */
    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 200)
    public static void dryingRackProgressesWithoutRain(GameTestHelper helper) {
        helper.setBlock(MACHINE, PreservingModule.DRYING_RACK.get());
        DryingRackBlockEntity rack = helper.getBlockEntity(MACHINE);
        ServerLevel level = helper.getLevel();
        BlockPos abs = helper.absolutePos(MACHINE);
        rack.inventory().setStackInSlot(0, new ItemStack(Items.APPLE, 2));
        level.setWeatherParameters(6000, 0, false, false);
        level.setRainLevel(0.0f);
        for (int i = 0; i < 100; i++) {
            rack.serverTick(level, abs, level.getBlockState(abs));
        }
        helper.assertTrue(rack.progress() > 0,
                "le séchoir sans pluie et au ciel ouvert doit progresser, progression " + rack.progress());
        helper.succeed();
    }
}
