package fr.terresvivantes.automation;

import fr.terresvivantes.TerresVivantes;
import fr.terresvivantes.core.TVModule;
import fr.terresvivantes.core.registry.TVCreativeTab;
import fr.terresvivantes.datagen.LangSink;
import fr.terresvivantes.tools.ToolsModule;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.ExplosionCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.BiConsumer;

/**
 * Module automation (v0.6) : la récolteuse (récolte les cultures mûres devant elle) et la planteuse (plante les graines
 * de son inventaire). Les cycles sont cadencés par {@link AutomationConfig}; rien ne tourne si le module est désactivé.
 */
public final class AutomationModule implements TVModule {

    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(TerresVivantes.MODID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TerresVivantes.MODID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, TerresVivantes.MODID);

    public static final DeferredBlock<HarvesterBlock> HARVESTER = BLOCKS.register("harvester",
            () -> new HarvesterBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(2.5f)
                    .sound(SoundType.METAL)));
    public static final DeferredItem<BlockItem> HARVESTER_ITEM = ITEMS.registerSimpleBlockItem("harvester", HARVESTER);

    public static final DeferredBlock<PlanterBlock> PLANTER = BLOCKS.register("planter",
            () -> new PlanterBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(2.5f)
                    .sound(SoundType.WOOD)));
    public static final DeferredItem<BlockItem> PLANTER_ITEM = ITEMS.registerSimpleBlockItem("planter", PLANTER);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HarvesterBlockEntity>> HARVESTER_BE =
            BLOCK_ENTITIES.register("harvester", () -> BlockEntityType.Builder
                    .of(HarvesterBlockEntity::new, HARVESTER.get())
                    .build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PlanterBlockEntity>> PLANTER_BE =
            BLOCK_ENTITIES.register("planter", () -> BlockEntityType.Builder
                    .of(PlanterBlockEntity::new, PLANTER.get())
                    .build(null));

    @Override
    public void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        modBus.addListener(AutomationModule::registerCapabilities);

        TVCreativeTab.add(HARVESTER_ITEM);
        TVCreativeTab.add(PLANTER_ITEM);
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, HARVESTER_BE.get(),
                (machine, side) -> machine.outputHandler());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, PLANTER_BE.get(),
                (machine, side) -> machine.insertHandler());
    }

    @Override
    public void registerLang(LangSink sink) {
        sink.add("block.terresvivantes.harvester", "Harvester", "Récolteuse");
        sink.add("block.terresvivantes.planter", "Planter", "Planteuse");
    }

    @Override
    public void registerItemModels(ItemModelProvider provider) {
        // Modèles d'item autonomes (même géométrie que le bloc au repos).
        provider.withExistingParent("harvester", ResourceLocation.withDefaultNamespace("block/orientable"))
                .texture("front", texture("block/harvester_front"))
                .texture("side", texture("block/harvester_side"))
                .texture("top", texture("block/harvester_top"));
        provider.withExistingParent("planter", ResourceLocation.withDefaultNamespace("block/orientable"))
                .texture("front", texture("block/planter_front"))
                .texture("side", texture("block/planter_side"))
                .texture("top", texture("block/planter_top"));
    }

    @Override
    public void registerBlockStates(BlockStateProvider provider) {
        ModelFile harvesterIdle = provider.models().orientable("harvester",
                texture("block/harvester_side"), texture("block/harvester_front"), texture("block/harvester_top"));
        ModelFile harvesterActive = provider.models().orientable("harvester_active",
                texture("block/harvester_side"), texture("block/harvester_front_active"), texture("block/harvester_top"));
        provider.getVariantBuilder(HARVESTER.get()).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(state.getValue(AutomationBlock.ACTIVE) ? harvesterActive : harvesterIdle)
                .rotationY(rotationY(state.getValue(HorizontalDirectionalBlock.FACING)))
                .build());

        // La planteuse n'a pas de texture « active » : le visuel passe par le cycle, sans modèle distinct.
        ModelFile planter = provider.models().orientable("planter",
                texture("block/planter_side"), texture("block/planter_front"), texture("block/planter_top"));
        provider.getVariantBuilder(PLANTER.get()).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(planter)
                .rotationY(rotationY(state.getValue(HorizontalDirectionalBlock.FACING)))
                .build());
    }

    private static ResourceLocation texture(String path) {
        return ResourceLocation.fromNamespaceAndPath(TerresVivantes.MODID, path);
    }

    private static int rotationY(Direction facing) {
        return switch (facing) {
            case EAST -> 90;
            case SOUTH -> 180;
            case WEST -> 270;
            default -> 0;
        };
    }

    @Override
    public void registerRecipes(RecipeOutput output, HolderLookup.Provider lookup) {
        // Récolteuse : 4 fers, une faucille, un piston et de la redstone.
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, HARVESTER_ITEM.get())
                .pattern("ISI")
                .pattern("IRI")
                .pattern(" P ")
                .define('I', Items.IRON_INGOT)
                .define('S', ToolsModule.SICKLE.get())
                .define('R', Items.REDSTONE)
                .define('P', Items.PISTON)
                .unlockedBy("has_sickle", InventoryChangeTrigger.TriggerInstance
                        .hasItems(ToolsModule.SICKLE.get()))
                .save(output);
        // Planteuse : 4 fers, un distributeur et un coffre.
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, PLANTER_ITEM.get())
                .pattern("IDI")
                .pattern("ICI")
                .define('I', Items.IRON_INGOT)
                .define('D', Blocks.DISPENSER)
                .define('C', Blocks.CHEST)
                .unlockedBy("has_iron", InventoryChangeTrigger.TriggerInstance
                        .hasItems(Items.IRON_INGOT))
                .save(output);
    }

    @Override
    public void registerLootTables(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> output,
                                   HolderLookup.Provider lookup) {
        output.accept(lootKey("harvester"), LootTable.lootTable().withPool(LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1.0f))
                .add(LootItem.lootTableItem(HARVESTER.get()))
                .when(ExplosionCondition.survivesExplosion())));
        output.accept(lootKey("planter"), LootTable.lootTable().withPool(LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1.0f))
                .add(LootItem.lootTableItem(PLANTER.get()))
                .when(ExplosionCondition.survivesExplosion())));
    }

    private static ResourceKey<LootTable> lootKey(String block) {
        return ResourceKey.create(Registries.LOOT_TABLE,
                ResourceLocation.fromNamespaceAndPath(TerresVivantes.MODID, "blocks/" + block));
    }
}
