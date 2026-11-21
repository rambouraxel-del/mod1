package fr.terresvivantes.vineyard;

import fr.terresvivantes.climate.ClimateApi;
import fr.terresvivantes.season.SeasonApi;
import fr.terresvivantes.season.logic.Season;
import fr.terresvivantes.soil.SoilApi;
import fr.terresvivantes.vineyard.logic.VineyardLogic;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.List;

/**
 * Vigne pérenne sur treillis. Croît par randomTick (pas de BlockEntity), ne meurt jamais.
 * Stades 0 à 3 (propriété {@code stage}), cépage ({@code variety}), et {@code harvested} : vrai après une récolte,
 * remis à faux par la taille d'hiver. Casser une vigne mature rend une bouture et le treillis.
 */
public final class GrapevineBlock extends Block {

    public static final EnumProperty<GrapeVariety> VARIETY = EnumProperty.create("variety", GrapeVariety.class);
    public static final IntegerProperty STAGE = IntegerProperty.create("stage",
            VineyardLogic.PLANTED_STAGE, VineyardLogic.RIPE_STAGE);
    public static final BooleanProperty HARVESTED = BooleanProperty.create("harvested");

    public GrapevineBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(VARIETY, GrapeVariety.RED)
                .setValue(STAGE, VineyardLogic.PLANTED_STAGE)
                .setValue(HARVESTED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(VARIETY, STAGE, HARVESTED);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return TrellisBlock.POSTS;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return TrellisBlock.POSTS;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return TrellisBlock.isSoil(level.getBlockState(pos.below()));
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbourState,
                                     LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
        if (direction == Direction.DOWN && !canSurvive(state, level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighbourState, level, pos, neighbourPos);
    }

    /** Casser : une bouture si la vigne est mature (stade 2 ou plus), et toujours le treillis. */
    @Override
    protected List<net.minecraft.world.item.ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        List<ItemStack> drops = new ArrayList<>();
        if (VineyardLogic.yieldsCutting(state.getValue(STAGE))) {
            drops.add(new ItemStack(cuttingOf(state.getValue(VARIETY))));
        }
        drops.add(new ItemStack(VineyardModule.TRELLIS_ITEM.get()));
        return drops;
    }

    /** Clic droit à main vide sur une vigne au stade 3 : grappes de la variété, retour au stade 2. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (state.getValue(STAGE) != VineyardLogic.RIPE_STAGE || !player.getMainHandItem().isEmpty()) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        int count = VineyardLogic.harvestCount(level.getRandom().nextDouble());
        ItemStack grapes = new ItemStack(grapesOf(state.getValue(VARIETY)), count);
        if (!player.getInventory().add(grapes)) {
            player.drop(grapes, false);
        }
        level.setBlock(pos, state.setValue(STAGE, VineyardLogic.MATURE_STAGE).setValue(HARVESTED, true),
                Block.UPDATE_CLIENTS);
        return InteractionResult.SUCCESS;
    }

    /** Croissance par randomTick : saison x climat x sol, puis taille d'hiver. */
    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int stage = state.getValue(STAGE);
        boolean harvested = state.getValue(HARVESTED);
        Season season = SeasonApi.getSeason(level);
        if (season == Season.WINTER) {
            int pruned = VineyardLogic.winterStage(stage);
            if (pruned != stage || harvested) {
                level.setBlock(pos, state.setValue(STAGE, pruned).setValue(HARVESTED, false), Block.UPDATE_CLIENTS);
            }
            return;
        }
        if (!level.isAreaLoaded(pos, 1)) {
            return;
        }
        boolean farmland = level.getBlockState(pos.below()).is(Blocks.FARMLAND);
        float nutrients = farmland ? SoilApi.nutrientFactor(level, pos.below()) : 1.0f;
        float soil = VineyardLogic.soilFactor(farmland, nutrients);
        float factor = VineyardLogic.growthFactor(
                SeasonApi.seasonGrowthMultiplier(level, pos),
                VineyardLogic.climateFactor(ClimateApi.temperatureAt(level, pos)),
                soil);
        if (random.nextDouble() >= VineyardLogic.attemptChance(factor, VineyardConfig.stepDays())) {
            return;
        }
        int next = VineyardLogic.nextStage(stage, harvested, season,
                SeasonApi.getDayOfSeason(level), SeasonApi.getDaysPerSeason());
        if (next != stage) {
            level.setBlock(pos, state.setValue(STAGE, next), Block.UPDATE_CLIENTS);
        }
    }

    /** Bouture rendue par la casse ou plantée : une par cépage. */
    public static Item cuttingOf(GrapeVariety variety) {
        return variety == GrapeVariety.RED
                ? VineyardModule.RED_CUTTING.get()
                : VineyardModule.WHITE_CUTTING.get();
    }

    /** Raisins récoltés pour un cépage. */
    public static Item grapesOf(GrapeVariety variety) {
        return variety == GrapeVariety.RED
                ? VineyardModule.RED_GRAPES.get()
                : VineyardModule.WHITE_GRAPES.get();
    }
}
