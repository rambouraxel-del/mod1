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
import net.minecraft.world.level.block.state.properties.EnumProperty;

/** Cuve à fromage : lait et présure vers caillé, lait de chèvre vers fromage de chèvre. */
public final class CheeseVatBlock extends ProcessingBlock {

    public static final MapCodec<CheeseVatBlock> CODEC = BlockBehaviour.simpleCodec(CheeseVatBlock::new);

    /** Ce que montre la surface de la cuve (vide, lait, caillé). */
    public static final EnumProperty<VatContent> CONTENT = EnumProperty.create("content", VatContent.class);

    public CheeseVatBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(CONTENT, VatContent.EMPTY));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(CONTENT);
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CheeseVatBlockEntity(pos, state);
    }
}
