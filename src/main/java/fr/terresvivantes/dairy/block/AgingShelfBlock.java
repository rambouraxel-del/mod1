package fr.terresvivantes.dairy.block;

import com.mojang.serialization.MapCodec;
import fr.terresvivantes.processing.ProcessingBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/** Étagère d'affinage : caillé vers fromage jeune, puis fromage jeune vers fromage affiné. */
public final class AgingShelfBlock extends ProcessingBlock {

    public static final MapCodec<AgingShelfBlock> CODEC = BlockBehaviour.simpleCodec(AgingShelfBlock::new);

    /** Vrai quand l'étagère porte un fromage (visuel « front_cheese »). */
    public static final BooleanProperty HAS_CHEESE = BooleanProperty.create("has_cheese");

    public AgingShelfBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(HAS_CHEESE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(HAS_CHEESE);
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AgingShelfBlockEntity(pos, state);
    }
}
