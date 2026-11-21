package fr.terresvivantes.vineyard;

import com.mojang.serialization.Codec;
import fr.terresvivantes.TerresVivantes;
import fr.terresvivantes.core.TVModule;
import fr.terresvivantes.core.quality.QualityModule;
import fr.terresvivantes.core.quality.logic.QualityLogic;
import fr.terresvivantes.vineyard.logic.VineyardLogic;
import fr.terresvivantes.core.registry.TVCreativeTab;
import fr.terresvivantes.datagen.LangSink;
import fr.terresvivantes.processing.ProcessingApi;
import fr.terresvivantes.processing.ProcessingRecipe;
import fr.terresvivantes.season.SeasonApi;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.block.SoundType;
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
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.client.model.generators.ModelBuilder;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.village.WandererTradesEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.BiConsumer;

/**
 * Module vineyard (v0.9) : treillis, vigne pérenne (boutures, croissance par saison, récolte de raisins), vinification
 * (pressoir, tonneau, étagère) et millésime. Les recettes sont des fichiers JSON {@code vineyard_*}.
 *
 * <p>Millésime : composant {@code terresvivantes:vintage} posé à la fin du craft d'un vin rouge ou blanc, par un
 * écouteur de sortie de {@link ProcessingApi} (hook minimal ajouté au framework pour ce besoin). Le même écouteur
 * applique l'affinage du vin rouge (une étoile par 6 jours MC).
 */
