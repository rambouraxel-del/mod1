package fr.terresvivantes.husbandry;

import fr.terresvivantes.husbandry.logic.FeedType;
import fr.terresvivantes.husbandry.logic.TroughLogic;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Auge à fourrage : bloc demi-hauteur (16 x 8 x 16). Clic droit avec un aliment : remplissage. Clic droit main vide :
 * état. Le dessus change selon l'aliment ({@link #FILL}); le comparateur lit le stock.
 */
public final class FeedingTroughBlock extends Block implements EntityBlock {

    public static final EnumProperty<TroughFill> FILL = EnumProperty.create("fill", TroughFill.class);

    private static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 8.0, 16.0);

    public FeedingTroughBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FILL, TroughFill.EMPTY));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FILL);
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FeedingTroughBlockEntity(pos, state);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
                                           CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof FeedingTroughBlockEntity trough
                ? TroughLogic.comparatorSignal(trough.stock())
                : 0;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        FeedType feed = HusbandryModule.feedOf(stack);
        if (feed == null) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }
        if (!(level.getBlockEntity(pos) instanceof FeedingTroughBlockEntity trough)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        int accepted = trough.insert(feed, stack.getCount());
        if (accepted > 0) {
            if (!player.getAbilities().instabuild) {
                stack.shrink(accepted);
            }
            return ItemInteractionResult.SUCCESS;
        }
        boolean mixed = trough.type() != null && trough.type() != feed;
        player.displayClientMessage(Component.translatable(mixed
                ? "message.terresvivantes.trough.mixed"
                : "message.terresvivantes.trough.full"), true);
        return ItemInteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof FeedingTroughBlockEntity trough) {
            player.displayClientMessage(trough.describe(), true);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
}
