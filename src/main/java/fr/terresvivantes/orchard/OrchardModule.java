package fr.terresvivantes.orchard;

import fr.terresvivantes.TerresVivantes;
import fr.terresvivantes.core.TVModule;
import fr.terresvivantes.core.registry.TVCreativeTab;
import fr.terresvivantes.datagen.LangSink;
import fr.terresvivantes.orchard.logic.FruitTreeDefinition;
import fr.terresvivantes.orchard.logic.FruitTreeDefinitions;
import fr.terresvivantes.orchard.logic.OrchardLogic;
import net.minecraft.advancements.critereon.EnchantmentPredicate;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.advancements.critereon.ItemEnchantmentsPredicate;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.advancements.critereon.ItemSubPredicates;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.advancements.critereon.StatePropertiesPredicate;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.predicates.MatchTool;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.BasicItemListing;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.village.WandererTradesEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Module Orchard : six espèces fruitières (saplings, feuillage à quatre stades, arbres sauvages),
 * fruits comestibles et cinq plats. Le feuillage évolue par randomTick selon la saison et la température.
 */
public final class OrchardModule implements TVModule {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(TerresVivantes.MODID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TerresVivantes.MODID);
    private static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(Registries.FEATURE, TerresVivantes.MODID);

    private static final Map<String, DeferredBlock<FruitSaplingBlock>> SAPLINGS = new LinkedHashMap<>();
    private static final Map<String, DeferredBlock<FruitLeavesBlock>> LEAVES = new LinkedHashMap<>();
    private static final Map<String, DeferredItem<BlockItem>> SAPLING_ITEMS = new LinkedHashMap<>();
    private static final Map<String, DeferredItem<BlockItem>> LEAF_ITEMS = new LinkedHashMap<>();
    private static final Map<String, DeferredItem<Item>> FRUITS = new LinkedHashMap<>();
    private static final Map<String, DeferredItem<? extends Item>> DISHES = new LinkedHashMap<>();

    /** Nom anglais et français d'une espèce (sapling et feuillage). */
    private record TreeNames(String id, String enSapling, String frSapling, String enLeaves, String frLeaves) {
    }

    private static final List<TreeNames> TREE_NAMES = List.of(
            new TreeNames("apple", "Apple Sapling", "Pousse de pommier", "Apple Leaves", "Feuillage de pommier"),
            new TreeNames("cherry", "Cherry Sapling", "Pousse de cerisier", "Cherry Leaves", "Feuillage de cerisier"),
            new TreeNames("orange", "Orange Sapling", "Pousse d'oranger", "Orange Leaves", "Feuillage d'oranger"),
            new TreeNames("olive", "Olive Sapling", "Pousse d'olivier", "Olive Leaves", "Feuillage d'olivier"),
            new TreeNames("walnut", "Walnut Sapling", "Pousse de noyer", "Walnut Leaves", "Feuillage de noyer"),
            new TreeNames("peach", "Peach Sapling", "Pousse de pêcher", "Peach Leaves", "Feuillage de pêcher"));

    /** Fruits créés par le module (la pomme est vanille). */
    private record FruitFood(String id, int nutrition, float saturation, String en, String fr) {
    }

    private static final List<FruitFood> FRUIT_FOODS = List.of(
            new FruitFood("cherry", 2, 0.2f, "Cherry", "Cerise"),
            new FruitFood("orange", 3, 0.2f, "Orange", "Orange"),
            new FruitFood("olive", 1, 0.1f, "Olive", "Olive"),
            new FruitFood("walnut", 2, 0.3f, "Walnut", "Noix"),
            new FruitFood("peach", 3, 0.3f, "Peach", "Pêche"));

