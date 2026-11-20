package fr.terresvivantes.datagen;

import fr.terresvivantes.TerresVivantes;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Point d'entree datagen. Chaque provider interroge tous les modules via leurs hooks;
 * les modules n'editent jamais ce fichier.
 */
public final class TVDataGen {
    private TVDataGen() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(TVDataGen::onGatherData);
    }

    private static void onGatherData(GatherDataEvent event) {
        var generator = event.getGenerator();
        PackOutput output = generator.getPackOutput();
        ExistingFileHelper helper = event.getExistingFileHelper();
        CompletableFuture<HolderLookup.Provider> lookup = event.getLookupProvider();
        boolean client = event.includeClient();
        boolean server = event.includeServer();

        generator.addProvider(client, new TVLanguageProvider(output, false));
        generator.addProvider(client, new TVLanguageProvider(output, true));
        generator.addProvider(client, new TVItemModelProvider(output, helper));
        generator.addProvider(client, new TVBlockStateProvider(output, helper));

        generator.addProvider(server, new TVRecipeProvider(output, lookup));
        generator.addProvider(server, new LootTableProvider(output, Set.of(),
                List.of(new LootTableProvider.SubProviderEntry(TVBlockLoot::new, LootContextParamSets.BLOCK)),
                lookup));
        generator.addProvider(server, new TVBlockTagsProvider(output, lookup, helper));
    }
}
