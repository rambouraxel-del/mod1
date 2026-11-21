package fr.terresvivantes.economy;

import com.mojang.serialization.Codec;
import fr.terresvivantes.TerresVivantes;
import fr.terresvivantes.core.TVModule;
import fr.terresvivantes.core.registry.TVCreativeTab;
import fr.terresvivantes.datagen.LangSink;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.core.HolderLookup;

/**
 * Module economy (v0.7) : pièces de cuivre, d'argent et d'or, réputation par joueur (data attachment).
 * Prix et saturation : {@link EconomyApi}. Commandes {@code /tv price} et {@code /tv reputation} : {@link EconomyCommands}.
 */
public final class EconomyModule implements TVModule {

    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TerresVivantes.MODID);
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, TerresVivantes.MODID);

    public static final DeferredItem<Item> COPPER_COIN = ITEMS.registerSimpleItem("copper_coin");
    public static final DeferredItem<Item> SILVER_COIN = ITEMS.registerSimpleItem("silver_coin");
    public static final DeferredItem<Item> GOLD_COIN = ITEMS.registerSimpleItem("gold_coin");

    /** Réputation du joueur : entier persistant, copié à la mort. */
    public static final Supplier<AttachmentType<Integer>> REPUTATION = ATTACHMENTS.register("reputation",
            () -> AttachmentType.builder(() -> 0).serialize(Codec.INT).copyOnDeath().build());

    @Override
    public void register(IEventBus modBus) {
        ITEMS.register(modBus);
        ATTACHMENTS.register(modBus);
        TVCreativeTab.add(COPPER_COIN);
        TVCreativeTab.add(SILVER_COIN);
        TVCreativeTab.add(GOLD_COIN);
    }

    @Override
    public void registerLang(LangSink sink) {
        sink.add("item.terresvivantes.copper_coin", "Copper Coin", "Pièce de cuivre");
        sink.add("item.terresvivantes.silver_coin", "Silver Coin", "Pièce d'argent");
        sink.add("item.terresvivantes.gold_coin", "Gold Coin", "Pièce d'or");
        sink.add("terresvivantes.command.price.hand_empty", "Hold an item to see its price.",
                "Tenez un objet en main pour voir son prix.");
        sink.add("terresvivantes.command.price.not_sellable", "This item cannot be sold.",
                "Cet objet ne se vend pas.");
        sink.add("terresvivantes.command.price.result", "%1$s: %2$s copper per unit (market at %3$s %% of base)",
                "%1$s : %2$s cuivre par unité (marché à %3$s %% de la base)");
        sink.add("terresvivantes.command.reputation.info", "Reputation: %1$s (%2$s)",
                "Réputation : %1$s (%2$s)");
        sink.add("terresvivantes.command.player_only", "This command needs a player.",
                "Cette commande demande un joueur.");
        sink.add("terresvivantes.economy.tier.unknown", "Unknown", "Inconnu");
        sink.add("terresvivantes.economy.tier.regular", "Regular", "Habitué");
        sink.add("terresvivantes.economy.tier.supplier", "Supplier", "Fournisseur");
        sink.add("terresvivantes.economy.tier.reputed_producer", "Reputed producer", "Producteur réputé");
        sink.add("terresvivantes.economy.tier.master_farmer", "Master farmer", "Maître fermier");
    }

    @Override
    public void registerItemModels(ItemModelProvider provider) {
        provider.basicItem(COPPER_COIN.get());
        provider.basicItem(SILVER_COIN.get());
        provider.basicItem(GOLD_COIN.get());
    }

    /**
     * Conversions dans les deux sens, 9 petites pièces = 1 grosse : 1 argent = 9 cuivre, 1 or = 9 argent.
     * Le sens petite vers grosse prend 9 pièces, soit la grille entière (une recette de grille
     * consomme un item par emplacement, 9 au plus).
     */
    @Override
    public void registerRecipes(RecipeOutput output, HolderLookup.Provider lookup) {
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, COPPER_COIN.get(), 9)
                .requires(SILVER_COIN.get())
                .unlockedBy("has_silver_coin", InventoryChangeTrigger.TriggerInstance.hasItems(SILVER_COIN.get()))
                .save(output, ResourceLocation.fromNamespaceAndPath(TerresVivantes.MODID, "copper_from_silver"));
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, SILVER_COIN.get(), 9)
                .requires(GOLD_COIN.get())
                .unlockedBy("has_gold_coin", InventoryChangeTrigger.TriggerInstance.hasItems(GOLD_COIN.get()))
                .save(output, ResourceLocation.fromNamespaceAndPath(TerresVivantes.MODID, "silver_from_gold"));
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, SILVER_COIN.get(), 1)
                .requires(COPPER_COIN.get(), 9)
                .unlockedBy("has_copper_coin", InventoryChangeTrigger.TriggerInstance.hasItems(COPPER_COIN.get()))
                .save(output, ResourceLocation.fromNamespaceAndPath(TerresVivantes.MODID, "silver_from_copper"));
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, GOLD_COIN.get(), 1)
                .requires(SILVER_COIN.get(), 9)
                .unlockedBy("has_silver_coin", InventoryChangeTrigger.TriggerInstance.hasItems(SILVER_COIN.get()))
                .save(output, ResourceLocation.fromNamespaceAndPath(TerresVivantes.MODID, "gold_from_silver"));
    }
}
