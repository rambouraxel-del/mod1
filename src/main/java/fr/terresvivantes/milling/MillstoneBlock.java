package fr.terresvivantes.milling;

import com.mojang.serialization.MapCodec;
import fr.terresvivantes.processing.ProcessingBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/** Meule : bloc de la machine de référence de la transformation (1 entrée, pas de conteneur). */
public final class MillstoneBlock extends ProcessingBlock {

    public static final MapCodec<MillstoneBlock> CODEC = BlockBehaviour.simpleCodec(MillstoneBlock::new);

    public MillstoneBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MillstoneBlockEntity(pos, state);
    }
}
