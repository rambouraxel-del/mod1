package fr.terresvivantes.automation;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/** Planteuse : bloc orienté qui plante les graines de son inventaire sur les terres labourées devant elle. */
public final class PlanterBlock extends AutomationBlock {

    public static final MapCodec<PlanterBlock> CODEC = simpleCodec(PlanterBlock::new);

    public PlanterBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<PlanterBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PlanterBlockEntity(pos, state);
    }
}