public final class VineyardModule implements TVModule {

    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(TerresVivantes.MODID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TerresVivantes.MODID);
    private static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, TerresVivantes.MODID);

    /** Millésime (année MC, 1-based) d'un vin. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> VINTAGE =
            COMPONENTS.registerComponentType("vintage", builder -> builder
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT));

    private static final FoodProperties GRAPE_FOOD = new FoodProperties.Builder()
            .nutrition(1)
            .saturationModifier(0.1f)
            .build();
    private static final FoodProperties JUICE_FOOD = new FoodProperties.Builder()
            .nutrition(2)
            .saturationModifier(0.3f)
            .build();
    /** Vin : régénération I pendant 10 secondes (200 ticks). */
    private static final FoodProperties WINE_FOOD = new FoodProperties.Builder()
            .nutrition(2)
            .saturationModifier(0.2f)
            .effect(() -> new MobEffectInstance(MobEffects.REGENERATION, 200, 0), 1.0f)
            .build();

    public static final DeferredBlock<TrellisBlock> TRELLIS = BLOCKS.register("trellis",
            () -> new TrellisBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(0.6f)
                    .sound(SoundType.WOOD)
                    .noOcclusion()));
    public static final DeferredItem<BlockItem> TRELLIS_ITEM = ITEMS.registerSimpleBlockItem("trellis", TRELLIS);

    public static final DeferredBlock<GrapevineBlock> GRAPEVINE = BLOCKS.register("grapevine",
            () -> new GrapevineBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PLANT)
                    .strength(0.6f)
                    .sound(SoundType.WOOD)
                    .noOcclusion()
                    .randomTicks()));

    public static final DeferredItem<Item> RED_GRAPES = ITEMS.register("red_grapes",
            () -> new Item(new Item.Properties().food(GRAPE_FOOD)));
    public static final DeferredItem<Item> WHITE_GRAPES = ITEMS.register("white_grapes",
            () -> new Item(new Item.Properties().food(GRAPE_FOOD)));
    public static final DeferredItem<GrapevineCuttingItem> RED_CUTTING = ITEMS.register("red_grapevine_cutting",
            () -> new GrapevineCuttingItem(new Item.Properties(), GrapeVariety.RED));
    public static final DeferredItem<GrapevineCuttingItem> WHITE_CUTTING = ITEMS.register("white_grapevine_cutting",
            () -> new GrapevineCuttingItem(new Item.Properties(), GrapeVariety.WHITE));
    public static final DeferredItem<VineyardDrinkItem> GRAPE_JUICE = ITEMS.register("grape_juice",
            () -> new VineyardDrinkItem(new Item.Properties().food(JUICE_FOOD), Items.GLASS_BOTTLE));
    /** Vin jeune : mis en bouteille à la fermentation (bouteille consommée, rendue en buvant), comme le vin blanc. */
    public static final DeferredItem<VineyardDrinkItem> YOUNG_WINE = ITEMS.register("young_wine",
            () -> new VineyardDrinkItem(new Item.Properties().food(WINE_FOOD), Items.GLASS_BOTTLE));
    /**
     * Vin rouge : sans bouteille. L'étagère d'affinage n'a pas d'emplacement de contenant (machine à {@code container=false}),
     * une recette avec contenant ne s'y lancerait jamais. Le rouge ne rend donc pas de bouteille en buvant.
     */
    public static final DeferredItem<VineyardDrinkItem> RED_WINE = ITEMS.register("red_wine",
            () -> new VineyardDrinkItem(new Item.Properties().food(WINE_FOOD), null));
    public static final DeferredItem<VineyardDrinkItem> WHITE_WINE = ITEMS.register("white_wine",
            () -> new VineyardDrinkItem(new Item.Properties().food(WINE_FOOD), Items.GLASS_BOTTLE));

    @Override
    public void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        COMPONENTS.register(modBus);

        NeoForge.EVENT_BUS.addListener(VineyardModule::onTooltip);
        NeoForge.EVENT_BUS.addListener(VineyardModule::onWandererTrades);
        ProcessingApi.registerOutputListener(VineyardModule::onCrafted);

        TVCreativeTab.add(TRELLIS_ITEM);
        TVCreativeTab.add(RED_CUTTING);
        TVCreativeTab.add(WHITE_CUTTING);
        TVCreativeTab.add(RED_GRAPES);
        TVCreativeTab.add(WHITE_GRAPES);
        TVCreativeTab.add(GRAPE_JUICE);
        TVCreativeTab.add(YOUNG_WINE);
        TVCreativeTab.add(RED_WINE);
        TVCreativeTab.add(WHITE_WINE);
    }

    /**
     * Fin de craft d'un vin : le vin rouge gagne une étoile à l'affinage ({@link VineyardLogic#agedRedWine}); un vin
     * rouge ou blanc reçoit le millésime, l'année courante.
     */
    private static void onCrafted(ServerLevel level, BlockPos pos, ProcessingRecipe recipe, ItemStack made) {
        if (made.is(RED_WINE.get())) {
            int base = QualityModule.qualityOf(made);
            if (base != QualityLogic.ABSENT) {
                QualityModule.setQuality(made, VineyardLogic.agedRedWine(base, recipe.time()));
            }
        }
        if (made.is(RED_WINE.get()) || made.is(WHITE_WINE.get())) {
            made.set(VINTAGE.get(), SeasonApi.getYear(level));
        }
    }

    private static void onTooltip(ItemTooltipEvent event) {
        Integer year = event.getItemStack().get(VINTAGE.get());
        if (year != null) {
            event.getToolTip().add(Component.translatable("tooltip.terresvivantes.vintage", year));
        }
    }

    /** Le marchand ambulant vend des boutures des deux cépages (toujours disponibles, prix modeste). */
    private static void onWandererTrades(WandererTradesEvent event) {
        event.getGenericTrades().add((trader, random) -> new MerchantOffer(
                new ItemCost(Items.EMERALD, 3), new ItemStack(RED_CUTTING.get()), 8, 1, 0.05f));
        event.getGenericTrades().add((trader, random) -> new MerchantOffer(
                new ItemCost(Items.EMERALD, 3), new ItemStack(WHITE_CUTTING.get()), 8, 1, 0.05f));
    }

    @Override
    public void registerLang(LangSink sink) {
        sink.add("block.terresvivantes.trellis", "Trellis", "Treillis");
        sink.add("block.terresvivantes.grapevine", "Grapevine", "Vigne");
        sink.add("item.terresvivantes.red_grapes", "Red Grapes", "Raisins rouges");
        sink.add("item.terresvivantes.white_grapes", "White Grapes", "Raisins blancs");
        sink.add("item.terresvivantes.red_grapevine_cutting", "Red Grapevine Cutting", "Bouture de vigne rouge");
        sink.add("item.terresvivantes.white_grapevine_cutting", "White Grapevine Cutting", "Bouture de vigne blanche");
        sink.add("item.terresvivantes.grape_juice", "Grape Juice", "Jus de raisin");
        sink.add("item.terresvivantes.young_wine", "Young Wine", "Vin jeune");
        sink.add("item.terresvivantes.red_wine", "Red Wine", "Vin rouge");
        sink.add("item.terresvivantes.white_wine", "White Wine", "Vin blanc");
        sink.add("tooltip.terresvivantes.vintage", "Vintage: Year %s", "Millésime : An %s");
    }

    @Override
    public void registerItemModels(ItemModelProvider provider) {
        // Treillis : même géométrie que le bloc, en modèle d'item autonome (ne dépend pas de l'ordre des providers).
        ModelBuilder<?> trellisItem = provider.getBuilder("item/trellis")
                .texture("t", texture("block/trellis"))
                .texture("particle", texture("block/trellis"))
                .renderType("minecraft:cutout");
        addTrellis(trellisItem);
        provider.basicItem(RED_GRAPES.get());
        provider.basicItem(WHITE_GRAPES.get());
        provider.withExistingParent("red_grapevine_cutting", ResourceLocation.withDefaultNamespace("item/generated"))
                .texture("layer0", texture("item/grapevine_cutting"));
        provider.withExistingParent("white_grapevine_cutting", ResourceLocation.withDefaultNamespace("item/generated"))
                .texture("layer0", texture("item/grapevine_cutting"));
        provider.basicItem(GRAPE_JUICE.get());
        provider.basicItem(YOUNG_WINE.get());
        provider.basicItem(RED_WINE.get());
        provider.basicItem(WHITE_WINE.get());
    }

    @Override
    public void registerBlockStates(BlockStateProvider provider) {
        BlockModelBuilder trellis = provider.models().getBuilder("trellis")
                .texture("t", texture("block/trellis"))
                .texture("particle", texture("block/trellis"));
        addTrellis(trellis);
        trellis.renderType("minecraft:cutout");
        provider.simpleBlock(TRELLIS.get(), trellis);

        // Vigne : treillis et croix de feuillage. Stades 0 à 2 partagent la texture; le stade 3 dépend du cépage.
        ModelFile stage0 = vineModel(provider, "grapevine_stage0", texture("block/grapevine_stage0"));
        ModelFile stage1 = vineModel(provider, "grapevine_stage1", texture("block/grapevine_stage1"));
        ModelFile stage2 = vineModel(provider, "grapevine_stage2", texture("block/grapevine_stage2"));
        ModelFile red3 = vineModel(provider, "grapevine_red_stage3", texture("block/red_grapevine_stage3"));
        ModelFile white3 = vineModel(provider, "grapevine_white_stage3", texture("block/white_grapevine_stage3"));
        provider.getVariantBuilder(GRAPEVINE.get()).forAllStates(state -> {
            int stage = state.getValue(GrapevineBlock.STAGE);
            ModelFile model = switch (stage) {
                case 0 -> stage0;
                case 1 -> stage1;
                case 2 -> stage2;
                default -> state.getValue(GrapevineBlock.VARIETY) == GrapeVariety.RED ? red3 : white3;
            };
            return ConfiguredModel.builder().modelFile(model).build();
        });
    }

    /** Modèle d'une vigne à un stade : treillis et croix de la texture donnée. */
    private static ModelFile vineModel(BlockStateProvider provider, String name, ResourceLocation vine) {
        BlockModelBuilder model = provider.models().getBuilder(name)
                .texture("t", texture("block/trellis"))
                .texture("v", vine)
                .texture("particle", texture("block/trellis"));
        addTrellis(model);
        addCross(model);
        model.renderType("minecraft:cutout");
        return model;
    }

    /** Deux montants et deux traverses du treillis (texture {@code #t}). */
    private static void addTrellis(ModelBuilder<?> model) {
        addBox(model, 0, 0, 7, 2, 16, 9, "#t");
        addBox(model, 14, 0, 7, 16, 16, 9, "#t");
        addBox(model, 0, 4, 7, 16, 6, 9, "#t");
        addBox(model, 0, 10, 7, 16, 12, 9, "#t");
    }

    /** Deux plans croisés au centre (texture {@code #v}), visibles des deux côtés. */
    private static void addCross(ModelBuilder<?> model) {
        model.element().from(0, 0, 8).to(16, 16, 8)
                .face(Direction.NORTH).texture("#v").end()
                .face(Direction.SOUTH).texture("#v").end()
                .end();
        model.element().from(8, 0, 0).to(8, 16, 16)
                .face(Direction.EAST).texture("#v").end()
                .face(Direction.WEST).texture("#v").end()
                .end();
    }

    /** Pavé à six faces avec la même texture. */
    private static void addBox(ModelBuilder<?> model, float x1, float y1, float z1, float x2, float y2, float z2,
                               String texture) {
        model.element().from(x1, y1, z1).to(x2, y2, z2)
                .face(Direction.DOWN).texture(texture).end()
                .face(Direction.UP).texture(texture).end()
                .face(Direction.NORTH).texture(texture).end()
                .face(Direction.SOUTH).texture(texture).end()
                .face(Direction.WEST).texture(texture).end()
                .face(Direction.EAST).texture(texture).end()
                .end();
    }

    private static ResourceLocation texture(String path) {
        return ResourceLocation.fromNamespaceAndPath(TerresVivantes.MODID, path);
    }

    @Override
    public void registerRecipes(RecipeOutput output, HolderLookup.Provider lookup) {
        // 6 bâtons donnent 3 treillis.
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, TRELLIS_ITEM.get(), 3)
                .pattern("S S")
                .pattern("S S")
                .pattern("S S")
                .define('S', Items.STICK)
                .unlockedBy("has_stick", InventoryChangeTrigger.TriggerInstance.hasItems(Items.STICK))
                .save(output);
    }

    @Override
    public void registerLootTables(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> output,
                                   HolderLookup.Provider lookup) {
        output.accept(lootKey("trellis"), LootTable.lootTable().withPool(LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1.0f))
                .add(LootItem.lootTableItem(TRELLIS.get()))
                .when(ExplosionCondition.survivesExplosion())));
        // La vigne rend ses drops par GrapevineBlock.getDrops (bouture au stade 2 ou plus, et treillis).
        output.accept(lootKey("grapevine"), LootTable.lootTable());
    }

    private static ResourceKey<LootTable> lootKey(String block) {
        return ResourceKey.create(Registries.LOOT_TABLE,
                ResourceLocation.fromNamespaceAndPath(TerresVivantes.MODID, "blocks/" + block));
    }
}
