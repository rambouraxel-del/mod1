package fr.terresvivantes.apiculture;

import com.mojang.serialization.Codec;
import fr.terresvivantes.TerresVivantes;
import fr.terresvivantes.apiculture.logic.ApiaryLogic;
import fr.terresvivantes.apiculture.logic.HoneyType;
import fr.terresvivantes.core.TVModule;
import fr.terresvivantes.core.registry.TVCreativeTab;
import fr.terresvivantes.datagen.LangSink;
import fr.terresvivantes.pollination.BiodiversityApi;
import fr.terresvivantes.pollination.PollinationApi;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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
import java.util.function.Function;

/**
 * Module apiculture (v0.3) : apiaire (colonie et cadres de miel), extracteur, enfumoir, quatre miels, cire.
 * Enregistre sa source de pollinisation et son habitat de biodiversité; les autres modules passent par
 * {@link ApicultureApi} pour déclarer leurs fleurs.
 */
public final class ApicultureModule implements TVModule {

    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(TerresVivantes.MODID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TerresVivantes.MODID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, TerresVivantes.MODID);
    private static final DeferredRegister.DataComponents DATA_COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, TerresVivantes.MODID);

    /** Type de miel d'un cadre de miel. */
    private static final Codec<HoneyType> HONEY_CODEC = Codec.STRING.xmap(HoneyType::fromId, HoneyType::id);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<HoneyType>> HONEY_TYPE =
            DATA_COMPONENTS.registerComponentType("honey_type", builder -> builder
                    .persistent(HONEY_CODEC)
                    .networkSynchronized(ByteBufCodecs.STRING_UTF8.map(HoneyType::fromId, HoneyType::id)));

    public static final DeferredBlock<ApiaryBlock> APIARY = BLOCKS.register("apiary",
            () -> new ApiaryBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(2.5f)
                    .sound(SoundType.WOOD)));
    public static final DeferredItem<BlockItem> APIARY_ITEM = ITEMS.registerSimpleBlockItem("apiary", APIARY);

    public static final DeferredBlock<HoneyExtractorBlock> HONEY_EXTRACTOR = BLOCKS.register("honey_extractor",
            () -> new HoneyExtractorBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.0f)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()));
    public static final DeferredItem<BlockItem> HONEY_EXTRACTOR_ITEM =
            ITEMS.registerSimpleBlockItem("honey_extractor", HONEY_EXTRACTOR);

    public static final DeferredItem<Item> EMPTY_FRAME = ITEMS.registerSimpleItem("empty_frame");
    public static final DeferredItem<HoneyFrameItem> HONEY_FRAME = ITEMS.register("honey_frame",
            () -> new HoneyFrameItem(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<BeeSmokerItem> BEE_SMOKER = ITEMS.register("bee_smoker",
            () -> new BeeSmokerItem(new Item.Properties().durability(64)));
    public static final DeferredItem<BeeswaxItem> BEESWAX = ITEMS.register("beeswax",
            () -> new BeeswaxItem(new Item.Properties()));

    /** Miel sauvage : comme la bouteille de miel vanilla, sans effet propre. */
    public static final DeferredItem<HoneyItem> WILDFLOWER_HONEY = ITEMS.register("wildflower_honey",
            () -> new HoneyItem(honeyProperties(new FoodProperties.Builder()
                    .nutrition(6).saturationModifier(0.1f).build())));
    /** Miel de trèfle : davantage de saturation. */
    public static final DeferredItem<HoneyItem> CLOVER_HONEY = ITEMS.register("clover_honey",
            () -> new HoneyItem(honeyProperties(new FoodProperties.Builder()
                    .nutrition(4).saturationModifier(0.6f).build())));
    /** Miel de lavande : régénération courte (5 s). */
    public static final DeferredItem<HoneyItem> LAVENDER_HONEY = ITEMS.register("lavender_honey",
            () -> new HoneyItem(honeyProperties(new FoodProperties.Builder()
                    .nutrition(4).saturationModifier(0.3f)
                    .effect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0), 1.0f).build())));
    /** Miel de verger : vitesse courte (20 s). */
    public static final DeferredItem<HoneyItem> ORCHARD_HONEY = ITEMS.register("orchard_honey",
            () -> new HoneyItem(honeyProperties(new FoodProperties.Builder()
                    .nutrition(4).saturationModifier(0.3f)
                    .effect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 400, 0), 1.0f).build())));

    /** Tag d'items des miels : les miels du mod et la bouteille de miel vanilla. */
    public static final TagKey<Item> HONEY_TAG = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(TerresVivantes.MODID, "honey"));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ApiaryBlockEntity>> APIARY_BE =
            BLOCK_ENTITIES.register("apiary", () -> BlockEntityType.Builder
                    .of(ApiaryBlockEntity::new, APIARY.get())
                    .build(null));

    @Override
    public void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        DATA_COMPONENTS.register(modBus);
        modBus.addListener(ApicultureModule::registerCapabilities);

        TVCreativeTab.add(APIARY_ITEM);
        TVCreativeTab.add(HONEY_EXTRACTOR_ITEM);
        TVCreativeTab.add(BEE_SMOKER);
        TVCreativeTab.add(EMPTY_FRAME);
        TVCreativeTab.add(HONEY_FRAME);
        TVCreativeTab.add(BEESWAX);
        TVCreativeTab.add(WILDFLOWER_HONEY);
        TVCreativeTab.add(CLOVER_HONEY);
        TVCreativeTab.add(LAVENDER_HONEY);
        TVCreativeTab.add(ORCHARD_HONEY);

        PollinationApi.registerPollinatorSource((level, center) -> ApiaryLogic.pollinatorContribution(
                ApicultureApi.populatedApiaries(level, center, ApiaryLogic.POLLINATOR_RADIUS)));
        BiodiversityApi.registerHabitatSource((level, center) -> ApiaryLogic.habitatContribution(
                ApicultureApi.populatedApiaries(level, center, ApiaryLogic.POLLINATOR_RADIUS)));
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, APIARY_BE.get(),
                (apiary, side) -> apiary.inventory());
    }

    /** Item du miel de ce type. */
    public static Item honeyItem(HoneyType type) {
        return switch (type) {
            case WILDFLOWER -> WILDFLOWER_HONEY.get();
            case CLOVER -> CLOVER_HONEY.get();
            case LAVENDER -> LAVENDER_HONEY.get();
            case ORCHARD -> ORCHARD_HONEY.get();
        };
    }

    /** Propriétés d'une bouteille de miel : pile de 16, rend une bouteille en verre. */
    private static Item.Properties honeyProperties(FoodProperties food) {
        return new Item.Properties().stacksTo(16).food(food);
    }

    @Override
    public void registerLang(LangSink sink) {
        sink.add("block.terresvivantes.apiary", "Apiary", "Apiaire");
        sink.add("block.terresvivantes.honey_extractor", "Honey Extractor", "Extracteur à miel");
        sink.add("item.terresvivantes.empty_frame", "Empty Frame", "Cadre vide");
        sink.add("item.terresvivantes.honey_frame", "Honey Frame", "Cadre de miel");
        sink.add("item.terresvivantes.bee_smoker", "Bee Smoker", "Enfumoir");
        sink.add("item.terresvivantes.beeswax", "Beeswax", "Cire d'abeille");
        sink.add("item.terresvivantes.wildflower_honey", "Wildflower Honey", "Miel de fleurs sauvages");
        sink.add("item.terresvivantes.clover_honey", "Clover Honey", "Miel de trèfle");
        sink.add("item.terresvivantes.lavender_honey", "Lavender Honey", "Miel de lavande");
        sink.add("item.terresvivantes.orchard_honey", "Orchard Honey", "Miel de verger");
        sink.add("tooltip.terresvivantes.honey_frame", "Honey: %s", "Miel : %s");
        sink.add("message.terresvivantes.apiary.status",
                "Apiary: population %s/%s, frames %s/%s, honey: %s",
                "Apiaire : population %s/%s, cadres %s/%s, miel : %s");
        sink.add("message.terresvivantes.apiary.no_honey", "no flowers", "aucune fleur");
        sink.add("message.terresvivantes.apiary.full", "No free frame slot", "Aucun emplacement libre");
        sink.add("message.terresvivantes.apiary.harvested", "Harvested %s honey frame(s)",
                "%s cadre(s) de miel récolté(s)");
        sink.add("message.terresvivantes.apiary.no_frames", "No honey frame to harvest",
                "Aucun cadre de miel à récolter");
        sink.add("message.terresvivantes.extractor.no_bottle", "You need a glass bottle",
                "Il faut une bouteille en verre");
    }

    @Override
    public void registerItemModels(ItemModelProvider provider) {
        provider.basicItem(EMPTY_FRAME.get());
        provider.basicItem(HONEY_FRAME.get());
        provider.basicItem(BEE_SMOKER.get());
        provider.basicItem(BEESWAX.get());
        provider.basicItem(WILDFLOWER_HONEY.get());
        provider.basicItem(CLOVER_HONEY.get());
        provider.basicItem(LAVENDER_HONEY.get());
        provider.basicItem(ORCHARD_HONEY.get());
    }

    @Override
    public void registerBlockStates(BlockStateProvider provider) {
        ModelFile plain = provider.models().orientableWithBottom("block/apiary",
                provider.modLoc("block/apiary_side"), provider.modLoc("block/apiary_front"),
                provider.modLoc("block/apiary_top"), provider.modLoc("block/apiary_bottom"));
        ModelFile honey = provider.models().orientableWithBottom("block/apiary_honey",
                provider.modLoc("block/apiary_side"), provider.modLoc("block/apiary_front_honey"),
                provider.modLoc("block/apiary_top"), provider.modLoc("block/apiary_bottom"));
        provider.getVariantBuilder(APIARY.get()).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(state.getValue(ApiaryBlock.HONEY) ? honey : plain)
                .rotationY(((int) state.getValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING)
                        .toYRot() + 180) % 360)
                .build());
        provider.simpleBlockItem(APIARY.get(), plain);

        ModelFile extractor = provider.models().cubeBottomTop("block/honey_extractor",
                provider.modLoc("block/honey_extractor_side"),
                provider.modLoc("block/honey_extractor_top"),
                provider.modLoc("block/honey_extractor_top"));
        provider.simpleBlock(HONEY_EXTRACTOR.get(), extractor);
        provider.simpleBlockItem(HONEY_EXTRACTOR.get(), extractor);
    }

    @Override
    public void registerRecipes(RecipeOutput output, HolderLookup.Provider lookup) {
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, APIARY_ITEM.get())
                .pattern("PPP")
                .pattern("FHF")
                .pattern("PPP")
                .define('P', ItemTags.PLANKS)
                .define('F', EMPTY_FRAME.get())
                .define('H', Items.HONEYCOMB)
                .unlockedBy("has_frame", InventoryChangeTrigger.TriggerInstance.hasItems(EMPTY_FRAME.get()))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, EMPTY_FRAME.get(), 4)
                .pattern("SSS")
                .pattern("SWS")
                .pattern("SSS")
                .define('S', Items.STICK)
                .define('W', Items.STRING)
                .unlockedBy("has_string", InventoryChangeTrigger.TriggerInstance.hasItems(Items.STRING))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, HONEY_EXTRACTOR_ITEM.get())
                .pattern("I I")
                .pattern("IBI")
                .pattern("III")
                .define('I', Items.IRON_INGOT)
                .define('B', Items.BUCKET)
                .unlockedBy("has_iron", InventoryChangeTrigger.TriggerInstance.hasItems(Items.IRON_INGOT))
                .save(output);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.TOOLS, BEE_SMOKER.get())
                .requires(Items.IRON_INGOT)
                .requires(Items.LEATHER)
                .requires(Items.FLINT)
                .unlockedBy("has_iron", InventoryChangeTrigger.TriggerInstance.hasItems(Items.IRON_INGOT))
                .save(output);

        // Clé explicite : la bougie vanilla garde son identifiant minecraft:candle, sans écraser la recette vanilla.
        ShapelessRecipeBuilder.shapeless(RecipeCategory.DECORATIONS, Items.CANDLE)
                .requires(BEESWAX.get())
                .requires(Items.STRING)
                .unlockedBy("has_beeswax", InventoryChangeTrigger.TriggerInstance.hasItems(BEESWAX.get()))
                .save(output, ResourceLocation.fromNamespaceAndPath(TerresVivantes.MODID, "beeswax_candle"));
    }

    @Override
    public void registerLootTables(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> output,
                                   HolderLookup.Provider lookup) {
        dropSelf(output, APIARY.get(), "apiary");
        dropSelf(output, HONEY_EXTRACTOR.get(), "honey_extractor");
    }

    private static void dropSelf(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> output, Block block,
                                 String id) {
        output.accept(ResourceKey.create(Registries.LOOT_TABLE,
                        ResourceLocation.fromNamespaceAndPath(TerresVivantes.MODID, "blocks/" + id)),
                LootTable.lootTable().withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0f))
                        .add(LootItem.lootTableItem(block))
                        .when(ExplosionCondition.survivesExplosion())));
    }

    @Override
    public void registerBlockTags(Function<TagKey<Block>, TagsProvider.TagAppender<Block>> tags,
                                  net.minecraft.core.HolderLookup.Provider lookup) {
        tags.apply(BlockTags.MINEABLE_WITH_AXE).add(APIARY.getKey());
        tags.apply(BlockTags.MINEABLE_WITH_PICKAXE).add(HONEY_EXTRACTOR.getKey());
    }

    @Override
    public void registerItemTags(Function<TagKey<Item>, TagsProvider.TagAppender<Item>> tags,
                                 HolderLookup.Provider lookup) {
        tags.apply(HONEY_TAG)
                .add(WILDFLOWER_HONEY.getKey(), CLOVER_HONEY.getKey(), LAVENDER_HONEY.getKey(),
                        ORCHARD_HONEY.getKey())
                .add(ResourceKey.create(Registries.ITEM, ResourceLocation.withDefaultNamespace("honey_bottle")));
    }
}
