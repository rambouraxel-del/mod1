package fr.terresvivantes.flora;

import fr.terresvivantes.TerresVivantes;
import fr.terresvivantes.core.TVModule;
import fr.terresvivantes.core.registry.TVCreativeTab;
import fr.terresvivantes.datagen.LangSink;
import fr.terresvivantes.flora.logic.FloraLogic;
import fr.terresvivantes.pollination.BiodiversityApi;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.WallSide;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.client.model.generators.MultiPartBlockStateBuilder;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Module flora (v0.3) : quatre fleurs sauvages (trèfle, lavande, camomille, bourrache) avec pots, teintures,
 * tags de fleurs et propagation lente; haie d'aubépine qui fleurit au printemps et sert d'habitat.
 * Logique pure dans {@link FloraLogic}; propagation dans {@link FloraSpread}.
 */
public final class FloraModule implements TVModule {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(TerresVivantes.MODID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TerresVivantes.MODID);

    /** Tag d'items compostables, défini par le module compost; les fleurs y sont ajoutées par ce module. */
    private static final TagKey<Item> COMPOSTABLE = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(TerresVivantes.MODID, "compostable"));

    /** Fleur sauvage : identifiant, effet de ragoût suspect, durée (s), noms en/fr, teinture obtenue. */
    private record Wildflower(String id, Holder<MobEffect> stewEffect, float stewSeconds,
                              String en, String fr, Item dye) {
    }

    private static final List<Wildflower> WILDFLOWERS = List.of(
            new Wildflower("clover", MobEffects.REGENERATION, 7f, "Clover", "Trèfle", Items.WHITE_DYE),
            new Wildflower("lavender", MobEffects.NIGHT_VISION, 10f, "Lavender", "Lavande", Items.PURPLE_DYE),
            new Wildflower("chamomile", MobEffects.ABSORPTION, 8f, "Chamomile", "Camomille", Items.YELLOW_DYE),
            new Wildflower("borage", MobEffects.DAMAGE_BOOST, 8f, "Borage", "Bourrache", Items.LIGHT_BLUE_DYE));

    private static final Map<String, DeferredBlock<WildFlowerBlock>> FLOWERS = new LinkedHashMap<>();
    private static final Map<String, DeferredItem<BlockItem>> FLOWER_ITEMS = new LinkedHashMap<>();
    private static final Map<String, DeferredBlock<FlowerPotBlock>> POTTED = new LinkedHashMap<>();
    private static final DeferredBlock<HawthornHedgeBlock> HEDGE;
    private static final DeferredItem<BlockItem> HEDGE_ITEM;

    static {
        for (Wildflower flower : WILDFLOWERS) {
            FLOWERS.put(flower.id(), BLOCKS.register(flower.id(), () -> new WildFlowerBlock(flower.id(),
                    flower.stewEffect(), flower.stewSeconds(),
                    BlockBehaviour.Properties.ofFullCopy(Blocks.POPPY).randomTicks())));
            FLOWER_ITEMS.put(flower.id(), ITEMS.registerSimpleBlockItem(flower.id(), FLOWERS.get(flower.id())));
        }
        for (Wildflower flower : WILDFLOWERS) {
            POTTED.put(flower.id(), BLOCKS.register("potted_" + flower.id(), () -> new FlowerPotBlock(
                    () -> (FlowerPotBlock) Blocks.FLOWER_POT,
                    () -> FLOWERS.get(flower.id()).get(),
                    BlockBehaviour.Properties.ofFullCopy(Blocks.POTTED_POPPY))));
        }
        HEDGE = BLOCKS.register("hawthorn_hedge", () -> new HawthornHedgeBlock(
                BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LEAVES).randomTicks()));
        HEDGE_ITEM = ITEMS.registerSimpleBlockItem("hawthorn_hedge", HEDGE);
    }

    @Override
    public void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        for (Wildflower flower : WILDFLOWERS) {
            TVCreativeTab.add(FLOWER_ITEMS.get(flower.id()));
        }
        TVCreativeTab.add(HEDGE_ITEM);
        BiodiversityApi.registerHabitatSource((level, center) ->
                FloraLogic.habitatContribution(countHedges(level, center)));
        modBus.addListener((FMLCommonSetupEvent event) -> onCommonSetup(event));
    }

    /** Ajoute chaque fleur au pot vanilla : le pot sait alors afficher et placer la fleur. */
    private static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            FlowerPotBlock emptyPot = (FlowerPotBlock) Blocks.FLOWER_POT;
            for (Wildflower flower : WILDFLOWERS) {
                DeferredBlock<FlowerPotBlock> potted = POTTED.get(flower.id());
                emptyPot.addPlant(ResourceLocation.fromNamespaceAndPath(TerresVivantes.MODID, flower.id()),
                        potted::get);
            }
        });
    }

    /** Fleur sauvage par identifiant court (ex. {@code clover}). */
    public static WildFlowerBlock flower(String id) {
        return FLOWERS.get(id).get();
    }

    /** Bloc de la haie d'aubépine. */
    public static HawthornHedgeBlock hedge() {
        return HEDGE.get();
    }

    /** Haies d'aubépine dans le rayon d'habitat (hauteur ±3). */
    static int countHedges(ServerLevel level, BlockPos center) {
        int r = FloraLogic.HABITAT_RADIUS;
        int count = 0;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = -r; x <= r; x++) {
            for (int y = -3; y <= 3; y++) {
                for (int z = -r; z <= r; z++) {
                    cursor.set(center.getX() + x, center.getY() + y, center.getZ() + z);
                    if (level.isLoaded(cursor) && level.getBlockState(cursor).getBlock() instanceof HawthornHedgeBlock) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    @Override
    public void registerLang(LangSink sink) {
        for (Wildflower flower : WILDFLOWERS) {
            sink.add("block.terresvivantes." + flower.id(), flower.en(), flower.fr());
            sink.add("block.terresvivantes.potted_" + flower.id(), "Potted " + flower.en(), flower.fr() + " en pot");
        }
        sink.add("block.terresvivantes.hawthorn_hedge", "Hawthorn Hedge", "Haie d'aubépine");
    }

    /**
     * Fleurs : modèle croisé (cutout), pot par fleur (modèle flower_pot_cross), haie en mur à deux états
     * (fleurie ou non) avec connexions de mur.
     */
    @Override
    public void registerBlockStates(BlockStateProvider provider) {
        for (Wildflower flower : WILDFLOWERS) {
            Block block = FLOWERS.get(flower.id()).get();
            ModelFile cross = provider.models()
                    .cross("block/" + flower.id(), provider.modLoc("block/" + flower.id()))
                    .renderType("minecraft:cutout");
            provider.simpleBlock(block, cross);
            provider.simpleBlockItem(block, cross);

            ModelFile potted = provider.models()
                    .singleTexture("block/potted_" + flower.id(),
                            ResourceLocation.withDefaultNamespace("block/flower_pot_cross"),
                            "plant", provider.modLoc("block/" + flower.id()))
                    .renderType("minecraft:cutout");
            provider.simpleBlock(POTTED.get(flower.id()).get(), potted);
        }
        registerHedgeStates(provider);
    }

    /** Haie : une partie multipart par orientation et par état de floraison, comme les murs vanilla. */
    private static void registerHedgeStates(BlockStateProvider provider) {
        HawthornHedgeBlock hedge = HEDGE.get();
        MultiPartBlockStateBuilder builder = provider.getMultipartBuilder(hedge);
        for (boolean flowering : new boolean[] {false, true}) {
            String name = flowering ? "block/hawthorn_hedge_flowering" : "block/hawthorn_hedge";
            ResourceLocation texture = provider.modLoc(name);
            ModelFile post = provider.models().wallPost(name + "_post", texture)
                    .renderType("minecraft:cutout");
            ModelFile side = provider.models().wallSide(name + "_side", texture)
                    .renderType("minecraft:cutout");
            ModelFile sideTall = provider.models().wallSideTall(name + "_side_tall", texture)
                    .renderType("minecraft:cutout");

            builder.part().modelFile(post).addModel()
                    .condition(WallBlock.UP, true)
                    .condition(HawthornHedgeBlock.FLOWERING, flowering)
                    .end();
            addSidePart(builder, side, Direction.NORTH, WallBlock.NORTH_WALL, WallSide.LOW, flowering);
            addSidePart(builder, sideTall, Direction.NORTH, WallBlock.NORTH_WALL, WallSide.TALL, flowering);
            addSidePart(builder, side, Direction.EAST, WallBlock.EAST_WALL, WallSide.LOW, flowering);
            addSidePart(builder, sideTall, Direction.EAST, WallBlock.EAST_WALL, WallSide.TALL, flowering);
            addSidePart(builder, side, Direction.SOUTH, WallBlock.SOUTH_WALL, WallSide.LOW, flowering);
            addSidePart(builder, sideTall, Direction.SOUTH, WallBlock.SOUTH_WALL, WallSide.TALL, flowering);
            addSidePart(builder, side, Direction.WEST, WallBlock.WEST_WALL, WallSide.LOW, flowering);
            addSidePart(builder, sideTall, Direction.WEST, WallBlock.WEST_WALL, WallSide.TALL, flowering);
        }
        provider.simpleBlockItem(hedge, provider.models()
                .wallInventory("block/hawthorn_hedge_inventory", provider.modLoc("block/hawthorn_hedge"))
                .renderType("minecraft:cutout"));
    }

    private static void addSidePart(MultiPartBlockStateBuilder builder, ModelFile model, Direction direction,
                                    EnumProperty<WallSide> wall,
                                    WallSide height, boolean flowering) {
        builder.part()
                .modelFile(model)
                .rotationY((((int) direction.toYRot()) + 180) % 360)
                .uvLock(true)
                .addModel()
                .condition(wall, height)
                .condition(HawthornHedgeBlock.FLOWERING, flowering)
                .end();
    }

    /** Teintures (fleur sans crafting, 1 pour 1) et haie (6 feuilles et un bâton pour 6 haies). */
    @Override
    public void registerRecipes(RecipeOutput output, HolderLookup.Provider lookup) {
        for (Wildflower flower : WILDFLOWERS) {
            Block block = FLOWERS.get(flower.id()).get();
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, flower.dye(), 1)
                    .requires(block)
                    .unlockedBy("has_" + flower.id(), InventoryChangeTrigger.TriggerInstance.hasItems(block))
                    .save(output, ResourceLocation.fromNamespaceAndPath(TerresVivantes.MODID, "dye_" + flower.id()));
        }
        ShapelessRecipeBuilder.shapeless(RecipeCategory.DECORATIONS, HEDGE_ITEM.get(), 6)
                .requires(Ingredient.of(ItemTags.LEAVES), 6)
                .requires(Items.STICK)
                .unlockedBy("has_stick", InventoryChangeTrigger.TriggerInstance.hasItems(Items.STICK))
                .save(output);
    }

    /** Fleurs et haie : lâchent leur bloc. Pot : le pot vide et la fleur. */
    @Override
    public void registerLootTables(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> output,
                                   HolderLookup.Provider lookup) {
        for (Wildflower flower : WILDFLOWERS) {
            Block block = FLOWERS.get(flower.id()).get();
            output.accept(FLOWERS.get(flower.id()).get().getLootTable(), LootTable.lootTable()
                    .withPool(LootPool.lootPool()
                            .setRolls(ConstantValue.exactly(1.0f))
                            .add(LootItem.lootTableItem(block))));
            output.accept(POTTED.get(flower.id()).get().getLootTable(), LootTable.lootTable()
                    .withPool(LootPool.lootPool()
                            .setRolls(ConstantValue.exactly(1.0f))
                            .add(LootItem.lootTableItem(Items.FLOWER_POT)))
                    .withPool(LootPool.lootPool()
                            .setRolls(ConstantValue.exactly(1.0f))
                            .add(LootItem.lootTableItem(block))));
        }
        output.accept(HEDGE.get().getLootTable(), LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0f))
                        .add(LootItem.lootTableItem(HEDGE.get()))));
    }

    /** Fleurs dans minecraft:flowers et small_flowers (abeilles, pollinisation); pots; haie comme mur. */
    @Override
    public void registerBlockTags(Function<TagKey<Block>, TagsProvider.TagAppender<Block>> tags,
                                  HolderLookup.Provider lookup) {
        for (Wildflower flower : WILDFLOWERS) {
            tags.apply(BlockTags.FLOWERS).add(FLOWERS.get(flower.id()).getKey());
            tags.apply(BlockTags.SMALL_FLOWERS).add(FLOWERS.get(flower.id()).getKey());
            tags.apply(BlockTags.FLOWER_POTS).add(POTTED.get(flower.id()).getKey());
        }
        tags.apply(BlockTags.WALLS).add(HEDGE.getKey());
        tags.apply(BlockTags.MINEABLE_WITH_HOE).add(HEDGE.getKey());
    }

    /** Fleurs dans minecraft:flowers et small_flowers (items), et compostables. */
    @Override
    public void registerItemTags(Function<TagKey<Item>, TagsProvider.TagAppender<Item>> tags,
                                 HolderLookup.Provider lookup) {
        for (Wildflower flower : WILDFLOWERS) {
            tags.apply(ItemTags.FLOWERS).add(FLOWER_ITEMS.get(flower.id()).getKey());
            tags.apply(ItemTags.SMALL_FLOWERS).add(FLOWER_ITEMS.get(flower.id()).getKey());
            tags.apply(COMPOSTABLE).add(FLOWER_ITEMS.get(flower.id()).getKey());
        }
    }
}
