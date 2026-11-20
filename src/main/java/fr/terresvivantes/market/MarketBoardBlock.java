package fr.terresvivantes.market;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Tableau d'annonces (v0.7) : bloc orienté (face avant = {@link #FACING}). Clic droit à main vide : ouvre la liste des
 * contrats; clic droit avec un contrat ou un produit demandé : livraison. Les contrats sont dans {@link ContractBoardData}.
 */
public final class MarketBoardBlock extends HorizontalDirectionalBlock {

    public static final MapCodec<MarketBoardBlock> CODEC = BlockBehaviour.simpleCodec(MarketBoardBlock::new);

    public MarketBoardBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    /** Clic droit avec un item : livraison si l'item est un contrat ou un produit demandé. */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (stack.is(MarketModule.CONTRACT.get())) {
            ContractBoard.deliverHeld(serverPlayer, stack);
        } else {
            ContractBoard.deliverMatching(serverPlayer, stack.getItem());
        }
        return ItemInteractionResult.SUCCESS;
    }

    /** Clic droit à main vide : ouvre le tableau (liste des contrats ouverts). */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            ContractBoard.openBoard(serverPlayer);
        }
        return InteractionResult.SUCCESS;
    }
}