    static {
        for (FruitTreeDefinition def : FruitTreeDefinitions.all()) {
            String id = def.id();
            Supplier<FruitLeavesBlock> leaves = () -> LEAVES.get(id).get();
            SAPLINGS.put(id, BLOCKS.register(id + "_sapling", () -> new FruitSaplingBlock(def, leaves,
                    BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SAPLING).randomTicks())));
            LEAVES.put(id, BLOCKS.register(id + "_leaves", () -> new FruitLeavesBlock(def,
                    BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LEAVES))));
            SAPLING_ITEMS.put(id, ITEMS.registerSimpleBlockItem(id + "_sapling", SAPLINGS.get(id)));
            LEAF_ITEMS.put(id, ITEMS.registerSimpleBlockItem(id + "_leaves", LEAVES.get(id)));
            FEATURES.register("wild_" + id + "_tree", () -> new WildFruitTreeFeature(def, leaves));
        }

        for (FruitFood fruit : FRUIT_FOODS) {
            FoodProperties food = new FoodProperties.Builder()
                    .nutrition(fruit.nutrition())
                    .saturationModifier(fruit.saturation())
                    .build();
            FRUITS.put(fruit.id(), ITEMS.registerSimpleItem(fruit.id(), new Item.Properties().food(food)));
        }

        DISHES.put("cherry_pie", ITEMS.registerSimpleItem("cherry_pie", new Item.Properties()
                .food(food(6, 0.5f, null))));
        DISHES.put("orange_juice", ITEMS.register("orange_juice", () -> new VesselFoodItem(
                new Item.Properties().food(food(4, 0.2f, new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 0))),
                Items.GLASS_BOTTLE)));
        DISHES.put("fruit_salad", ITEMS.register("fruit_salad", () -> new VesselFoodItem(
                new Item.Properties().food(food(8, 0.6f, null)), Items.BOWL)));
        DISHES.put("walnut_bread", ITEMS.registerSimpleItem("walnut_bread", new Item.Properties()
                .food(food(6, 0.6f, null))));
        DISHES.put("peach_jam", ITEMS.register("peach_jam", () -> new VesselFoodItem(
                new Item.Properties().food(food(4, 0.4f, null)), Items.GLASS_BOTTLE)));
    }

    private static FoodProperties food(int nutrition, float saturation, MobEffectInstance effect) {
        FoodProperties.Builder builder = new FoodProperties.Builder()
                .nutrition(nutrition)
                .saturationModifier(saturation);
        if (effect != null) {
            builder.effect(effect, 1.0f);
        }
        return builder.build();
    }

    /** Saplings, feuillages, fruits et plats : tous les items du module, pour le registre et l'onglet. */
    @Override
    public void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        FEATURES.register(modBus);
        for (FruitTreeDefinition def : FruitTreeDefinitions.all()) {
            TVCreativeTab.add(SAPLING_ITEMS.get(def.id()));
            TVCreativeTab.add(LEAF_ITEMS.get(def.id()));
        }
        for (FruitFood fruit : FRUIT_FOODS) {
            TVCreativeTab.add(FRUITS.get(fruit.id()));
        }
        for (DeferredItem<? extends Item> dish : DISHES.values()) {
            TVCreativeTab.add(dish);
        }
        NeoForge.EVENT_BUS.addListener(OrchardModule::onWandererTrades);
    }

    /** Item fruitier d'une espèce, résolu depuis son identifiant (pomme vanille ou fruit du module). */
    public static Item fruitItem(FruitTreeDefinition def) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.parse(def.fruit()));
    }

    /** Sapling d'une espèce, par identifiant court (ex. {@code apple}). */
    public static FruitSaplingBlock saplingBlock(String id) {
        return SAPLINGS.get(id).get();
    }

    /** Feuillage d'une espèce, par identifiant court (ex. {@code apple}). */
    public static FruitLeavesBlock leavesBlock(String id) {
        return LEAVES.get(id).get();
    }

    /** Marchand ambulant : un sapling de chaque espèce dans les offres générales. */
    private static void onWandererTrades(WandererTradesEvent event) {
        for (FruitTreeDefinition def : FruitTreeDefinitions.all()) {
            event.getGenericTrades().add(new BasicItemListing(2, new ItemStack(SAPLING_ITEMS.get(def.id()).get()), 6, 1));
        }
    }

    @Override
    public void registerLang(LangSink sink) {
        for (TreeNames names : TREE_NAMES) {
            sink.add("block.terresvivantes." + names.id() + "_sapling", names.enSapling(), names.frSapling());
            sink.add("block.terresvivantes." + names.id() + "_leaves", names.enLeaves(), names.frLeaves());
        }
        for (FruitFood fruit : FRUIT_FOODS) {
            sink.add("item.terresvivantes." + fruit.id(), fruit.en(), fruit.fr());
        }
        sink.add("item.terresvivantes.cherry_pie", "Cherry Pie", "Tarte aux cerises");
        sink.add("item.terresvivantes.orange_juice", "Orange Juice", "Jus d'orange");
        sink.add("item.terresvivantes.fruit_salad", "Fruit Salad", "Salade de fruits");
        sink.add("item.terresvivantes.walnut_bread", "Walnut Bread", "Pain aux noix");
        sink.add("item.terresvivantes.peach_jam", "Peach Jam", "Confiture de pêche");
    }

    @Override
    public void registerItemModels(ItemModelProvider provider) {
        for (FruitFood fruit : FRUIT_FOODS) {
            provider.basicItem(FRUITS.get(fruit.id()).get());
        }
        for (DeferredItem<? extends Item> dish : DISHES.values()) {
            provider.basicItem(dish.get());
        }
    }

    /**
     * Sapling en croix, feuillage à un modèle par stade (cutout_mipped) et item de bloc du feuillage au stade 0.
     */
    @Override
    public void registerBlockStates(BlockStateProvider provider) {
        String[] textureSuffix = {"", "_flowering", "_unripe", "_ripe"};
        for (FruitTreeDefinition def : FruitTreeDefinitions.all()) {
            String id = def.id();
            FruitSaplingBlock sapling = SAPLINGS.get(id).get();
            ModelFile saplingModel = provider.models().cross("block/" + id + "_sapling",
                    provider.modLoc("block/" + id + "_sapling"));
            provider.simpleBlock(sapling, saplingModel);
            provider.simpleBlockItem(sapling, saplingModel);

            ModelFile[] stages = new ModelFile[OrchardLogic.MAX_STAGE + 1];
            for (int stage = 0; stage < stages.length; stage++) {
                stages[stage] = provider.models()
                        .cubeAll("block/" + id + "_leaves_stage" + stage,
                                provider.modLoc("block/" + id + "_leaves" + textureSuffix[stage]))
                        .renderType("minecraft:cutout_mipped");
            }
            FruitLeavesBlock leaves = LEAVES.get(id).get();
            provider.getVariantBuilder(leaves).forAllStates(state -> ConfiguredModel.builder()
                    .modelFile(stages[state.getValue(FruitLeavesBlock.STAGE)])
                    .build());
            provider.simpleBlockItem(leaves, stages[0]);
        }
    }

    private static Item dish(String id) {
        return DISHES.get(id).get();
    }

    @Override
    public void registerRecipes(RecipeOutput output, HolderLookup.Provider lookup) {
        ShapelessRecipeBuilder.shapeless(RecipeCategory.FOOD, dish("cherry_pie"), 1)
                .requires(FRUITS.get("cherry").get(), 3)
                .requires(Items.WHEAT)
                .requires(Items.SUGAR)
                .unlockedBy("has_cherry", InventoryChangeTrigger.TriggerInstance.hasItems(FRUITS.get("cherry").get()))
                .save(output);
        ShapelessRecipeBuilder.shapeless(RecipeCategory.FOOD, dish("orange_juice"), 1)
                .requires(FRUITS.get("orange").get(), 3)
                .requires(Items.GLASS_BOTTLE)
                .unlockedBy("has_orange", InventoryChangeTrigger.TriggerInstance.hasItems(FRUITS.get("orange").get()))
                .save(output);
        ShapelessRecipeBuilder.shapeless(RecipeCategory.FOOD, dish("fruit_salad"), 1)
                .requires(Items.APPLE)
                .requires(FRUITS.get("cherry").get())
                .requires(FRUITS.get("peach").get())
                .requires(Items.BOWL)
                .unlockedBy("has_peach", InventoryChangeTrigger.TriggerInstance.hasItems(FRUITS.get("peach").get()))
                .save(output);
        ShapelessRecipeBuilder.shapeless(RecipeCategory.FOOD, dish("walnut_bread"), 1)
                .requires(FRUITS.get("walnut").get(), 2)
                .requires(Items.WHEAT, 2)
                .unlockedBy("has_walnut", InventoryChangeTrigger.TriggerInstance.hasItems(FRUITS.get("walnut").get()))
                .save(output);
        ShapelessRecipeBuilder.shapeless(RecipeCategory.FOOD, dish("peach_jam"), 1)
                .requires(FRUITS.get("peach").get(), 4)
                .requires(Items.SUGAR)
                .requires(Items.GLASS_BOTTLE)
                .unlockedBy("has_peach", InventoryChangeTrigger.TriggerInstance.hasItems(FRUITS.get("peach").get()))
                .save(output);
    }

    /**
     * Feuillage : sans toucher de soie, 5 % de lâcher un sapling et, au stade 3, 1 à 3 fruits.
     * Avec toucher de soie, le feuillage entier. Sapling : se lâche lui-même.
     */
    @Override
    public void registerLootTables(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> output,
                                   HolderLookup.Provider lookup) {
        HolderLookup.RegistryLookup<Enchantment> enchantments = lookup.lookupOrThrow(Registries.ENCHANTMENT);
        LootItemCondition.Builder silkTouch = MatchTool.toolMatches(ItemPredicate.Builder.item()
                .withSubPredicate(ItemSubPredicates.ENCHANTMENTS, ItemEnchantmentsPredicate.enchantments(List.of(
                        new EnchantmentPredicate(enchantments.getOrThrow(Enchantments.SILK_TOUCH),
                                MinMaxBounds.Ints.atLeast(1))))));
        for (FruitTreeDefinition def : FruitTreeDefinitions.all()) {
            FruitSaplingBlock sapling = SAPLINGS.get(def.id()).get();
            FruitLeavesBlock leaves = LEAVES.get(def.id()).get();
            LootItemCondition.Builder ripe = LootItemBlockStatePropertyCondition.hasBlockStateProperties(leaves)
                    .setProperties(StatePropertiesPredicate.Builder.properties()
                            .hasProperty(FruitLeavesBlock.STAGE, OrchardLogic.MAX_STAGE));

            output.accept(sapling.getLootTable(), LootTable.lootTable().withPool(LootPool.lootPool()
                    .setRolls(ConstantValue.exactly(1.0f))
                    .add(LootItem.lootTableItem(sapling))));
            output.accept(leaves.getLootTable(), LootTable.lootTable()
                    .withPool(LootPool.lootPool()
                            .setRolls(ConstantValue.exactly(1.0f))
                            .add(LootItem.lootTableItem(leaves).when(silkTouch)))
                    .withPool(LootPool.lootPool()
                            .setRolls(ConstantValue.exactly(1.0f))
                            .add(LootItem.lootTableItem(sapling)
                                    .when(silkTouch.invert())
                                    .when(LootItemRandomChanceCondition.randomChance((float) OrchardLogic.SAPLING_DROP_CHANCE))))
                    .withPool(LootPool.lootPool()
                            .setRolls(ConstantValue.exactly(1.0f))
                            .add(LootItem.lootTableItem(fruitItem(def))
                                    .apply(SetItemCountFunction.setCount(
                                            UniformGenerator.between(def.yieldMin(), def.yieldMax()))))
                            .when(ripe)
                            .when(silkTouch.invert())));
        }
    }

    /** Feuillages et saplings : outils et tags vanilla. */
    @Override
    public void registerBlockTags(Function<TagKey<Block>, TagsProvider.TagAppender<Block>> tags,
                                  HolderLookup.Provider lookup) {
        for (FruitTreeDefinition def : FruitTreeDefinitions.all()) {
            tags.apply(BlockTags.SAPLINGS).add(SAPLINGS.get(def.id()).getKey());
            tags.apply(BlockTags.LEAVES).add(LEAVES.get(def.id()).getKey());
            tags.apply(BlockTags.MINEABLE_WITH_HOE).add(LEAVES.get(def.id()).getKey());
        }
    }

    @Override
    public void registerItemTags(Function<TagKey<Item>, TagsProvider.TagAppender<Item>> tags,
                                 HolderLookup.Provider lookup) {
        for (FruitTreeDefinition def : FruitTreeDefinitions.all()) {
            tags.apply(ItemTags.SAPLINGS).add(SAPLING_ITEMS.get(def.id()).getKey());
            tags.apply(ItemTags.LEAVES).add(LEAF_ITEMS.get(def.id()).getKey());
        }
    }
}
