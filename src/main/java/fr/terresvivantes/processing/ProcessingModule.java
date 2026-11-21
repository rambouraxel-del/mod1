package fr.terresvivantes.processing;

import fr.terresvivantes.TerresVivantes;
import fr.terresvivantes.core.TVModule;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Infrastructure de transformation (v0.5) : type de recette {@code terresvivantes:processing}, son sérialiseur, le menu
 * générique et son écran. Ne possède aucune machine : chaque module déclare la sienne avec {@link ProcessingApi}.
 */
public final class ProcessingModule implements TVModule {

    private static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, TerresVivantes.MODID);
    private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, TerresVivantes.MODID);
    private static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, TerresVivantes.MODID);

    public static final DeferredHolder<RecipeType<?>, RecipeType<ProcessingRecipe>> RECIPE_TYPE =
            RECIPE_TYPES.register("processing", () -> new RecipeType<ProcessingRecipe>() {
                @Override
                public String toString() {
                    return TerresVivantes.MODID + ":processing";
                }
            });

    public static final DeferredHolder<RecipeSerializer<?>, ProcessingRecipeSerializer> SERIALIZER =
            SERIALIZERS.register("processing", ProcessingRecipeSerializer::new);

    public static final DeferredHolder<MenuType<?>, MenuType<ProcessingMenu>> MENU =
            MENUS.register("processing", () -> IMenuTypeExtension.create(ProcessingMenu::fromNetwork));

    @Override
    public void register(IEventBus modBus) {
        RECIPE_TYPES.register(modBus);
        SERIALIZERS.register(modBus);
        MENUS.register(modBus);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            fr.terresvivantes.processing.client.ProcessingClient.register(modBus);
        }
    }
}
