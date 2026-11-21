package fr.terresvivantes.guide;

import com.mojang.serialization.Codec;
import fr.terresvivantes.TerresVivantes;
import fr.terresvivantes.core.TVModule;
import fr.terresvivantes.core.registry.TVCreativeTab;
import fr.terresvivantes.datagen.LangSink;
import fr.terresvivantes.guide.item.FarmersManualItem;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

/**
 * Module guide (Manuel du fermier) : item qui ouvre un livre en jeu, contenu data-driven dans
 * {@code assets/terresvivantes/guide/<lang>/chapters/}, remise à la première connexion.
 * Aucune dépendance externe. Logique pure : {@link fr.terresvivantes.guide.logic.GuideLogic}.
 */
public final class GuideModule implements TVModule {

    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TerresVivantes.MODID);
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, TerresVivantes.MODID);

    public static final DeferredItem<FarmersManualItem> FARMERS_MANUAL =
            ITEMS.register("farmers_manual", () -> new FarmersManualItem(new Item.Properties().stacksTo(1)));

    /** Vrai une fois le manuel remis au joueur. Copié à la mort : pas de seconde remise. */
    public static final Supplier<AttachmentType<Boolean>> MANUAL_GIVEN = ATTACHMENTS.register("manual_given",
            () -> AttachmentType.builder(() -> false).serialize(Codec.BOOL).copyOnDeath().build());

    @Override
    public void register(IEventBus modBus) {
        ITEMS.register(modBus);
        ATTACHMENTS.register(modBus);
        TVCreativeTab.add(FARMERS_MANUAL);
        ModLoadingContext.get().getActiveContainer()
                .registerConfig(ModConfig.Type.COMMON, GuideConfig.SPEC, GuideConfig.FILE_NAME);
        NeoForge.EVENT_BUS.addListener(GuideEvents::onPlayerLoggedIn);
    }

    @Override
    public void registerLang(LangSink sink) {
        sink.add("item.terresvivantes.farmers_manual", "Farmer's Manual", "Manuel du fermier");
        sink.add("terresvivantes.guide.toc", "Contents", "Sommaire");
    }

    @Override
    public void registerItemModels(ItemModelProvider provider) {
        provider.basicItem(FARMERS_MANUAL.get());
    }

    @Override
    public void registerRecipes(RecipeOutput output, HolderLookup.Provider lookup) {
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, FARMERS_MANUAL.get())
                .requires(Items.BOOK)
                .requires(Items.WHEAT_SEEDS)
                .unlockedBy("has_book", InventoryChangeTrigger.TriggerInstance.hasItems(Items.BOOK))
                .save(output);
    }
}
