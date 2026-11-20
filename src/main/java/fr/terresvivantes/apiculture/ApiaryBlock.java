package fr.terresvivantes.apiculture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Apiaire : bloc orienté, colonie et quatre cadres. Clic droit main vide : état; shift-clic droit : récolte des
 * cadres de miel; clic droit avec un cadre vide : insertion. HONEY vrai quand un cadre de miel est présent.
 */
public final class ApiaryBlock extends Block implements EntityBlock {

    public static final BooleanProperty HONEY = BooleanProperty.create("honey");

    public ApiaryBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH)
                .setValue(HONEY, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HorizontalDirectionalBlock.FACING, HONEY);
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(HorizontalDirectionalBlock.FACING,
                context.getHorizontalDirection().getOpposite());
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ApiaryBlockEntity(pos, state);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer,
                            ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level instanceof ServerLevel server && level.getBlockEntity(pos) instanceof ApiaryBlockEntity apiary) {
            apiary.onPlaced(server, pos);
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof ApiaryBlockEntity apiary) {
            apiary.dropContents(level, pos);
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    @Override
    @Nullable
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        if (level.isClientSide || type != ApicultureModule.APIARY_BE.get()) {
            return null;
        }
        return (BlockEntityTicker<T>) (BlockEntityTicker<ApiaryBlockEntity>) (lvl, pos, st, be) ->
                be.serverTick((ServerLevel) lvl, pos, st);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(ApicultureModule.EMPTY_FRAME.get())) {
            return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof ApiaryBlockEntity apiary && apiary.insertEmptyFrame()) {
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            return ItemInteractionResult.SUCCESS;
        }
        player.displayClientMessage(Component.translatable("message.terresvivantes.apiary.full"), true);
        return ItemInteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(level.getBlockEntity(pos) instanceof ApiaryBlockEntity apiary)) {
            return InteractionResult.PASS;
        }
        if (player.isShiftKeyDown()) {
            int harvested = apiary.harvest((ServerLevel) level, player);
            player.displayClientMessage(harvested > 0
                    ? Component.translatable("message.terresvivantes.apiary.harvested", harvested)
                    : Component.translatable("message.terresvivantes.apiary.no_frames"), true);
        } else {
            player.displayClientMessage(apiary.describe(), true);
        }
        return InteractionResult.SUCCESS;
    }

    /** Client : quelques grains de pollen qui tombent devant la colonie quand elle est peuplée. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!(level.getBlockEntity(pos) instanceof ApiaryBlockEntity apiary) || !apiary.isActive()) {
            return;
        }
        if (random.nextInt(4) != 0) {
            return;
        }
        Direction front = state.getValue(HorizontalDirectionalBlock.FACING);
        double x = pos.getX() + 0.5 + front.getStepX() * 0.55;
        double y = pos.getY() + 0.75;
        double z = pos.getZ() + 0.5 + front.getStepZ() * 0.55;
        level.addParticle(ParticleTypes.FALLING_NECTAR, x, y, z, 0.0, 0.0, 0.0);
    }
}
