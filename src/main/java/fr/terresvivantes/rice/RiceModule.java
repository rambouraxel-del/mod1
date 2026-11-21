package fr.terresvivantes.rice;

import com.mojang.serialization.MapCodec;
import fr.terresvivantes.TerresVivantes;
import fr.terresvivantes.core.TVModule;
import fr.terresvivantes.core.registry.TVCreativeTab;
import fr.terresvivantes.datagen.LangSink;
import fr.terresvivantes.rice.logic.RiceLogic;
import net.minecraft.advancements.critereon.StatePropertiesPredicate;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.BiConsumer;

/**
 * Module rice (v0.9) : la rizière (culture aquatique), le riz et ses produits (panicule, risotto, bol de riz,
 * gâteau de riz). Les recettes de meule, de marmite et de cuisson sont des fichiers JSON {@code rice_*}.
 */
public final class RiceModule implements TVModule {

    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(TerresVivantes.MODID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TerresVivantes.MODID);
    private static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> LOOT_MODIFIERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, TerresVivantes.MODID);

    /** Nourriture crue du riz : comparable aux produits des cultures. */
    private static final FoodProperties RICE_FOOD = new FoodProperties.Builder().nutrition(1).saturationModifier(0.1f).build();
    private static final FoodProperties RISOTTO_FOOD = new FoodProperties.Builder().nutrition(10).saturationModifier(0.9f).build();
    private static final FoodProperties RICE_BOWL_FOOD = new FoodProperties.Builder().nutrition(6).saturationModifier(0.6f).build();
    private static final FoodProperties RICE_CAKE_FOOD = new FoodProperties.Builder().nutrition(4).saturationModifier(0.5f).build();

    public static final DeferredItem<Item> RICE = ITEMS.registerSimpleItem("rice",
            new Item.Properties().food(RICE_FOOD));
    public static final DeferredItem<Item> RICE_PANICLE = ITEMS.registerSimpleItem("rice_panicle");
    public static final DeferredItem<Item> RICE_SEEDLING = ITEMS.registerSimpleItem("rice_seedling");
    public static final DeferredItem<Item> RISOTTO = ITEMS.register("risotto",
            () -> new RiceBowlItem(new Item.Properties().stacksTo(1).food(RISOTTO_FOOD), Items.BOWL));
    public static final DeferredItem<Item> RICE_BOWL = ITEMS.register("rice_bowl",
            () -> new RiceBowlItem(new Item.Properties().stacksTo(1).food(RICE_BOWL_FOOD), Items.BOWL));
    public static final DeferredItem<Item> RICE_CAKE = ITEMS.registerSimpleItem("rice_cake",
            new Item.Properties().food(RICE_CAKE_FOOD));

    public static final DeferredBlock<RiceCropBlock> RICE_CROP = BLOCKS.register("rice_crop",
            () -> new RiceCropBlock(RICE_SEEDLING, BlockBehaviour.Properties.ofFullCopy(Blocks.WHEAT)));

    @Override
    public void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        LOOT_MODIFIERS.register(modBus);
        LOOT_MODIFIERS.register("rice_seedling_grass", () -> RiceSeedLootModifier.CODEC);
        RiceEvents.register();

        TVCreativeTab.add(RICE);
        TVCreativeTab.add(RICE_PANICLE);
        TVCreativeTab.add(RICE_SEEDLING);
        TVCreativeTab.add(RISOTTO);
        TVCreativeTab.add(RICE_BOWL);
        TVCreativeTab.add(RICE_CAKE);
    }

    @Override
    public void registerLang(LangSink sink) {
        sink.add("block.terresvivantes.rice_crop", "Rice Crop", "Rizière");
        sink.add("item.terresvivantes.rice", "Rice", "Riz");
        sink.add("item.terresvivantes.rice_panicle", "Rice Panicle", "Panicule de riz");
        sink.add("item.terresvivantes.rice_seedling", "Rice Seedling", "Plant de riz");
        sink.add("item.terresvivantes.risotto", "Risotto", "Risotto");
        sink.add("item.terresvivantes.rice_bowl", "Rice Bowl", "Bol de riz");
        sink.add("item.terresvivantes.rice_cake", "Rice Cake", "Galette de riz");
    }

    @Override
    public void registerItemModels(ItemModelProvider provider) {
        provider.basicItem(RICE.get());
        provider.basicItem(RICE_PANICLE.get());
        provider.basicItem(RICE_SEEDLING.get());
        provider.basicItem(RISOTTO.get());
        provider.basicItem(RICE_BOWL.get());
        provider.basicItem(RICE_CAKE.get());
    }

    /** Un modèle par stade visuel, basé sur le modèle vanilla {@code block/crop} (cutout). L'eau est rendue par le fluide. */
    @Override
    public void registerBlockStates(BlockStateProvider provider) {
        ModelFile[] stages = new ModelFile[4];
        for (int stage = 0; stage < stages.length; stage++) {
            stages[stage] = provider.models()
                    .withExistingParent("block/rice_crop_stage" + stage,
                            ResourceLocation.withDefaultNamespace("block/crop"))
                    .texture("crop", provider.modLoc("block/rice_stage" + stage))
                    .renderType("minecraft:cutout");
        }
        provider.getVariantBuilder(RICE_CROP.get()).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(stages[RiceLogic.stageForAge(state.getValue(CropBlock.AGE))])
                .build());
    }

    /**
     * Mûre : 2 à 3 panicules. Toujours : 1 plant de riz (récolte, casse). Le bloc casse en plant de riz.
     */
    @Override
    public void registerLootTables(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> output,
                                   HolderLookup.Provider lookup) {
        var block = RICE_CROP.get();
        LootItemCondition.Builder mature = LootItemBlockStatePropertyCondition.hasBlockStateProperties(block)
                .setProperties(StatePropertiesPredicate.Builder.properties()
                        .hasProperty(CropBlock.AGE, RiceLogic.MAX_AGE));
        LootTable.Builder table = LootTable.lootTable()
                .withPool(LootPool.lootPool().when(mature).add(LootItem.lootTableItem(RICE_PANICLE.get())
                        .apply(SetItemCountFunction.setCount(
                                UniformGenerator.between(RiceLogic.PANICLES_MIN, RiceLogic.PANICLES_MAX)))))
                .withPool(LootPool.lootPool().add(LootItem.lootTableItem(RICE_SEEDLING.get())));
        output.accept(block.getLootTable(), table);
    }
}
