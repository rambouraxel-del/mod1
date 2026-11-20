package fr.terresvivantes.guild;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.core.Direction;

/**
 * Bureau de la guilde : bloc orienté. Le bloc est le point d'embauche (POI {@code terresvivantes:guild_desk}) du
 * profession {@code guild_merchant}.
 */
public final class GuildDeskBlock extends HorizontalDirectionalBlock {

    public static final MapCodec<GuildDeskBlock> CODEC = BlockBehaviour.simpleCodec(GuildDeskBlock::new);

    public GuildDeskBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }
}
