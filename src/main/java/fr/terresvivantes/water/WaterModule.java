package fr.terresvivantes.water;

import fr.terresvivantes.TerresVivantes;
import fr.terresvivantes.core.TVModule;
import fr.terresvivantes.core.registry.TVCreativeTab;
import fr.terresvivantes.crop.CropGrowthHooks;
import fr.terresvivantes.datagen.LangSink;
import fr.terresvivantes.water.logic.WaterLogic;
import net.minecraft.core.HolderLookup;
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
import net.neoforged.neoforge.client.model.generators.BlockModelBuilder;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Module Water : arroseurs en cuivre et en fer, tonneau de pluie, sécheresse estivale.
 * Les arroseurs et le tonneau sont enregistrés ici; la croissance s'y branche par {@link CropGrowthHooks}.
 */
public final class WaterModule implements TVModule {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(TerresVivantes.MODID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TerresVivantes.MODID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, TerresVivantes.MODID);

    public static final DeferredBlock<SprinklerBlock> COPPER_SPRINKLER = BLOCKS.register("copper_sprinkler",
            () -> new SprinklerBlock(sprinklerProperties(MapColor.COLOR_ORANGE), WaterLogic.COPPER_RADIUS));
    public static final DeferredItem<BlockItem> COPPER_SPRINKLER_ITEM =
            ITEMS.registerSimpleBlockItem("copper_sprinkler", COPPER_SPRINKLER);

    public static final DeferredBlock<SprinklerBlock> IRON_SPRINKLER = BLOCKS.register("iron_sprinkler",
            () -> new SprinklerBlock(sprinklerProperties(MapColor.METAL), WaterLogic.IRON_RADIUS));
    public static final DeferredItem<BlockItem> IRON_SPRINKLER_ITEM =
            ITEMS.registerSimpleBlockItem("iron_sprinkler", IRON_SPRINKLER);

