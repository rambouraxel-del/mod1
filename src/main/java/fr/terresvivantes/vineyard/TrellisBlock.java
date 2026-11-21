package fr.terresvivantes.vineyard;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.tags.BlockTags;

/**
 * Treillis posable sur la terre, l'herbe ou une terre labourée. Non plein, sans occlusion. La collision suit les
 * deux montants (fine), pas le volume entier. Une vigne le remplace à la plantation d'une bouture.
 */
public final class TrellisBlock extends Block {

    /** Deux montants et deux traverses; la collision ne garde que les montants. */
    static final VoxelShape POSTS = Shapes.or(
            Block.box(0, 0, 7, 2, 16, 9),
            Block.box(14, 0, 7, 16, 16, 9));

    public TrellisBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    /** Terre, herbe ou terre labourée sous le treillis. */
    public static boolean isSoil(BlockState below) {
        return below.is(BlockTags.DIRT) || below.is(Blocks.GRASS_BLOCK) || below.is(Blocks.FARMLAND);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return POSTS;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return POSTS;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return isSoil(level.getBlockState(pos.below()));
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbourState,
                                     LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
        if (direction == Direction.DOWN && !canSurvive(state, level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighbourState, level, pos, neighbourPos);
    }
}
