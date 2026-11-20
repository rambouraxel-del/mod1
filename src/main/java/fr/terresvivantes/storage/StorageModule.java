package fr.terresvivantes.storage;

import fr.terresvivantes.TerresVivantes;
import fr.terresvivantes.core.TVModule;
import fr.terresvivantes.core.registry.TVCreativeTab;
import fr.terresvivantes.datagen.LangSink;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
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
import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.world.level.block.Block;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.resources.ResourceKey;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLEnvironment;
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
 * Module storage (v0.6) : la caisse (un type d'item, 8 stacks, rendu sur la face avant) et le silo (colonne empilable
 * de produits agricoles). Aucune configuration.
 */
public final class StorageModule implements TVModule {

    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(TerresVivantes.MODID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TerresVivantes.MODID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, TerresVivantes.MODID);
    private static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, TerresVivantes.MODID);

    /** Produits et graines que le silo accepte (tag d'items généré par {@link #registerItemTags}). */
    public static final TagKey<Item> SILO_STORABLE = TagKey.create(Registries.ITEM, id("silo_storable"));

    /** Contenu d'une caisse, porté par l'item cassé. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CrateContents>> CRATE_CONTENTS =
            COMPONENTS.registerComponentType("crate_contents", builder -> builder
                    .persistent(CrateContents.CODEC)
                    .networkSynchronized(CrateContents.STREAM_CODEC));

    public static final DeferredBlock<CrateBlock> CRATE = BLOCKS.register("crate",
            () -> new CrateBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(2.0f)
                    .sound(SoundType.WOOD)));
    public static final DeferredItem<CrateItem> CRATE_ITEM = ITEMS.register("crate",
            () -> new CrateItem(CRATE.get(), new Item.Properties()));

    public static final DeferredBlock<SiloBlock> SILO = BLOCKS.register("silo",
            () -> new SiloBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(2.0f)
                    .sound(SoundType.WOOD)));
    public static final DeferredItem<BlockItem> SILO_ITEM = ITEMS.registerSimpleBlockItem("silo", SILO);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CrateBlockEntity>> CRATE_BE =
            BLOCK_ENTITIES.register("crate", () -> BlockEntityType.Builder
                    .of(CrateBlockEntity::new, CRATE.get())
                    .build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SiloBlockEntity>> SILO_BE =
            BLOCK_ENTITIES.register("silo", () -> BlockEntityType.Builder
                    .of(SiloBlockEntity::new, SILO.get())
                    .build(null));

    @Override
    public void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        COMPONENTS.register(modBus);
        modBus.addListener(StorageModule::registerCapabilities);

        TVCreativeTab.add(CRATE_ITEM);
        TVCreativeTab.add(SILO_ITEM);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            fr.terresvivantes.storage.client.StorageClient.register(modBus);
        }
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, CRATE_BE.get(), (be, side) -> be.itemHandler());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, SILO_BE.get(), (be, side) -> be.itemHandler(side));
    }

    @Override
    public void registerLang(LangSink sink) {
        sink.add("block.terresvivantes.crate", "Crate", "Caisse");
        sink.add("block.terresvivantes.silo", "Silo", "Silo");
        sink.add("message.terresvivantes.crate_contents", "x %1$s %2$s", "x %1$s %2$s");
        sink.add("message.terresvivantes.crate_empty", "Empty crate", "Caisse vide");
        sink.add("message.terresvivantes.silo_summary", "Silo: %1$s types, %2$s items, %3$s%% full",
                "Silo : %1$s types, %2$s objets, %3$s%% plein");
        sink.add("message.terresvivantes.silo_dropped",
                "The silo held %1$s items: they fell to the ground.",
                "Le silo contenait %1$s objets : ils sont tombés au sol.");
    }

    @Override
    public void registerItemModels(ItemModelProvider provider) {
        // Modèles d'item autonomes (même géométrie que les blocs) : pas de dépendance à l'ordre des providers.
        provider.withExistingParent("crate", ResourceLocation.withDefaultNamespace("block/cube"))
                .texture("down", texture("block/crate_top"))
                .texture("up", texture("block/crate_top"))
                .texture("north", texture("block/crate_front"))
                .texture("south", texture("block/crate_side"))
                .texture("east", texture("block/crate_side"))
                .texture("west", texture("block/crate_side"))
                .texture("particle", texture("block/crate_side"));
        provider.withExistingParent("silo", ResourceLocation.withDefaultNamespace("block/cube_bottom_top"))
                .texture("top", texture("block/silo_top"))
                .texture("side", texture("block/silo_side"))
                .texture("bottom", texture("block/silo_bottom"));
    }

    @Override
    public void registerBlockStates(BlockStateProvider provider) {
        ModelFile crate = provider.models().cube("crate",
                texture("block/crate_top"), texture("block/crate_top"), texture("block/crate_front"),
                texture("block/crate_side"), texture("block/crate_side"), texture("block/crate_side"));
        provider.getVariantBuilder(CRATE.get()).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(crate)
                .rotationY(rotationY(state.getValue(HorizontalDirectionalBlock.FACING)))
                .build());

        ModelFile silo = provider.models().cubeBottomTop("silo",
                texture("block/silo_side"), texture("block/silo_top"), texture("block/silo_bottom"));
        provider.simpleBlock(SILO.get(), silo);
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
        // Caisse : 8 planches autour d'un lingot de fer.
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, CRATE_ITEM.get())
                .pattern("PPP")
                .pattern("PIP")
                .pattern("PPP")
                .define('P', ItemTags.PLANKS)
                .define('I', Items.IRON_INGOT)
                .unlockedBy("has_iron", InventoryChangeTrigger.TriggerInstance.hasItems(Items.IRON_INGOT))
                .save(output);
        // Silo : 6 lingots de fer cerclant 3 planches.
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, SILO_ITEM.get())
                .pattern("IPI")
                .pattern("IPI")
                .pattern("IPI")
                .define('P', ItemTags.PLANKS)
                .define('I', Items.IRON_INGOT)
                .unlockedBy("has_iron", InventoryChangeTrigger.TriggerInstance.hasItems(Items.IRON_INGOT))
                .save(output);
    }

    @Override
    public void registerLootTables(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> output,
                                   HolderLookup.Provider lookup) {
        // La caisse n'a pas de loot : son item (avec contenu) est donné par onRemove, pour éviter un doublon.
        output.accept(lootKey("crate"), LootTable.lootTable());
        output.accept(lootKey("silo"), LootTable.lootTable().withPool(LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1.0f))
                .add(LootItem.lootTableItem(SILO.get()))
                .when(ExplosionCondition.survivesExplosion())));
    }

    @Override
    public void registerItemTags(Function<TagKey<Item>, TagsProvider.TagAppender<Item>> tags,
                                 HolderLookup.Provider lookup) {
        TagsProvider.TagAppender<Item> storable = tags.apply(SILO_STORABLE);
        storable.addOptionalTag(ResourceLocation.fromNamespaceAndPath("c", "seeds"))
                .addOptionalTag(ResourceLocation.fromNamespaceAndPath("c", "crops"))
                .add(ResourceKey.create(Registries.ITEM, ResourceLocation.withDefaultNamespace("wheat")));
        for (fr.terresvivantes.crop.logic.CropDefinition def : fr.terresvivantes.crop.logic.CropDefinitions.all()) {
            storable.add(ResourceKey.create(Registries.ITEM,
                    ResourceLocation.fromNamespaceAndPath(TerresVivantes.MODID, def.id())));
            storable.add(ResourceKey.create(Registries.ITEM,
                    ResourceLocation.fromNamespaceAndPath(TerresVivantes.MODID, def.id() + "_seeds")));
        }
    }

    private static ResourceKey<LootTable> lootKey(String block) {
        return ResourceKey.create(Registries.LOOT_TABLE,
                ResourceLocation.fromNamespaceAndPath(TerresVivantes.MODID, "blocks/" + block));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(TerresVivantes.MODID, path);
    }
}
