package fr.terresvivantes.market;

import fr.terresvivantes.TerresVivantes;
import fr.terresvivantes.core.TVModule;
import fr.terresvivantes.core.registry.TVCreativeTab;
import fr.terresvivantes.datagen.LangSink;
import fr.terresvivantes.economy.EconomyModule;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.ExplosionCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;

import java.util.function.BiConsumer;

/**
 * Module market (v0.7) : tableau d'annonces (contrats de livraison partagés par dimension), étal (vente automatique
 * à des villageois) et grand livre (prix du marché). Les prix passent par {@code economy.EconomyApi}.
 */
public final class MarketModule implements TVModule {

    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(TerresVivantes.MODID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TerresVivantes.MODID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, TerresVivantes.MODID);
    private static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, TerresVivantes.MODID);
    private static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, TerresVivantes.MODID);

    /** Contrat accepté : données portées par l'item (identifiant, produit, quantité, qualité, échéance, gain). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ContractData>> CONTRACT_DATA =
            COMPONENTS.registerComponentType("contract_data", builder -> builder
                    .persistent(ContractData.CODEC)
                    .networkSynchronized(ContractData.STREAM_CODEC));

    public static final DeferredBlock<MarketBoardBlock> MARKET_BOARD = BLOCKS.register("market_board",
            () -> new MarketBoardBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(2.0f)
                    .sound(SoundType.WOOD)));
    public static final DeferredItem<BlockItem> MARKET_BOARD_ITEM =
            ITEMS.registerSimpleBlockItem("market_board", MARKET_BOARD);

    public static final DeferredBlock<MarketStallBlock> MARKET_STALL = BLOCKS.register("market_stall",
            () -> new MarketStallBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(2.0f)
                    .sound(SoundType.WOOD)));
    public static final DeferredItem<BlockItem> MARKET_STALL_ITEM =
            ITEMS.registerSimpleBlockItem("market_stall", MARKET_STALL);

    public static final DeferredItem<ContractItem> CONTRACT = ITEMS.register("contract",
            () -> new ContractItem(new Item.Properties()));
    public static final DeferredItem<LedgerItem> MARKET_LEDGER = ITEMS.register("market_ledger",
            () -> new LedgerItem(new Item.Properties()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MarketStallBlockEntity>> STALL_BE =
            BLOCK_ENTITIES.register("market_stall", () -> BlockEntityType.Builder
                    .of(MarketStallBlockEntity::new, MARKET_STALL.get())
                    .build(null));

    public static final DeferredHolder<MenuType<?>, MenuType<MarketStallMenu>> STALL_MENU =
            MENUS.register("market_stall", () -> new MenuType<>(MarketStallMenu::fromNetwork,
                    FeatureFlags.DEFAULT_FLAGS));

    @Override
    public void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        MENUS.register(modBus);
        COMPONENTS.register(modBus);
        MarketNetwork.register(modBus);

        NeoForge.EVENT_BUS.addListener(MarketModule::onLevelTick);
        NeoForge.EVENT_BUS.addListener(MarketModule::onPlayerLogin);

        TVCreativeTab.add(MARKET_BOARD_ITEM);
        TVCreativeTab.add(MARKET_STALL_ITEM);
        TVCreativeTab.add(MARKET_LEDGER);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            fr.terresvivantes.market.client.MarketClient.register(modBus);
        }
    }

    /** Renouvellement du tableau à l'aube, dans chaque dimension serveur. */
    private static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level) {
            ContractBoard.onLevelTick(level);
        }
    }

    /** Pénalités d'expiration survenues pendant que le joueur était hors ligne. */
    private static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ContractBoard.onPlayerLogin(player);
        }
    }

    @Override
    public void registerLang(LangSink sink) {
        sink.add("block.terresvivantes.market_board", "Market Board", "Tableau d'annonces");
        sink.add("block.terresvivantes.market_stall", "Market Stall", "Étal");
        sink.add("item.terresvivantes.contract", "Contract", "Contrat");
        sink.add("item.terresvivantes.market_ledger", "Market Ledger", "Grand livre du marché");
        sink.add("container.terresvivantes.market_stall", "Market Stall", "Étal");
        sink.add("screen.terresvivantes.market_board.title", "Market board", "Tableau d'annonces");
        sink.add("screen.terresvivantes.market_board.empty", "No open contract.", "Aucun contrat ouvert.");
        sink.add("screen.terresvivantes.market_board.accept", "Accept", "Accepter");
        sink.add("screen.terresvivantes.market_board.line", "%1$s x %2$s, quality %3$s, due day %4$s",
                "%1$s x %2$s, qualité %3$s, à livrer avant le jour %4$s");
        sink.add("screen.terresvivantes.market_board.reward", "Reward: %1$s copper, reputation +%2$s",
                "Récompense : %1$s cuivre, réputation +%2$s");
        sink.add("tooltip.terresvivantes.contract.deliver", "Deliver %1$s x %2$s",
                "Livrer %1$s x %2$s");
        sink.add("tooltip.terresvivantes.contract.quality", "Minimum quality: %1$s",
                "Qualité minimale : %1$s");
        sink.add("tooltip.terresvivantes.contract.deadline", "Due before day %1$s",
                "À livrer avant le jour %1$s");
        sink.add("tooltip.terresvivantes.contract.reward", "Reward: %1$s copper",
                "Récompense : %1$s cuivre");
        sink.add("tooltip.terresvivantes.contract.expired", "Expired",
                "Expiré");
        sink.add("message.terresvivantes.market.accepted", "Contract accepted.",
                "Contrat accepté.");
        sink.add("message.terresvivantes.market.unavailable", "This contract is no longer available.",
                "Ce contrat n'est plus disponible.");
        sink.add("message.terresvivantes.market.not_yours", "This contract belongs to another player.",
                "Ce contrat appartient à un autre joueur.");
        sink.add("message.terresvivantes.market.no_match", "No accepted contract needs this item.",
                "Aucun contrat accepté ne demande cet objet.");
        sink.add("message.terresvivantes.market.delivered", "Delivered %1$s, %2$s left to deliver.",
                "Livré : %1$s, reste %2$s à livrer.");
        sink.add("message.terresvivantes.market.completed", "Contract complete: %1$s copper, reputation +%2$s.",
                "Contrat terminé : %1$s cuivre, réputation +%2$s.");
        sink.add("message.terresvivantes.market.expired", "A contract expired: reputation -%1$s.",
                "Un contrat a expiré : réputation -%1$s.");
        sink.add("message.terresvivantes.market.ledger_empty", "No sellable product in your inventory.",
                "Aucun produit vendable dans l'inventaire.");
        sink.add("message.terresvivantes.market.ledger_title", "Market prices (10 most expensive):",
                "Prix du marché (les 10 plus chers) :");
        sink.add("message.terresvivantes.market.ledger_line", "%1$s: %2$s copper per unit, market at %3$s %%",
                "%1$s : %2$s cuivre l'unité, marché à %3$s %%");
    }

    @Override
    public void registerItemModels(ItemModelProvider provider) {
        // Modèles d'item autonomes (même géométrie que les blocs), sans dépendance à l'ordre des providers.
        provider.withExistingParent("market_board", ResourceLocation.withDefaultNamespace("block/cube"))
                .texture("down", texture("block/market_board_top"))
                .texture("up", texture("block/market_board_top"))
                .texture("north", texture("block/market_board_front"))
                .texture("south", texture("block/market_board_side"))
                .texture("east", texture("block/market_board_side"))
                .texture("west", texture("block/market_board_side"))
                .texture("particle", texture("block/market_board_side"));
        provider.withExistingParent("market_stall", ResourceLocation.withDefaultNamespace("block/cube"))
                .texture("down", texture("block/market_stall_bottom"))
                .texture("up", texture("block/market_stall_top"))
                .texture("north", texture("block/market_stall_front"))
                .texture("south", texture("block/market_stall_side"))
                .texture("east", texture("block/market_stall_side"))
                .texture("west", texture("block/market_stall_side"))
                .texture("particle", texture("block/market_stall_side"));
        provider.basicItem(CONTRACT.get());
        provider.basicItem(MARKET_LEDGER.get());
    }

    @Override
    public void registerBlockStates(BlockStateProvider provider) {
        ModelFile board = provider.models().cube("market_board",
                texture("block/market_board_top"), texture("block/market_board_top"),
                texture("block/market_board_front"), texture("block/market_board_side"),
                texture("block/market_board_side"), texture("block/market_board_side"));
        provider.getVariantBuilder(MARKET_BOARD.get()).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(board)
                .rotationY(rotationY(state.getValue(HorizontalDirectionalBlock.FACING)))
                .build());

        ModelFile stall = provider.models().cube("market_stall",
                texture("block/market_stall_bottom"), texture("block/market_stall_top"),
                texture("block/market_stall_front"), texture("block/market_stall_side"),
                texture("block/market_stall_side"), texture("block/market_stall_side"));
        provider.getVariantBuilder(MARKET_STALL.get()).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(stall)
                .rotationY(rotationY(state.getValue(HorizontalDirectionalBlock.FACING)))
                .build());
    }

    private static ResourceLocation texture(String path) {
        return ResourceLocation.fromNamespaceAndPath(TerresVivantes.MODID, path);
    }

    private static int rotationY(Direction facing) {
        return switch (facing) {
            case EAST -> 90;
            case SOUTH -> 180;
            case WEST -> 270;
            default -> 0;
        };
    }

    @Override
    public void registerRecipes(RecipeOutput output, HolderLookup.Provider lookup) {
        // Tableau : 3 papiers, 2 bâtons, 3 planches.
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, MARKET_BOARD_ITEM.get())
                .pattern("AAA")
                .pattern("S S")
                .pattern("PPP")
                .define('A', Items.PAPER)
                .define('S', Items.STICK)
                .define('P', ItemTags.PLANKS)
                .unlockedBy("has_paper", InventoryChangeTrigger.TriggerInstance.hasItems(Items.PAPER))
                .save(output);
        // Étal : laine rouge et blanche, clôtures et planches.
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, MARKET_STALL_ITEM.get())
                .pattern("RWR")
                .pattern("FPF")
                .pattern("PPP")
                .define('R', Items.RED_WOOL)
                .define('W', Items.WHITE_WOOL)
                .define('F', ItemTags.FENCES)
                .define('P', ItemTags.PLANKS)
                .unlockedBy("has_wool", InventoryChangeTrigger.TriggerInstance.hasItems(Items.WHITE_WOOL))
                .save(output);
        // Grand livre : un livre et une pièce de cuivre.
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, MARKET_LEDGER.get())
                .requires(Items.BOOK)
                .requires(EconomyModule.COPPER_COIN.get())
                .unlockedBy("has_book", InventoryChangeTrigger.TriggerInstance.hasItems(Items.BOOK))
                .save(output);
    }

    @Override
    public void registerLootTables(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> output,
                                   HolderLookup.Provider lookup) {
        // Les étals drop leur contenu via onRemove : le loot ne donne que le bloc, sans doublon.
        output.accept(lootKey("market_board"), selfDrop(MARKET_BOARD.get()));
        output.accept(lootKey("market_stall"), selfDrop(MARKET_STALL.get()));
    }

    private static LootTable.Builder selfDrop(Block block) {
        return LootTable.lootTable().withPool(LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1.0f))
                .add(LootItem.lootTableItem(block))
                .when(ExplosionCondition.survivesExplosion()));
    }

    private static ResourceKey<LootTable> lootKey(String block) {
        return ResourceKey.create(Registries.LOOT_TABLE,
                ResourceLocation.fromNamespaceAndPath(TerresVivantes.MODID, "blocks/" + block));
    }
}
