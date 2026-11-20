package fr.terresvivantes.core;

import fr.terresvivantes.datagen.LangSink;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.data.tags.TagsProvider;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;

import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Contrat d'un module. Chaque module est un package avec son DeferredRegister
 * et ses hooks datagen; il n'edite jamais de fichier partage.
 * Les hooks par defaut ne font rien.
 */
public interface TVModule {

    /** Enregistre les DeferredRegister du module sur le bus mod. */
    void register(IEventBus modBus);

    /** Ajoute les traductions du module. Chaque appel fournit en_us et fr_fr. */
    default void registerLang(LangSink sink) {
    }

    /** Declare les modeles d'items du module. */
    default void registerItemModels(ItemModelProvider provider) {
    }

    /** Declare les blockstates et modeles de blocs du module. */
    default void registerBlockStates(BlockStateProvider provider) {
    }

    /** Ajoute les recettes du module. */
    default void registerRecipes(RecipeOutput output, HolderLookup.Provider lookup) {
    }

    /** Ajoute les loot tables de blocs du module. */
    default void registerLootTables(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> output,
                                    HolderLookup.Provider lookup) {
    }

    /** Ajoute les tags de blocs du module via la fonction fournie par le provider. */
    default void registerBlockTags(Function<TagKey<Block>, TagsProvider.TagAppender<Block>> tags,
                                   HolderLookup.Provider lookup) {
    }

    /** Ajoute les tags d'items du module via la fonction fournie par le provider. */
    default void registerItemTags(Function<TagKey<Item>, TagsProvider.TagAppender<Item>> tags,
                                  HolderLookup.Provider lookup) {
    }
}
