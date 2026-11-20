package fr.terresvivantes.tools;

import fr.terresvivantes.TerresVivantes;
import fr.terresvivantes.core.TVModule;
import fr.terresvivantes.core.registry.TVCreativeTab;
import fr.terresvivantes.datagen.LangSink;
import fr.terresvivantes.tools.item.AlmanacItem;
import fr.terresvivantes.tools.item.SickleItem;
import fr.terresvivantes.tools.item.SoilProbeItem;
import fr.terresvivantes.tools.item.WateringCanItem;
import fr.terresvivantes.tools.logic.ToolsLogic;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Module Tools : sonde a sol, almanach, faucille et arrosoir. */
public final class ToolsModule implements TVModule {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TerresVivantes.MODID);

    public static final DeferredItem<SoilProbeItem> SOIL_PROBE =
            ITEMS.register("soil_probe", () -> new SoilProbeItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<AlmanacItem> ALMANAC =
            ITEMS.register("almanac", () -> new AlmanacItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<SickleItem> SICKLE =
            ITEMS.register("sickle", () -> new SickleItem(new Item.Properties().stacksTo(1)
                    .durability(ToolsLogic.SICKLE_DURABILITY)));
    public static final DeferredItem<WateringCanItem> WATERING_CAN =
            ITEMS.register("watering_can", () -> new WateringCanItem(new Item.Properties().stacksTo(1), false));
    public static final DeferredItem<WateringCanItem> WATERING_CAN_FILLED =
            ITEMS.register("watering_can_filled", () -> new WateringCanItem(
                    new Item.Properties().stacksTo(1).durability(ToolsLogic.WATERING_CAN_USES), true));

    @Override
    public void register(IEventBus modBus) {
        ITEMS.register(modBus);
        TVCreativeTab.add(SOIL_PROBE);
        TVCreativeTab.add(ALMANAC);
        TVCreativeTab.add(SICKLE);
        TVCreativeTab.add(WATERING_CAN);
        TVCreativeTab.add(WATERING_CAN_FILLED);
    }

    @Override
    public void registerLang(LangSink sink) {
        sink.add("item.terresvivantes.soil_probe", "Soil Probe", "Sonde à sol");
        sink.add("item.terresvivantes.almanac", "Almanac", "Almanach");
        sink.add("item.terresvivantes.sickle", "Sickle", "Faucille");
        sink.add("item.terresvivantes.watering_can", "Watering Can", "Arrosoir");
        sink.add("item.terresvivantes.watering_can_filled", "Watering Can (Filled)", "Arrosoir (plein)");

        sink.add("message.terresvivantes.soil_probe.nitrogen", "Nitrogen (N): %s", "Azote (N) : %s");
        sink.add("message.terresvivantes.soil_probe.phosphorus", "Phosphorus (P): %s", "Phosphore (P) : %s");
        sink.add("message.terresvivantes.soil_probe.potassium", "Potassium (K): %s", "Potassium (K) : %s");
        sink.add("message.terresvivantes.soil_probe.organic", "Organic matter: %s", "Matière organique : %s");
        sink.add("message.terresvivantes.soil_probe.moisture", "Moisture: %s / %s", "Humidité : %s / %s");

        sink.add("message.terresvivantes.almanac.date", "%s, day %s of %s", "%s, jour %s sur %s");
        sink.add("message.terresvivantes.almanac.temperature", "Local temperature: %s °C", "Température locale : %s °C");
        sink.add("message.terresvivantes.almanac.crops", "Best to plant now: %s", "À planter maintenant : %s");
        sink.add("message.terresvivantes.almanac.no_crops", "No crop is ideal this season.",
                "Aucune culture idéale cette saison.");
    }

    @Override
    public void registerItemModels(ItemModelProvider provider) {
        provider.basicItem(SOIL_PROBE.get());
        provider.basicItem(ALMANAC.get());
        provider.basicItem(SICKLE.get());
        provider.basicItem(WATERING_CAN.get());
        provider.basicItem(WATERING_CAN_FILLED.get());
    }

    @Override
    public void registerRecipes(RecipeOutput output, HolderLookup.Provider lookup) {
        ShapelessRecipeBuilder.shapeless(RecipeCategory.TOOLS, SOIL_PROBE.get())
                .requires(Items.STICK)
                .requires(Items.IRON_NUGGET)
                .unlockedBy("has_stick", InventoryChangeTrigger.TriggerInstance.hasItems(Items.STICK))
                .save(output);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.TOOLS, ALMANAC.get())
                .requires(Items.BOOK)
                .requires(Items.WHEAT)
                .unlockedBy("has_book", InventoryChangeTrigger.TriggerInstance.hasItems(Items.BOOK))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, SICKLE.get())
                .pattern("II")
                .pattern(" S")
                .define('I', Items.IRON_INGOT)
                .define('S', Items.STICK)
                .unlockedBy("has_iron", InventoryChangeTrigger.TriggerInstance.hasItems(Items.IRON_INGOT))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, WATERING_CAN.get())
                .pattern("  I")
                .pattern("IBI")
                .define('I', Items.IRON_INGOT)
                .define('B', Items.BUCKET)
                .unlockedBy("has_iron", InventoryChangeTrigger.TriggerInstance.hasItems(Items.IRON_INGOT))
                .save(output);
    }
}
