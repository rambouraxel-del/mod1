package fr.terresvivantes.processing;

import fr.terresvivantes.core.quality.QualityModule;
import fr.terresvivantes.core.quality.logic.QualityLogic;
import fr.terresvivantes.processing.logic.ProcessingLogic;
import fr.terresvivantes.processing.logic.QualityMode;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.List;
import java.util.Optional;

/**
 * Recette de transformation (type {@code terresvivantes:processing}, données dans {@code data/terresvivantes/recipe/}).
 * Une recette appartient à une machine ({@link MachineType#id()}), consomme des ingrédients et, si elle en a un, un
 * item de conteneur (1 unité). La qualité de la sortie suit son {@link QualityMode}.
 */
public final class ProcessingRecipe implements Recipe<ProcessingRecipeInput> {

    /** Un ingrédient : son {@link Ingredient} et la quantité consommée. */
    public record IngredientEntry(Ingredient ingredient, int count) {
    }

    private final String machine;
    private final List<IngredientEntry> ingredients;
    private final Optional<Item> container;
    private final ItemStack result;
    private final int time;
    private final QualityMode quality;

    public ProcessingRecipe(String machine, List<IngredientEntry> ingredients, Optional<Item> container,
                            ItemStack result, int time, QualityMode quality) {
        this.machine = machine;
        this.ingredients = List.copyOf(ingredients);
        this.container = container;
        this.result = result;
        this.time = time;
        this.quality = quality;
    }

    public String machine() {
        return machine;
    }

    public List<IngredientEntry> ingredients() {
        return ingredients;
    }

    public Optional<Item> container() {
        return container;
    }

    public ItemStack result() {
        return result.copy();
    }

    /** Durée en ticks de progression. */
    public int time() {
        return time;
    }

    public QualityMode quality() {
        return quality;
    }

    /**
     * Slot d'entrée fournissant chaque ingrédient, ou {@code null} si la recette ne peut pas être faite avec cette
     * entrée (mauvaise machine, conteneur absent, ingrédients manquants).
     */
    int[] plan(ProcessingRecipeInput input) {
        if (!machine.equals(input.machine())) {
            return null;
        }
        if (container.isPresent()) {
            ItemStack stack = input.container();
            if (stack.isEmpty() || !stack.is(container.get())) {
                return null;
            }
        }
        int slots = input.inputs().size();
        int n = ingredients.size();
        boolean[][] canUse = new boolean[n][slots];
        int[] stock = new int[slots];
        int[] need = new int[n];
        for (int slot = 0; slot < slots; slot++) {
            ItemStack stack = input.inputs().get(slot);
            stock[slot] = stack.getCount();
            for (int i = 0; i < n; i++) {
                canUse[i][slot] = !stack.isEmpty() && ingredients.get(i).ingredient().test(stack);
            }
        }
        for (int i = 0; i < n; i++) {
            need[i] = ingredients.get(i).count();
        }
        return ProcessingLogic.allocate(canUse, stock, need);
    }

    @Override
    public boolean matches(ProcessingRecipeInput input, net.minecraft.world.level.Level level) {
        return plan(input) != null;
    }

    /** Sortie de la recette avec sa qualité, sans consommer les entrées. Vide si la recette ne s'applique pas. */
    @Override
    public ItemStack assemble(ProcessingRecipeInput input, HolderLookup.Provider registries) {
        int[] slots = plan(input);
        if (slots == null) {
            return ItemStack.EMPTY;
        }
        int[] qualities = new int[slots.length];
        for (int i = 0; i < slots.length; i++) {
            qualities[i] = QualityModule.qualityOf(input.inputs().get(slots[i]));
        }
        ItemStack out = result.copy();
        int outQuality = ProcessingLogic.outputQuality(quality, qualities);
        if (outQuality != QualityLogic.ABSENT) {
            QualityModule.setQuality(out, outQuality);
        }
        return out;
    }

    /**
     * Fait la recette : renvoie la sortie et retire les ingrédients (et le conteneur) de l'entrée. Les stacks retirés
     * sont réduits en place; l'appelant nettoie les stacks vides. Vide si la recette ne s'applique pas.
     */
    public ItemStack craft(ProcessingRecipeInput input, HolderLookup.Provider registries) {
        int[] slots = plan(input);
        if (slots == null) {
            return ItemStack.EMPTY;
        }
        ItemStack out = assemble(input, registries);
        for (int i = 0; i < slots.length; i++) {
            input.inputs().get(slots[i]).shrink(ingredients.get(i).count());
        }
        if (container.isPresent()) {
            input.container().shrink(1);
        }
        return out;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return result.copy();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ProcessingModule.SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ProcessingModule.RECIPE_TYPE.get();
    }
}
