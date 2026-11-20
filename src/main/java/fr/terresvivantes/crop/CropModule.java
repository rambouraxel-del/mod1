package fr.terresvivantes.crop;

import com.mojang.serialization.MapCodec;
import fr.terresvivantes.TerresVivantes;
import fr.terresvivantes.core.TVModule;
import fr.terresvivantes.core.registry.TVCreativeTab;
import fr.terresvivantes.crop.logic.CropDefinition;
import fr.terresvivantes.crop.logic.CropDefinitions;
import fr.terresvivantes.datagen.LangSink;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Module Crop : 12 cultures évoluant par randomTick, sans BlockEntity; graines; intégration vanilla.
 */
public final class CropModule implements TVModule {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(TerresVivantes.MODID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TerresVivantes.MODID);
    private static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> LOOT_MODIFIERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, TerresVivantes.MODID);

    private static final Map<String, DeferredBlock<TVCropBlock>> CROP_BLOCKS = new LinkedHashMap<>();
    private static final Map<String, DeferredItem<Item>> SEEDS = new LinkedHashMap<>();
    private static final Map<String, DeferredItem<Item>> PRODUCTS = new LinkedHashMap<>();

    static {
        FoodProperties food = new FoodProperties.Builder().nutrition(1).saturationModifier(0.1f).build();
        for (CropDefinition def : CropDefinitions.all()) {
            String id = def.id();
            PRODUCTS.put(id, ITEMS.registerSimpleItem(id, new Item.Properties().food(food)));
            SEEDS.put(id, ITEMS.register(id + "_seeds",
                    () -> new ItemNameBlockItem(cropBlock(id), new Item.Properties())));
            CROP_BLOCKS.put(id, BLOCKS.register(id + "_crop",
                    () -> new TVCropBlock(def, () -> seedItem(id), BlockBehaviour.Properties.ofFullCopy(Blocks.WHEAT))));
        }
    }

    /** Bloc de culture du mod (sans BlockItem). */
    public static Block cropBlock(String id) {
        return CROP_BLOCKS.get(id).get();
    }

    /** Graine d'une culture. */
    public static Item seedItem(String id) {
        return SEEDS.get(id).get();
    }

    /** Produit comestible d'une culture. */
    public static Item productItem(String id) {
        return PRODUCTS.get(id).get();
    }

    static DeferredBlock<TVCropBlock> cropHolder(String id) {
        return CROP_BLOCKS.get(id);
    }

    static DeferredItem<Item> seedHolder(String id) {
        return SEEDS.get(id);
    }

    static DeferredItem<Item> productHolder(String id) {
        return PRODUCTS.get(id);
    }

    @Override
    public void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        LOOT_MODIFIERS.register(modBus);
        LOOT_MODIFIERS.register("grass_seeds", () -> CropSeedLootModifier.CODEC);
        CropEvents.register();
        for (CropDefinition def : CropDefinitions.all()) {
            TVCreativeTab.add(productHolder(def.id()));
            TVCreativeTab.add(seedHolder(def.id()));
        }
    }

    @Override
    public void registerLang(LangSink sink) {
        CropDataGen.registerLang(sink);
    }

    @Override
    public void registerItemModels(ItemModelProvider provider) {
        CropDataGen.registerItemModels(provider);
    }

    @Override
    public void registerBlockStates(BlockStateProvider provider) {
        CropDataGen.registerBlockStates(provider);
    }

    @Override
    public void registerLootTables(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> output,
                                   HolderLookup.Provider lookup) {
        CropDataGen.registerLootTables(output);
    }

    @Override
    public void registerBlockTags(Function<TagKey<Block>, TagsProvider.TagAppender<Block>> tags,
                                  HolderLookup.Provider lookup) {
        CropDataGen.registerBlockTags(tags);
    }
}
