package fr.terresvivantes.preserving;

import com.mojang.serialization.MapCodec;
import fr.terresvivantes.processing.ProcessingBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/** Tonneau de fermentation : 2 entrées et un conteneur (bouteille en verre). */
public final class FermentationBarrelBlock extends ProcessingBlock {

    public static final MapCodec<FermentationBarrelBlock> CODEC = BlockBehaviour.simpleCodec(FermentationBarrelBlock::new);

    public FermentationBarrelBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FermentationBarrelBlockEntity(pos, state);
    }
}
