package fr.terresvivantes.preserving;

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
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
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
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.model.generators.BlockModelBuilder;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Module preserving (v0.5) : tonneau de fermentation (cidre, vinaigre, choucroute, cornichons), séchoir (fruits secs,
 * viande séchée) et fumoir (jambon, poisson et viande fumés). Les recettes sont des JSON {@code preserving_*} dans
 * {@code data/terresvivantes/recipe/}.
 */
public final class PreservingModule implements TVModule {

    /** Tonneau : 2 entrées et un conteneur (bouteille en verre). */
    public static final MachineType BARREL_TYPE = ProcessingApi.registerMachine("fermentation_barrel", 2, true);
    /** Séchoir : 1 entrée, pas de conteneur. */
    public static final MachineType DRYING_RACK_TYPE = ProcessingApi.registerMachine("drying_rack", 1, false);
    /** Fumoir : 1 entrée et un conteneur combustible. */
    public static final MachineType SMOKEHOUSE_TYPE = ProcessingApi.registerMachine("smokehouse", 1, true);

    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(TerresVivantes.MODID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TerresVivantes.MODID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, TerresVivantes.MODID);

    public static final DeferredBlock<FermentationBarrelBlock> FERMENTATION_BARREL = BLOCKS.register("fermentation_barrel",
            () -> new FermentationBarrelBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(2.5f)
                    .sound(SoundType.WOOD)));
    public static final DeferredItem<BlockItem> FERMENTATION_BARREL_ITEM =
            ITEMS.registerSimpleBlockItem("fermentation_barrel", FERMENTATION_BARREL);

    public static final DeferredBlock<DryingRackBlock> DRYING_RACK = BLOCKS.register("drying_rack",
            () -> new DryingRackBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(1.5f)
                    .sound(SoundType.WOOD)
                    .noOcclusion()));
    public static final DeferredItem<BlockItem> DRYING_RACK_ITEM =
            ITEMS.registerSimpleBlockItem("drying_rack", DRYING_RACK);

    public static final DeferredBlock<SmokehouseBlock> SMOKEHOUSE = BLOCKS.register("smokehouse",
            () -> new SmokehouseBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(2.5f)
                    .sound(SoundType.STONE)));
    public static final DeferredItem<BlockItem> SMOKEHOUSE_ITEM = ITEMS.registerSimpleBlockItem("smokehouse", SMOKEHOUSE);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FermentationBarrelBlockEntity>>
            FERMENTATION_BARREL_BE = BLOCK_ENTITIES.register("fermentation_barrel", () -> BlockEntityType.Builder
                    .of(FermentationBarrelBlockEntity::new, FERMENTATION_BARREL.get())
                    .build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DryingRackBlockEntity>>
            DRYING_RACK_BE = BLOCK_ENTITIES.register("drying_rack", () -> BlockEntityType.Builder
                    .of(DryingRackBlockEntity::new, DRYING_RACK.get())
                    .build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SmokehouseBlockEntity>>
            SMOKEHOUSE_BE = BLOCK_ENTITIES.register("smokehouse", () -> BlockEntityType.Builder
                    .of(SmokehouseBlockEntity::new, SMOKEHOUSE.get())
                    .build(null));

    public static final DeferredItem<Item> CIDER = ITEMS.register("cider", () -> new Item(new Item.Properties()
            .food(new FoodProperties.Builder()
                    .nutrition(3)
                    .saturationModifier(0.5f)
                    .effect(new MobEffectInstance(MobEffects.ABSORPTION, 300, 0), 1.0f)
                    .build())
            .craftRemainder(Items.GLASS_BOTTLE)));
    public static final DeferredItem<Item> VINEGAR = ITEMS.registerSimpleItem("vinegar");
    public static final DeferredItem<Item> SAUERKRAUT = ITEMS.register("sauerkraut", () -> new Item(new Item.Properties()
            .food(new FoodProperties.Builder().nutrition(6).saturationModifier(0.9f).build())));
    public static final DeferredItem<Item> PICKLES = ITEMS.register("pickles", () -> new Item(new Item.Properties()
            .food(new FoodProperties.Builder().nutrition(4).saturationModifier(0.7f).build())
            .craftRemainder(Items.GLASS_BOTTLE)));
    public static final DeferredItem<Item> DRIED_FRUIT = ITEMS.register("dried_fruit", () -> new Item(new Item.Properties()
            .food(new FoodProperties.Builder().nutrition(4).saturationModifier(0.5f).fast().build())));
    public static final DeferredItem<Item> JERKY = ITEMS.register("jerky", () -> new Item(new Item.Properties()
            .food(new FoodProperties.Builder().nutrition(6).saturationModifier(0.9f).build())));
    public static final DeferredItem<Item> SMOKED_HAM = ITEMS.register("smoked_ham", () -> new Item(new Item.Properties()
            .food(new FoodProperties.Builder().nutrition(10).saturationModifier(1.0f).build())));
    public static final DeferredItem<Item> SMOKED_FISH = ITEMS.register("smoked_fish", () -> new Item(new Item.Properties()
            .food(new FoodProperties.Builder().nutrition(7).saturationModifier(0.9f).build())));

    @Override
    public void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        modBus.addListener(PreservingModule::registerCapabilities);

        TVCreativeTab.add(FERMENTATION_BARREL_ITEM);
        TVCreativeTab.add(DRYING_RACK_ITEM);
        TVCreativeTab.add(SMOKEHOUSE_ITEM);
        for (DeferredItem<Item> item : List.of(CIDER, VINEGAR, SAUERKRAUT, PICKLES, DRIED_FRUIT, JERKY,
                SMOKED_HAM, SMOKED_FISH)) {
            TVCreativeTab.add(item);
        }
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        ProcessingApi.registerItemHandler(event, FERMENTATION_BARREL_BE.get());
        ProcessingApi.registerItemHandler(event, DRYING_RACK_BE.get());
        ProcessingApi.registerItemHandler(event, SMOKEHOUSE_BE.get());
    }

    @Override
    public void registerLang(LangSink sink) {
        sink.add("block.terresvivantes.fermentation_barrel", "Fermentation Barrel", "Tonneau de fermentation");
        sink.add("block.terresvivantes.drying_rack", "Drying Rack", "Séchoir");
        sink.add("block.terresvivantes.smokehouse", "Smokehouse", "Fumoir");
        sink.add("item.terresvivantes.cider", "Cider", "Cidre");
        sink.add("item.terresvivantes.vinegar", "Vinegar", "Vinaigre");
        sink.add("item.terresvivantes.sauerkraut", "Sauerkraut", "Choucroute");
        sink.add("item.terresvivantes.pickles", "Pickles", "Cornichons");
        sink.add("item.terresvivantes.dried_fruit", "Dried Fruit", "Fruits secs");
        sink.add("item.terresvivantes.jerky", "Jerky", "Viande séchée");
        sink.add("item.terresvivantes.smoked_ham", "Smoked Ham", "Jambon fumé");
        sink.add("item.terresvivantes.smoked_fish", "Smoked Fish", "Poisson fumé");
    }

    @Override
    public void registerItemModels(ItemModelProvider provider) {
        provider.basicItem(CIDER.get());
        provider.basicItem(VINEGAR.get());
        provider.basicItem(SAUERKRAUT.get());
        provider.basicItem(PICKLES.get());
        provider.basicItem(DRIED_FRUIT.get());
        provider.basicItem(JERKY.get());
        provider.basicItem(SMOKED_HAM.get());
        provider.basicItem(SMOKED_FISH.get());
    }

    @Override
    public void registerBlockStates(BlockStateProvider provider) {
        // Tonneau : cube à fond et dessus; le dessus s'allume quand la fermentation avance.
        ModelFile barrelIdle = provider.models().cubeBottomTop("fermentation_barrel",
                texture("block/fermentation_barrel_side"), texture("block/fermentation_barrel_top"),
                texture("block/fermentation_barrel_top"));
        ModelFile barrelActive = provider.models().cubeBottomTop("fermentation_barrel_active",
                texture("block/fermentation_barrel_side"), texture("block/fermentation_barrel_top"),
                texture("block/fermentation_barrel_top_active"));
        provider.getVariantBuilder(FERMENTATION_BARREL.get()).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(state.getValue(ProcessingBlock.ACTIVE) ? barrelActive : barrelIdle)
                .rotationY(rotationY(state.getValue(ProcessingBlock.FACING)))
                .build());
        provider.simpleBlockItem(FERMENTATION_BARREL.get(), barrelIdle);

        // Séchoir : panneau plat (cutout), face avant pleine quand il sèche.
        ModelFile rackIdle = dryingRackModel(provider, "drying_rack", "drying_rack_front");
        ModelFile rackFull = dryingRackModel(provider, "drying_rack_full", "drying_rack_front_full");
        provider.getVariantBuilder(DRYING_RACK.get()).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(state.getValue(ProcessingBlock.ACTIVE) ? rackFull : rackIdle)
                .rotationY(rotationY(state.getValue(ProcessingBlock.FACING)))
                .build());
        provider.simpleBlockItem(DRYING_RACK.get(), rackIdle);

        // Fumoir : face avant allumée quand il fume.
        ModelFile smokeIdle = provider.models().orientable("smokehouse",
                texture("block/smokehouse_side"), texture("block/smokehouse_front"), texture("block/smokehouse_top"));
        ModelFile smokeLit = provider.models().orientable("smokehouse_lit",
                texture("block/smokehouse_side"), texture("block/smokehouse_front_lit"), texture("block/smokehouse_top"));
        provider.getVariantBuilder(SMOKEHOUSE.get()).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(state.getValue(ProcessingBlock.ACTIVE) ? smokeLit : smokeIdle)
                .rotationY(rotationY(state.getValue(ProcessingBlock.FACING)))
                .build());
        provider.simpleBlockItem(SMOKEHOUSE.get(), smokeIdle);
    }

    /**
     * Panneau 16 x 16 x 4 collé au mur : le modèle est orienté vers le nord (face avant au nord, z = 12), la rotation
     * d'état le tourne ensuite comme les autres machines.
     */
    private static ModelFile dryingRackModel(BlockStateProvider provider, String name, String front) {
        BlockModelBuilder builder = provider.models().getBuilder("block/" + name)
                .parent(new ModelFile.UncheckedModelFile("minecraft:block/block"))
                .texture("front", texture("block/" + front))
                .texture("side", texture("block/drying_rack_side"))
                .texture("particle", texture("block/drying_rack_side"))
                .renderType("minecraft:cutout");
        builder.element()
                .from(0, 0, 12)
                .to(16, 16, 16)
                .face(Direction.NORTH).texture("#front").end()
                .face(Direction.SOUTH).texture("#side").end()
                .face(Direction.EAST).texture("#side").end()
                .face(Direction.WEST).texture("#side").end()
                .face(Direction.UP).texture("#side").end()
                .face(Direction.DOWN).texture("#side").end()
                .end();
        return builder;
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

    /** Combustible du fumoir : bûches, charbon et charbon de bois (une seule recette par produit). */
    public static final TagKey<Item> SMOKER_FUEL = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(TerresVivantes.MODID, "smoker_fuel"));

    @Override
    public void registerItemTags(Function<TagKey<Item>, TagsProvider.TagAppender<Item>> tags,
                                 HolderLookup.Provider lookup) {
        tags.apply(SMOKER_FUEL)
                .addOptionalTag(ResourceLocation.withDefaultNamespace("logs_that_burn"))
                .add(ResourceKey.create(Registries.ITEM, ResourceLocation.withDefaultNamespace("coal")),
                        ResourceKey.create(Registries.ITEM, ResourceLocation.withDefaultNamespace("charcoal")));
    }

    @Override
    public void registerRecipes(RecipeOutput output, HolderLookup.Provider lookup) {
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, FERMENTATION_BARREL_ITEM.get())
                .pattern("PSP")
                .pattern("P P")
                .pattern("PSP")
                .define('P', ItemTags.PLANKS)
                .define('S', ItemTags.SLABS)
                .unlockedBy("has_planks", InventoryChangeTrigger.TriggerInstance.hasItems(Items.OAK_PLANKS))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, DRYING_RACK_ITEM.get())
                .pattern("S S")
                .pattern("SSS")
                .pattern("S S")
                .define('S', Items.STICK)
                .unlockedBy("has_stick", InventoryChangeTrigger.TriggerInstance.hasItems(Items.STICK))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, SMOKEHOUSE_ITEM.get())
                .pattern("BBB")
                .pattern("BFB")
                .pattern("BBB")
                .define('B', Items.STONE_BRICKS)
                .define('F', Items.FURNACE)
                .unlockedBy("has_furnace", InventoryChangeTrigger.TriggerInstance.hasItems(Items.FURNACE))
                .save(output);
    }

    @Override
    public void registerLootTables(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> output,
                                   HolderLookup.Provider lookup) {
        dropSelf(output, "fermentation_barrel", FERMENTATION_BARREL.get());
        dropSelf(output, "drying_rack", DRYING_RACK.get());
        dropSelf(output, "smokehouse", SMOKEHOUSE.get());
    }

    private static void dropSelf(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> output, String id, Block block) {
        output.accept(ResourceKey.create(Registries.LOOT_TABLE,
                        ResourceLocation.fromNamespaceAndPath(TerresVivantes.MODID, "blocks/" + id)),
                LootTable.lootTable().withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0f))
                        .add(LootItem.lootTableItem(block))
                        .when(ExplosionCondition.survivesExplosion())));
    }
}
