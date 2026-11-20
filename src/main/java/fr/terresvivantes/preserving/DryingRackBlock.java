package fr.terresvivantes.preserving;

import com.mojang.serialization.MapCodec;
import fr.terresvivantes.processing.ProcessingBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Séchoir : panneau fin (16 x 16 x 4) posé contre un mur. La face avant (FACING) est tournée vers le joueur; le dos
 * est collé au mur, opposé à FACING.
 */
public final class DryingRackBlock extends ProcessingBlock {

    public static final MapCodec<DryingRackBlock> CODEC = BlockBehaviour.simpleCodec(DryingRackBlock::new);

    private static final VoxelShape SHAPE_NORTH = Block.box(0, 0, 12, 16, 16, 16);
    private static final VoxelShape SHAPE_SOUTH = Block.box(0, 0, 0, 16, 16, 4);
    private static final VoxelShape SHAPE_EAST = Block.box(0, 0, 0, 4, 16, 16);
    private static final VoxelShape SHAPE_WEST = Block.box(12, 0, 0, 16, 16, 16);

    public DryingRackBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DryingRackBlockEntity(pos, state);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeFor(state.getValue(FACING));
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
                                           CollisionContext context) {
        return shapeFor(state.getValue(FACING));
    }

    private static VoxelShape shapeFor(Direction facing) {
        return switch (facing) {
            case SOUTH -> SHAPE_SOUTH;
            case EAST -> SHAPE_EAST;
            case WEST -> SHAPE_WEST;
            default -> SHAPE_NORTH;
        };
    }
}
