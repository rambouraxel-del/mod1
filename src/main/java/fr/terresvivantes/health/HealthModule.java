package fr.terresvivantes.health;

import fr.terresvivantes.TerresVivantes;
import fr.terresvivantes.core.TVModule;
import fr.terresvivantes.core.registry.TVCreativeTab;
import fr.terresvivantes.crop.CropGrowthHooks;
import fr.terresvivantes.datagen.LangSink;
import fr.terresvivantes.health.client.HealthClient;
import fr.terresvivantes.health.item.FungicideItem;
import fr.terresvivantes.health.item.SprayerItem;
import fr.terresvivantes.health.logic.HealthModel;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.crafting.DataComponentIngredient;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * Module Health (v0.2) : stress et flétrissement des cultures, maladies (oïdium, rouille), fongicide et pulvérisateur.
 */
public final class HealthModule implements TVModule {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(TerresVivantes.MODID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TerresVivantes.MODID);
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, TerresVivantes.MODID);
    private static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create(Registries.PARTICLE_TYPE, TerresVivantes.MODID);

    /** Culture flétrie (plante en croix, sans BlockItem). */
    public static final DeferredBlock<WitheredCropBlock> WITHERED_CROP = BLOCKS.register("withered_crop",
            () -> new WitheredCropBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.DEAD_BUSH)));

    public static final DeferredItem<FungicideItem> FUNGICIDE =
            ITEMS.register("fungicide", () -> new FungicideItem(new Item.Properties()));

    public static final DeferredItem<SprayerItem> SPRAYER =
            ITEMS.register("sprayer", () -> new SprayerItem(new Item.Properties().stacksTo(1)
                    .durability(HealthModel.SPRAYER_CHARGES)));

    /** Santé des cultures, par chunk. */
    public static final Supplier<AttachmentType<HealthChunkData>> CHUNK_DATA = ATTACHMENTS.register("crop_health",
            () -> AttachmentType.builder(() -> new HealthChunkData()).serialize(HealthChunkData.CODEC).build());

    /** Particules de spores, teintes par maladie (texture particle/spore). */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SPORE_MILDEW =
            PARTICLES.register("spore_mildew", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SPORE_RUST =
            PARTICLES.register("spore_rust", () -> new SimpleParticleType(false));

    @Override
    public void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        ATTACHMENTS.register(modBus);
        PARTICLES.register(modBus);
        TVCreativeTab.add(FUNGICIDE);
        TVCreativeTab.add(SPRAYER);
        NeoForge.EVENT_BUS.addListener(HealthModule::onBlockBreak);
        HealthGrowth.register();
        if (FMLEnvironment.dist == Dist.CLIENT) {
            HealthClient.register(modBus);
        }
    }

    /** Une culture cassée perd sa santé. */
    private static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel() instanceof Level level) {
            HealthApi.clear(level, event.getPos());
        }
    }

    @Override
    public void registerLang(LangSink sink) {
        sink.add("block.terresvivantes.withered_crop", "Withered Crop", "Culture flétrie");
        sink.add("item.terresvivantes.fungicide", "Fungicide", "Fongicide");
        sink.add("item.terresvivantes.sprayer", "Sprayer", "Pulvérisateur");

        sink.add("message.terresvivantes.health.stress", "Stress: %s / %s", "Stress : %s / %s");
        sink.add("message.terresvivantes.health.disease", "Disease: %s", "Maladie : %s");
        sink.add("message.terresvivantes.health.healthy", "Disease: none", "Maladie : aucune");
        sink.add("message.terresvivantes.health.disease.mildew", "powdery mildew", "oïdium");
        sink.add("message.terresvivantes.health.disease.rust", "rust", "rouille");
        sink.add("message.terresvivantes.health.disease.aphids", "aphids", "pucerons");
        sink.add("message.terresvivantes.health.immune", "Immunity: %s", "Immunité : %s");
        sink.add("message.terresvivantes.health.yes", "until end of season", "jusqu'à la fin de la saison");
        sink.add("message.terresvivantes.health.no", "no", "non");
        sink.add("message.terresvivantes.health.sprayer_empty",
                "Sprayer empty: hold fungicide in off-hand and right-click a crop to refill.",
                "Pulvérisateur vide : tenir du fongicide en main secondaire et cliquer sur une culture pour le recharger.");
        sink.add("message.terresvivantes.health.sprayer_refilled", "Sprayer refilled.", "Pulvérisateur rechargé.");
    }

    @Override
    public void registerItemModels(ItemModelProvider provider) {
        provider.basicItem(FUNGICIDE.get());
        provider.basicItem(SPRAYER.get());
    }

    @Override
    public void registerBlockStates(BlockStateProvider provider) {
        provider.simpleBlock(WITHERED_CROP.get(),
                provider.models().cross("withered_crop", provider.modLoc("block/withered_crop"))
                        .renderType("minecraft:cutout"));
    }

    @Override
    public void registerRecipes(RecipeOutput output, HolderLookup.Provider lookup) {
        Ingredient water = DataComponentIngredient.of(false,
                PotionContents.createItemStack(Items.POTION, Potions.WATER));
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, FUNGICIDE.get(), 2)
                .requires(Items.COPPER_INGOT)
                .requires(water)
                .requires(Items.BONE_MEAL)
                .unlockedBy("has_copper", InventoryChangeTrigger.TriggerInstance.hasItems(Items.COPPER_INGOT))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, SPRAYER.get())
                .pattern(" I ")
                .pattern("ICI")
                .pattern(" S ")
                .define('I', Items.IRON_INGOT)
                .define('C', Items.COPPER_INGOT)
                .define('S', Items.STICK)
                .unlockedBy("has_copper", InventoryChangeTrigger.TriggerInstance.hasItems(Items.COPPER_INGOT))
                .save(output);
    }

    /** Culture flétrie : un bâton, et un tiers du temps une graine de blé. */
    @Override
    public void registerLootTables(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> output,
                                   HolderLookup.Provider lookup) {
        LootTable.Builder table = LootTable.lootTable()
                .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1))
                        .add(LootItem.lootTableItem(Items.STICK)))
                .withPool(LootPool.lootPool()
                        .when(LootItemRandomChanceCondition.randomChance(0.25f))
                        .add(LootItem.lootTableItem(Items.WHEAT_SEEDS)));
        output.accept(WITHERED_CROP.get().getLootTable(), table);
    }
}
