package fr.terresvivantes.milling;

import fr.terresvivantes.TerresVivantes;
import fr.terresvivantes.core.TVModule;
import fr.terresvivantes.core.registry.TVCreativeTab;
import fr.terresvivantes.datagen.LangSink;
import fr.terresvivantes.orchard.VesselFoodItem;
import fr.terresvivantes.processing.MachineType;
import fr.terresvivantes.processing.ProcessingApi;
import fr.terresvivantes.processing.ProcessingBlock;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
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

import java.util.function.BiConsumer;

/**
 * Module milling (v0.5) : la meule et le pressoir à fruits, machines de la transformation, et leurs produits (farines,
 * jus, huile d'olive). Les recettes de mouture et de pressage sont des fichiers JSON {@code milling_*} et {@code press_*}.
 */
public final class MillingModule implements TVModule {

    /** Machine « millstone » : une entrée, pas de conteneur. */
    public static final MachineType MILLSTONE_TYPE = ProcessingApi.registerMachine("millstone", 1, false);
    /** Machine « fruit_press » : une entrée et un conteneur (bouteille de verre). */
    public static final MachineType FRUIT_PRESS_TYPE = ProcessingApi.registerMachine("fruit_press", 1, true);

    /** Nourriture d'un jus : un peu de saturation, pas d'effet. Le contenant (bouteille) est rendu. */
    private static final FoodProperties JUICE_FOOD = new FoodProperties.Builder()
            .nutrition(2)
            .saturationModifier(0.1f)
            .build();

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

