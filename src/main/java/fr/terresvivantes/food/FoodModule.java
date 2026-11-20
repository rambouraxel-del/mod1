package fr.terresvivantes.food;

import fr.terresvivantes.TerresVivantes;
import fr.terresvivantes.core.TVModule;
import fr.terresvivantes.core.registry.TVCreativeTab;
import fr.terresvivantes.datagen.LangSink;
import fr.terresvivantes.food.logic.FoodLogic;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.List;

/** Module Food : les 10 plats de la v0.1. Recettes dans data/terresvivantes/recipe (JSON). */
public final class FoodModule implements TVModule {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TerresVivantes.MODID);
    private static final List<DeferredItem<Item>> DISHES = new ArrayList<>();

    static {
        for (FoodLogic.Dish dish : FoodLogic.DISHES) {
            DISHES.add(ITEMS.register(dish.id(), () -> new Item(properties(dish))));
        }
    }

    @Override
    public void register(IEventBus modBus) {
        ITEMS.register(modBus);
        for (DeferredItem<Item> dish : DISHES) {
            TVCreativeTab.add(dish);
        }
    }

    @Override
    public void registerLang(LangSink sink) {
        sink.add("item.terresvivantes.tomato_soup", "Tomato Soup", "Soupe de tomate");
        sink.add("item.terresvivantes.garden_salad", "Garden Salad", "Salade du jardin");
        sink.add("item.terresvivantes.ratatouille", "Ratatouille", "Ratatouille");
        sink.add("item.terresvivantes.vegetable_stew", "Vegetable Stew", "Ragoût de légumes");
        sink.add("item.terresvivantes.corn_bread", "Corn Bread", "Pain de maïs");
        sink.add("item.terresvivantes.garlic_bread", "Garlic Bread", "Pain à l'ail");
        sink.add("item.terresvivantes.barley_bread", "Barley Bread", "Pain d'orge");
        sink.add("item.terresvivantes.grilled_corn", "Grilled Corn", "Maïs grillé");
        sink.add("item.terresvivantes.stuffed_pepper", "Stuffed Pepper", "Poivron farci");
        sink.add("item.terresvivantes.strawberry_jam", "Strawberry Jam", "Confiture de fraises");
    }

    @Override
    public void registerItemModels(ItemModelProvider provider) {
        for (DeferredItem<Item> dish : DISHES) {
            provider.basicItem(dish.get());
        }
    }

    private static Item.Properties properties(FoodLogic.Dish dish) {
        FoodProperties.Builder food = new FoodProperties.Builder()
                .nutrition(dish.nutrition())
                .saturationModifier(dish.saturation());
        if (dish.regeneration()) {
            food.effect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0), 1.0f);
        }
        Item.Properties props = new Item.Properties().food(food.build());
        if (dish.stacksToOne()) {
            props.stacksTo(1);
        }
        switch (dish.vessel()) {
            case BOWL -> props.craftRemainder(Items.BOWL);
            case GLASS_BOTTLE -> props.craftRemainder(Items.GLASS_BOTTLE);
            case NONE -> {
            }
        }
        return props;
    }
}