    public static final DeferredBlock<RainBarrelBlock> RAIN_BARREL = BLOCKS.register("rain_barrel",
            () -> new RainBarrelBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(2.5f)
                    .sound(SoundType.WOOD)
                    .randomTicks()));
    public static final DeferredItem<BlockItem> RAIN_BARREL_ITEM =
            ITEMS.registerSimpleBlockItem("rain_barrel", RAIN_BARREL);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SprinklerBlockEntity>> SPRINKLER_BE =
            BLOCK_ENTITIES.register("sprinkler", () -> BlockEntityType.Builder
                    .of(SprinklerBlockEntity::new, COPPER_SPRINKLER.get(), IRON_SPRINKLER.get())
                    .build(null));

    @Override
    public void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        TVCreativeTab.add(COPPER_SPRINKLER_ITEM);
        TVCreativeTab.add(IRON_SPRINKLER_ITEM);
        TVCreativeTab.add(RAIN_BARREL_ITEM);
        CropGrowthHooks.registerModifier(WaterGrowth.MODIFIER);
        CropGrowthHooks.registerListener(WaterGrowth.DROUGHT);
    }

    @Override
    public void registerLang(LangSink sink) {
        sink.add("block.terresvivantes.copper_sprinkler", "Copper Sprinkler", "Arroseur en cuivre");
        sink.add("block.terresvivantes.iron_sprinkler", "Iron Sprinkler", "Arroseur en fer");
        sink.add("block.terresvivantes.rain_barrel", "Rain Barrel", "Tonneau de pluie");
    }

    @Override
    public void registerBlockStates(BlockStateProvider provider) {
        ModelFile copper = sprinklerModel(provider, "copper_sprinkler");
        provider.simpleBlock(COPPER_SPRINKLER.get(), copper);
        provider.simpleBlockItem(COPPER_SPRINKLER.get(), copper);

        ModelFile iron = sprinklerModel(provider, "iron_sprinkler");
        provider.simpleBlock(IRON_SPRINKLER.get(), iron);
        provider.simpleBlockItem(IRON_SPRINKLER.get(), iron);

        ModelFile dry = provider.models().cubeBottomTop("block/rain_barrel",
                provider.modLoc("block/rain_barrel_side"),
                provider.modLoc("block/rain_barrel_top"),
                provider.modLoc("block/rain_barrel_top"));
        ModelFile wet = provider.models().cubeBottomTop("block/rain_barrel_water",
                provider.modLoc("block/rain_barrel_side"),
                provider.modLoc("block/rain_barrel_top"),
                provider.modLoc("block/rain_barrel_top_water"));
        provider.getVariantBuilder(RAIN_BARREL.get()).forAllStates(state ->
                ConfiguredModel.builder()
                        .modelFile(state.getValue(RainBarrelBlock.LEVEL) > 0 ? wet : dry)
                        .build());
        provider.simpleBlockItem(RAIN_BARREL.get(), dry);
    }

    private static BlockBehaviour.Properties sprinklerProperties(MapColor color) {
        return BlockBehaviour.Properties.of()
                .mapColor(color)
                .strength(1.5f)
                .sound(SoundType.METAL)
                .noOcclusion()
                .requiresCorrectToolForDrops();
    }

    /**
     * Modèle de l'arroseur : socle (8x2x8), tube (2x6x2), buse (4x2x4). Emprise 6x10x6 px au centre.
     * La même texture sert à toutes les faces.
     */
    private static ModelFile sprinklerModel(BlockStateProvider provider, String id) {
        BlockModelBuilder builder = provider.models().getBuilder("block/" + id)
                .texture("all", provider.modLoc("block/" + id))
                .texture("particle", "#all");
        addBox(builder, 5, 0, 5, 11, 2, 11);
        addBox(builder, 7, 2, 7, 9, 8, 9);
        addBox(builder, 6, 8, 6, 10, 10, 10);
        return builder;
    }

    private static void addBox(BlockModelBuilder builder, float x1, float y1, float z1,
                               float x2, float y2, float z2) {
        builder.element()
                .from(x1, y1, z1)
                .to(x2, y2, z2)
                .allFaces((direction, face) -> face.texture("#all"))
                .end();
    }

    @Override
    public void registerRecipes(RecipeOutput output, HolderLookup.Provider lookup) {
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, COPPER_SPRINKLER_ITEM.get())
                .pattern(" C ")
                .pattern("CBC")
                .define('C', Items.COPPER_INGOT)
                .define('B', Items.BUCKET)
                .unlockedBy("has_copper", InventoryChangeTrigger.TriggerInstance.hasItems(Items.COPPER_INGOT))
                .save(output);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.DECORATIONS, IRON_SPRINKLER_ITEM.get())
                .requires(COPPER_SPRINKLER_ITEM.get())
                .requires(Items.IRON_INGOT)
                .unlockedBy("has_copper_sprinkler", InventoryChangeTrigger.TriggerInstance.hasItems(COPPER_SPRINKLER_ITEM.get()))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, RAIN_BARREL_ITEM.get())
                .pattern("PIP")
                .pattern("PSP")
                .pattern("PIP")
                .define('P', ItemTags.PLANKS)
                .define('S', ItemTags.WOODEN_SLABS)
                .define('I', Items.IRON_INGOT)
                .unlockedBy("has_planks", InventoryChangeTrigger.TriggerInstance.hasItems(Items.OAK_PLANKS))
                .save(output);
    }

    @Override
    public void registerLootTables(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> output,
                                   HolderLookup.Provider lookup) {
        dropSelf(output, COPPER_SPRINKLER.get(), "copper_sprinkler");
        dropSelf(output, IRON_SPRINKLER.get(), "iron_sprinkler");
        dropSelf(output, RAIN_BARREL.get(), "rain_barrel");
    }

    private static void dropSelf(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> output, Block block, String id) {
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
        tags.apply(BlockTags.MINEABLE_WITH_PICKAXE).add(COPPER_SPRINKLER.getKey(), IRON_SPRINKLER.getKey());
        tags.apply(BlockTags.MINEABLE_WITH_AXE).add(RAIN_BARREL.getKey());
    }
}
