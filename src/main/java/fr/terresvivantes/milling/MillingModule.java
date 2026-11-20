package fr.terresvivantes.milling;

import fr.terresvivantes.TerresVivantes;
import fr.terresvivantes.core.TVModule;
import fr.terresvivantes.core.registry.TVCreativeTab;
import fr.terresvivantes.datagen.LangSink;
import fr.terresvivantes.processing.MachineType;
import fr.terresvivantes.processing.ProcessingApi;
import fr.terresvivantes.processing.ProcessingBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
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
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.BiConsumer;

/**
 * Module milling (v0.5) : la meule, machine de référence de la transformation, et la farine de blé. Le module étend
 * la transformation : pressoir, farines d'orge et de maïs viendront avec leurs recettes JSON.
 */
public final class MillingModule implements TVModule {

    /** Machine « millstone » : une entrée, pas de conteneur. */
    public static final MachineType MILLSTONE_TYPE = ProcessingApi.registerMachine("millstone", 1, false);

    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(TerresVivantes.MODID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TerresVivantes.MODID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, TerresVivantes.MODID);

    public static final DeferredBlock<MillstoneBlock> MILLSTONE = BLOCKS.register("millstone",
            () -> new MillstoneBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(2.5f)
                    .sound(SoundType.STONE)));
    public static final DeferredItem<BlockItem> MILLSTONE_ITEM = ITEMS.registerSimpleBlockItem("millstone", MILLSTONE);

    public static final DeferredItem<Item> WHEAT_FLOUR = ITEMS.registerSimpleItem("wheat_flour");

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MillstoneBlockEntity>> MILLSTONE_BE =
            BLOCK_ENTITIES.register("millstone", () -> BlockEntityType.Builder
                    .of(MillstoneBlockEntity::new, MILLSTONE.get())
                    .build(null));

    @Override
    public void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        modBus.addListener(MillingModule::registerCapabilities);

        TVCreativeTab.add(MILLSTONE_ITEM);
        TVCreativeTab.add(WHEAT_FLOUR);
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        ProcessingApi.registerItemHandler(event, MILLSTONE_BE.get());
    }

    @Override
    public void registerLang(LangSink sink) {
        sink.add("block.terresvivantes.millstone", "Millstone", "Meule");
        sink.add("item.terresvivantes.wheat_flour", "Wheat Flour", "Farine de blé");
    }

    @Override
    public void registerItemModels(ItemModelProvider provider) {
        // Modèle d'item autonome (même géométrie que la meule au repos) : ne dépend pas de l'ordre des providers.
        provider.withExistingParent("millstone", ResourceLocation.withDefaultNamespace("block/cube_bottom_top"))
                .texture("top", texture("block/millstone_top"))
                .texture("side", texture("block/millstone_side"))
                .texture("bottom", texture("block/millstone_top"));
        provider.basicItem(WHEAT_FLOUR.get());
    }

    @Override
    public void registerBlockStates(BlockStateProvider provider) {
        ModelFile idle = provider.models().cubeBottomTop("millstone",
                texture("block/millstone_side"), texture("block/millstone_top"),
                texture("block/millstone_top"));
        ModelFile active = provider.models().cubeBottomTop("millstone_active",
                texture("block/millstone_side"), texture("block/millstone_top"),
                texture("block/millstone_top_active"));
        provider.getVariantBuilder(MILLSTONE.get()).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(state.getValue(ProcessingBlock.ACTIVE) ? active : idle)
                .rotationY(rotationY(state.getValue(ProcessingBlock.FACING)))
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
    public void registerLootTables(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> output,
                                   HolderLookup.Provider lookup) {
        output.accept(ResourceKey.create(Registries.LOOT_TABLE,
                        ResourceLocation.fromNamespaceAndPath(TerresVivantes.MODID, "blocks/millstone")),
                LootTable.lootTable().withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0f))
                        .add(LootItem.lootTableItem(MILLSTONE.get()))
                        .when(ExplosionCondition.survivesExplosion())));
    }
}
