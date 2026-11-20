package fr.terresvivantes.automation;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
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
 * Base des machines automatiques : bloc orienté (la face avant est {@link #FACING}), propriété {@link #ACTIVE} pour le
 * visuel, ticker serveur qui délègue à la BlockEntity, clic droit qui ouvre un coffre 9 cases, contenu éjecté à la casse.
 */
public abstract class AutomationBlock extends HorizontalDirectionalBlock implements EntityBlock {

    /** Vrai pendant un cycle où la machine a travaillé (pour le modèle « actif »). */
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    protected AutomationBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(ACTIVE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ACTIVE);
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        return (lvl, pos, st, blockEntity) -> {
            if (blockEntity instanceof AutomationBlockEntity machine && lvl instanceof ServerLevel server) {
                machine.serverTick(server);
            }
        };
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof AutomationBlockEntity machine) {
            player.openMenu(new SimpleMenuProvider(
                    (id, inventory, p) -> new ChestMenu(MenuType.GENERIC_9x1, id, inventory, machine.container(), 1),
                    getName()));
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && level instanceof ServerLevel
                && level.getBlockEntity(pos) instanceof AutomationBlockEntity machine) {
            Containers.dropContents(level, pos, machine.container());
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    /** Codec du bloc, à déclarer par chaque machine concrète. */
    @Override
    protected abstract MapCodec<? extends HorizontalDirectionalBlock> codec();
}
