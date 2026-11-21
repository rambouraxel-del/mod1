package fr.terresvivantes.kitchen;

import fr.terresvivantes.TerresVivantes;
import fr.terresvivantes.core.TVModule;
import fr.terresvivantes.core.quality.QualityModule;
import fr.terresvivantes.core.registry.TVCreativeTab;
import fr.terresvivantes.datagen.LangSink;
import fr.terresvivantes.kitchen.logic.KitchenLogic;
import fr.terresvivantes.processing.MachineType;
import fr.terresvivantes.processing.ProcessingApi;
import fr.terresvivantes.processing.ProcessingBlock;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.ExplosionCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.TagKey;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.client.model.generators.ModelBuilder;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Module kitchen (v0.5) : la marmite (machine à 4 entrées, un conteneur pour les bols, chauffée par une source de
 * chaleur placée dessous), la pâte, la pâte aux olives, et les 12 plats. Les recettes sont des JSON préfixés
 * {@code kitchen_} dans {@code data/terresvivantes/recipe/}.
 */
public final class KitchenModule implements TVModule {

    /** Machine « cooking_pot » : 4 entrées, un conteneur (bol) et une sortie. */
    public static final MachineType COOKING_POT_TYPE = ProcessingApi.registerMachine("cooking_pot", 4, true);

