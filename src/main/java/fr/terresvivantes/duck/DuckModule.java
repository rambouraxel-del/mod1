package fr.terresvivantes.duck;

import fr.terresvivantes.TerresVivantes;
import fr.terresvivantes.core.TVModule;
import fr.terresvivantes.core.registry.TVCreativeTab;
import fr.terresvivantes.datagen.LangSink;
import fr.terresvivantes.duck.client.DuckClient;
import fr.terresvivantes.duck.entity.Duck;
import fr.terresvivantes.duck.entity.DuckEggEntity;
import fr.terresvivantes.duck.item.DuckEggItem;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Module duck (v0.4) : le canard (entité d'élevage qui nage et flotte), son œuf lancé, sa viande crue et cuite, son
 * œuf d'apparition, et les productions liées au génome : laine supplémentaire à la tonte, qualité du lait en
 * étoiles (composant {@code terresvivantes:milk_quality}, utilisé en v0.5).
 *
 * <p>Façade de productivité : {@link fr.terresvivantes.livestock.LivestockApi}. Règles pures : {@code duck.logic}.
 */
public final class DuckModule implements TVModule {

    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TerresVivantes.MODID);
    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, TerresVivantes.MODID);
    private static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, TerresVivantes.MODID);

    /** Viande crue : peut donner une faim légère (30 %), comme le poulet cru. */
    private static final FoodProperties RAW_FOOD = new FoodProperties.Builder()
            .nutrition(2)
            .saturationModifier(0.3F)
            .effect(() -> new MobEffectInstance(MobEffects.HUNGER, 600, 0), 0.3F)
            .build();

    private static final FoodProperties COOKED_FOOD = new FoodProperties.Builder()
            .nutrition(6)
            .saturationModifier(0.6F)
            .build();

    public static final DeferredHolder<EntityType<?>, EntityType<Duck>> DUCK = ENTITY_TYPES.register("duck",
            () -> EntityType.Builder.of(Duck::new, MobCategory.CREATURE)
                    .sized(0.5F, 0.7F)
                    .clientTrackingRange(10)
                    .build("duck"));

    /** Œuf de canard lancé en vol (entité de projectile). */
    public static final DeferredHolder<EntityType<?>, EntityType<DuckEggEntity>> THROWN_DUCK_EGG = ENTITY_TYPES.register(
            "thrown_duck_egg", () -> EntityType.Builder.<DuckEggEntity>of(DuckEggEntity::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(4)
                    .updateInterval(10)
                    .build("thrown_duck_egg"));

    public static final DeferredItem<DuckEggItem> DUCK_EGG = ITEMS.register("duck_egg",
            () -> new DuckEggItem(new Item.Properties().stacksTo(16)));

    public static final DeferredItem<Item> RAW_DUCK = ITEMS.register("raw_duck",
            () -> new Item(new Item.Properties().food(RAW_FOOD)));

    public static final DeferredItem<Item> COOKED_DUCK = ITEMS.register("cooked_duck",
            () -> new Item(new Item.Properties().food(COOKED_FOOD)));

    public static final DeferredItem<DeferredSpawnEggItem> DUCK_SPAWN_EGG = ITEMS.register("duck_spawn_egg",
            () -> new DeferredSpawnEggItem(DUCK, 0x3E7D3B, 0x7A5230, new Item.Properties()));

    /** Qualité du lait (1 à 3 étoiles), posée sur le seau de lait à la traite. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> MILK_QUALITY =
            COMPONENTS.registerComponentType("milk_quality", builder -> builder
                    .persistent(ExtraCodecs.intRange(1, 3))
                    .networkSynchronized(ByteBufCodecs.VAR_INT));

    @Override
    public void register(IEventBus modBus) {
        ITEMS.register(modBus);
        ENTITY_TYPES.register(modBus);
        COMPONENTS.register(modBus);
        modBus.addListener(DuckModule::onEntityAttributes);
        modBus.addListener(DuckModule::onSpawnPlacements);

        TVCreativeTab.add(DUCK_EGG);
        TVCreativeTab.add(RAW_DUCK);
        TVCreativeTab.add(COOKED_DUCK);
        TVCreativeTab.add(DUCK_SPAWN_EGG);

        DuckEvents.register(NeoForge.EVENT_BUS);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            DuckClient.register(modBus);
        }
    }

    private static void onEntityAttributes(EntityAttributeCreationEvent event) {
        event.put(DUCK.get(), Duck.createAttributes().build());
    }

    private static void onSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(DUCK.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Duck::checkSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.OR);
    }

    @Override
    public void registerLang(LangSink sink) {
        sink.add("entity.terresvivantes.duck", "Duck", "Canard");
        sink.add("entity.terresvivantes.thrown_duck_egg", "Thrown Duck Egg", "Œuf de canard lancé");
        sink.add("item.terresvivantes.duck_egg", "Duck Egg", "Œuf de canard");
        sink.add("item.terresvivantes.raw_duck", "Raw Duck", "Canard cru");
        sink.add("item.terresvivantes.cooked_duck", "Cooked Duck", "Canard cuit");
        sink.add("item.terresvivantes.duck_spawn_egg", "Duck Spawn Egg", "Œuf d'apparition de canard");
        sink.add("tooltip.terresvivantes.milk_quality", "Quality: %1$s", "Qualité: %1$s");
    }

    @Override
    public void registerItemModels(ItemModelProvider provider) {
        provider.basicItem(DUCK_EGG.get());
        provider.basicItem(RAW_DUCK.get());
        provider.basicItem(COOKED_DUCK.get());
        provider.withExistingParent("duck_spawn_egg", ResourceLocation.withDefaultNamespace("item/template_spawn_egg"));
    }

    @Override
    public void registerRecipes(RecipeOutput output, HolderLookup.Provider lookup) {
        Ingredient raw = Ingredient.of(RAW_DUCK.get());
        var unlock = InventoryChangeTrigger.TriggerInstance.hasItems(RAW_DUCK.get());
        SimpleCookingRecipeBuilder.smelting(raw, RecipeCategory.FOOD, COOKED_DUCK.get(), 0.35F, 200)
                .unlockedBy("has_raw_duck", unlock)
                .save(output, ResourceLocation.fromNamespaceAndPath(TerresVivantes.MODID, "cooked_duck_from_smelting"));
        SimpleCookingRecipeBuilder.smoking(raw, RecipeCategory.FOOD, COOKED_DUCK.get(), 0.35F, 100)
                .unlockedBy("has_raw_duck", unlock)
                .save(output, ResourceLocation.fromNamespaceAndPath(TerresVivantes.MODID, "cooked_duck_from_smoking"));
        SimpleCookingRecipeBuilder.campfireCooking(raw, RecipeCategory.FOOD, COOKED_DUCK.get(), 0.35F, 600)
                .unlockedBy("has_raw_duck", unlock)
                .save(output, ResourceLocation.fromNamespaceAndPath(TerresVivantes.MODID, "cooked_duck_from_campfire"));
    }
}
