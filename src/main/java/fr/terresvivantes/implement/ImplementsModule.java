package fr.terresvivantes.implement;

import fr.terresvivantes.TerresVivantes;
import fr.terresvivantes.core.TVModule;
import fr.terresvivantes.core.registry.TVCreativeTab;
import fr.terresvivantes.datagen.LangSink;
import fr.terresvivantes.implement.item.BroadforkItem;
import fr.terresvivantes.implement.item.DiamondSickleItem;
import fr.terresvivantes.implement.item.SeederItem;
import fr.terresvivantes.implement.logic.ImplementsLogic;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.tags.ItemTags;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Module Implements : grelinette, semoir et faucille en diamant. */
public final class ImplementsModule implements TVModule {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TerresVivantes.MODID);

    public static final DeferredItem<BroadforkItem> BROADFORK =
            ITEMS.register("broadfork", () -> new BroadforkItem(new Item.Properties().stacksTo(1)
                    .durability(ImplementsLogic.BROADFORK_DURABILITY)));
    public static final DeferredItem<SeederItem> SEEDER =
            ITEMS.register("seeder", () -> new SeederItem(new Item.Properties().stacksTo(1)
                    .durability(ImplementsLogic.SEEDER_DURABILITY)));
    public static final DeferredItem<DiamondSickleItem> DIAMOND_SICKLE =
            ITEMS.register("diamond_sickle", () -> new DiamondSickleItem(new Item.Properties().stacksTo(1)
                    .durability(ImplementsLogic.DIAMOND_SICKLE_DURABILITY)));

    @Override
    public void register(IEventBus modBus) {
        ITEMS.register(modBus);
        TVCreativeTab.add(BROADFORK);
        TVCreativeTab.add(SEEDER);
        TVCreativeTab.add(DIAMOND_SICKLE);
    }

    @Override
    public void registerLang(LangSink sink) {
        sink.add("item.terresvivantes.broadfork", "Broadfork", "Grelinette");
        sink.add("item.terresvivantes.seeder", "Seeder", "Semoir");
        sink.add("item.terresvivantes.diamond_sickle", "Diamond Sickle", "Faucille en diamant");
        sink.add("message.terresvivantes.seeder.no_seeds",
                "No seeds in the off-hand.", "Aucune graine dans la main secondaire.");
    }

    @Override
    public void registerItemModels(ItemModelProvider provider) {
        provider.basicItem(BROADFORK.get());
        provider.basicItem(SEEDER.get());
        provider.basicItem(DIAMOND_SICKLE.get());
    }

    @Override
    public void registerRecipes(RecipeOutput output, HolderLookup.Provider lookup) {
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, BROADFORK.get())
                .pattern("III")
                .pattern(" S ")
                .pattern(" S ")
                .define('I', Items.IRON_INGOT)
                .define('S', Items.STICK)
                .unlockedBy("has_iron", InventoryChangeTrigger.TriggerInstance.hasItems(Items.IRON_INGOT))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, SEEDER.get())
                .pattern("PPP")
                .pattern(" C ")
                .pattern("I I")
                .define('P', ItemTags.PLANKS)
                .define('C', Items.CHEST)
                .define('I', Items.IRON_INGOT)
                .unlockedBy("has_iron", InventoryChangeTrigger.TriggerInstance.hasItems(Items.IRON_INGOT))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, DIAMOND_SICKLE.get())
                .pattern("DD")
                .pattern(" S")
                .pattern("D ")
                .define('D', Items.DIAMOND)
                .define('S', Items.STICK)
                .unlockedBy("has_diamond", InventoryChangeTrigger.TriggerInstance.hasItems(Items.DIAMOND))
                .save(output);
    }
}
