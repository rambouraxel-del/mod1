package fr.terresvivantes.storage.gametest;

import fr.terresvivantes.TerresVivantes;
import fr.terresvivantes.storage.CrateContents;
import fr.terresvivantes.storage.SiloBlockEntity;
import fr.terresvivantes.storage.StorageModule;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Tests de jeu du module storage (caisse et silo). Gabarit {@code orchard_arena}. */
@GameTestHolder(TerresVivantes.MODID)
@PrefixGameTestTemplate(false)
public final class StorageGameTests {

    private static final String TEMPLATE = "orchard_arena";
    private static final String BATCH = "storage";
    private static final BlockPos CRATE = new BlockPos(5, 2, 5);
    private static final BlockPos SILO_BOTTOM = new BlockPos(5, 2, 5);
    private static final BlockPos SILO_MIDDLE = new BlockPos(5, 3, 5);
    private static final BlockPos SILO_TOP = new BlockPos(5, 4, 5);
    private static final BlockPos HOPPER_ABOVE = new BlockPos(5, 5, 5);
    private static final BlockPos HOPPER_BELOW = new BlockPos(5, 1, 5);

    private StorageGameTests() {
    }

    /** Capacité d'items d'un bloc, vue par une face (null = accès complet). */
    @Nullable
    private static IItemHandler handlerAt(GameTestHelper helper, BlockPos relative, @Nullable Direction side) {
        return helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(relative), side);
    }

    /** Colonne de trois silos, du bas (contrôleur) vers le haut. */
    private static void buildSiloColumn(GameTestHelper helper) {
        helper.setBlock(SILO_BOTTOM, StorageModule.SILO.get());
        helper.setBlock(SILO_MIDDLE, StorageModule.SILO.get());
        helper.setBlock(SILO_TOP, StorageModule.SILO.get());
    }

    private static int countItem(HopperBlockEntity hopper, net.minecraft.world.item.Item item) {
        int total = 0;
        for (int slot = 0; slot < hopper.getContainerSize(); slot++) {
            ItemStack stack = hopper.getItem(slot);
            if (stack.is(item)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    /** La caisse accepte trois stacks de blé, puis refuse les carottes (autre type). */
    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void crateAcceptsThreeStacksOfWheatThenRefusesCarrots(GameTestHelper helper) {
        helper.setBlock(CRATE, StorageModule.CRATE.get());
        IItemHandler crate = handlerAt(helper, CRATE, null);
        helper.assertTrue(crate != null, "la caisse doit exposer une capacité d'items");
        for (int i = 0; i < 3; i++) {
            ItemStack rest = crate.insertItem(0, new ItemStack(Items.WHEAT, 64), false);
            helper.assertTrue(rest.isEmpty(), "une stack de blé doit entrer, reste : " + rest);
        }
        ItemStack refused = crate.insertItem(0, new ItemStack(Items.CARROT, 1), false);
        helper.assertTrue(refused.getCount() == 1, "les carottes doivent être refusées, reste : " + refused);
        helper.succeed();
    }

    /** Une caisse cassée tombe avec son contenu dans le composant de l'item. */
    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void brokenCrateDropsItsContentInTheItem(GameTestHelper helper) {
        helper.setBlock(CRATE, StorageModule.CRATE.get());
        IItemHandler crate = handlerAt(helper, CRATE, null);
        helper.assertTrue(crate != null, "la caisse doit exposer une capacité d'items");
        for (int i = 0; i < 3; i++) {
            crate.insertItem(0, new ItemStack(Items.WHEAT, 64), false);
        }
        ServerLevel level = helper.getLevel();
        BlockPos abs = helper.absolutePos(CRATE);
        level.destroyBlock(abs, true);
        helper.runAfterDelay(2, () -> {
            List<ItemEntity> drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(abs).inflate(3));
            boolean kept = drops.stream().map(ItemEntity::getItem).anyMatch(stack -> {
                CrateContents contents = stack.get(StorageModule.CRATE_CONTENTS.get());
                return stack.is(StorageModule.CRATE_ITEM.get()) && contents != null
                        && contents.item() == Items.WHEAT && contents.count() == 192;
            });
            helper.assertTrue(kept, "la caisse cassée doit lâcher un item portant 192 blés");
            helper.succeed();
        });
    }

    /** Silo de trois blocs : capacité de 48 stacks, la 49e est refusée. */
    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void siloOfThreeBlocksHoldsFortyEightStacks(GameTestHelper helper) {
        buildSiloColumn(helper);
        IItemHandler silo = handlerAt(helper, SILO_BOTTOM, null);
        helper.assertTrue(silo != null, "le silo doit exposer une capacité d'items");
        helper.assertTrue(silo.getSlots() == 48, "capacité attendue 48 stacks, obtenu " + silo.getSlots());
        for (int i = 0; i < 48; i++) {
            ItemStack rest = silo.insertItem(0, new ItemStack(Items.WHEAT, 64), false);
            helper.assertTrue(rest.isEmpty(), "la stack " + (i + 1) + " doit entrer, reste : " + rest);
        }
        ItemStack refused = silo.insertItem(0, new ItemStack(Items.WHEAT, 64), false);
        helper.assertTrue(refused.getCount() == 64, "la 49e stack doit être refusée, reste : " + refused);
        helper.succeed();
    }

    /** Une trémie posée au-dessus du silo insère par le haut dans la colonne. */
    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 200)
    public static void hopperAboveSiloColumnFillsItFromTheTop(GameTestHelper helper) {
        buildSiloColumn(helper);
        helper.setBlock(HOPPER_ABOVE, Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
        HopperBlockEntity hopper = helper.getBlockEntity(HOPPER_ABOVE);
        hopper.setItem(0, new ItemStack(Items.WHEAT, 4));
        SiloBlockEntity bottom = helper.getBlockEntity(SILO_BOTTOM);
        helper.succeedWhen(() -> helper.assertTrue(bottom.totalItems() == 4,
                "le blé de la trémie doit rejoindre le silo, total : " + bottom.totalItems()));
    }

    /** Le bas du silo extrait, le haut n'extrait pas. */
    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void siloExtractsOnlyFromItsBottom(GameTestHelper helper) {
        buildSiloColumn(helper);
        IItemHandler silo = handlerAt(helper, SILO_BOTTOM, null);
        helper.assertTrue(silo != null, "le silo doit exposer une capacité d'items");
        silo.insertItem(0, new ItemStack(Items.WHEAT, 10), false);
        IItemHandler top = handlerAt(helper, SILO_TOP, Direction.UP);
        helper.assertTrue(top != null, "le haut du silo doit exposer une capacité d'items");
        helper.assertTrue(top.extractItem(0, 1, false).isEmpty(), "le haut du silo ne doit pas extraire");
        IItemHandler bottom = handlerAt(helper, SILO_BOTTOM, Direction.DOWN);
        helper.assertTrue(bottom != null, "le bas du silo doit exposer une capacité d'items");
        ItemStack out = bottom.extractItem(0, 64, false);
        helper.assertTrue(out.is(Items.WHEAT) && out.getCount() == 10,
                "le bas doit extraire les 10 blés, obtenu : " + out);
        SiloBlockEntity controller = helper.getBlockEntity(SILO_BOTTOM);
        helper.assertTrue(controller.totalItems() == 0, "le silo doit être vide après extraction");
        helper.succeed();
    }

    /** Une trémie posée sous le silo tire le blé par le bas de la colonne. */
    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 200)
    public static void hopperBelowSiloColumnDrawsFromItsBottom(GameTestHelper helper) {
        buildSiloColumn(helper);
        IItemHandler silo = handlerAt(helper, SILO_BOTTOM, null);
        helper.assertTrue(silo != null, "le silo doit exposer une capacité d'items");
        silo.insertItem(0, new ItemStack(Items.WHEAT, 5), false);
        helper.setBlock(HOPPER_BELOW, Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
        HopperBlockEntity hopper = helper.getBlockEntity(HOPPER_BELOW);
        helper.succeedWhen(() -> helper.assertTrue(countItem(hopper, Items.WHEAT) == 5,
                "la trémie doit tirer les 5 blés du bas du silo, obtenu : " + countItem(hopper, Items.WHEAT)));
    }
}
