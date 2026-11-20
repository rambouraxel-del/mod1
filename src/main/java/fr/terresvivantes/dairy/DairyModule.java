package fr.terresvivantes.dairy;

import com.mojang.serialization.Codec;
import fr.terresvivantes.TerresVivantes;
import fr.terresvivantes.core.TVModule;
import fr.terresvivantes.core.registry.TVCreativeTab;
import fr.terresvivantes.dairy.item.QualityFoodItem;
import fr.terresvivantes.datagen.LangSink;
import fr.terresvivantes.processing.MachineType;
import fr.terresvivantes.processing.ProcessingApi;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
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
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import fr.terresvivantes.dairy.block.AgingShelfBlock;
import fr.terresvivantes.dairy.block.AgingShelfBlockEntity;
import fr.terresvivantes.dairy.block.ChurnBlock;
import fr.terresvivantes.dairy.block.ChurnBlockEntity;
import fr.terresvivantes.dairy.block.CheeseVatBlock;
import fr.terresvivantes.dairy.block.CheeseVatBlockEntity;
import fr.terresvivantes.processing.ProcessingBlock;

import java.util.function.BiConsumer;

/**
 * Module dairy (v0.5) : laiterie et fromagerie. Trois machines de transformation : la baratte (lait vers crème, crème
 * vers beurre), la cuve à fromage (lait vers caillé, lait de chèvre vers fromage de chèvre) et l'étagère d'affinage
 * (caillé vers fromage jeune, puis fromage affiné). Le composant {@code terresvivantes:milk_source} du seau de lait
 * indique son espèce (vache ou chèvre); il est posé par la traite du module duck.
 */
public final class DairyModule implements TVModule {

