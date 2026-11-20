package fr.terresvivantes.health;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Culture flétrie : plante en croix posée sur terre labourée. Remplace une culture qui a trop souffert
 * (stress ou maladie). Ne pousse pas. Drops : un bâton et parfois des graines de blé.
 */
public final class WitheredCropBlock extends BushBlock {

    public static final MapCodec<WitheredCropBlock> CODEC = simpleCodec(WitheredCropBlock::new);

    private static final VoxelShape SHAPE = Shapes.box(0.125, 0.0, 0.125, 0.875, 0.8125, 0.875);

    public WitheredCropBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BushBlock> codec() {
        return CODEC;
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(Blocks.FARMLAND);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}
