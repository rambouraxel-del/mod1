package fr.terresvivantes.pests.block;

import com.mojang.serialization.MapCodec;
import fr.terresvivantes.pests.PestsApi;
import fr.terresvivantes.pests.PestsConfig;
import fr.terresvivantes.pests.logic.ScarecrowLogic;
import fr.terresvivantes.season.SeasonApi;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.jetbrains.annotations.Nullable;

/**
 * Épouvantail : bloc de deux de haut ({@link DoubleBlockHalf}), orienté ({@link #FACING}). La partie basse porte le
 * BlockEntity et le suivi du registre ; la partie haute se pose avec elle et disparaît avec elle.
 */
public final class ScarecrowBlock extends Block implements EntityBlock {

    public static final MapCodec<ScarecrowBlock> CODEC = simpleCodec(ScarecrowBlock::new);
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    private static final VoxelShape SHAPE = Block.box(3.0, 0.0, 3.0, 13.0, 16.0, 13.0);

    public ScarecrowBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(HALF, DoubleBlockHalf.LOWER)
                .setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HALF, FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        if (pos.getY() >= context.getLevel().getMaxBuildHeight() - 1) {
            return null;
        }
        if (!context.getLevel().getBlockState(pos.above()).canBeReplaced(context)) {
            return null;
        }
        return this.defaultBlockState()
                .setValue(HALF, DoubleBlockHalf.LOWER)
                .setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    /** Pose la partie haute au-dessus de la partie basse, puis enregistre le jour de pose. */
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        BlockState placed = level.getBlockState(pos);
        if (placed.getValue(HALF) != DoubleBlockHalf.LOWER) {
            return;
        }
        level.setBlock(pos.above(), placed.setValue(HALF, DoubleBlockHalf.UPPER), Block.UPDATE_ALL);
        if (level instanceof ServerLevel server && level.getBlockEntity(pos) instanceof ScarecrowBlockEntity be) {
            be.setPlacedDay(SeasonApi.dayIndex(server));
        }
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (state.getValue(HALF) == DoubleBlockHalf.LOWER) {
            PestsApi.track(level, pos);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && state.getValue(HALF) == DoubleBlockHalf.LOWER) {
            PestsApi.untrack(level, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    /** Une moitié sans son partenaire disparaît : casser une moitié casse l'autre sans la rendre en double. */
    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                     LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        DoubleBlockHalf half = state.getValue(HALF);
        boolean vertical = direction.getAxis() == Direction.Axis.Y;
        if (vertical && (half == DoubleBlockHalf.LOWER) == (direction == Direction.UP)) {
            boolean partner = neighborState.is(this) && neighborState.getValue(HALF) != half;
            if (!partner) {
                return Blocks.AIR.defaultBlockState();
            }
        }
        return canSurvive(state, level, pos) ? state : Blocks.AIR.defaultBlockState();
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (state.getValue(HALF) == DoubleBlockHalf.UPPER) {
            BlockState below = level.getBlockState(pos.below());
            return below.is(this) && below.getValue(HALF) == DoubleBlockHalf.LOWER;
        }
        BlockPos ground = pos.below();
        return level.getBlockState(ground).isFaceSturdy(level, ground, Direction.UP);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    /** Seule la partie basse porte un BlockEntity. */
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER ? new ScarecrowBlockEntity(pos, state) : null;
    }

    /** Clic droit : affiche le rayon effectif et l'ancienneté dans la barre d'action. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (level.isClientSide || !(level instanceof ServerLevel server)) {
            return InteractionResult.SUCCESS;
        }
        BlockPos lower = state.getValue(HALF) == DoubleBlockHalf.LOWER ? pos : pos.below();
        if (server.getBlockEntity(lower) instanceof ScarecrowBlockEntity be) {
            long today = SeasonApi.dayIndex(server);
            long placed = be.ensurePlacedDay(today);
            boolean stale = ScarecrowLogic.isStale(placed, today);
            int radius = ScarecrowLogic.effectiveRadius(PestsConfig.scarecrowRadius(), stale);
            String key = stale ? "message.terresvivantes.scarecrow.stale" : "message.terresvivantes.scarecrow.fresh";
            player.displayClientMessage(Component.translatable(key, radius, ScarecrowLogic.daysSince(placed, today)), true);
        }
        return InteractionResult.SUCCESS;
    }
}