    /** Baratte : une entrée, pas de conteneur. */
    public static final MachineType CHURN_TYPE = ProcessingApi.registerMachine("churn", 1, false);
    /** Cuve à fromage : une entrée, pas de conteneur. */
    public static final MachineType CHEESE_VAT_TYPE = ProcessingApi.registerMachine("cheese_vat", 1, false);
    /** Étagère d'affinage : une entrée, pas de conteneur. */
    public static final MachineType AGING_SHELF_TYPE = ProcessingApi.registerMachine("aging_shelf", 1, false);

    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(TerresVivantes.MODID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TerresVivantes.MODID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, TerresVivantes.MODID);
    private static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, TerresVivantes.MODID);

    /** Espèce du lait : {@link fr.terresvivantes.dairy.logic.MilkSource#COW} ou {@code GOAT}. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> MILK_SOURCE =
            COMPONENTS.registerComponentType("milk_source", builder -> builder
                    .persistent(Codec.STRING)
                    .networkSynchronized(ByteBufCodecs.STRING_UTF8));

    public static final DeferredBlock<ChurnBlock> CHURN = BLOCKS.register("churn",
            () -> new ChurnBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD).strength(2.5f).sound(SoundType.WOOD)));
    public static final DeferredItem<BlockItem> CHURN_ITEM = ITEMS.registerSimpleBlockItem("churn", CHURN);

    public static final DeferredBlock<CheeseVatBlock> CHEESE_VAT = BLOCKS.register("cheese_vat",
            () -> new CheeseVatBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL).strength(3.0f).sound(SoundType.METAL)));
    public static final DeferredItem<BlockItem> CHEESE_VAT_ITEM = ITEMS.registerSimpleBlockItem("cheese_vat", CHEESE_VAT);

    public static final DeferredBlock<AgingShelfBlock> AGING_SHELF = BLOCKS.register("aging_shelf",
            () -> new AgingShelfBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD).strength(2.5f).sound(SoundType.WOOD)));
    public static final DeferredItem<BlockItem> AGING_SHELF_ITEM = ITEMS.registerSimpleBlockItem("aging_shelf", AGING_SHELF);

    public static final DeferredItem<Item> BUTTER = ITEMS.registerSimpleItem("butter");
    public static final DeferredItem<Item> CREAM = ITEMS.registerSimpleItem("cream");
    public static final DeferredItem<Item> CHEESE_CURD = ITEMS.registerSimpleItem("cheese_curd");
    public static final DeferredItem<Item> YOUNG_CHEESE = ITEMS.register("young_cheese",
            () -> new Item(new Item.Properties().food(medium())));
    /** Fromage affiné : sa saturation croît avec sa qualité (voir {@link QualityFoodItem}). */
    public static final DeferredItem<QualityFoodItem> AGED_CHEESE = ITEMS.register("aged_cheese",
            () -> new QualityFoodItem(new Item.Properties().food(rich())));
    public static final DeferredItem<Item> GOAT_CHEESE = ITEMS.register("goat_cheese",
            () -> new Item(new Item.Properties().food(medium())));
    public static final DeferredItem<Item> CHEESE_SLICE = ITEMS.register("cheese_slice",
            () -> new Item(new Item.Properties().food(light())));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ChurnBlockEntity>> CHURN_BE =
            BLOCK_ENTITIES.register("churn", () -> BlockEntityType.Builder
                    .of(ChurnBlockEntity::new, CHURN.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CheeseVatBlockEntity>> CHEESE_VAT_BE =
            BLOCK_ENTITIES.register("cheese_vat", () -> BlockEntityType.Builder
                    .of(CheeseVatBlockEntity::new, CHEESE_VAT.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AgingShelfBlockEntity>> AGING_SHELF_BE =
            BLOCK_ENTITIES.register("aging_shelf", () -> BlockEntityType.Builder
                    .of(AgingShelfBlockEntity::new, AGING_SHELF.get()).build(null));

    @Override
    public void register(IEventBus modBus) {
        COMPONENTS.register(modBus);
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        modBus.addListener(DairyModule::registerCapabilities);

        TVCreativeTab.add(CHURN_ITEM);
        TVCreativeTab.add(CHEESE_VAT_ITEM);
        TVCreativeTab.add(AGING_SHELF_ITEM);
        TVCreativeTab.add(BUTTER);
        TVCreativeTab.add(CREAM);
        TVCreativeTab.add(CHEESE_CURD);
        TVCreativeTab.add(YOUNG_CHEESE);
        TVCreativeTab.add(AGED_CHEESE);
        TVCreativeTab.add(GOAT_CHEESE);
        TVCreativeTab.add(CHEESE_SLICE);
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        ProcessingApi.registerItemHandler(event, CHURN_BE.get());
        ProcessingApi.registerItemHandler(event, CHEESE_VAT_BE.get());
        ProcessingApi.registerItemHandler(event, AGING_SHELF_BE.get());
    }

    private static FoodProperties light() {
        return new FoodProperties.Builder().nutrition(2).saturationModifier(0.2F).build();
    }

    private static FoodProperties medium() {
        return new FoodProperties.Builder().nutrition(4).saturationModifier(0.3F).build();
    }

    private static FoodProperties rich() {
        return new FoodProperties.Builder().nutrition(8).saturationModifier(0.6F).build();
    }

    @Override
    public void registerLang(LangSink sink) {
        sink.add("block.terresvivantes.churn", "Churn", "Baratte");
        sink.add("block.terresvivantes.cheese_vat", "Cheese Vat", "Cuve à fromage");
        sink.add("block.terresvivantes.aging_shelf", "Aging Shelf", "Étagère d'affinage");
        sink.add("item.terresvivantes.butter", "Butter", "Beurre");
        sink.add("item.terresvivantes.cream", "Cream", "Crème");
        sink.add("item.terresvivantes.cheese_curd", "Cheese Curd", "Caillé");
        sink.add("item.terresvivantes.young_cheese", "Young Cheese", "Fromage jeune");
        sink.add("item.terresvivantes.aged_cheese", "Aged Cheese", "Fromage affiné");
        sink.add("item.terresvivantes.goat_cheese", "Goat Cheese", "Fromage de chèvre");
        sink.add("item.terresvivantes.cheese_slice", "Cheese Slice", "Tranche de fromage");
    }

    @Override
    public void registerItemModels(ItemModelProvider provider) {
        // Modèles d'item autonomes (même géométrie que les blocs), sans dépendre de l'ordre des providers.
        ResourceLocation cube = ResourceLocation.withDefaultNamespace("block/cube_bottom_top");
        provider.withExistingParent("churn", cube)
                .texture("side", texture("block/churn_side"))
                .texture("top", texture("block/churn_top"))
                .texture("bottom", texture("block/churn_top"));
        provider.withExistingParent("cheese_vat", cube)
                .texture("side", texture("block/cheese_vat_side"))
                .texture("top", texture("block/cheese_vat_top"))
                .texture("bottom", texture("block/cheese_vat_top"));
        provider.withExistingParent("aging_shelf", ResourceLocation.withDefaultNamespace("block/orientable"))
                .texture("side", texture("block/aging_shelf_side"))
                .texture("front", texture("block/aging_shelf_front"))
                .texture("top", texture("block/aging_shelf_side"));
        provider.basicItem(BUTTER.get());
        provider.basicItem(CREAM.get());
        provider.basicItem(CHEESE_CURD.get());
        provider.basicItem(YOUNG_CHEESE.get());
        provider.basicItem(AGED_CHEESE.get());
        provider.basicItem(GOAT_CHEESE.get());
        provider.basicItem(CHEESE_SLICE.get());
    }

    @Override
    public void registerBlockStates(BlockStateProvider provider) {
        ModelFile churn = provider.models().cubeBottomTop("churn",
                texture("block/churn_side"), texture("block/churn_top"), texture("block/churn_top"));
        // La baratte n'a pas de texture « active » : l'état ACTIVE joue le son et les particules côté BlockEntity.
        provider.getVariantBuilder(CHURN.get()).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(churn)
                .rotationY(rotationY(state.getValue(ProcessingBlock.FACING)))
                .build());

        ModelFile vatEmpty = provider.models().cubeBottomTop("cheese_vat",
                texture("block/cheese_vat_side"), texture("block/cheese_vat_top"), texture("block/cheese_vat_top"));
        ModelFile vatMilk = provider.models().cubeBottomTop("cheese_vat_milk",
                texture("block/cheese_vat_side"), texture("block/cheese_vat_top_milk"), texture("block/cheese_vat_top"));
        ModelFile vatCurd = provider.models().cubeBottomTop("cheese_vat_curd",
                texture("block/cheese_vat_side"), texture("block/cheese_vat_top_curd"), texture("block/cheese_vat_top"));
        provider.getVariantBuilder(CHEESE_VAT.get()).forAllStates(state -> {
            ModelFile model = switch (state.getValue(CheeseVatBlock.CONTENT)) {
                case EMPTY -> vatEmpty;
                case MILK -> vatMilk;
                case CURD -> vatCurd;
            };
            return ConfiguredModel.builder().modelFile(model)
                    .rotationY(rotationY(state.getValue(ProcessingBlock.FACING)))
                    .build();
        });

        ModelFile shelf = provider.models().orientable("aging_shelf",
                texture("block/aging_shelf_side"), texture("block/aging_shelf_front"), texture("block/aging_shelf_side"));
        ModelFile shelfCheese = provider.models().orientable("aging_shelf_cheese",
                texture("block/aging_shelf_side"), texture("block/aging_shelf_front_cheese"), texture("block/aging_shelf_side"));
        provider.getVariantBuilder(AGING_SHELF.get()).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(state.getValue(AgingShelfBlock.HAS_CHEESE) ? shelfCheese : shelf)
                .rotationY(rotationY(state.getValue(ProcessingBlock.FACING)))
                .build());
    }

    private static ResourceLocation texture(String path) {
        return ResourceLocation.fromNamespaceAndPath(TerresVivantes.MODID, path);
    }

    private static int rotationY(net.minecraft.core.Direction facing) {
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
        lootTable(output, CHURN.get(), "churn");
        lootTable(output, CHEESE_VAT.get(), "cheese_vat");
        lootTable(output, AGING_SHELF.get(), "aging_shelf");
    }

    private static void lootTable(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> output,
                                  net.minecraft.world.level.block.Block block, String name) {
        output.accept(ResourceKey.create(Registries.LOOT_TABLE,
                        ResourceLocation.fromNamespaceAndPath(TerresVivantes.MODID, "blocks/" + name)),
                LootTable.lootTable().withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0f))
                        .add(LootItem.lootTableItem(block))
                        .when(ExplosionCondition.survivesExplosion())));
    }
}
