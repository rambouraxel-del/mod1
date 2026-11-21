package fr.terresvivantes.pests;

import fr.terresvivantes.TerresVivantes;
import fr.terresvivantes.core.TVModule;
import fr.terresvivantes.core.registry.TVCreativeTab;
import fr.terresvivantes.datagen.LangSink;
import fr.terresvivantes.health.HealthApi;
import fr.terresvivantes.pests.block.ScarecrowBlock;
import fr.terresvivantes.pests.block.ScarecrowBlockEntity;
import fr.terresvivantes.pests.client.PestsClient;
import fr.terresvivantes.pests.entity.Crow;
import java.util.function.BiConsumer;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Module pests (v0.6) : le corbeau, qui picore les cultures mûres de jour hors hiver, et l'épouvantail, qui éloigne
 * les corbeaux et les ravageurs dans son rayon. Règles pures : {@code pests.logic}.
 *
 * <p>Branchements : {@link HealthApi#registerPestSuppressor} (ravageurs). Les corbeaux se posent sur les cultures
 * via {@link fr.terresvivantes.pests.CropPecking}.
 */
public final class PestsModule implements TVModule {

    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(TerresVivantes.MODID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TerresVivantes.MODID);
    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, TerresVivantes.MODID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, TerresVivantes.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<Crow>> CROW = ENTITY_TYPES.register("crow",
            () -> EntityType.Builder.of(Crow::new, MobCategory.CREATURE)
                    .sized(0.4F, 0.5F)
                    .clientTrackingRange(8)
                    .build("crow"));

    public static final DeferredBlock<ScarecrowBlock> SCARECROW = BLOCKS.register("scarecrow",
            () -> new ScarecrowBlock(BlockBehaviour.Properties.of()
                    .strength(1.0F)
                    .sound(SoundType.WOOD)
                    .noOcclusion()));

    public static final DeferredItem<BlockItem> SCARECROW_ITEM = ITEMS.registerSimpleBlockItem("scarecrow", SCARECROW);

    public static final DeferredItem<Item> CROW_FEATHER = ITEMS.register("crow_feather",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<DeferredSpawnEggItem> CROW_SPAWN_EGG = ITEMS.register("crow_spawn_egg",
            () -> new DeferredSpawnEggItem(CROW, 0x2A2A2E, 0x8A7CA0, new Item.Properties()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ScarecrowBlockEntity>> SCARECROW_BE =
            BLOCK_ENTITY_TYPES.register("scarecrow",
                    () -> BlockEntityType.Builder.of(ScarecrowBlockEntity::new, SCARECROW.get()).build(null));

    @Override
    public void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        ENTITY_TYPES.register(modBus);
        BLOCK_ENTITY_TYPES.register(modBus);
        modBus.addListener(PestsModule::onEntityAttributes);
        modBus.addListener(PestsModule::onSpawnPlacements);

        TVCreativeTab.add(SCARECROW_ITEM);
        TVCreativeTab.add(CROW_FEATHER);
        TVCreativeTab.add(CROW_SPAWN_EGG);

        NeoForge.EVENT_BUS.addListener(PestsModule::onChunkLoad);
        NeoForge.EVENT_BUS.addListener(PestsModule::onServerStopped);

        HealthApi.registerPestSuppressor(PestsApi::pestMultiplier);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            PestsClient.register(modBus);
        }
    }

    private static void onEntityAttributes(EntityAttributeCreationEvent event) {
        event.put(CROW.get(), Crow.createAttributes().build());
    }

    private static void onSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(CROW.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Crow::checkSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.OR);
    }

    /** Retrouve les épouvantails d'un chunk chargé depuis le disque (leur registre n'est pas persisté). */
    private static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        for (BlockPos pos : event.getChunk().getBlockEntitiesPos()) {
            if (event.getChunk().getBlockEntity(pos) instanceof ScarecrowBlockEntity) {
                PestsApi.track(level, pos);
            }
        }
    }

    private static void onServerStopped(ServerStoppedEvent event) {
        PestsApi.clear();
    }

    @Override
    public void registerLang(LangSink sink) {
        sink.add("entity.terresvivantes.crow", "Crow", "Corbeau");
        sink.add("item.terresvivantes.crow_feather", "Crow Feather", "Plume de corbeau");
        sink.add("item.terresvivantes.crow_spawn_egg", "Crow Spawn Egg", "Œuf d'apparition de corbeau");
        sink.add("block.terresvivantes.scarecrow", "Scarecrow", "Épouvantail");
        sink.add("message.terresvivantes.scarecrow.fresh",
                "Scarecrow: effective radius %1$s blocks, placed %2$s days ago.",
                "Épouvantail : rayon de %1$s blocs, posé il y a %2$s jours.");
        sink.add("message.terresvivantes.scarecrow.stale",
                "Worn scarecrow: radius halved to %1$s blocks, placed %2$s days ago. Move it to restore its effect.",
                "Épouvantail usé : rayon réduit à %1$s blocs, posé il y a %2$s jours. Déplacez-le pour rétablir son effet.");
    }

    @Override
    public void registerItemModels(ItemModelProvider provider) {
        provider.basicItem(CROW_FEATHER.get());
        provider.withExistingParent("crow_spawn_egg", ResourceLocation.withDefaultNamespace("item/template_spawn_egg"));
        PestsDataGen.registerItemModel(provider);
    }

    @Override
    public void registerBlockStates(BlockStateProvider provider) {
        PestsDataGen.registerBlockStates(provider);
    }

    @Override
    public void registerRecipes(RecipeOutput output, HolderLookup.Provider lookup) {
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, SCARECROW_ITEM.get())
                .pattern(" P ")
                .pattern("WHW")
                .pattern("S S")
                .define('P', Items.CARVED_PUMPKIN)
                .define('W', ItemTags.WOOL)
                .define('H', Items.HAY_BLOCK)
                .define('S', Items.STICK)
                .unlockedBy("has_hay_block", InventoryChangeTrigger.TriggerInstance.hasItems(Items.HAY_BLOCK))
                .save(output, ResourceLocation.fromNamespaceAndPath(TerresVivantes.MODID, "scarecrow"));
    }

    @Override
    public void registerLootTables(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> output,
                                   HolderLookup.Provider lookup) {
        LootTable.Builder table = LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(LootItem.lootTableItem(SCARECROW_ITEM.get())));
        output.accept(SCARECROW.get().getLootTable(), table);
    }
}