    public static final DeferredBlock<FruitPressBlock> FRUIT_PRESS = BLOCKS.register("fruit_press",
            () -> new FruitPressBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(2.5f)
                    .sound(SoundType.WOOD)));
    public static final DeferredItem<BlockItem> FRUIT_PRESS_ITEM =
            ITEMS.registerSimpleBlockItem("fruit_press", FRUIT_PRESS);

    public static final DeferredItem<Item> WHEAT_FLOUR = ITEMS.registerSimpleItem("wheat_flour");
    public static final DeferredItem<Item> BARLEY_FLOUR = ITEMS.registerSimpleItem("barley_flour");
    public static final DeferredItem<Item> CORN_MEAL = ITEMS.registerSimpleItem("corn_meal");
    public static final DeferredItem<Item> OLIVE_OIL = ITEMS.registerSimpleItem("olive_oil");
    public static final DeferredItem<VesselFoodItem> APPLE_JUICE = ITEMS.register("apple_juice",
            () -> new VesselFoodItem(new Item.Properties().food(JUICE_FOOD), Items.GLASS_BOTTLE));
    public static final DeferredItem<VesselFoodItem> PEACH_JUICE = ITEMS.register("peach_juice",
            () -> new VesselFoodItem(new Item.Properties().food(JUICE_FOOD), Items.GLASS_BOTTLE));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MillstoneBlockEntity>> MILLSTONE_BE =
            BLOCK_ENTITIES.register("millstone", () -> BlockEntityType.Builder
                    .of(MillstoneBlockEntity::new, MILLSTONE.get())
                    .build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FruitPressBlockEntity>> FRUIT_PRESS_BE =
            BLOCK_ENTITIES.register("fruit_press", () -> BlockEntityType.Builder
                    .of(FruitPressBlockEntity::new, FRUIT_PRESS.get())
                    .build(null));

    @Override
    public void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        modBus.addListener(MillingModule::registerCapabilities);

        TVCreativeTab.add(MILLSTONE_ITEM);
        TVCreativeTab.add(FRUIT_PRESS_ITEM);
        TVCreativeTab.add(WHEAT_FLOUR);
        TVCreativeTab.add(BARLEY_FLOUR);
        TVCreativeTab.add(CORN_MEAL);
        TVCreativeTab.add(OLIVE_OIL);
        TVCreativeTab.add(APPLE_JUICE);
        TVCreativeTab.add(PEACH_JUICE);
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        ProcessingApi.registerItemHandler(event, MILLSTONE_BE.get());
        ProcessingApi.registerItemHandler(event, FRUIT_PRESS_BE.get());
    }

    @Override
    public void registerLang(LangSink sink) {
        sink.add("block.terresvivantes.millstone", "Millstone", "Meule");
        sink.add("block.terresvivantes.fruit_press", "Fruit Press", "Pressoir à fruits");
        sink.add("item.terresvivantes.wheat_flour", "Wheat Flour", "Farine de blé");
        sink.add("item.terresvivantes.barley_flour", "Barley Flour", "Farine d'orge");
        sink.add("item.terresvivantes.corn_meal", "Corn Meal", "Farine de maïs");
        sink.add("item.terresvivantes.olive_oil", "Olive Oil", "Huile d'olive");
        sink.add("item.terresvivantes.apple_juice", "Apple Juice", "Jus de pomme");
        sink.add("item.terresvivantes.peach_juice", "Peach Juice", "Jus de pêche");
    }

    @Override
    public void registerItemModels(ItemModelProvider provider) {
        // Modèles d'item autonomes (même géométrie que les blocs au repos) : ne dépendent pas de l'ordre des providers.
        provider.withExistingParent("millstone", ResourceLocation.withDefaultNamespace("block/cube_bottom_top"))
                .texture("top", texture("block/millstone_top"))
                .texture("side", texture("block/millstone_side"))
                .texture("bottom", texture("block/millstone_top"));
        provider.withExistingParent("fruit_press", ResourceLocation.withDefaultNamespace("block/cube_bottom_top"))
                .texture("top", texture("block/fruit_press_top"))
                .texture("side", texture("block/fruit_press_side"))
                .texture("bottom", texture("block/fruit_press_top"));
        provider.basicItem(WHEAT_FLOUR.get());
        provider.basicItem(BARLEY_FLOUR.get());
        provider.basicItem(CORN_MEAL.get());
        provider.basicItem(OLIVE_OIL.get());
        provider.basicItem(APPLE_JUICE.get());
        provider.basicItem(PEACH_JUICE.get());
    }

    @Override
    public void registerBlockStates(BlockStateProvider provider) {
        ModelFile millIdle = provider.models().cubeBottomTop("millstone",
                texture("block/millstone_side"), texture("block/millstone_top"),
                texture("block/millstone_top"));
        ModelFile millActive = provider.models().cubeBottomTop("millstone_active",
                texture("block/millstone_side"), texture("block/millstone_top"),
                texture("block/millstone_top_active"));
        provider.getVariantBuilder(MILLSTONE.get()).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(state.getValue(ProcessingBlock.ACTIVE) ? millActive : millIdle)
                .rotationY(rotationY(state.getValue(ProcessingBlock.FACING)))
                .build());

        // Pressoir : seule la face du dessus existe en texture; le visuel « actif » passe par les particules.
        ModelFile pressIdle = provider.models().cubeBottomTop("fruit_press",
                texture("block/fruit_press_side"), texture("block/fruit_press_top"),
                texture("block/fruit_press_top"));
        ModelFile pressActive = provider.models().cubeBottomTop("fruit_press_active",
                texture("block/fruit_press_side"), texture("block/fruit_press_top"),
                texture("block/fruit_press_top"));
        provider.getVariantBuilder(FRUIT_PRESS.get()).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(state.getValue(ProcessingBlock.ACTIVE) ? pressActive : pressIdle)
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
    public void registerRecipes(RecipeOutput output, HolderLookup.Provider lookup) {
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, MILLSTONE_ITEM.get())
                .requires(Items.SMOOTH_STONE)
                .requires(Items.STICK)
                .requires(Items.FLINT)
                .unlockedBy("has_flint", InventoryChangeTrigger.TriggerInstance.hasItems(Items.FLINT))
                .save(output);
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, FRUIT_PRESS_ITEM.get())
                .pattern("PPP")
                .pattern("IKI")
                .pattern("PPP")
                .define('P', ItemTags.PLANKS)
                .define('I', Items.IRON_INGOT)
                .define('K', Items.PISTON)
                .unlockedBy("has_piston", InventoryChangeTrigger.TriggerInstance.hasItems(Items.PISTON))
                .save(output);
    }

    @Override
    public void registerLootTables(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> output,
                                   HolderLookup.Provider lookup) {
        output.accept(lootKey("millstone"), LootTable.lootTable().withPool(LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1.0f))
                .add(LootItem.lootTableItem(MILLSTONE.get()))
                .when(ExplosionCondition.survivesExplosion())));
        output.accept(lootKey("fruit_press"), LootTable.lootTable().withPool(LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1.0f))
                .add(LootItem.lootTableItem(FRUIT_PRESS.get()))
                .when(ExplosionCondition.survivesExplosion())));
    }

    private static ResourceKey<LootTable> lootKey(String block) {
        return ResourceKey.create(Registries.LOOT_TABLE,
                ResourceLocation.fromNamespaceAndPath(TerresVivantes.MODID, "blocks/" + block));
    }
}
