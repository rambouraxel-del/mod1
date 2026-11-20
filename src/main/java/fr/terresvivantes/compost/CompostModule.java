package fr.terresvivantes.compost;

import fr.terresvivantes.TerresVivantes;
import fr.terresvivantes.compost.logic.CompostLogic;
import fr.terresvivantes.core.TVModule;
import fr.terresvivantes.core.registry.TVCreativeTab;
import fr.terresvivantes.crop.logic.CropDefinition;
import fr.terresvivantes.crop.logic.CropDefinitions;
import fr.terresvivantes.datagen.LangSink;
import fr.terresvivantes.soil.SoilApi;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.ExplosionCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.model.generators.BlockModelBuilder;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.BiConsumer;
import java.util.function.Function;

/** Module Compost : bac a compost, fertilisants, bone meal sur terre labouree, fumier des animaux. */
public final class CompostModule implements TVModule {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(TerresVivantes.MODID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TerresVivantes.MODID);

    /** Matieres organiques acceptees par le bac (produits, graines, feuilles, herbe, ...). */
    public static final TagKey<Item> COMPOSTABLE = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(TerresVivantes.MODID, "compostable"));

    public static final DeferredItem<FertilizerItem> COMPOST = ITEMS.register("compost",
            () -> new FertilizerItem(new Item.Properties(), CompostLogic.Fertilizer.COMPOST));
    public static final DeferredItem<FertilizerItem> MANURE = ITEMS.register("manure",
            () -> new FertilizerItem(new Item.Properties(), CompostLogic.Fertilizer.MANURE));
    public static final DeferredItem<FertilizerItem> WOOD_ASH = ITEMS.register("wood_ash",
            () -> new FertilizerItem(new Item.Properties(), CompostLogic.Fertilizer.WOOD_ASH));

    public static final DeferredBlock<CompostBinBlock> COMPOST_BIN = BLOCKS.register("compost_bin",
            () -> new CompostBinBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(0.6f)
                    .sound(SoundType.WOOD)
                    .noOcclusion()
                    .randomTicks()));
    public static final DeferredItem<BlockItem> COMPOST_BIN_ITEM =
            ITEMS.registerSimpleBlockItem("compost_bin", COMPOST_BIN);

    @Override
    public void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        TVCreativeTab.add(COMPOST);
        TVCreativeTab.add(MANURE);
        TVCreativeTab.add(WOOD_ASH);
        TVCreativeTab.add(COMPOST_BIN_ITEM);
        NeoForge.EVENT_BUS.addListener(CompostModule::onRightClickBlock);
        NeoForge.EVENT_BUS.addListener(CompostModule::onEntityTick);
    }

    /** Bone meal sur une terre labouree : +15 P. L'effet vanilla sur les plantes n'est pas modifie. */
    private static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        if (level.isClientSide || !event.getItemStack().is(Items.BONE_MEAL)) {
            return;
        }
        BlockPos pos = event.getPos();
        if (!level.getBlockState(pos).is(Blocks.FARMLAND)) {
            return;
        }
        Player player = event.getEntity();
        SoilApi.fertilize(level, pos, 0, CompostLogic.BONE_MEAL_PHOSPHORUS, 0, 0);
        FertilizerItem.feedback(level, pos);
        if (!player.getAbilities().instabuild) {
            event.getItemStack().shrink(1);
        }
    }

    /** Fumier : les vaches, cochons, moutons et chevaux en lachent periodiquement, cote serveur. */
    private static void onEntityTick(EntityTickEvent.Post event) {
        Entity entity = event.getEntity();
        Level level = entity.level();
        if (level.isClientSide || !isManureAnimal(entity)) {
            return;
        }
        if (!CompostLogic.isManureCheckTick(entity.tickCount, entity.getId())) {
            return;
        }
        if (CompostLogic.dropsManure(level.getRandom().nextDouble(), CompostConfig.manureChance())) {
            entity.spawnAtLocation(MANURE.get());
        }
    }

    private static boolean isManureAnimal(Entity entity) {
        return entity instanceof Cow || entity instanceof Pig || entity instanceof Sheep || entity instanceof Horse;
    }

    @Override
    public void registerLang(LangSink sink) {
        sink.add("block.terresvivantes.compost_bin", "Compost Bin", "Bac à compost");
        sink.add("item.terresvivantes.compost", "Compost", "Compost");
        sink.add("item.terresvivantes.manure", "Manure", "Fumier");
        sink.add("item.terresvivantes.wood_ash", "Wood Ash", "Cendre de bois");
    }

    @Override
    public void registerItemModels(ItemModelProvider provider) {
        provider.basicItem(COMPOST.get());
        provider.basicItem(MANURE.get());
        provider.basicItem(WOOD_ASH.get());
    }

    @Override
    public void registerBlockStates(BlockStateProvider provider) {
        ModelFile[] levels = new ModelFile[CompostLogic.MAX_LEVEL + 1];
        for (int level = 0; level <= CompostLogic.MAX_LEVEL; level++) {
            levels[level] = binModel(provider, "block/compost_bin_level_" + level, level, "contents");
        }
        ModelFile ready = binModel(provider, "block/compost_bin_ready", CompostLogic.MAX_LEVEL, "ready");

        provider.getVariantBuilder(COMPOST_BIN.get()).forAllStates(state -> {
            ModelFile model = state.getValue(CompostBinBlock.READY)
                    ? ready
                    : levels[state.getValue(CompostBinBlock.LEVEL)];
            int yRotation = switch (state.getValue(CompostBinBlock.FACING)) {
                case EAST -> 90;
                case SOUTH -> 180;
                case WEST -> 270;
                default -> 0;
            };
            return ConfiguredModel.builder().modelFile(model).rotationY(yRotation).build();
        });
        provider.simpleBlockItem(COMPOST_BIN.get(), levels[0]);
    }

    /**
     * Modele du bac : cadre en quatre parois (cotes, dessus, dessous) et un plateau de contenu
     * dont la hauteur suit le niveau. Le plateau utilise la texture "contents" ou "ready".
     */
    private static BlockModelBuilder binModel(BlockStateProvider provider, String path, int level, String contentKey) {
        float height = 2f + 12f * level / CompostLogic.MAX_LEVEL;
        BlockModelBuilder builder = provider.models().getBuilder(path)
                .texture("particle", "#side")
                .texture("side", provider.modLoc("block/compost_bin_side"))
                .texture("top", provider.modLoc("block/compost_bin_top"))
                .texture("bottom", provider.modLoc("block/compost_bin_bottom"))
                .texture("contents", provider.modLoc("block/compost_bin_contents"))
                .texture("ready", provider.modLoc("block/compost_bin_ready"));
        addWall(builder, 0, 0, 0, 16, 16, 2);
        addWall(builder, 0, 0, 14, 16, 16, 16);
        addWall(builder, 0, 0, 2, 2, 16, 14);
        addWall(builder, 14, 0, 2, 16, 16, 14);
        builder.element()
                .from(2, 0, 2)
                .to(14, height, 14)
                .face(Direction.UP).texture("#" + contentKey).end()
                .end();
        return builder;
    }

    private static void addWall(BlockModelBuilder builder, float x1, float y1, float z1,
                                float x2, float y2, float z2) {
        builder.element()
                .from(x1, y1, z1)
                .to(x2, y2, z2)
                .allFaces((direction, face) -> face.texture(switch (direction) {
                    case UP -> "#top";
                    case DOWN -> "#bottom";
                    default -> "#side";
                }))
                .end();
    }

    @Override
    public void registerRecipes(RecipeOutput output, HolderLookup.Provider lookup) {
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, COMPOST_BIN.get())
                .pattern("P P")
                .pattern("PSP")
                .pattern("P P")
                .define('P', ItemTags.PLANKS)
                .define('S', ItemTags.WOODEN_SLABS)
                .unlockedBy("has_planks", InventoryChangeTrigger.TriggerInstance.hasItems(Items.OAK_PLANKS))
                .save(output);
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, WOOD_ASH.get(), 2)
                .requires(Items.CHARCOAL)
                .unlockedBy("has_charcoal", InventoryChangeTrigger.TriggerInstance.hasItems(Items.CHARCOAL))
                .save(output);
    }

    @Override
    public void registerLootTables(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> output,
                                   HolderLookup.Provider lookup) {
        output.accept(ResourceKey.create(Registries.LOOT_TABLE,
                        ResourceLocation.fromNamespaceAndPath(TerresVivantes.MODID, "blocks/compost_bin")),
                LootTable.lootTable().withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0f))
                        .add(LootItem.lootTableItem(COMPOST_BIN.get()))
                        .when(ExplosionCondition.survivesExplosion())));
    }

    /**
     * Tag {@code terresvivantes:compostable} : feuilles, herbes, graines et légumes vanilla organiques,
     * plus les 12 produits et les 12 graines du module crop.
     */
    @Override
    public void registerItemTags(Function<TagKey<Item>, TagsProvider.TagAppender<Item>> tags,
                                 HolderLookup.Provider lookup) {
        TagsProvider.TagAppender<Item> compostable = tags.apply(COMPOSTABLE);
        compostable.addOptionalTag(ResourceLocation.withDefaultNamespace("leaves"))
                .add(vanilla("short_grass"), vanilla("tall_grass"), vanilla("fern"))
                .add(vanilla("wheat"), vanilla("wheat_seeds"), vanilla("beetroot"), vanilla("beetroot_seeds"))
                .add(vanilla("carrot"), vanilla("potato"), vanilla("pumpkin_seeds"), vanilla("melon_seeds"),
                        vanilla("sweet_berries"));
        for (CropDefinition def : CropDefinitions.all()) {
            compostable.add(ResourceKey.create(Registries.ITEM,
                    ResourceLocation.fromNamespaceAndPath(TerresVivantes.MODID, def.id())));
            compostable.add(ResourceKey.create(Registries.ITEM,
                    ResourceLocation.fromNamespaceAndPath(TerresVivantes.MODID, def.id() + "_seeds")));
        }
    }

    private static ResourceKey<Item> vanilla(String path) {
        return ResourceKey.create(Registries.ITEM, ResourceLocation.withDefaultNamespace(path));
    }

    @Override
    public void registerBlockTags(Function<TagKey<Block>, TagsProvider.TagAppender<Block>> tags,
                                  HolderLookup.Provider lookup) {
        tags.apply(BlockTags.MINEABLE_WITH_AXE).add(COMPOST_BIN.getKey());
    }
}