    /** Blocs qui chauffent la marmite posée dessus (tag {@code data/terresvivantes/tags/block/heat_sources.json}). */
    public static final TagKey<Block> HEAT_SOURCES =
            TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(TerresVivantes.MODID, "heat_sources"));

    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(TerresVivantes.MODID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TerresVivantes.MODID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, TerresVivantes.MODID);

    public static final DeferredBlock<CookingPotBlock> COOKING_POT = BLOCKS.register("cooking_pot",
            () -> new CookingPotBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(2.0f)
                    .sound(SoundType.METAL)
                    .noOcclusion()));
    public static final DeferredItem<BlockItem> COOKING_POT_ITEM = ITEMS.registerSimpleBlockItem("cooking_pot", COOKING_POT);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CookingPotBlockEntity>> COOKING_POT_BE =
            BLOCK_ENTITIES.register("cooking_pot", () -> BlockEntityType.Builder
                    .of(CookingPotBlockEntity::new, COOKING_POT.get())
                    .build(null));

    public static final DeferredItem<Item> DOUGH = ITEMS.registerSimpleItem("dough");
    /** Pâte aux olives crue : passe au four (recette de cuisson) pour donner le pain aux olives. */
    public static final DeferredItem<Item> OLIVE_DOUGH = ITEMS.registerSimpleItem("olive_dough");

    /** Les 12 plats : valeurs de nourriture, effet court éventuel, et si c'est une soupe en bol. */
    private static final List<Dish> DISH_DEFS = List.of(
            new Dish("pizza_slice", "Pizza Slice", "Part de pizza", 8, 0.8f, null, 0, false),
            new Dish("quiche", "Quiche", "Quiche", 8, 0.8f, null, 0, false),
            new Dish("cheese_omelette", "Cheese Omelette", "Omelette au fromage", 7, 0.8f, null, 0, false),
            new Dish("apple_tart", "Apple Tart", "Tarte aux pommes", 6, 0.6f, MobEffects.MOVEMENT_SPEED, 200, false),
            new Dish("cheese_sandwich", "Cheese Sandwich", "Sandwich au fromage", 7, 0.7f, null, 0, false),
            new Dish("olive_bread", "Olive Bread", "Pain aux olives", 6, 0.6f, null, 0, false),
            new Dish("duck_confit", "Duck Confit", "Confit de canard", 10, 0.9f,
                    MobEffects.DAMAGE_RESISTANCE, 200, false),
            new Dish("corn_tortilla", "Corn Tortilla", "Tortilla de maïs", 4, 0.5f, null, 0, false),
            new Dish("pancakes", "Pancakes", "Crêpes", 6, 0.6f, null, 0, false),
            new Dish("vegetable_gratin", "Vegetable Gratin", "Gratin de légumes", 9, 0.8f, null, 0, false),
            new Dish("french_onion_soup", "French Onion Soup", "Soupe à l'oignon gratinée", 8, 0.9f, null, 0, true),
            new Dish("butter_cookie", "Butter Cookie", "Sablé au beurre", 4, 0.3f, null, 0, false));

    private static final List<DeferredItem<Item>> DISHES = new ArrayList<>();

    static {
        for (Dish dish : DISH_DEFS) {
            DISHES.add(ITEMS.registerItem(dish.id(), props -> new Item(dish.properties(props))));
        }
    }

    @Override
    public void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        modBus.addListener(KitchenModule::registerCapabilities);
        NeoForge.EVENT_BUS.addListener(KitchenModule::onDishFinished);

        TVCreativeTab.add(COOKING_POT_ITEM);
        TVCreativeTab.add(DOUGH);
        TVCreativeTab.add(OLIVE_DOUGH);
        for (DeferredItem<Item> dish : DISHES) {
            TVCreativeTab.add(dish);
        }
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        ProcessingApi.registerItemHandler(event, COOKING_POT_BE.get());
    }

    /**
     * Un plat de qualité 3 donne plus de saturation (bonus de {@link KitchenLogic#QUALITY_SATURATION_BONUS}). Ajouté à
     * la fin de la consommation, côté serveur, sans toucher au FoodProperties des items.
     */
    private static void onDishFinished(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide) {
            return;
        }
        ItemStack stack = event.getItem();
        if (DISHES.stream().noneMatch(dish -> stack.is(dish.get()))) {
            return;
        }
        FoodProperties food = stack.get(DataComponents.FOOD);
        if (food == null) {
            return;
        }
        float bonus = KitchenLogic.saturationBonus(food.saturation(), QualityModule.qualityOf(stack));
        if (bonus > 0f) {
            FoodData data = player.getFoodData();
            data.setSaturation(Math.min(data.getSaturationLevel() + bonus, data.getFoodLevel()));
        }
    }

    @Override
    public void registerLang(LangSink sink) {
        sink.add("block.terresvivantes.cooking_pot", "Cooking Pot", "Marmite");
        sink.add("item.terresvivantes.dough", "Dough", "Pâte");
        sink.add("item.terresvivantes.olive_dough", "Olive Dough", "Pâte aux olives");
        for (Dish dish : DISH_DEFS) {
            sink.add("item.terresvivantes." + dish.id(), dish.nameEn(), dish.nameFr());
        }
    }

    @Override
    public void registerItemModels(ItemModelProvider provider) {
        potModel(provider.getBuilder("item/cooking_pot"), "block/cooking_pot_top");
        provider.basicItem(DOUGH.get());
        // Pas de texture propre à la pâte aux olives : elle réutilise celle de la pâte, en attendant un sprite dédié.
        provider.withExistingParent("olive_dough", ResourceLocation.withDefaultNamespace("item/generated"))
                .texture("layer0", texture("item/dough"));
        for (DeferredItem<Item> dish : DISHES) {
            provider.basicItem(dish.get());
        }
    }

    @Override
    public void registerBlockStates(BlockStateProvider provider) {
        ModelFile idle = potModel(provider.models().getBuilder("cooking_pot"), "block/cooking_pot_top");
        ModelFile active = potModel(provider.models().getBuilder("cooking_pot_active"), "block/cooking_pot_top_stew");
        provider.getVariantBuilder(COOKING_POT.get()).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(state.getValue(ProcessingBlock.ACTIVE) ? active : idle)
                .rotationY(rotationY(state.getValue(ProcessingBlock.FACING)))
                .build());
    }

    /**
     * Modèle de la marmite : un volume de 12 x 10 x 12 pixels centré (le chaudron réduit), la face haute porte
     * l'intérieur, en stew quand la marmite cuit.
     */
    private static <T extends ModelBuilder<T>> T potModel(T model, String topTexture) {
        return model
                .texture("particle", texture("block/cooking_pot_side"))
                .texture("side", texture("block/cooking_pot_side"))
                .texture("top", texture(topTexture))
                .element()
                .from(2, 0, 2)
                .to(14, 10, 14)
                .face(Direction.NORTH).texture("#side").end()
                .face(Direction.SOUTH).texture("#side").end()
                .face(Direction.EAST).texture("#side").end()
                .face(Direction.WEST).texture("#side").end()
                .face(Direction.UP).texture("#top").end()
                .face(Direction.DOWN).texture("#side").end()
                .end();
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
    public void registerLootTables(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> output,
                                   HolderLookup.Provider lookup) {
        output.accept(ResourceKey.create(Registries.LOOT_TABLE,
                        ResourceLocation.fromNamespaceAndPath(TerresVivantes.MODID, "blocks/cooking_pot")),
                LootTable.lootTable().withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0f))
                        .add(LootItem.lootTableItem(COOKING_POT.get()))
                        .when(ExplosionCondition.survivesExplosion())));
    }

    /** Définition d'un plat : nourriture, effet court facultatif, soupe en bol (le bol revient à la consommation). */
    private record Dish(String id, String nameEn, String nameFr, int nutrition, float saturation,
                        @Nullable Holder<MobEffect> effect, int effectTicks, boolean soup) {

        Item.Properties properties(Item.Properties base) {
            FoodProperties.Builder food = new FoodProperties.Builder()
                    .nutrition(nutrition)
                    .saturationModifier(saturation);
            if (effect != null) {
                food.effect(new MobEffectInstance(effect, effectTicks, 0), 1.0f);
            }
            Item.Properties props = base.food(food.build());
            if (soup) {
                props = props.stacksTo(1).craftRemainder(Items.BOWL);
            }
            return props;
        }
    }
}
