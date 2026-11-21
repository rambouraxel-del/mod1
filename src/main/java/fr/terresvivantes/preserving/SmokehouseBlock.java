package fr.terresvivantes.preserving;

import com.mojang.serialization.MapCodec;
import fr.terresvivantes.processing.ProcessingBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/** Fumoir : une entrée et un conteneur combustible, consommé à chaque cycle. */
public final class SmokehouseBlock extends ProcessingBlock {

    public static final MapCodec<SmokehouseBlock> CODEC = BlockBehaviour.simpleCodec(SmokehouseBlock::new);

    public SmokehouseBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SmokehouseBlockEntity(pos, state);
    }
}
