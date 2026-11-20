package fr.terresvivantes.vet;

import fr.terresvivantes.TerresVivantes;
import fr.terresvivantes.core.TVModule;
import fr.terresvivantes.core.registry.TVCreativeTab;
import fr.terresvivantes.datagen.LangSink;
import fr.terresvivantes.livestock.LivestockApi;
import fr.terresvivantes.vet.block.SaltLickBlock;
import fr.terresvivantes.vet.item.VeterinaryKitItem;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.ExplosionCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.crafting.DataComponentIngredient;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.BiConsumer;

/**
 * Module vet (v0.4) : santé animale. Maladies et parasites évalués dans le cycle de besoins de livestock
 * (écouteur {@link VetNeeds}), contagion, ralentissement, mort à santé 0. Trousse vétérinaire et bloc de sel.
 *
 * <p>Façade : {@link VetApi}. Règles pures : {@code vet.logic}.
 */
public final class VetModule implements TVModule {

    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(TerresVivantes.MODID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TerresVivantes.MODID);

    /** Trousse vétérinaire : 16 utilisations, soigne un animal malade (clic droit). */
    public static final DeferredItem<VeterinaryKitItem> VETERINARY_KIT = ITEMS.register("veterinary_kit",
            () -> new VeterinaryKitItem(new Item.Properties().durability(16)));

    /** Bloc de sel : préventif près des animaux, s'use puis disparaît. */
    public static final DeferredBlock<SaltLickBlock> SALT_LICK = BLOCKS.register("salt_lick",
            () -> new SaltLickBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.TERRACOTTA_WHITE)
                    .strength(1.0f)
                    .sound(SoundType.STONE)
                    .randomTicks()));
    public static final DeferredItem<net.minecraft.world.item.BlockItem> SALT_LICK_ITEM =
            ITEMS.registerSimpleBlockItem("salt_lick", SALT_LICK);

    @Override
    public void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);

        TVCreativeTab.add(VETERINARY_KIT);
        TVCreativeTab.add(SALT_LICK_ITEM);

        LivestockApi.registerNeedsListener(VetNeeds::onNeedsTick);
    }

    @Override
    public void registerLang(LangSink sink) {
        sink.add("item.terresvivantes.veterinary_kit", "Veterinary Kit", "Trousse vétérinaire");
        sink.add("block.terresvivantes.salt_lick", "Salt Lick", "Bloc de sel");
    }

    @Override
    public void registerItemModels(ItemModelProvider provider) {
        provider.basicItem(VETERINARY_KIT.get());
    }

    @Override
    public void registerBlockStates(BlockStateProvider provider) {
        ModelFile model = provider.models().cubeBottomTop("salt_lick",
                provider.modLoc("block/salt_lick"), provider.modLoc("block/salt_lick"),
                provider.modLoc("block/salt_lick_top"));
        provider.simpleBlock(SALT_LICK.get(), model);
        provider.simpleBlockItem(SALT_LICK.get(), model);
    }

    @Override
    public void registerRecipes(RecipeOutput output, HolderLookup.Provider lookup) {
        // La camomille est une fleur de la flore (v0.3); repli sur la marguerite vanille si son identifiant manque.
        Item chamomile = BuiltInRegistries.ITEM
                .getOptional(ResourceLocation.fromNamespaceAndPath(TerresVivantes.MODID, "chamomile"))
                .orElse(Items.OXEYE_DAISY);
        Ingredient water = DataComponentIngredient.of(false,
                PotionContents.createItemStack(Items.POTION, Potions.WATER));
        ShapelessRecipeBuilder.shapeless(RecipeCategory.TOOLS, VETERINARY_KIT.get())
                .requires(Items.WHITE_WOOL)
                .requires(Items.IRON_INGOT)
                .requires(water)
                .requires(chamomile)
                .unlockedBy("has_iron_ingot", InventoryChangeTrigger.TriggerInstance.hasItems(Items.IRON_INGOT))
                .save(output);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, SALT_LICK.get())
                .requires(Items.BONE_MEAL, 4)
                .requires(Items.STONE)
                .unlockedBy("has_bone_meal", InventoryChangeTrigger.TriggerInstance.hasItems(Items.BONE_MEAL))
                .save(output);
    }

    @Override
    public void registerLootTables(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> output,
                                   HolderLookup.Provider lookup) {
        output.accept(SALT_LICK.get().getLootTable(), LootTable.lootTable().withPool(LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1.0f))
                .add(LootItem.lootTableItem(SALT_LICK.get()))
                .when(ExplosionCondition.survivesExplosion())));
    }
}
