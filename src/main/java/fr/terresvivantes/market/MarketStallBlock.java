package fr.terresvivantes.market;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.SimpleMenuProvider;
import org.jetbrains.annotations.Nullable;

/** Étal (v0.7) : bloc orienté, contenu dans {@link MarketStallBlockEntity}, ouvert par clic droit à main vide. */
public final class MarketStallBlock extends BaseEntityBlock {

    public static final MapCodec<MarketStallBlock> CODEC = BlockBehaviour.simpleCodec(MarketStallBlock::new);

    public MarketStallBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(HorizontalDirectionalBlock.FACING);
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(HorizontalDirectionalBlock.FACING,
                context.getHorizontalDirection().getOpposite());
    }

    /** Modèle plein : le bloc a une entité mais pas de rendu spécial (garde-fou RenderShapeGameTests). */
    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MarketStallBlockEntity(pos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        return createTickerHelper(type, MarketModule.STALL_BE.get(),
                (tickLevel, pos, tickState, be) -> be.serverTick((ServerLevel) tickLevel));
    }

    /** Clic droit à main vide : ouvre l'étal (9 cases de vente, 1 caisse). */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof MarketStallBlockEntity stall) {
            MenuProvider provider = new SimpleMenuProvider(
                    (containerId, inventory, p) -> new MarketStallMenu(containerId, inventory, stall),
                    Component.translatable("container.terresvivantes.market_stall"));
            player.openMenu(provider);
        }
        return InteractionResult.SUCCESS;
    }

    /** Retrait : les produits et la caisse tombent au sol. */
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && level instanceof ServerLevel serverLevel
                && level.getBlockEntity(pos) instanceof MarketStallBlockEntity stall) {
            stall.dropContents(serverLevel, pos);
        }
        super.onRemove(state, level, pos, newState, moved);
    }
}
