package fr.terresvivantes.processing;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

import java.util.List;

/**
 * Entrée d'une recherche de recette : machine, stacks des entrées (références vivantes de l'inventaire) et stack du
 * conteneur ({@link ItemStack#EMPTY} s'il n'y en a pas). L'index 0..n-1 désigne les entrées, n le conteneur.
 */
public record ProcessingRecipeInput(String machine, List<ItemStack> inputs, ItemStack container) implements RecipeInput {

    @Override
    public ItemStack getItem(int index) {
        return index < inputs.size() ? inputs.get(index) : container;
    }

    @Override
    public int size() {
        return inputs.size() + 1;
    }
}
