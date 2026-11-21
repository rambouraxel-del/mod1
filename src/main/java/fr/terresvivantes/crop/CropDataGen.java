package fr.terresvivantes.crop;

import fr.terresvivantes.crop.logic.CropDefinition;
import fr.terresvivantes.crop.logic.CropDefinitions;
import fr.terresvivantes.crop.logic.CropLogic;
import fr.terresvivantes.datagen.LangSink;
import net.minecraft.advancements.critereon.StatePropertiesPredicate;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

/** Hooks datagen du module crop : langue, modèles, blockstates, loot et tags. */
final class CropDataGen {

    /** Noms affichés des cultures, en anglais et en français. */
    private record Names(String id, String en, String fr, String frSeeds, String frCrop) {
    }

    private static final List<Names> NAMES = List.of(
            new Names("tomato", "Tomato", "Tomate", "Graines de tomate", "Culture de tomate"),
            new Names("corn", "Corn", "Maïs", "Graines de maïs", "Culture de maïs"),
            new Names("soybean", "Soybean", "Soja", "Graines de soja", "Culture de soja"),
            new Names("lettuce", "Lettuce", "Laitue", "Graines de laitue", "Culture de laitue"),
            new Names("onion", "Onion", "Oignon", "Graines d'oignon", "Culture d'oignon"),
            new Names("garlic", "Garlic", "Ail", "Graines d'ail", "Culture d'ail"),
            new Names("bell_pepper", "Bell Pepper", "Poivron", "Graines de poivron", "Culture de poivron"),
            new Names("eggplant", "Eggplant", "Aubergine", "Graines d'aubergine", "Culture d'aubergine"),
            new Names("strawberry", "Strawberry", "Fraise", "Graines de fraise", "Culture de fraise"),
            new Names("barley", "Barley", "Orge", "Graines d'orge", "Culture d'orge"),
            new Names("cabbage", "Cabbage", "Chou", "Graines de chou", "Culture de chou"),
            new Names("cucumber", "Cucumber", "Concombre", "Graines de concombre", "Culture de concombre"));

    private CropDataGen() {
    }

    static void registerLang(LangSink sink) {
        for (Names names : NAMES) {
            sink.add("item.terresvivantes." + names.id(), names.en(), names.fr());
            sink.add("item.terresvivantes." + names.id() + "_seeds", names.en() + " Seeds", names.frSeeds());
            sink.add("block.terresvivantes." + names.id() + "_crop", names.en() + " Crop", names.frCrop());
        }
    }

    static void registerItemModels(ItemModelProvider provider) {
        for (CropDefinition def : CropDefinitions.all()) {
            provider.basicItem(CropModule.productItem(def.id()));
            provider.basicItem(CropModule.seedItem(def.id()));
        }
    }

    /** Un modèle par stade visuel, basé sur le modèle vanilla {@code block/crop}. */
    static void registerBlockStates(BlockStateProvider provider) {
        for (CropDefinition def : CropDefinitions.all()) {
            String id = def.id();
            ModelFile[] stages = new ModelFile[4];
            for (int stage = 0; stage < stages.length; stage++) {
                stages[stage] = provider.models()
                        .withExistingParent("block/" + id + "_crop_stage" + stage,
                                ResourceLocation.withDefaultNamespace("block/crop"))
                        .texture("crop", provider.modLoc("block/" + id + "_stage" + stage))
                        .renderType("minecraft:cutout");
            }
            provider.getVariantBuilder(CropModule.cropBlock(id)).forAllStates(state -> ConfiguredModel.builder()
                    .modelFile(stages[CropLogic.stageForAge(state.getValue(CropBlock.AGE))])
                    .build());
        }
    }

    /**
     * Mûre : produit (rendement) et 1 à 2 graines. Non mûre : 1 graine.
     */
    static void registerLootTables(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> output) {
        for (CropDefinition def : CropDefinitions.all()) {
            String id = def.id();
            Block block = CropModule.cropBlock(id);
            LootItemCondition.Builder mature = LootItemBlockStatePropertyCondition.hasBlockStateProperties(block)
                    .setProperties(StatePropertiesPredicate.Builder.properties()
                            .hasProperty(CropBlock.AGE, CropLogic.MAX_AGE));
            LootItemCondition.Builder immature = mature.invert();
            LootTable.Builder table = LootTable.lootTable()
                    .withPool(LootPool.lootPool().when(mature).add(LootItem.lootTableItem(CropModule.productItem(id))
                            .apply(SetItemCountFunction.setCount(UniformGenerator.between(def.yieldMin(), def.yieldMax())))))
                    .withPool(LootPool.lootPool().when(mature).add(LootItem.lootTableItem(CropModule.seedItem(id))
                            .apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 2)))))
                    .withPool(LootPool.lootPool().when(immature).add(LootItem.lootTableItem(CropModule.seedItem(id))));
            output.accept(block.getLootTable(), table);
        }
    }

    /** Les cultures du mod sont des cultures vanilla pour les mécaniques de plantation et de conservation du sol. */
    static void registerBlockTags(Function<TagKey<Block>, TagsProvider.TagAppender<Block>> tags) {
        for (CropDefinition def : CropDefinitions.all()) {
            ResourceKey<Block> key = CropModule.cropHolder(def.id()).getKey();
            tags.apply(BlockTags.CROPS).add(key);
            tags.apply(BlockTags.MAINTAINS_FARMLAND).add(key);
        }
    }
}
