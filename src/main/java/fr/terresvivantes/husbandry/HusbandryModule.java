package fr.terresvivantes.husbandry;

import fr.terresvivantes.TerresVivantes;
import fr.terresvivantes.core.TVModule;
import fr.terresvivantes.core.registry.TVCreativeTab;
import fr.terresvivantes.datagen.LangSink;
import fr.terresvivantes.husbandry.logic.FeedType;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
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
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.Direction;

import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Module husbandry (v0.4) : bâtiments et alimentation. L'auge à fourrage (foin, grain, ensilage, aliment composé)
 * nourrit les animaux d'élevage à proximité via {@code LivestockApi}; le nichoir reçoit les œufs des poules;
 * ensilage et aliment composé se fabriquent à partir des cultures.
 *
 * <p>Logique pure : {@code husbandry.logic}. Configuration : {@link HusbandryConfig}.
 */
public final class HusbandryModule implements TVModule {

    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(TerresVivantes.MODID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TerresVivantes.MODID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, TerresVivantes.MODID);

    public static final DeferredBlock<FeedingTroughBlock> TROUGH = BLOCKS.register("feeding_trough",
            () -> new FeedingTroughBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(2.0f)
                    .sound(SoundType.WOOD)
                    .noOcclusion()));
    public static final DeferredItem<BlockItem> TROUGH_ITEM = ITEMS.registerSimpleBlockItem("feeding_trough", TROUGH);

    public static final DeferredBlock<NestingBoxBlock> NEST = BLOCKS.register("nesting_box",
            () -> new NestingBoxBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(2.0f)
                    .sound(SoundType.WOOD)));
    public static final DeferredItem<BlockItem> NEST_ITEM = ITEMS.registerSimpleBlockItem("nesting_box", NEST);

    public static final DeferredItem<Item> SILAGE = ITEMS.registerSimpleItem("silage");
    public static final DeferredItem<Item> FEED_MIX = ITEMS.registerSimpleItem("feed_mix");

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FeedingTroughBlockEntity>> TROUGH_BE =
            BLOCK_ENTITIES.register("feeding_trough", () -> BlockEntityType.Builder
                    .of(FeedingTroughBlockEntity::new, TROUGH.get())
                    .build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<NestingBoxBlockEntity>> NEST_BE =
            BLOCK_ENTITIES.register("nesting_box", () -> BlockEntityType.Builder
                    .of(NestingBoxBlockEntity::new, NEST.get())
                    .build(null));

    /** Aliments acceptés par l'auge, par type (tags d'items générés par {@link #registerItemTags}). */
    public static final TagKey<Item> TROUGH_HAY = itemTag("trough_hay");
    public static final TagKey<Item> TROUGH_GRAIN = itemTag("trough_grain");
    public static final TagKey<Item> TROUGH_SILAGE = itemTag("trough_silage");
    public static final TagKey<Item> TROUGH_MIX = itemTag("trough_mix");

    /** Œufs acceptés par le nichoir : œuf de poule et, s'il existe, œuf de canard. */
    public static final TagKey<Item> NEST_EGGS = itemTag("nest_eggs");

    @Override
    public void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        modBus.addListener(HusbandryModule::registerCapabilities);

        TVCreativeTab.add(TROUGH_ITEM);
        TVCreativeTab.add(NEST_ITEM);
        TVCreativeTab.add(SILAGE);
        TVCreativeTab.add(FEED_MIX);

        HusbandryEvents.register(NeoForge.EVENT_BUS);
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, TROUGH_BE.get(),
                (trough, side) -> trough.input());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, NEST_BE.get(),
                (nest, side) -> nest.handler());
    }

    /** Type d'aliment d'un objet, ou null si ce n'est pas un aliment d'auge. */
    public static FeedType feedOf(ItemStack stack) {
        if (stack.is(TROUGH_HAY)) {
            return FeedType.HAY;
        }
        if (stack.is(TROUGH_GRAIN)) {
            return FeedType.GRAIN;
        }
        if (stack.is(TROUGH_SILAGE)) {
            return FeedType.SILAGE;
        }
        if (stack.is(TROUGH_MIX)) {
            return FeedType.MIX;
        }
        return null;
    }

    /** Objet représentant un type d'aliment (particules de repas). */
    public static Item feedItem(FeedType type) {
        return switch (type) {
            case HAY -> Items.HAY_BLOCK;
            case GRAIN -> Items.WHEAT;
            case SILAGE -> SILAGE.get();
            case MIX -> FEED_MIX.get();
        };
    }

    private static TagKey<Item> itemTag(String id) {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(TerresVivantes.MODID, id));
    }

    /** Objet d'une culture du mod (orge, maïs, soja), lu par identifiant : pas de dépendance de classe au module crop. */
    private static Item cropItem(String id) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(TerresVivantes.MODID, id));
    }

    @Override
    public void registerLang(LangSink sink) {
        sink.add("block.terresvivantes.feeding_trough", "Feeding Trough", "Auge");
        sink.add("block.terresvivantes.nesting_box", "Nesting Box", "Nichoir");
        sink.add("item.terresvivantes.silage", "Silage", "Ensilage");
        sink.add("item.terresvivantes.feed_mix", "Feed Mix", "Aliment composé");
        sink.add("message.terresvivantes.trough.status", "Trough: %1$s/%2$s units, %3$s",
                "Auge : %1$s/%2$s unités, %3$s");
        sink.add("message.terresvivantes.trough.type.empty", "empty", "vide");
        sink.add("message.terresvivantes.trough.type.hay", "hay", "foin");
        sink.add("message.terresvivantes.trough.type.grain", "grain", "grain");
        sink.add("message.terresvivantes.trough.type.silage", "silage", "ensilage");
        sink.add("message.terresvivantes.trough.type.mix", "feed mix", "aliment composé");
        sink.add("message.terresvivantes.trough.mixed", "The trough already holds another feed",
                "L'auge contient déjà un autre aliment");
        sink.add("message.terresvivantes.trough.full", "The trough is full", "L'auge est pleine");
        sink.add("message.terresvivantes.nest.collected", "Collected %1$s egg(s)",
                "%1$s œuf(s) récupéré(s)");
        sink.add("message.terresvivantes.nest.empty", "No egg in the nest", "Aucun œuf dans le nichoir");
    }

    @Override
    public void registerItemModels(ItemModelProvider provider) {
        provider.basicItem(SILAGE.get());
        provider.basicItem(FEED_MIX.get());
    }

    @Override
    public void registerBlockStates(BlockStateProvider provider) {
        ModelFile empty = troughModel(provider, "empty", "feeding_trough_top");
        ModelFile hay = troughModel(provider, "hay", "feeding_trough_top_hay");
        ModelFile grain = troughModel(provider, "grain", "feeding_trough_top_grain");
        ModelFile silage = troughModel(provider, "silage", "feeding_trough_top_silage");
        provider.getVariantBuilder(TROUGH.get()).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(switch (state.getValue(FeedingTroughBlock.FILL)) {
                    case EMPTY -> empty;
                    case HAY -> hay;
                    case GRAIN -> grain;
                    case SILAGE -> silage;
                })
                .build());
        provider.simpleBlockItem(TROUGH.get(), empty);

        ModelFile plain = provider.models().orientable("block/nesting_box",
                provider.modLoc("block/nesting_box_side"), provider.modLoc("block/nesting_box_front"),
                provider.modLoc("block/nesting_box_top"));
        ModelFile withEggs = provider.models().orientable("block/nesting_box_eggs",
                provider.modLoc("block/nesting_box_side"), provider.modLoc("block/nesting_box_front_egg"),
                provider.modLoc("block/nesting_box_top"));
        provider.getVariantBuilder(NEST.get()).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(state.getValue(NestingBoxBlock.EGG) ? withEggs : plain)
                .rotationY(((int) state.getValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING)
                        .toYRot() + 180) % 360)
                .build());
        provider.simpleBlockItem(NEST.get(), plain);
    }

    /** Modèle de l'auge : pavé 16 x 8 x 16 dont le dessus change selon l'aliment. */
    private static ModelFile troughModel(BlockStateProvider provider, String suffix, String top) {
        return provider.models().getBuilder("block/feeding_trough_" + suffix)
                .parent(new ModelFile.UncheckedModelFile("minecraft:block/block"))
                .texture("side", provider.modLoc("block/feeding_trough_side"))
                .texture("top", provider.modLoc("block/" + top))
                .texture("bottom", provider.modLoc("block/feeding_trough_bottom"))
                .element()
                .from(0, 0, 0)
                .to(16, 8, 16)
                .face(Direction.UP).texture("#top").end()
                .face(Direction.DOWN).texture("#bottom").end()
                .face(Direction.NORTH).texture("#side").end()
                .face(Direction.SOUTH).texture("#side").end()
                .face(Direction.WEST).texture("#side").end()
                .face(Direction.EAST).texture("#side").end()
                .end();
    }

    @Override
    public void registerRecipes(RecipeOutput output, HolderLookup.Provider lookup) {
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, TROUGH_ITEM.get())
                .pattern("P P")
                .pattern("PSP")
                .define('P', ItemTags.PLANKS)
                .define('S', ItemTags.SLABS)
                .unlockedBy("has_planks", InventoryChangeTrigger.TriggerInstance.hasItems(Items.OAK_PLANKS))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, NEST_ITEM.get())
                .pattern("PPP")
                .pattern("WWW")
                .define('P', ItemTags.PLANKS)
                .define('W', Items.WHEAT)
                .unlockedBy("has_wheat", InventoryChangeTrigger.TriggerInstance.hasItems(Items.WHEAT))
                .save(output);

        // Ensilage : fermentation simulée d'un foin et de maïs, quatre unités par craft.
        ShapelessRecipeBuilder.shapeless(RecipeCategory.FOOD, SILAGE.get(), 4)
                .requires(Items.HAY_BLOCK)
                .requires(cropItem("corn"))
                .unlockedBy("has_hay", InventoryChangeTrigger.TriggerInstance.hasItems(Items.HAY_BLOCK))
                .save(output);

        // Aliment composé : orge, maïs et soja.
        ShapelessRecipeBuilder.shapeless(RecipeCategory.FOOD, FEED_MIX.get(), 3)
                .requires(cropItem("barley"))
                .requires(cropItem("corn"))
                .requires(cropItem("soybean"))
                .unlockedBy("has_corn", InventoryChangeTrigger.TriggerInstance.hasItems(cropItem("corn")))
                .save(output);
    }

    @Override
    public void registerLootTables(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> output,
                                   HolderLookup.Provider lookup) {
        dropSelf(output, TROUGH.get(), "feeding_trough");
        dropSelf(output, NEST.get(), "nesting_box");
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
                                  HolderLookup.Provider lookup) {
        tags.apply(BlockTags.MINEABLE_WITH_AXE).add(TROUGH.getKey(), NEST.getKey());
    }

    @Override
    public void registerItemTags(Function<TagKey<Item>, TagsProvider.TagAppender<Item>> tags,
                                 HolderLookup.Provider lookup) {
        tags.apply(TROUGH_HAY).add(ResourceKey.create(Registries.ITEM,
                ResourceLocation.withDefaultNamespace("hay_block")));
        tags.apply(TROUGH_GRAIN)
                .add(ResourceKey.create(Registries.ITEM, ResourceLocation.withDefaultNamespace("wheat")))
                .add(ResourceKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(TerresVivantes.MODID, "barley")));
        tags.apply(TROUGH_SILAGE).add(SILAGE.getKey());
        tags.apply(TROUGH_MIX).add(FEED_MIX.getKey());
        tags.apply(NEST_EGGS)
                .add(ResourceKey.create(Registries.ITEM, ResourceLocation.withDefaultNamespace("egg")))
                .addOptional(ResourceLocation.fromNamespaceAndPath(TerresVivantes.MODID, "duck_egg"));
    }
}
