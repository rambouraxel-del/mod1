package fr.terresvivantes.guild;

import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.Codec;
import fr.terresvivantes.TerresVivantes;
import fr.terresvivantes.core.TVModule;
import fr.terresvivantes.core.registry.TVCreativeTab;
import fr.terresvivantes.datagen.LangSink;
import fr.terresvivantes.economy.EconomyModule;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.ExplosionCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.client.model.generators.BlockModelBuilder;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Module guild (v0.7) : bureau de la guilde (POI), profession de marchand de la guilde, échanges, réputation,
 * titres de spécialisation et sceau de la guilde. Logique : {@code guild.logic}; façade : {@link GuildApi}.
 */
public final class GuildModule implements TVModule {

    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(TerresVivantes.MODID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TerresVivantes.MODID);
    private static final DeferredRegister<PoiType> POI_TYPES =
            DeferredRegister.create(Registries.POINT_OF_INTEREST_TYPE, TerresVivantes.MODID);
    private static final DeferredRegister<VillagerProfession> PROFESSIONS =
            DeferredRegister.create(Registries.VILLAGER_PROFESSION, TerresVivantes.MODID);
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, TerresVivantes.MODID);

    public static final DeferredBlock<GuildDeskBlock> GUILD_DESK = BLOCKS.register("guild_desk",
            () -> new GuildDeskBlock(BlockBehaviour.Properties.of().strength(2.5f).sound(SoundType.WOOD)));
    public static final DeferredItem<BlockItem> GUILD_DESK_ITEM = ITEMS.registerSimpleBlockItem("guild_desk", GUILD_DESK);
    public static final DeferredItem<Item> GUILD_SEAL = ITEMS.registerSimpleItem("guild_seal");

    /** Clé du POI du bureau : l'identifiant de POI sert aussi de clé de profession. */
    public static final ResourceKey<PoiType> GUILD_DESK_POI_KEY =
            ResourceKey.create(Registries.POINT_OF_INTEREST_TYPE, texture("guild_desk"));
    public static final DeferredHolder<PoiType, PoiType> GUILD_DESK_POI = POI_TYPES.register("guild_desk",
            () -> new PoiType(ImmutableSet.copyOf(GUILD_DESK.get().getStateDefinition().getPossibleStates()), 1, 1));

    public static final DeferredHolder<VillagerProfession, VillagerProfession> GUILD_MERCHANT =
            PROFESSIONS.register("guild_merchant",
                    () -> new VillagerProfession("guild_merchant",
                            holder -> holder.is(GUILD_DESK_POI_KEY),
                            holder -> holder.is(GUILD_DESK_POI_KEY),
                            ImmutableSet.<Item>of(), ImmutableSet.<Block>of(),
                            SoundEvents.VILLAGER_WORK_CARTOGRAPHER));

    /** Ventes au marchand par filière (identifiant de filière vers unités), persistant, copié à la mort. */
    public static final Supplier<AttachmentType<Map<String, Integer>>> SPECIALIZATION = ATTACHMENTS.register(
            "specialization",
            () -> AttachmentType.<Map<String, Integer>>builder(() -> Map.of())
                    .serialize(Codec.unboundedMap(Codec.STRING, Codec.INT))
                    .copyOnDeath()
                    .build());

    @Override
    public void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        POI_TYPES.register(modBus);
        PROFESSIONS.register(modBus);
        ATTACHMENTS.register(modBus);
        TVCreativeTab.add(GUILD_DESK_ITEM);
        TVCreativeTab.add(GUILD_SEAL);
        NeoForge.EVENT_BUS.addListener(GuildEvents::onVillagerTrades);
        NeoForge.EVENT_BUS.addListener(GuildEvents::onEntityInteract);
        NeoForge.EVENT_BUS.addListener(GuildEvents::onTradeWithVillager);
        NeoForge.EVENT_BUS.addListener(GuildCommands::onRegisterCommands);
    }

    @Override
    public void registerLang(LangSink sink) {
        sink.add("block.terresvivantes.guild_desk", "Guild Desk", "Bureau de la guilde");
        sink.add("item.terresvivantes.guild_seal", "Guild Seal", "Sceau de la guilde");
        sink.add("entity.minecraft.villager.terresvivantes.guild_merchant", "Guild Merchant", "Marchand de la guilde");
        sink.add("terresvivantes.guild.title.gained", "Specialization title earned: %1$s",
                "Titre de spécialisation obtenu : %1$s");
        sink.add("terresvivantes.guild.filiere.maraicher", "Market gardener", "Maraîcher");
        sink.add("terresvivantes.guild.filiere.arboriculteur", "Orchardist", "Arboriculteur");
        sink.add("terresvivantes.guild.filiere.eleveur", "Breeder", "Éleveur");
        sink.add("terresvivantes.guild.filiere.fromager", "Cheesemaker", "Fromager");
        sink.add("terresvivantes.guild.filiere.apiculteur", "Beekeeper", "Apiculteur");
        sink.add("terresvivantes.guild.titles.progress", "%1$s: %2$s / %3$s units",
                "%1$s : %2$s / %3$s unités");
        sink.add("terresvivantes.guild.titles.obtained", "%1$s: title earned", "%1$s : titre obtenu");
        sink.add("terresvivantes.guild.command.player_only", "This command needs a player.",
                "Cette commande demande un joueur.");
    }

    @Override
    public void registerBlockStates(BlockStateProvider provider) {
        BlockModelBuilder model = provider.models().orientable("guild_desk",
                texture("block/guild_desk_side"), texture("block/guild_desk_front"), texture("block/guild_desk_top"));
        provider.horizontalBlock(GUILD_DESK.get(), model);
    }

    @Override
    public void registerItemModels(ItemModelProvider provider) {
        // Modèle d'item autonome (ne dépend pas du modèle de bloc créé par registerBlockStates).
        provider.withExistingParent("guild_desk", ResourceLocation.withDefaultNamespace("block/orientable"))
                .texture("side", texture("block/guild_desk_side"))
                .texture("front", texture("block/guild_desk_front"))
                .texture("top", texture("block/guild_desk_top"));
        provider.basicItem(GUILD_SEAL.get());
    }

    @Override
    public void registerRecipes(RecipeOutput output, HolderLookup.Provider lookup) {
        // Planches de chêne noir, livre et pièce d'or.
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, GUILD_DESK.get())
                .pattern("PPP")
                .pattern("PBP")
                .pattern("PGP")
                .define('P', Items.DARK_OAK_PLANKS)
                .define('B', Items.BOOK)
                .define('G', EconomyModule.GOLD_COIN.get())
                .unlockedBy("has_book", InventoryChangeTrigger.TriggerInstance.hasItems(Items.BOOK))
                .save(output);
    }

    @Override
    public void registerBlockTags(Function<TagKey<Block>, TagsProvider.TagAppender<Block>> tags,
                                  HolderLookup.Provider lookup) {
        tags.apply(BlockTags.MINEABLE_WITH_AXE).add(GUILD_DESK.getKey());
    }

    @Override
    public void registerLootTables(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> output,
                                   HolderLookup.Provider lookup) {
        output.accept(ResourceKey.create(Registries.LOOT_TABLE, texture("blocks/guild_desk")),
                LootTable.lootTable().withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0f))
                        .add(LootItem.lootTableItem(GUILD_DESK.get()))
                        .when(ExplosionCondition.survivesExplosion())));
    }

    private static ResourceLocation texture(String path) {
        return ResourceLocation.fromNamespaceAndPath(TerresVivantes.MODID, path);
    }
}
