package fr.terresvivantes.datagen;

import fr.terresvivantes.TerresVivantes;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;

import java.util.concurrent.CompletableFuture;

/** Recettes du mod, agregees depuis les hooks des modules. */
public final class TVRecipeProvider extends RecipeProvider {

    public TVRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup) {
        super(output, lookup);
    }

    @Override
    protected void buildRecipes(RecipeOutput output, HolderLookup.Provider lookup) {
        for (var module : TerresVivantes.modules()) {
            module.registerRecipes(output, lookup);
        }
    }
}
