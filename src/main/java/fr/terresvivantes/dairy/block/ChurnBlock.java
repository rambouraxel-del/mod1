package fr.terresvivantes.dairy.block;

import com.mojang.serialization.MapCodec;
import fr.terresvivantes.processing.ProcessingBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/** Baratte : lait vers crème, crème vers beurre. */
public final class ChurnBlock extends ProcessingBlock {

    public static final MapCodec<ChurnBlock> CODEC = BlockBehaviour.simpleCodec(ChurnBlock::new);

    public ChurnBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ChurnBlockEntity(pos, state);
    }
}
