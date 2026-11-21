package fr.terresvivantes.datagen;

import fr.terresvivantes.TerresVivantes;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import java.util.concurrent.CompletableFuture;

public final class TVBlockTagsProvider extends BlockTagsProvider {
    public TVBlockTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup,
                               ExistingFileHelper helper) {
        super(output, lookup, TerresVivantes.MODID, helper);
    }

    @Override
    protected void addTags(HolderLookup.Provider lookup) {
        for (var module : TerresVivantes.modules()) {
            module.registerBlockTags(this::tag, lookup);
        }
    }
}
