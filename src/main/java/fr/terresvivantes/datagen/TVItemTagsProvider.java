package fr.terresvivantes.datagen;

import fr.terresvivantes.TerresVivantes;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.world.level.block.Block;

import java.util.concurrent.CompletableFuture;

/** Provider des tags d'items : interroge le hook {@code registerItemTags} de chaque module. */
public final class TVItemTagsProvider extends ItemTagsProvider {
    public TVItemTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup,
                              CompletableFuture<TagsProvider.TagLookup<Block>> blockTags) {
        super(output, lookup, blockTags);
    }

    @Override
    protected void addTags(HolderLookup.Provider lookup) {
        for (var module : TerresVivantes.modules()) {
            module.registerItemTags(this::tag, lookup);
        }
    }
}
