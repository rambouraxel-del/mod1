package fr.terresvivantes.insects;

import fr.terresvivantes.TerresVivantes;
import fr.terresvivantes.core.TVModule;
import fr.terresvivantes.core.registry.TVCreativeTab;
import fr.terresvivantes.crop.CropGrowthHooks;
import fr.terresvivantes.datagen.LangSink;
import fr.terresvivantes.health.HealthApi;
import fr.terresvivantes.insects.client.InsectsClient;
import fr.terresvivantes.insects.entity.Butterfly;
import fr.terresvivantes.insects.entity.Ladybug;
import fr.terresvivantes.insects.item.ButterflyNetItem;
import fr.terresvivantes.insects.item.InsectJarItem;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.common.NeoForge;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.resources.ResourceLocation;

/**
 * Module insects (v0.3) : papillons, coccinelles, pucerons, lucioles, filet et bocaux.
 *
 * <p>Les pucerons passent par {@link HealthApi} (infestation, suppresseurs) et {@link CropGrowthHooks}; la
 * pollinisation et la biodiversité reçoivent les papillons et les coccinelles via {@link InsectSources}. Les lucioles
 * sont purement visuelles, côté client.
 */
public final class InsectsModule implements TVModule {

    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TerresVivantes.MODID);
    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, TerresVivantes.MODID);
    private static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create(Registries.PARTICLE_TYPE, TerresVivantes.MODID);
    private static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, TerresVivantes.MODID);

    /** Variante d'un insecte capturé (0 à 3 pour les papillons), conservée dans le bocal. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> INSECT_VARIANT =
            COMPONENTS.registerComponentType("insect_variant", builder -> builder
                    .persistent(ExtraCodecs.NON_NEGATIVE_INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT));

    public static final DeferredHolder<EntityType<?>, EntityType<Butterfly>> BUTTERFLY = ENTITY_TYPES.register(
            "butterfly", () -> EntityType.Builder.of(Butterfly::new, MobCategory.AMBIENT)
                    .sized(0.4f, 0.3f)
                    .clientTrackingRange(8)
                    .build("butterfly"));

    public static final DeferredHolder<EntityType<?>, EntityType<Ladybug>> LADYBUG = ENTITY_TYPES.register(
            "ladybug", () -> EntityType.Builder.of(Ladybug::new, MobCategory.CREATURE)
                    .sized(0.25f, 0.2f)
                    .clientTrackingRange(8)
                    .build("ladybug"));

    public static final DeferredItem<ButterflyNetItem> BUTTERFLY_NET = ITEMS.register("butterfly_net",
            () -> new ButterflyNetItem(new Item.Properties().stacksTo(1).durability(32)));

    public static final DeferredItem<InsectJarItem> BUTTERFLY_JAR = ITEMS.register("butterfly_jar",
            () -> new InsectJarItem(BUTTERFLY, new Item.Properties().stacksTo(1)));

    public static final DeferredItem<InsectJarItem> LADYBUG_JAR = ITEMS.register("ladybug_jar",
            () -> new InsectJarItem(LADYBUG, new Item.Properties().stacksTo(1)));

    public static final DeferredItem<DeferredSpawnEggItem> BUTTERFLY_SPAWN_EGG = ITEMS.register("butterfly_spawn_egg",
            () -> new DeferredSpawnEggItem(BUTTERFLY, 0xE8A23C, 0x3B2A16, new Item.Properties()));

    public static final DeferredItem<DeferredSpawnEggItem> LADYBUG_SPAWN_EGG = ITEMS.register("ladybug_spawn_egg",
            () -> new DeferredSpawnEggItem(LADYBUG, 0xC8201C, 0x111111, new Item.Properties()));

    /** Puceron : particule de la culture infestée (texture particle/aphid). */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> APHID =
            PARTICLES.register("aphid", () -> new SimpleParticleType(false));

    /** Luciole : particule lumineuse émise côté client (texture particle/firefly). */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> FIREFLY =
            PARTICLES.register("firefly", () -> new SimpleParticleType(false));

    @Override
    public void register(IEventBus modBus) {
        ITEMS.register(modBus);
        ENTITY_TYPES.register(modBus);
        PARTICLES.register(modBus);
        COMPONENTS.register(modBus);
        modBus.addListener(InsectsModule::onEntityAttributes);
        modBus.addListener(InsectsModule::onSpawnPlacements);

        TVCreativeTab.add(BUTTERFLY_NET);
        TVCreativeTab.add(BUTTERFLY_JAR);
        TVCreativeTab.add(LADYBUG_JAR);
        TVCreativeTab.add(BUTTERFLY_SPAWN_EGG);
        TVCreativeTab.add(LADYBUG_SPAWN_EGG);

        InsectSources.register();
        CropGrowthHooks.registerListener(AphidGrowth::onGrowthAttempt);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            InsectsClient.register(modBus);
        }
    }

    private static void onEntityAttributes(EntityAttributeCreationEvent event) {
        event.put(BUTTERFLY.get(), Butterfly.createAttributes().build());
        event.put(LADYBUG.get(), Ladybug.createAttributes().build());
    }

    private static void onSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(BUTTERFLY.get(), SpawnPlacementTypes.NO_RESTRICTIONS,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Butterfly::checkSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.OR);
        event.register(LADYBUG.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Ladybug::checkSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.OR);
    }

    @Override
    public void registerLang(LangSink sink) {
        sink.add("entity.terresvivantes.butterfly", "Butterfly", "Papillon");
        sink.add("entity.terresvivantes.ladybug", "Ladybug", "Coccinelle");
        sink.add("item.terresvivantes.butterfly_net", "Butterfly Net", "Filet à papillons");
        sink.add("item.terresvivantes.butterfly_jar", "Butterfly Jar", "Bocal à papillon");
        sink.add("item.terresvivantes.ladybug_jar", "Ladybug Jar", "Bocal à coccinelle");
        sink.add("item.terresvivantes.butterfly_spawn_egg", "Butterfly Spawn Egg", "Œuf d'apparition de papillon");
        sink.add("item.terresvivantes.ladybug_spawn_egg", "Ladybug Spawn Egg", "Œuf d'apparition de coccinelle");
        sink.add("message.terresvivantes.insects.variant.monarch", "Monarch", "Monarque");
        sink.add("message.terresvivantes.insects.variant.cabbage_white", "Cabbage white", "Piéride du chou");
        sink.add("message.terresvivantes.insects.variant.blue_morpho", "Blue morpho", "Morpho bleu");
        sink.add("message.terresvivantes.insects.variant.brimstone", "Brimstone", "Citron");
        sink.add("message.terresvivantes.insects.no_bottle",
                "A glass bottle is needed to capture the insect.",
                "Il faut une bouteille en verre pour capturer l'insecte.");
    }

    @Override
    public void registerItemModels(ItemModelProvider provider) {
        provider.basicItem(BUTTERFLY_NET.get());
        provider.basicItem(BUTTERFLY_JAR.get());
        provider.basicItem(LADYBUG_JAR.get());
        provider.withExistingParent("butterfly_spawn_egg", ResourceLocation.withDefaultNamespace("item/template_spawn_egg"));
        provider.withExistingParent("ladybug_spawn_egg", ResourceLocation.withDefaultNamespace("item/template_spawn_egg"));
    }

    @Override
    public void registerRecipes(RecipeOutput output, HolderLookup.Provider lookup) {
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, BUTTERFLY_NET.get())
                .pattern("  T")
                .pattern(" TS")
                .pattern("S  ")
                .define('T', Items.STRING)
                .define('S', Items.STICK)
                .unlockedBy("has_string", net.minecraft.advancements.critereon.InventoryChangeTrigger.TriggerInstance
                        .hasItems(Items.STRING))
                .save(output);
    }
}
